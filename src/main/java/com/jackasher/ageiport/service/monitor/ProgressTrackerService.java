package com.jackasher.ageiport.service.monitor;

import com.jackasher.ageiport.constant.PostProcessingTaskStatus;
import com.jackasher.ageiport.model.dto.FullProgress;
import com.jackasher.ageiport.model.dto.ProgressSummary;
import com.jackasher.ageiport.model.dto.SubTaskProgressDetail;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ProgressTrackerService {

    private static final String REDIS_KEY_PREFIX = "ageiport:attachment_progress:";
    private static final String SUMMARY_FIELD = "summary";
    private static final String SUBTASK_FIELD_PREFIX = "subtask:";
    private static final long PROGRESS_TTL_HOURS = 24;

    // 仍然使用 <String, Object> 因为 Hash 中确实有两种类型的对象
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 【新增】初始化宏观进度，明确设置总子任务数。
     * 这个方法应该在所有子任务被创建和分发之前调用一次。
     *
     * @param mainTaskId 主任务ID
     * @param totalSubTasks 附件处理流程总共包含的子任务（批次）数量
     */
    public void initializeSummary(String mainTaskId, int totalSubTasks) {
        String redisKey = REDIS_KEY_PREFIX + mainTaskId;

        ProgressSummary summary = new ProgressSummary();
        summary.setTotalSubTasks(totalSubTasks);
        summary.setStatus(PostProcessingTaskStatus.PENDING.name()); // 初始状态为等待中

        putSummary(redisKey, summary);
        redisTemplate.expire(redisKey, PROGRESS_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 初始化一个子任务（批次）的进度条目。
     */
    public void initializeSubTask(String mainTaskId, String subTaskId, int subTaskNo, int totalItemsInBatch) {
        String redisKey = REDIS_KEY_PREFIX + mainTaskId;

        SubTaskProgressDetail detail = new SubTaskProgressDetail();
        detail.setSubTaskNo(subTaskNo);
        detail.setSubTaskId(subTaskId);
        detail.setStartTime(System.currentTimeMillis());
        detail.setMainTaskId(mainTaskId);

        putSubTaskDetail(redisKey, subTaskId, detail);

        // 获取或创建宏观统计对象并更新状态
        getSummary(redisKey).ifPresent(summary -> {
            // 当第一个子任务初始化时，将宏观状态从 PENDING 变为 PROCESSING
            if (PostProcessingTaskStatus.PENDING.name().equals(summary.getStatus())) {
                summary.setStatus(PostProcessingTaskStatus.PROCESSING.name());
                putSummary(redisKey, summary);
            }
        });

        redisTemplate.expire(redisKey, PROGRESS_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 当一个批次开始处理时，更新该批次的总附件数。
     * 这个方法应该在消费者接收到消息后，开始处理前调用。
     */
    public void updateTotalItemsForSubTask(String mainTaskId, String subTaskId, int totalItemsInBatch) {
        String redisKey = REDIS_KEY_PREFIX + mainTaskId;

        // 更新子任务详情
        getSubTaskDetail(redisKey, subTaskId).ifPresent(detail -> {
            detail.setTotalItems(totalItemsInBatch);
            putSubTaskDetail(redisKey, subTaskId, detail);
        });
    }

    /**
     * 更新指定批次的进度，并同步更新宏观进度。
     */
    public void updateSubTaskProgress(String mainTaskId, String subTaskId, int successIncrement, int failureIncrement) {
        String redisKey = REDIS_KEY_PREFIX + mainTaskId;

        // 更新子任务微观进度
        getSubTaskDetail(redisKey, subTaskId).ifPresent(detail -> {
            detail.setProcessedItems(detail.getProcessedItems() + successIncrement);
            detail.setFailedItems(detail.getFailedItems() + failureIncrement);
            detail.setStatus(PostProcessingTaskStatus.PROCESSING.name());
            putSubTaskDetail(redisKey, subTaskId, detail);
        });

        redisTemplate.expire(redisKey, PROGRESS_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * 标记一个批次为完成或失败状态，并累加最终的item统计。
     */
    public void markSubTaskAsFinished(String mainTaskId, String subTaskId, PostProcessingTaskStatus status, String message) {
        String redisKey = REDIS_KEY_PREFIX + mainTaskId;

        // 先获取子任务的最终处理结果
        getSubTaskDetail(redisKey, subTaskId).ifPresent(detail -> {
            detail.setStatus(status.name());
            detail.setResultMessage(message);
            detail.setFinishTime(System.currentTimeMillis());
            putSubTaskDetail(redisKey, subTaskId, detail);

            // 在批次完成后，将该批次的处理结果累加到宏观统计中
            getSummary(redisKey).ifPresent(summary -> {
                summary.setCompletedSubTasks(summary.getCompletedSubTasks() + 1);
                // 累加 item 数量，用于最终展示
                summary.setTotalItems(summary.getTotalItems() + detail.getTotalItems());
                summary.setProcessedItems(summary.getProcessedItems() + detail.getProcessedItems());
                summary.setFailedItems(summary.getFailedItems() + detail.getFailedItems());
                putSummary(redisKey, summary);
            });
        });

        redisTemplate.expire(redisKey, PROGRESS_TTL_HOURS, TimeUnit.HOURS);
    }
    /**
     * 获取完整的进度信息 DTO。
     */
    public FullProgress geFullProgress(String mainTaskId) {
        String redisKey = REDIS_KEY_PREFIX + mainTaskId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            return null;
        }

        FullProgress fullProgress = new FullProgress();

        // 使用类型安全的 getSummary 方法
        getSummary(redisKey).ifPresent(summary -> {
            if (summary.getTotalSubTasks() > 0 && summary.getCompletedSubTasks() >= summary.getTotalSubTasks()) {
                String newStatus = summary.getFailedItems() > 0 ? PostProcessingTaskStatus.PARTIALLY_COMPLETED.name() : PostProcessingTaskStatus.COMPLETED.name();
                if (!summary.getStatus().equals(newStatus)) {
                    summary.setStatus(newStatus);
                    putSummary(redisKey, summary);
                }
            }
            fullProgress.setSummary(summary);
        });

        // 使用类型安全的 getAllSubTaskDetails 方法
        fullProgress.setSubTasks(getAllSubTaskDetails(redisKey));

        return fullProgress;
    }

    public void cleanup(String mainTaskId) {
        redisTemplate.delete(REDIS_KEY_PREFIX + mainTaskId);
    }

    // ====================================================================
    //              私有、类型安全的 Redis Hash 访问器方法
    // ====================================================================

    private void putSummary(String redisKey, ProgressSummary summary) {
        redisTemplate.opsForHash().put(redisKey, SUMMARY_FIELD, summary);
    }

    private Optional<ProgressSummary> getSummary(String redisKey) {
        Object raw = redisTemplate.opsForHash().get(redisKey, SUMMARY_FIELD);
        return Optional.ofNullable((ProgressSummary) raw);
    }

    private void putSubTaskDetail(String redisKey, String subTaskId, SubTaskProgressDetail detail) {
        redisTemplate.opsForHash().put(redisKey, SUBTASK_FIELD_PREFIX + subTaskId, detail);
    }

    public Optional<SubTaskProgressDetail> getSubTaskDetail(String redisKey, String subTaskId) {
        Object raw = redisTemplate.opsForHash().get(redisKey, SUBTASK_FIELD_PREFIX + subTaskId);
        return Optional.ofNullable((SubTaskProgressDetail) raw);
    }

    private List<SubTaskProgressDetail> getAllSubTaskDetails(String redisKey) {
        Map<Object, Object> rawData = redisTemplate.opsForHash().entries(redisKey);
        return rawData.entrySet().stream()
                .filter(entry -> ((String) entry.getKey()).startsWith(SUBTASK_FIELD_PREFIX))
                .map(entry -> (SubTaskProgressDetail) entry.getValue())
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(SubTaskProgressDetail::getSubTaskNo))
                .collect(Collectors.toList());
    }
}
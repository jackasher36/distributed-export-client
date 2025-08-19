package com.jackasher.ageiport.processer;

import com.alibaba.ageiport.processor.core.annotation.ExportSpecification;
import com.alibaba.ageiport.processor.core.constants.ExecuteType;
import com.alibaba.ageiport.processor.core.exception.BizException;
import com.alibaba.ageiport.processor.core.model.api.BizExportPage;
import com.alibaba.ageiport.processor.core.model.api.BizUser;
import com.alibaba.ageiport.processor.core.task.exporter.ExportProcessor;
import com.alibaba.ageiport.processor.core.task.exporter.api.BizExportTaskRuntimeConfig;
import com.alibaba.ageiport.processor.core.task.exporter.api.BizExportTaskRuntimeConfigImpl;
import com.jackasher.ageiport.model.user.UserData;
import com.jackasher.ageiport.model.user.UserQuery;
import com.jackasher.ageiport.model.user.UserView;
import com.jackasher.ageiport.demo.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 写demo是测试用例,可忽略
 */
@ExportSpecification(
        code = "UserCSVExportProcessor",
        name = "UserCSVExportProcessor",
        fileType = "csv",
        executeType = ExecuteType.CLUSTER
)
@Deprecated
public class UserCSVExportProcessor implements ExportProcessor<UserQuery, UserData, UserView> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Integer totalCount(BizUser bizUser, UserQuery query) throws BizException {
        System.out.println("开始统计用户总数...");

        String sql = "SELECT COUNT(*) FROM user WHERE is_delete = 0";
        List<Object> params = new ArrayList<>();

        // 添加查询条件
        if (query.getUsername() != null && !query.getUsername().trim().isEmpty()) {
            sql += " AND username LIKE ?";
            params.add("%" + query.getUsername() + "%");
        }

        if (query.getUserStatus() != null) {
            sql += " AND user_status = ?";
            params.add(query.getUserStatus());
        }

        if (query.getUserRole() != null) {
            sql += " AND user_role = ?";
            params.add(query.getUserRole());
        }

        if (query.getEmail() != null && !query.getEmail().trim().isEmpty()) {
            sql += " AND email LIKE ?";
            params.add("%" + query.getEmail() + "%");
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("数据库中符合条件的用户总数: " + count);

                // 返回实际总数和查询条件中指定总数的较小值
                return Math.min(count, query.getTotalCount());
            }
            return 0;

        } catch (SQLException e) {
            throw new BizException("QUERY_TOTAL_COUNT_ERROR", "查询用户总数失败: " + e.getMessage());
        }
    }

    @Override
    public List<UserData> queryData(BizUser user, UserQuery query, BizExportPage page) throws BizException {
        System.out.println("查询用户数据 - 偏移量: " + page.getOffset() + ", 大小: " + page.getSize());

        String sql = "SELECT id, username, user_account, avatar_url, gender, phone, email, " +
                "user_status, create_time, update_time, user_role, tags, profile " +
                "FROM user WHERE is_delete = 0";

        List<Object> params = new ArrayList<>();

        // 添加查询条件
        if (query.getUsername() != null && !query.getUsername().trim().isEmpty()) {
            sql += " AND username LIKE ?";
            params.add("%" + query.getUsername() + "%");
        }

        if (query.getUserStatus() != null) {
            sql += " AND user_status = ?";
            params.add(query.getUserStatus());
        }

        if (query.getUserRole() != null) {
            sql += " AND user_role = ?";
            params.add(query.getUserRole());
        }

        if (query.getEmail() != null && !query.getEmail().trim().isEmpty()) {
            sql += " AND email LIKE ?";
            params.add("%" + query.getEmail() + "%");
        }

        // 添加分页
        sql += " ORDER BY id LIMIT ? OFFSET ?";
        params.add(page.getSize());
        params.add(page.getOffset());

        List<UserData> dataList = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // 设置参数
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserData data = new UserData();
                data.setId(rs.getLong("id"));
                data.setUsername(rs.getString("username"));
                data.setUserAccount(rs.getString("user_account"));
                data.setAvatarUrl(rs.getString("avatar_url"));
                data.setGender(rs.getInt("gender"));
                data.setPhone(rs.getString("phone"));
                data.setEmail(rs.getString("email"));
                data.setUserStatus(rs.getInt("user_status"));
                data.setCreateTime(rs.getTimestamp("create_time"));
                data.setUpdateTime(rs.getTimestamp("update_time"));
                data.setUserRole(rs.getInt("user_role"));
                data.setTags(rs.getString("tags"));
                data.setProfile(rs.getString("profile"));

//                System.out.println(data.getTags() + ":下载到本地磁盘");

                dataList.add(data);
            }

            System.out.println("查询到 " + dataList.size() + " 条用户数据");
            return dataList;

        } catch (SQLException e) {
            throw new BizException("QUERY_USER_DATA_ERROR", "查询用户数据失败: " + e.getMessage());
        }
    }

    @Override
    public List<UserView> convert(BizUser user, UserQuery query, List<UserData> data) throws BizException {
        System.out.println("转换 " + data.size() + " 条数据为CSV视图格式...");

        List<UserView> viewList = new ArrayList<>();
        for (UserData userData : data) {
            UserView view = new UserView();

            // 基本字段复制
            view.setId(userData.getId());
            view.setUsername(userData.getUsername());
            view.setUserAccount(userData.getUserAccount());
            view.setAvatarUrl(userData.getAvatarUrl());
            view.setPhone(userData.getPhone());
            view.setEmail(userData.getEmail());
            view.setTags(userData.getTags());
            view.setProfile(userData.getProfile());

            // 性别转换
            view.setGenderText(convertGender(userData.getGender()));

            // 用户状态转换
            view.setUserStatusText(convertUserStatus(userData.getUserStatus()));

            // 用户角色转换
            view.setUserRoleText(convertUserRole(userData.getUserRole()));

            // 时间格式化
            if (userData.getCreateTime() != null) {
                view.setCreateTime(dateFormat.format(userData.getCreateTime()));
            }

            viewList.add(view);
        }

        return viewList;
    }

    @Override
    public BizExportTaskRuntimeConfig taskRuntimeConfig(BizUser user, UserQuery query) throws BizException {
        BizExportTaskRuntimeConfigImpl config = new BizExportTaskRuntimeConfigImpl();
        config.setPageSize(500);
        return config;
    }

    // 辅助方法：性别转换
    private String convertGender(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        switch (gender) {
            case 0:
                return "女";
            case 1:
                return "男";
            default:
                return "其他";
        }
    }

    // 辅助方法：用户状态转换
    private String convertUserStatus(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "正常";
            case 1:
                return "禁用";
            default:
                return "其他";
        }
    }

    // 辅助方法：用户角色转换
    private String convertUserRole(Integer role) {
        if (role == null) {
            return "未知";
        }
        switch (role) {
            case 0:
                return "普通用户";
            case 1:
                return "管理员";
            default:
                return "其他";
        }
    }
}
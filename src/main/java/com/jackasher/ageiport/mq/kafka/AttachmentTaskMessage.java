package com.jackasher.ageiport.mq.kafka;

import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Kafka 版本的附件任务消息
 * @author Jackasher
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentTaskMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<IrMessageData> messages;
    private String subTaskId;
    private int subTaskNo;
    private IrMessageQuery query;
    private String mainTaskId;
}

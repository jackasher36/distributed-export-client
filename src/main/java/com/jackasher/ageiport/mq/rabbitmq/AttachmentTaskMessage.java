
// src/main/java/com/jackasher/ageiport/model/mq/AttachmentTaskMessage.java
package com.jackasher.ageiport.mq.rabbitmq;

import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

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
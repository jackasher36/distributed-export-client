package com.jackasher.ageiport.processer.impl.ir_message;

import org.springframework.stereotype.Component;

import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageView;
import com.jackasher.ageiport.processer.GenericDataConverter;
import com.jackasher.ageiport.utils.business.IrMessageUtils;

/**
 * IrMessage数据转换器实现
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
@Component
public class IrMessageDataConverter implements GenericDataConverter<IrMessageData, IrMessageView> {

    @Override
    public IrMessageView convertToView(IrMessageData data) {
        return IrMessageUtils.createViewFromData(data);
    }
}
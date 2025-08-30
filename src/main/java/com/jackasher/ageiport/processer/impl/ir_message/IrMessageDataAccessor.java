package com.jackasher.ageiport.processer.impl.ir_message;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import com.jackasher.ageiport.model.pojo.IrMessage;
import com.jackasher.ageiport.processer.GenericDataAccessor;
import com.jackasher.ageiport.utils.business.IrMessageUtils;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;

/**
 * IrMessage数据访问器实现
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
@Component
public class IrMessageDataAccessor implements GenericDataAccessor<IrMessageQuery, IrMessageData> {

    @Override
    public Long countByQuery(IrMessageQuery query) {
        LambdaQueryWrapper<IrMessage> queryWrapper = IrMessageUtils.irMessageQueryToirMessage(query, false);
        return SpringContextUtil.getIrMessageMapper().selectCount(queryWrapper);
    }

    @Override
    public List<IrMessageData> queryByPage(IrMessageQuery query, long offset, int size) {
        LambdaQueryWrapper<IrMessage> queryWrapper = IrMessageUtils.irMessageQueryToirMessage(query);
        long pageNum = (offset / size) + 1;
        Page<IrMessage> page = new Page<>(pageNum, size);

        IPage<IrMessage> resultPage = SpringContextUtil.getIrMessageMapper().selectPage(page, queryWrapper);
        List<IrMessage> irMessages = resultPage.getRecords();

        // 转换为IrMessageData
        List<IrMessageData> dataList = new ArrayList<>();
        for (IrMessage irMessage : irMessages) {
            IrMessageData data = IrMessageUtils.convertToIrMessageData(irMessage);
            dataList.add(data);
        }

        return dataList;
    }
}
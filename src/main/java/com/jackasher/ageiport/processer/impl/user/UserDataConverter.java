package com.jackasher.ageiport.processer.impl.user;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.jackasher.ageiport.model.user.UserData;
import com.jackasher.ageiport.model.user.UserView;
import com.jackasher.ageiport.processer.GenericDataConverter;

/**
 * 用户数据转换器实现示例
 * 展示如何为其他数据类型实现数据转换逻辑
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
@Component
public class UserDataConverter implements GenericDataConverter<UserData, UserView> {

    @Override
    public UserView convertToView(UserData data) {
        UserView view = new UserView();
        view.setId(data.getId());
        view.setUsername(data.getUsername());
        view.setEmail(data.getEmail());
        return view;
    }
}
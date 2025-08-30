package com.jackasher.ageiport.processer.impl.user;

import java.util.List;
import java.util.stream.Collectors;

import com.jackasher.ageiport.model.user.UserQuery;
import org.springframework.stereotype.Component;

import com.jackasher.ageiport.model.user.UserData;
import com.jackasher.ageiport.processer.GenericDataAccessor;

/**
 * 用户数据访问器实现示例
 * 展示如何为其他数据类型实现数据访问逻辑
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
@Component
public class UserDataAccessor implements GenericDataAccessor<UserQuery, UserData> {

    @Override
    public Long countByQuery(UserQuery query) {
        // 这里应该是真实的数据库查询逻辑
        // 为了示例，我们返回一个模拟的总数
        return 1000L;
    }

    @Override
    public List<UserData> queryByPage(UserQuery query, long offset, int size) {
        // 这里应该是真实的分页查询逻辑
        // 为了示例，我们生成模拟数据
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> {
                    UserData user = new UserData();
                    user.setId((long) (offset + i + 1));
                    user.setUsername("user" + (offset + i + 1));
                    user.setEmail("user" + (offset + i + 1) + "@example.com");
                    return user;
                })
                .collect(Collectors.toList());
    }
}
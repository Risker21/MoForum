package com.moforum;

import com.moforum.entity.User;
import com.moforum.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
class MoforumApplicationTests {

    @Autowired
    private UserMapper userMapper;
    @Test
    void testAddUser() {
//        User user = new User();
//        user.setId(1L);
//        user.setUsername("test");
//        user.setPassword("123456");
//        user.setCreateTime(LocalDateTime.now());
//        user.setUpdateTime(LocalDateTime.now());
//        userMapper.insert(user);
//        System.out.println(user);
    }
}

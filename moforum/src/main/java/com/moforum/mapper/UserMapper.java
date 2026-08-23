package com.moforum.mapper;

import com.moforum.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 作者:Momo同学
 * 日期: 2026/4/12 18:53
 */
@Mapper
@Repository
public interface UserMapper {
    /**
     * @param user 用户对象，包含用户名和密码等信息
     * @return 影响的行数，成功返回 1
     */
    /**
     * 用户注册
     */
    int insert(User user);
    /**
     * 根据 ID 查询用户
     */
    User selectById(@Param("id") Long id);

    /**
     * 根据用户名查询（用于登录校验）
     */
    User selectByUsername(@Param("username") String username);

    User selectByUserNo(@Param("userNo") Long userNo);

    int updateUserNo(@Param("id") Long id, @Param("userNo") Long userNo);

    int updateProfile(@Param("id") Long id, @Param("bio") String bio, @Param("avatarUrl") String avatarUrl);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int deleteById(@Param("id") Long id);

    List<User> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}

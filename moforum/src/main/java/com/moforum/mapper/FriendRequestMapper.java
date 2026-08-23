package com.moforum.mapper;

import com.moforum.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface FriendRequestMapper {
    int insert(FriendRequest request);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int deleteByUser(@Param("userId") Long userId);
    FriendRequest selectById(@Param("id") Long id);
    FriendRequest selectByUsers(@Param("fromId") Long fromId, @Param("toId") Long toId);
    List<FriendRequest> selectReceived(@Param("userId") Long userId);
    List<FriendRequest> selectSent(@Param("userId") Long userId);
    int countPendingReceived(@Param("userId") Long userId);
}

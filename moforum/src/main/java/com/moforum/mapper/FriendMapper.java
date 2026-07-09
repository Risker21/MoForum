package com.moforum.mapper;

import com.moforum.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface FriendMapper {
    int insert(Friend friend);
    Friend selectByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    List<Friend> selectFriends(@Param("userId") Long userId);
    List<Long> selectFriendIds(@Param("userId") Long userId);
    int checkFriendship(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}

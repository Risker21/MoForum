package com.moforum.mapper;

import com.moforum.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface MessageMapper {
    int insert(Message message);
    int deleteByUser(@Param("userId") Long userId);
    List<Message> selectConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2,
                                     @Param("offset") int offset, @Param("limit") int limit);
    int countConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    int markRead(@Param("toUserId") Long toUserId, @Param("fromUserId") Long fromUserId);
    int countUnread(@Param("userId") Long userId);
    List<Message> selectConversations(@Param("userId") Long userId);
}

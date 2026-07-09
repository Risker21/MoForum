package com.moforum.mapper;

import com.moforum.entity.Reply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface ReplyMapper {

    int insert(Reply reply);

    List<Reply> listByPostId(@Param("postId") Long postId);

    int deleteByPostId(@Param("postId") Long postId);
}

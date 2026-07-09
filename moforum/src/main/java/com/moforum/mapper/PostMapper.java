package com.moforum.mapper;

import com.moforum.entity.Post;
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
public interface PostMapper {
    /**
     * 发布帖子
     */
    int insert(Post post);

    /**
     * 按吧分页
     */
    List<Post> selectPageByBoard(@Param("boardId") long boardId, @Param("offset") int offset, @Param("limit") int limit);

    int countByBoard(@Param("boardId") long boardId);

    /**
     * 全站最新（首页）
     */
    List<Post> selectLatest(@Param("limit") int limit);

    Post selectByIdWithAuthor(@Param("id") Long id);

    int incrementViewCount(@Param("id") Long id);

    int incrementReplyCount(@Param("id") Long id);

    Post selectBasicById(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    List<Post> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    List<Post> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    int countByUserId(@Param("userId") Long userId);
}

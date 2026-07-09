package com.moforum.mapper;

import com.moforum.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface FollowMapper {
    int insert(Follow follow);
    int delete(@Param("followerId") Long followerId, @Param("followedId") Long followedId);
    Follow selectByFollowerAndFollowed(@Param("followerId") Long followerId, @Param("followedId") Long followedId);
    List<Follow> selectFollowers(@Param("userId") Long userId);
    List<Follow> selectFollowing(@Param("userId") Long userId);
    int countFollowers(@Param("userId") Long userId);
    int countFollowing(@Param("userId") Long userId);
}

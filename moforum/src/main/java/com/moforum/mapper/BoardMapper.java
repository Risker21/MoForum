package com.moforum.mapper;

import com.moforum.entity.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BoardMapper {

    List<Board> listAll();

    Board selectById(@Param("id") Long id);

    int incrementPostCount(@Param("id") Long id);

    int decrementPostCount(@Param("id") Long id);

    List<Board> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}

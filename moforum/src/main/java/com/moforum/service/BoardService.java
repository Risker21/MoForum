package com.moforum.service;

import com.moforum.entity.Board;
import com.moforum.mapper.BoardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardService {

    @Autowired
    private BoardMapper boardMapper;

    @Cacheable("boards")
    public List<Board> listAll() {
        return boardMapper.listAll();
    }

    public Board getById(Long id) {
        return boardMapper.selectById(id);
    }

    @CacheEvict(value = "boards", allEntries = true)
    public void evictBoardCache() {
    }
}

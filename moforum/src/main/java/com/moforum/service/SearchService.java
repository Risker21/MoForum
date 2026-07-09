package com.moforum.service;

import com.moforum.entity.Board;
import com.moforum.entity.Post;
import com.moforum.entity.User;
import com.moforum.mapper.BoardMapper;
import com.moforum.mapper.PostMapper;
import com.moforum.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchService {

    private final PostMapper postMapper;
    private final BoardMapper boardMapper;
    private final UserMapper userMapper;

    public SearchService(PostMapper postMapper, BoardMapper boardMapper, UserMapper userMapper) {
        this.postMapper = postMapper;
        this.boardMapper = boardMapper;
        this.userMapper = userMapper;
    }

    public Map<String, Object> search(String keyword, String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        String kw = "%" + keyword + "%";

        if ("all".equals(type) || "post".equals(type)) {
            List<Post> posts = postMapper.searchByKeyword(kw, 20);
            result.put("posts", posts != null ? posts : Collections.emptyList());
        }
        if ("all".equals(type) || "board".equals(type)) {
            List<Board> boards = boardMapper.searchByKeyword(kw, 20);
            result.put("boards", boards != null ? boards : Collections.emptyList());
        }
        if ("all".equals(type) || "user".equals(type)) {
            List<User> users = userMapper.searchByKeyword(kw, 20);
            result.put("users", users != null ? users : Collections.emptyList());
        }
        return result;
    }
}

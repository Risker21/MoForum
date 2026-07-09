package com.moforum.controller;

import com.moforum.entity.Board;
import com.moforum.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @GetMapping("/list")
    public List<Board> list() {
        return boardService.listAll();
    }

    @GetMapping("/detail")
    public ResponseEntity<Board> detail(@RequestParam Long id) {
        Board b = boardService.getById(id);
        return b == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(b);
    }
}

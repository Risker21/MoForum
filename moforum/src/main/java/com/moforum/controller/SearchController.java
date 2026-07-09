package com.moforum.controller;

import com.moforum.service.SearchService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public Map<String, Object> search(@RequestParam String q, @RequestParam(defaultValue = "all") String type) {
        return searchService.search(q, type);
    }
}

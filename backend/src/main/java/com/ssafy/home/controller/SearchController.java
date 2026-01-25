package com.ssafy.home.controller;

import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.dto.SuggestionItem;
import com.ssafy.home.model.dto.HouseInfo;
import com.ssafy.home.model.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "검색", description = "검색 및 자동완성")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    @Operation(summary = "아파트 검색", description = "아파트명/동명/도로명으로 키워드 검색")
    public ResponseEntity<ListResponse<HouseInfo>> search(@RequestParam(value = "q", required = false) String q,
                                                          @Parameter(description = "페이지 인덱스(0부터)", example = "0")
                                                          @RequestParam(value = "page", required = false) Integer page,
                                                          @Parameter(description = "페이지 크기", example = "20")
                                                          @RequestParam(value = "size", required = false) Integer size,
                                                          @Parameter(description = "정렬(현재 미사용)", example = "year_desc")
                                                          @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(searchService.search(q, page, size, sort));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "자동완성", description = "동/아파트 혼합 추천 목록 반환")
    public ResponseEntity<List<SuggestionItem>> suggestions(@RequestParam("q") String q,
                                                            @Parameter(description = "최대 개수", example = "10")
                                                            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(searchService.suggestions(q, limit));
    }
}

package com.ssafy.home.controller;

import com.ssafy.home.model.dto.AccessSummaryResponse;
import com.ssafy.home.model.service.AccessSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access")
@Tag(name = "생활권", description = "주변 접근성 요약(도보 분)")
public class AccessController {

    private final AccessSummaryService accessSummaryService;

    public AccessController(AccessSummaryService accessSummaryService) {
        this.accessSummaryService = accessSummaryService;
    }

    @GetMapping("/summary")
    @Operation(summary = "생활권 요약", description = "지하철/마트/편의점/학교/병원까지 도보 분 요약 (검색 반경 1000m 고정)")
    public ResponseEntity<?> summary(@RequestParam("aptSeq") String aptSeq) {
        try {
            AccessSummaryResponse res = accessSummaryService.summarize(aptSeq);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

package com.ssafy.home.controller;

import com.ssafy.home.model.dto.AptTradeRecord;
import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.service.AptTradeQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/live")
@Tag(name = "실거래API", description = "국토부 OpenAPI 실시간 조회")
public class AptTradeController {

    private final AptTradeQueryService service;

    public AptTradeController(AptTradeQueryService service) {
        this.service = service;
    }

    @GetMapping("/apt-trades")
    @Operation(summary = "아파트 매매 실거래(월)", description = "LAWD_CD(시군구 5자리), DEAL_YMD(YYYYMM) 기반 API 조회")
    public ResponseEntity<ListResponse<AptTradeRecord>> aptTrades(@RequestParam("sgg") String sggCd,
                                                                  @Parameter(description = "YYYYMM", example = "202501")
                                                                  @RequestParam("period") String yyyymm,
                                                                  @RequestParam(value = "page", required = false) Integer page,
                                                                  @RequestParam(value = "size", required = false) Integer size) {
        return ResponseEntity.ok(service.queryBySgg(sggCd, yyyymm, page, size));
    }
}


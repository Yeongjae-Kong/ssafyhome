package com.ssafy.home.controller;

import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.dto.RegionCodeResponse;
import com.ssafy.home.model.dto.HouseDeal;
import com.ssafy.home.model.dto.HouseInfo;
import com.ssafy.home.model.dto.DealsByAptResponse;
import com.ssafy.home.model.service.RegionService;
import com.ssafy.home.model.service.LiveTradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/region")
@Tag(name = "지역", description = "지역 코드 및 지역 기반 조회")
public class RegionController {

    private final RegionService regionService;
    private final LiveTradeService liveTradeService;
    private final com.ssafy.home.model.service.SearchService searchService;

    public RegionController(RegionService regionService, LiveTradeService liveTradeService, com.ssafy.home.model.service.SearchService searchService) {
        this.regionService = regionService;
        this.liveTradeService = liveTradeService;
        this.searchService = searchService;
    }

    @GetMapping("/code")
    @Operation(summary = "지역 코드 조회", description = "동이 지정되면 단일 코드, 미지정이면 구 하위 동 코드 목록 반환")
    public ResponseEntity<RegionCodeResponse> getCode(@RequestParam("sido") String sido,
                                                      @RequestParam("gugun") String gugun,
                                                      @Parameter(description = "동 이름(선택)", example = "역삼동")
                                                      @RequestParam(value = "dong", required = false) String dong) {
        return ResponseEntity.ok(regionService.getRegionCode(sido, gugun, dong));
    }

    @GetMapping("/houses")
    @Operation(summary = "지역 내 아파트 목록", description = "구 또는 동 단위로 아파트 목록")
    public ResponseEntity<ListResponse<HouseInfo>> getHouses(@RequestParam("sido") String sido,
                                                             @RequestParam("gugun") String gugun,
                                                             @Parameter(description = "동 이름(선택)", example = "역삼동")
                                                             @RequestParam(value = "dong", required = false) String dong) {
        return ResponseEntity.ok(regionService.getHouses(sido, gugun, dong));
    }

    @GetMapping("/deals")
    @Operation(summary = "지역 내 거래 목록(월, OpenAPI)", description = "구 또는 동 단위 거래 목록 - 국토부 OpenAPI(data.go.kr)")
    public ResponseEntity<ListResponse<HouseDeal>> getDeals(@RequestParam("sido") String sido,
                                                            @RequestParam("gugun") String gugun,
                                                            @Parameter(description = "동 이름(선택)", example = "역삼동")
                                                            @RequestParam(value = "dong", required = false) String dong,
                                                            @Parameter(description = "YYYYMM", example = "202501")
                                                            @RequestParam("period") String period,
                                                            @Parameter(description = "페이지 번호(1부터)", example = "1")
                                                            @RequestParam(value = "page", required = false) Integer page,
                                                            @Parameter(description = "페이지 크기", example = "100")
                                                            @RequestParam(value = "size", required = false) Integer size) {
        return ResponseEntity.ok(liveTradeService.regionDeals(sido, gugun, dong, period, page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "지역 한정 아파트 검색", description = "시/구(선택:동) 내 아파트명/주소 키워드 검색")
    public ResponseEntity<ListResponse<HouseInfo>> searchInRegion(@RequestParam("sido") String sido,
                                                                  @RequestParam("gugun") String gugun,
                                                                  @Parameter(description = "동 이름(선택)", example = "역삼동")
                                                                  @RequestParam(value = "dong", required = false) String dong,
                                                                  @Parameter(description = "검색어", example = "래미안")
                                                                  @RequestParam("q") String q,
                                                                  @RequestParam(value = "page", required = false) Integer page,
                                                                  @RequestParam(value = "size", required = false) Integer size) {
        return ResponseEntity.ok(searchService.searchInRegion(sido, gugun, dong, q, page, size));
    }

    @GetMapping("/deals/recent-bundle")
    @Operation(summary = "구 단위 최근 거래 번들(최대 12개월)", description = "최근 months개월(기본 12) 구 단위 거래를 아파트별로 그룹화하여 반환")
    public ResponseEntity<DealsByAptResponse> getRecentDealsBundle(@RequestParam("sido") String sido,
                                                                   @RequestParam("gugun") String gugun,
                                                                   @Parameter(description = "수집 개월 수(최대 12)", example = "12")
                                                                   @RequestParam(value = "months", required = false) Integer months,
                                                                   @Parameter(description = "아파트별 최대 건수", example = "200")
                                                                   @RequestParam(value = "limitPerApt", required = false) Integer limitPerApt) {
        int m = months == null ? 12 : Math.max(1, Math.min(12, months));
        int cap = limitPerApt == null ? 50 : Math.max(1, Math.min(1000, limitPerApt));

        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMM");
        List<String> periods = new ArrayList<>();
        for (int i = 0; i < m; i++) periods.add(now.minusMonths(i).format(fmt));

        // Build normalized name -> aptSeq map from region houses
        List<HouseInfo> houses = regionService.getHouses(sido, gugun, null).getItems();
        Map<String, String> nameToSeq = new HashMap<>();
        for (HouseInfo hi : houses) {
            if (hi.getAptNm() != null && hi.getAptSeq() != null) {
                nameToSeq.put(normalizeName(hi.getAptNm()), hi.getAptSeq());
            }
        }

        Map<String, List<HouseDeal>> grouped = new HashMap<>();
        for (String p : periods) {
            try {
                var lr = liveTradeService.regionDeals(sido, gugun, null, p, 1, 1000);
                List<HouseDeal> items = lr == null ? List.of() : (lr.getItems() == null ? List.of() : lr.getItems());
                for (HouseDeal d : items) {
                    String nm = normalizeName(d.getAptName());
                    if (nm.isEmpty()) continue;
                    String aptSeq = nameToSeq.get(nm);
                    if (aptSeq == null) continue;
                    var list = grouped.computeIfAbsent(aptSeq, k -> new ArrayList<>());
                    if (list.size() < cap) list.add(d);
                }
            } catch (Exception ignored) {
            }
        }

        DealsByAptResponse resp = new DealsByAptResponse();
        resp.setItems(grouped);
        resp.setMonths(m);
        resp.setSido(sido);
        resp.setGugun(gugun);
        return ResponseEntity.ok(resp);
    }

    private static String normalizeName(String s) {
        if (s == null) return "";
        String x = s.replaceAll("\\(.*?\\)", "");
        x = x.replaceAll("[^0-9A-Za-z가-힣]", "");
        x = x.toUpperCase();
        return x.trim();
    }
}

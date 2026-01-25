package com.ssafy.home.controller;

import com.ssafy.home.model.dto.ListResponse;
import com.ssafy.home.model.dto.HouseInfo;
import com.ssafy.home.model.service.MapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/map")
@Tag(name = "지도", description = "지도 기반 조회")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/items")
    @Operation(summary = "바운딩 박스 내 아파트 목록")
    public ResponseEntity<ListResponse<HouseInfo>> items(@RequestParam("minLat") double minLat,
                                                         @RequestParam("maxLat") double maxLat,
                                                         @RequestParam("minLon") double minLon,
                                                         @RequestParam("maxLon") double maxLon) {
        return ResponseEntity.ok(mapService.itemsInBbox(minLat, maxLat, minLon, maxLon));
    }
}
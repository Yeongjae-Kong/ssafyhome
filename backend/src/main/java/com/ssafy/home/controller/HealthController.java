package com.ssafy.home.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/health")
@Tag(name = "상태", description = "헬스체크")
public class HealthController {
    @GetMapping
    @Operation(summary = "헬스체크")
    public ResponseEntity<String> health() { return ResponseEntity.ok("OK"); }
}

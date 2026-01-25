package com.ssafy.home.controller;

import com.ssafy.home.model.dao.UserDao;
import com.ssafy.home.model.dto.*;
import com.ssafy.home.model.service.FavoriteApartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/me")
@Tag(name = "즐겨찾기", description = "관심 아파트 관리")
public class FavoritesController {

    private final FavoriteApartmentService favoriteApartmentService;
    private final UserDao userDao;

    public FavoritesController(FavoriteApartmentService favoriteApartmentService, UserDao userDao) {
        this.favoriteApartmentService = favoriteApartmentService;
        this.userDao = userDao;
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String email = auth.getName();
            return userDao.selectByEmail(email);
        }
        return null;
    }

    @GetMapping("/favorites")
    @Operation(summary = "즐겨찾기 목록", description = "내 관심 아파트 목록")
    public ResponseEntity<?> list() {
        User u = resolveCurrentUser();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
        List<FavoriteApartment> items = favoriteApartmentService.list(u.getMno());
        return ResponseEntity.ok(new ListResponse<>(items));
    }

    @PostMapping("/favorites")
    @Operation(summary = "즐겨찾기 추가", description = "aptSeq만 전달",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "FavoriteAddRequest",
                                    value = "{\n  \"aptSeq\": \"A123456789\"\n}")))
    )
    public ResponseEntity<?> add(@Valid @org.springframework.web.bind.annotation.RequestBody FavoriteAddRequest req) {
        User u = resolveCurrentUser();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
        String aptSeq = req == null ? null : req.getAptSeq();
        if (aptSeq == null || aptSeq.isBlank()) return ResponseEntity.badRequest().body("aptSeq is required");
        FavoriteApartment f = favoriteApartmentService.add(u.getMno(), aptSeq);
        return ResponseEntity.status(HttpStatus.CREATED).body(f);
    }

    @DeleteMapping("/favorites/{favoriteId}")
    @Operation(summary = "즐겨찾기 삭제", description = "내 즐겨찾기 항목 삭제")
    public ResponseEntity<?> remove(@PathVariable int favoriteId) {
        User u = resolveCurrentUser();
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
        boolean ok = favoriteApartmentService.remove(u.getMno(), favoriteId);
        if (ok) return ResponseEntity.noContent().build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("not found");
    }
}


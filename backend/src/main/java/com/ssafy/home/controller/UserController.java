package com.ssafy.home.controller;

import com.ssafy.home.model.service.UserService;
import com.ssafy.home.model.dao.UserDao;
import com.ssafy.home.model.dto.UserResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "사용자", description = "사용자 프로필")
public class UserController {

    private final UserService userService;
    private final UserDao userDao;

    public UserController(UserService userService, UserDao userDao) {
        this.userService = userService;
        this.userDao = userDao;
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "세션 필요")
    public ResponseEntity<?> me(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String email = auth.getName();
            com.ssafy.home.model.dto.User u = userDao.selectByEmail(email);
            if (u != null) {
                UserResponse r = new UserResponse();
                r.setMno(u.getMno());
                r.setName(u.getName());
                r.setEmail(u.getEmail());
                r.setRole(u.getRole());
                r.setCreatedAt(u.getCreatedAt());
                return ResponseEntity.ok(r);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized");
    }
}

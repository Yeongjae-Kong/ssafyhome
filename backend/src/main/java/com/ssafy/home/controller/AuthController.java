package com.ssafy.home.controller;

import com.ssafy.home.model.service.UserService;
import com.ssafy.home.model.service.AuthService;
import com.ssafy.home.model.dto.LoginRequest;
import com.ssafy.home.model.dto.UserResponse;
import com.ssafy.home.model.dto.UserSignupRequest;
import com.ssafy.home.model.dto.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "회원가입/로그인/로그아웃")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/signup")
    @Operation(
            summary = "회원가입",
            description = "새 사용자 계정을 생성합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "SignupRequest",
                                    value = "{\n  \"name\": \"홍길동\",\n  \"email\": \"user@example.com\",\n  \"password\": \"pass1234\"\n}"))
            )
    )
    public ResponseEntity<?> signup(@RequestBody UserSignupRequest req) {
        try {
            // 1) 사용자 생성
            userService.signup(req);
            // 2) 즉시 로그인 처리하여 토큰 발급
            LoginRequest lr = new LoginRequest();
            lr.setEmail(req.getEmail());
            lr.setPassword(req.getPassword());
            User user = authService.login(lr);
            String access = authService.issueAccessToken(user);
            String refresh = authService.issueRefreshToken(user);
            // 3) 응답 payload 통일: 로그인과 동일한 형태로 반환
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            UserResponse r = new UserResponse();
            r.setMno(user.getMno());
            r.setName(user.getName());
            r.setEmail(user.getEmail());
            r.setRole(user.getRole());
            r.setCreatedAt(user.getCreatedAt());
            body.put("accessToken", access);
            body.put("refreshToken", refresh);
            body.put("user", r);
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "이메일/비밀번호로 로그인하며 JWT 액세스/리프레시 토큰을 발급합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(name = "LoginRequest",
                                    value = "{\n  \"email\": \"user@example.com\",\n  \"password\": \"pass1234\"\n}"))
            )
    )
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            User user = authService.login(req);
            String access = authService.issueAccessToken(user);
            String refresh = authService.issueRefreshToken(user);
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            UserResponse r = new UserResponse();
            r.setMno(user.getMno());
            r.setName(user.getName());
            r.setEmail(user.getEmail());
            r.setRole(user.getRole());
            r.setCreatedAt(user.getCreatedAt());
            body.put("accessToken", access);
            body.put("refreshToken", refresh);
            body.put("user", r);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰을 재발급합니다.")
    public ResponseEntity<?> refresh(@RequestBody java.util.Map<String, String> body) {
        String refresh = body.get("refreshToken");
        if (refresh == null || refresh.isBlank()) return ResponseEntity.badRequest().body("refreshToken is required");
        try {
            String newAccess = authService.refreshAccessToken(refresh);
            return ResponseEntity.ok(java.util.Map.of("accessToken", newAccess));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "리프레시 토큰을 폐기합니다.")
    public ResponseEntity<?> logout(@RequestBody(required = false) java.util.Map<String, String> body) {
        String refresh = body == null ? null : body.get("refreshToken");
        authService.logout(refresh);
        return ResponseEntity.noContent().build();
    }
}

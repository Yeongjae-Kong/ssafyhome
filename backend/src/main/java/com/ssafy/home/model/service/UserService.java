package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.LoginRequest;
import com.ssafy.home.model.dto.UserResponse;
import com.ssafy.home.model.dto.UserSignupRequest;
import jakarta.servlet.http.HttpSession;

public interface UserService {
    UserResponse signup(UserSignupRequest req);
    UserResponse login(LoginRequest req, HttpSession session);
    void logout(HttpSession session);
    UserResponse me(HttpSession session);
}

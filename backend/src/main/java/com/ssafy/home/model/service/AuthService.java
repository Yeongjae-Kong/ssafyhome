package com.ssafy.home.model.service;

import com.ssafy.home.model.dto.LoginRequest;
import com.ssafy.home.model.dto.User;
import com.ssafy.home.model.dto.UserResponse;

public interface AuthService {
    User login(LoginRequest req);
    String issueAccessToken(User user);
    String issueRefreshToken(User user);
    String refreshAccessToken(String refreshToken);
    void logout(String refreshToken);
}


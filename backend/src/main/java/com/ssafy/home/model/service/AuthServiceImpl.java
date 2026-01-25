package com.ssafy.home.model.service;

import com.ssafy.home.config.jwt.JwtTokenProvider;
import com.ssafy.home.model.dao.RefreshTokenDao;
import com.ssafy.home.model.dao.UserDao;
import com.ssafy.home.model.dto.LoginRequest;
import com.ssafy.home.model.dto.RefreshToken;
import com.ssafy.home.model.dto.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserDao userDao;
    private final RefreshTokenDao refreshTokenDao;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserDao userDao, RefreshTokenDao refreshTokenDao, JwtTokenProvider jwtTokenProvider) {
        this.userDao = userDao;
        this.refreshTokenDao = refreshTokenDao;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public User login(LoginRequest req) {
        User u = userDao.selectByEmail(req.getEmail());
        if (u == null || u.getPassword() == null || !u.getPassword().equals(req.getPassword())) {
            throw new IllegalArgumentException("invalid credentials");
        }
        return u;
    }

    @Override
    public String issueAccessToken(User user) {
        return jwtTokenProvider.createAccessToken(user);
    }

    @Override
    public String issueRefreshToken(User user) {
        String token = jwtTokenProvider.createRefreshToken(user);
        refreshTokenDao.deleteByUserId(user.getMno());
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getMno());
        rt.setToken(token);
        refreshTokenDao.insert(rt);
        return token;
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        RefreshToken stored = refreshTokenDao.selectByToken(refreshToken);
        if (stored == null) throw new IllegalArgumentException("invalid refresh token");
        jwtTokenProvider.validateToken(refreshToken);
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) throw new IllegalArgumentException("not a refresh token");
        int userId = stored.getUserId();
        User u = userDao.selectById(userId);
        if (u == null) throw new IllegalArgumentException("user not found");
        // Optional: rotate refresh token
        String newAccess = jwtTokenProvider.createAccessToken(u);
        return newAccess;
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenDao.deleteByToken(refreshToken);
        }
    }
}


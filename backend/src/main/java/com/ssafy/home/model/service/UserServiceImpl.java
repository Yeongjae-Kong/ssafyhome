package com.ssafy.home.model.service;

import com.ssafy.home.model.service.UserService;
import com.ssafy.home.model.dto.User;
import com.ssafy.home.model.dao.UserDao;
import com.ssafy.home.model.dto.LoginRequest;
import com.ssafy.home.model.dto.UserResponse;
import com.ssafy.home.model.dto.UserSignupRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserResponse signup(UserSignupRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank() || req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("email and password are required");
        }
        User exists = userDao.selectByEmail(req.getEmail());
        if (exists != null) throw new IllegalStateException("email already exists");

        User u = new User();
        u.setName(req.getName());
        u.setEmail(req.getEmail());
        u.setPassword(req.getPassword()); // plain text for now
        u.setRole("USER");
        userDao.insert(u);

        return toResponse(u);
    }

    @Override
    public UserResponse login(LoginRequest req, HttpSession session) {
        User u = userDao.selectByEmail(req.getEmail());
        if (u == null || !u.getPassword().equals(req.getPassword())) {
            throw new IllegalArgumentException("invalid credentials");
        }
        session.setAttribute("userId", u.getMno());
        return toResponse(u);
    }

    @Override
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (id == null) return null;
        User u = userDao.selectById((Integer) id);
        if (u == null) return null;
        return toResponse(u);
    }

    private UserResponse toResponse(User u) {
        UserResponse r = new UserResponse();
        r.setMno(u.getMno());
        r.setName(u.getName());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }
}

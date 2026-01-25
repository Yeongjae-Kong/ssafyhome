package com.ssafy.home.config.jwt;

import com.ssafy.home.model.dao.UserDao;
import com.ssafy.home.model.dto.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDao userDao;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDao userDao) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDao = userDao;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                jwtTokenProvider.validateToken(token);
                if (!jwtTokenProvider.isAccessToken(token)) {
                    throw new RuntimeException("ACCESS 토큰이 아닙니다.");
                }
                Claims claims = jwtTokenProvider.parseClaims(token);
                String sub = claims.getSubject();
                Integer mno = Integer.parseInt(sub);
                User user = userDao.selectById(mno);
                if (user != null) {
                    String role = user.getRole() == null ? "USER" : user.getRole();
                    var auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}


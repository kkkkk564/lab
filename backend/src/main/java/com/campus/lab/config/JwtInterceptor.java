package com.campus.lab.config;

import com.campus.lab.entity.User;
import com.campus.lab.repository.UserRepository;
import com.campus.lab.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 认证拦截器：校验 Authorization: Bearer <token>，
 * 将 userId / role / userName 注入请求属性。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response);
        }
        try {
            Claims claims = jwtUtil.parse(auth.substring(7));
            Long userId = Long.valueOf(claims.getSubject());
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !Boolean.TRUE.equals(user.getActive())) {
                return reject(response);
            }
            request.setAttribute("userId", user.getId());
            request.setAttribute("role", user.getRole());
            request.setAttribute("userName", user.getName());
            return true;
        } catch (Exception e) {
            return reject(response);
        }
    }

    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已过期\",\"data\":null}");
        return false;
    }
}

package com.campus.lab.service;

import com.campus.lab.common.BizException;
import com.campus.lab.dto.LoginRequest;
import com.campus.lab.dto.LoginResponse;
import com.campus.lab.dto.PasswordChangeRequest;
import com.campus.lab.dto.UserInfo;
import com.campus.lab.entity.User;
import com.campus.lab.repository.UserRepository;
import com.campus.lab.util.JwtUtil;
import com.campus.lab.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BizException("账号已被停用，请联系管理员");
        }
        if (!PasswordUtil.verify(req.password(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, UserInfo.from(user));
    }

    public UserInfo me(Long userId) {
        return UserInfo.from(userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在")));
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        if (!PasswordUtil.verify(req.oldPassword(), user.getPassword())) {
            throw new BizException("原密码错误");
        }
        user.setPassword(PasswordUtil.hash(req.newPassword()));
        userRepository.save(user);
    }
}

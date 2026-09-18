package com.campus.lab.controller;

import com.campus.lab.common.Result;
import com.campus.lab.dto.LoginRequest;
import com.campus.lab.dto.LoginResponse;
import com.campus.lab.dto.PasswordChangeRequest;
import com.campus.lab.dto.UserInfo;
import com.campus.lab.service.AuthService;
import com.campus.lab.util.Auths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @GetMapping("/me")
    public Result<UserInfo> me(HttpServletRequest request) {
        return Result.ok(authService.me(Auths.userId(request)));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req,
                                       HttpServletRequest request) {
        authService.changePassword(Auths.userId(request), req);
        return Result.ok();
    }
}

package com.campus.lab.controller;

import com.campus.lab.common.Result;
import com.campus.lab.dto.UserInfo;
import com.campus.lab.dto.UserSaveRequest;
import com.campus.lab.service.UserService;
import com.campus.lab.util.Auths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<UserInfo>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String role,
                                       HttpServletRequest request) {
        Auths.requireAdmin(request);
        return Result.ok(userService.list(keyword, role));
    }

    @PostMapping
    public Result<UserInfo> create(@Valid @RequestBody UserSaveRequest req, HttpServletRequest request) {
        Auths.requireAdmin(request);
        return Result.ok(userService.create(req));
    }

    @PutMapping("/{id}")
    public Result<UserInfo> update(@PathVariable Long id, @Valid @RequestBody UserSaveRequest req,
                                   HttpServletRequest request) {
        Auths.requireAdmin(request);
        return Result.ok(userService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Auths.requireAdmin(request);
        userService.delete(id, Auths.userId(request));
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, HttpServletRequest request) {
        Auths.requireAdmin(request);
        userService.resetPassword(id);
        return Result.ok();
    }
}

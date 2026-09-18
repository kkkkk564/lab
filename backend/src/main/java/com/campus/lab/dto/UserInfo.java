package com.campus.lab.dto;

import com.campus.lab.entity.User;

import java.time.LocalDateTime;

public record UserInfo(Long id, String username, String name, String role, String department,
                       String phone, String email, Boolean active, LocalDateTime createdAt) {

    public static UserInfo from(User u) {
        return new UserInfo(u.getId(), u.getUsername(), u.getName(), u.getRole(), u.getDepartment(),
                u.getPhone(), u.getEmail(), u.getActive(), u.getCreatedAt());
    }
}

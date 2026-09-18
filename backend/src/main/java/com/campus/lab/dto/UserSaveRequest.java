package com.campus.lab.dto;

import jakarta.validation.constraints.NotBlank;

public record UserSaveRequest(

        @NotBlank(message = "用户名不能为空")
        String username,

        String password,

        @NotBlank(message = "姓名不能为空")
        String name,

        @NotBlank(message = "角色不能为空")
        String role,

        String department,

        String phone,

        String email,

        Boolean active) {
}

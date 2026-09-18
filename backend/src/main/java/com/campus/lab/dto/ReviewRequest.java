package com.campus.lab.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(

        @NotBlank(message = "审批动作不能为空")
        String action,

        String comment) {
}

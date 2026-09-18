package com.campus.lab.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceReportRequest(

        @NotBlank(message = "故障描述不能为空")
        String description) {
}

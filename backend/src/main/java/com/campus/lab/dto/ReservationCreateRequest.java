package com.campus.lab.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservationCreateRequest(

        @NotNull(message = "请选择实验室")
        Long labId,

        @NotBlank(message = "预约标题不能为空")
        String title,

        String purpose,

        @NotNull(message = "请选择日期")
        LocalDate date,

        @NotBlank(message = "请选择开始时间")
        String startTime,

        @NotBlank(message = "请选择结束时间")
        String endTime,

        @NotNull(message = "请填写使用人数")
        @Min(value = 1, message = "使用人数至少为 1")
        Integer headcount) {
}

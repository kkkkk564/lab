package com.campus.lab.dto;

import com.campus.lab.entity.Device;
import com.campus.lab.entity.Lab;

import java.time.LocalDateTime;

public record DeviceDTO(Long id, Long labId, String labName, String name, String model, String status,
                        String purchaseDate, Double price, String remark, String faultDesc, String reportBy,
                        LocalDateTime reportAt, LocalDateTime createdAt) {

    public static DeviceDTO of(Device d, Lab lab) {
        return new DeviceDTO(d.getId(), d.getLabId(), lab != null ? lab.getName() : "",
                d.getName(), d.getModel(), d.getStatus(), d.getPurchaseDate(), d.getPrice(), d.getRemark(),
                d.getFaultDesc(), d.getReportBy(), d.getReportAt(), d.getCreatedAt());
    }
}

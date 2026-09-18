package com.campus.lab.dto;

public record LabDTO(Long id, String code, String name, String building, String room, String category,
                     Integer capacity, String manager, String phone, String openTime, String closeTime,
                     String status, String description, String equipment,
                     long deviceCount, long faultyCount, long todayReservations, String currentStatus) {
}

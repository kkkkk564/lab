package com.campus.lab.dto;

public record LabUsage(Long labId, String name, long reservations, double hours, double utilization) {
}

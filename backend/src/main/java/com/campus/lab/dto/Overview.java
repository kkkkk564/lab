package com.campus.lab.dto;

public record Overview(long totalLabs, long openLabs, long inUseLabs, long totalDevices, long normalDevices,
                       long faultyDevices, long repairingDevices, long todayTotal, long todayApproved,
                       long todayPending, long activeUsers, long pendingApprovals) {
}

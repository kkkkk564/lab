package com.campus.lab.dto;

import com.campus.lab.entity.Lab;
import com.campus.lab.entity.Reservation;
import com.campus.lab.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservationDTO(Long id, Long labId, String labName, String room, Long userId, String username,
                             String name, String title, String purpose, LocalDate date, String startTime, String endTime,
                             Integer headcount, String status, String displayStatus, String reviewComment,
                             String reviewedBy, LocalDateTime reviewedAt, LocalDateTime createdAt) {

    public static ReservationDTO of(Reservation r, Lab lab, User u) {
        String display = r.getStatus();
        if ("APPROVED".equals(r.getStatus())) {
            LocalDateTime end = LocalDateTime.of(r.getDate(), LocalTime.parse(r.getEndTime()));
            if (end.isBefore(LocalDateTime.now())) {
                display = "FINISHED";
            }
        }
        return new ReservationDTO(
                r.getId(), r.getLabId(),
                lab != null ? lab.getName() : "", lab != null ? lab.getRoom() : "",
                r.getUserId(),
                u != null ? u.getUsername() : "", u != null ? u.getName() : "",
                r.getTitle(), r.getPurpose(), r.getDate(), r.getStartTime(), r.getEndTime(),
                r.getHeadcount(), r.getStatus(), display,
                r.getReviewComment(), r.getReviewedBy(), r.getReviewedAt(), r.getCreatedAt());
    }
}

package com.campus.lab.controller;

import com.campus.lab.common.Result;
import com.campus.lab.dto.ReservationCreateRequest;
import com.campus.lab.dto.ReservationDTO;
import com.campus.lab.dto.ReviewRequest;
import com.campus.lab.service.ReservationService;
import com.campus.lab.util.Auths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public Result<List<ReservationDTO>> list(@RequestParam(required = false) String scope,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             HttpServletRequest request) {
        return Result.ok(reservationService.list(Auths.userId(request), Auths.role(request),
                scope, status, date));
    }

    @PostMapping
    public Result<ReservationDTO> create(@Valid @RequestBody ReservationCreateRequest req,
                                         HttpServletRequest request) {
        return Result.ok(reservationService.create(Auths.userId(request), req));
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpServletRequest request) {
        reservationService.cancel(Auths.userId(request), Auths.role(request), id);
        return Result.ok();
    }

    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest req,
                               HttpServletRequest request) {
        Auths.requireAdmin(request);
        reservationService.review(Auths.userName(request), id, req);
        return Result.ok();
    }
}

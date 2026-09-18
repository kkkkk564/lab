package com.campus.lab.controller;

import com.campus.lab.common.Result;
import com.campus.lab.dto.LabDTO;
import com.campus.lab.dto.ReservationDTO;
import com.campus.lab.entity.Lab;
import com.campus.lab.service.LabService;
import com.campus.lab.util.Auths;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/labs")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    @GetMapping
    public Result<List<LabDTO>> list(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String status) {
        return Result.ok(labService.list(keyword, category, status));
    }

    @GetMapping("/{id}")
    public Result<LabDTO> detail(@PathVariable Long id) {
        return Result.ok(labService.detail(id));
    }

    @GetMapping("/{id}/timetable")
    public Result<List<ReservationDTO>> timetable(@PathVariable Long id,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(labService.timetable(id, date));
    }

    @PostMapping
    public Result<LabDTO> create(@RequestBody Lab lab, HttpServletRequest request) {
        Auths.requireAdmin(request);
        return Result.ok(labService.create(lab));
    }

    @PutMapping("/{id}")
    public Result<LabDTO> update(@PathVariable Long id, @RequestBody Lab lab, HttpServletRequest request) {
        Auths.requireAdmin(request);
        return Result.ok(labService.update(id, lab));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status,
                                     HttpServletRequest request) {
        Auths.requireAdmin(request);
        labService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Auths.requireAdmin(request);
        labService.delete(id);
        return Result.ok();
    }
}

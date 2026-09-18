package com.campus.lab.controller;

import com.campus.lab.common.Result;
import com.campus.lab.dto.DeviceDTO;
import com.campus.lab.dto.DeviceReportRequest;
import com.campus.lab.entity.Device;
import com.campus.lab.service.DeviceService;
import com.campus.lab.util.Auths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public Result<List<DeviceDTO>> list(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Long labId) {
        return Result.ok(deviceService.list(keyword, status, labId));
    }

    @PostMapping
    public Result<DeviceDTO> create(@RequestBody Device device, HttpServletRequest request) {
        Auths.requireAdmin(request);
        return Result.ok(deviceService.create(device));
    }

    @PutMapping("/{id}")
    public Result<DeviceDTO> update(@PathVariable Long id, @RequestBody Device device,
                                    HttpServletRequest request) {
        Auths.requireAdmin(request);
        return Result.ok(deviceService.update(id, device));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Auths.requireAdmin(request);
        deviceService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/report")
    public Result<Void> report(@PathVariable Long id, @Valid @RequestBody DeviceReportRequest req,
                               HttpServletRequest request) {
        deviceService.report(Auths.userName(request), id, req);
        return Result.ok();
    }

    @PutMapping("/{id}/repair")
    public Result<Void> repair(@PathVariable Long id, HttpServletRequest request) {
        Auths.requireAdmin(request);
        deviceService.repair(id);
        return Result.ok();
    }
}

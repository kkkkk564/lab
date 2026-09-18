package com.campus.lab.controller;

import com.campus.lab.common.Result;
import com.campus.lab.entity.Announcement;
import com.campus.lab.service.AnnouncementService;
import com.campus.lab.util.Auths;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public Result<List<Announcement>> list() {
        return Result.ok(announcementService.list());
    }

    @PostMapping
    public Result<Announcement> create(@RequestBody Announcement body, HttpServletRequest request) {
        Auths.requireAdmin(request);
        return Result.ok(announcementService.create(Auths.userName(request), body));
    }

    @PutMapping("/{id}")
    public Result<Announcement> update(@PathVariable Long id, @RequestBody Announcement body,
                                       HttpServletRequest request) {
        Auths.requireAdmin(request);
        return Result.ok(announcementService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Auths.requireAdmin(request);
        announcementService.delete(id);
        return Result.ok();
    }
}

package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.service.SystemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/dict")
    public ApiResponse<Object> dict() {
        return ApiResponse.ok(systemService.getDict());
    }

    @GetMapping("/overview")
    public ApiResponse<Object> overview() {
        return ApiResponse.ok(systemService.getOverview());
    }

    @GetMapping("/announcements")
    public ApiResponse<Object> announcements() {
        return ApiResponse.ok(systemService.publicAnnouncements());
    }
}

package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.dto.ConfigDTO;
import com.example.lostandfound.service.ConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/system")
    public ApiResponse<Object> getSystemConfig() {
        return ApiResponse.ok(configService.getSystemConfig());
    }

    @PutMapping("/system")
    public ApiResponse<Object> updateSystemConfig(@Valid @RequestBody ConfigDTO.UpdateSystemConfigRequest request) {
        return ApiResponse.ok(configService.updateSystemConfig(request));
    }
}

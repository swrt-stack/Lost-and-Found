package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.dto.UserDTO;
import com.example.lostandfound.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ApiResponse<Object> profile() {
        return ApiResponse.ok(userService.getProfile());
    }

    @PutMapping("/profile")
    public ApiResponse<Object> updateProfile(@Valid @RequestBody UserDTO.UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(request));
    }

    @GetMapping("/list")
    public ApiResponse<Object> list() {
        return ApiResponse.ok(userService.listUsers());
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Object> updateStatus(@PathVariable Long id, @RequestBody UserDTO.UpdateUserStatusRequest request) {
        return ApiResponse.ok(userService.updateStatus(id, request.getStatus()));
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<Object> updateRole(@PathVariable Long id, @Valid @RequestBody UserDTO.UpdateUserRoleRequest request) {
        return ApiResponse.ok(userService.updateRole(id, request.getRole()));
    }
}

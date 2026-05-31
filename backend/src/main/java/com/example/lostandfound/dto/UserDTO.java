package com.example.lostandfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class UserDTO {

    public record ProfileVO(String username, String nickname, String phone, String avatarUrl, String role) {
    }

    public record UserListVO(Long id, String username, String nickname, String phone, String avatarUrl, String role, String status) {
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "Nickname is required")
        @Size(max = 50, message = "Nickname is too long")
        private String nickname;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^1\\d{10}$", message = "Phone number format is invalid")
        private String phone;

        private String avatarUrl;
    }

    @Data
    public static class UpdateUserStatusRequest {
        private Integer status;
    }

    @Data
    public static class UpdateUserRoleRequest {
        @NotBlank(message = "Role is required")
        private String role;
    }

    public record UserActionVO(Long id, String result) {
    }
}

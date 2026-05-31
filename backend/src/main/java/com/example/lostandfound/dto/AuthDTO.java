package com.example.lostandfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDTO {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        @NotBlank(message = "Captcha id is required")
        private String captchaId;

        @NotBlank(message = "Captcha code is required")
        private String captchaCode;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
        private String username;

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^1\\d{10}$", message = "Phone must be an 11-digit mobile number")
        private String phone;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 20, message = "Password must be 6-20 characters")
        private String password;

        @NotBlank(message = "Captcha id is required")
        private String captchaId;

        @NotBlank(message = "Captcha code is required")
        private String captchaCode;
    }
}

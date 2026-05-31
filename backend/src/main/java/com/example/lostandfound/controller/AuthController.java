package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.dto.AuthDTO;
import com.example.lostandfound.ratelimit.RateLimit;
import com.example.lostandfound.ratelimit.RateLimitTarget;
import com.example.lostandfound.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/captcha")
    public ApiResponse<Map<String, Object>> captcha() {
        return ApiResponse.ok(authService.captcha());
    }

    @PostMapping("/login")
    @RateLimit(key = "auth:login", target = RateLimitTarget.IP,
            message = "Login attempts are too frequent, please try again later")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return ApiResponse.ok(authService.login(
                request.getUsername(),
                request.getPassword(),
                request.getCaptchaId(),
                request.getCaptchaCode()
        ));
    }

    @PostMapping("/register")
    @RateLimit(key = "auth:register", target = RateLimitTarget.IP,
            message = "Registration is too frequent, please try again later")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        return ApiResponse.ok(authService.register(
                request.getUsername(),
                request.getPhone(),
                request.getPassword(),
                request.getCaptchaId(),
                request.getCaptchaCode()
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(401, "Missing bearer token");
        }
        return ApiResponse.ok(authService.logout(authorization.substring(7)));
    }
}

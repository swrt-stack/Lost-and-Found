package com.example.lostandfound.service.impl;

import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.UserMapper;
import com.example.lostandfound.ratelimit.RateLimitTarget;
import com.example.lostandfound.ratelimit.RequestIdentityResolver;
import com.example.lostandfound.security.CaptchaService;
import com.example.lostandfound.service.AuthService;
import com.example.lostandfound.service.support.AuditLogSupport;
import com.example.lostandfound.security.LoginProtectionService;
import com.example.lostandfound.security.TokenSessionService;
import com.example.lostandfound.util.JwtUtil;
import com.mybatisflex.core.query.QueryWrapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final AuditLogSupport auditLogSupport;
    private final TokenSessionService tokenSessionService;
    private final LoginProtectionService loginProtectionService;
    private final RequestIdentityResolver requestIdentityResolver;
    private final HttpServletRequest request;
    private final CaptchaService captchaService;

    public AuthServiceImpl(PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserMapper userMapper,
                           AuditLogSupport auditLogSupport, TokenSessionService tokenSessionService,
                           LoginProtectionService loginProtectionService,
                           RequestIdentityResolver requestIdentityResolver,
                           HttpServletRequest request,
                           CaptchaService captchaService) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.auditLogSupport = auditLogSupport;
        this.tokenSessionService = tokenSessionService;
        this.loginProtectionService = loginProtectionService;
        this.requestIdentityResolver = requestIdentityResolver;
        this.request = request;
        this.captchaService = captchaService;
    }

    @Override
    public Map<String, Object> login(String username, String password, String captchaId, String captchaCode) {
        String ipIdentity = requestIdentityResolver.resolveIdentity(request, RateLimitTarget.IP);
        loginProtectionService.checkAllowed(username, ipIdentity);
        captchaService.validateAndConsume(captchaId, captchaCode);
        User user = userMapper.selectOneByQuery(QueryWrapper.create().where("username = ?", username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            loginProtectionService.recordFailure(username, ipIdentity);
            throw new BusinessException(401, "Invalid username or password");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "Account is disabled");
        }
        loginProtectionService.clearFailures(username, ipIdentity);
        String role = user.getRole() == null ? "USER" : user.getRole();
        String token = jwtUtil.generateToken(user.getUsername(), role);
        auditLogSupport.record(user.getId(), "LOGIN", "User logged in");
        return Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", role
        );
    }

    @Override
    public Map<String, Object> register(String username, String phone, String password, String captchaId, String captchaCode) {
        captchaService.validateAndConsume(captchaId, captchaCode);
        User existed = userMapper.selectOneByQuery(QueryWrapper.create().where("username = ?", username));
        if (existed != null) {
            throw new BusinessException(400, "Username already exists");
        }
        User phoneOwner = userMapper.selectOneByQuery(QueryWrapper.create().where("phone = ?", phone));
        if (phoneOwner != null) {
            throw new BusinessException(400, "Phone number already exists");
        }

        User user = new User();
        LocalDateTime now = LocalDateTime.now();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setPhone(phone);
        user.setRole("USER");
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        auditLogSupport.record(user.getId(), "REGISTER", "New user registered");

        return Map.of(
                "username", username,
                "role", "USER",
                "message", "Registration successful"
        );
    }

    @Override
    public Map<String, Object> logout(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "Missing bearer token");
        }
        try {
            Claims claims = jwtUtil.parseToken(token);
            tokenSessionService.blacklist(token, jwtUtil.remainingValidity(token));
            User user = userMapper.selectOneByQuery(QueryWrapper.create().where("username = ?", claims.getSubject()));
            if (user != null) {
                auditLogSupport.record(user.getId(), "LOGOUT", "User logged out");
            }
            return Map.of("message", "Logout successful");
        } catch (JwtException exception) {
            throw new BusinessException(401, "Invalid token");
        }
    }

    @Override
    public Map<String, Object> captcha() {
        CaptchaService.CaptchaPayload payload = captchaService.generate();
        return Map.of(
                "captchaId", payload.captchaId(),
                "imageData", payload.imageData(),
                "expiresInSeconds", payload.expiresInSeconds()
        );
    }
}

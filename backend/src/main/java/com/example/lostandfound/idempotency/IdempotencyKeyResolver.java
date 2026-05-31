package com.example.lostandfound.idempotency;

import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class IdempotencyKeyResolver {

    private final HttpServletRequest request;
    private final CurrentUserService currentUserService;

    public IdempotencyKeyResolver(HttpServletRequest request, CurrentUserService currentUserService) {
        this.request = request;
        this.currentUserService = currentUserService;
    }

    public String resolve(IdempotentScope scope) {
        return switch (scope) {
            case GLOBAL -> "global";
            case USER -> "user:" + currentUserService.requireUser().getId();
            case IP -> "ip:" + clientIp();
            case USER_OR_IP -> resolveUserOrIp();
        };
    }

    private String resolveUserOrIp() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "ip:" + clientIp();
        }
        try {
            return "user:" + currentUserService.requireUser().getId();
        } catch (BusinessException exception) {
            return "ip:" + clientIp();
        }
    }

    private String clientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }
}

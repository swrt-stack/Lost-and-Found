package com.example.lostandfound.ratelimit;

import com.example.lostandfound.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequestIdentityResolver {

    private final RateLimitProperties properties;

    public RequestIdentityResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    public String resolveIdentity(HttpServletRequest request, RateLimitTarget target) {
        return switch (target) {
            case GLOBAL -> "global";
            case IP -> "ip:" + resolveClientIp(request);
            case USER -> "user:" + resolveUsername();
            case USER_OR_IP -> resolveUserOrIp(request);
        };
    }

    private String resolveUserOrIp(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && StringUtils.hasText(authentication.getName())) {
            return "user:" + authentication.getName();
        }
        return "ip:" + resolveClientIp(request);
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !StringUtils.hasText(authentication.getName())) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private String resolveClientIp(HttpServletRequest request) {
        for (String headerName : properties.getIpHeaderNames()) {
            String value = request.getHeader(headerName);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String[] candidates = value.split(",");
            for (String candidate : candidates) {
                String normalized = candidate.trim();
                if (StringUtils.hasText(normalized) && !"unknown".equalsIgnoreCase(normalized)) {
                    return normalized;
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "unknown";
    }
}

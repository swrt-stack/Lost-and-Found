package com.example.lostandfound.service.support;

import com.example.lostandfound.entity.OperationLog;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.OperationLogMapper;
import com.example.lostandfound.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Component
public class AuditLogSupport {

    private final OperationLogMapper operationLogMapper;
    private final CurrentUserService currentUserService;

    public AuditLogSupport(OperationLogMapper operationLogMapper, CurrentUserService currentUserService) {
        this.operationLogMapper = operationLogMapper;
        this.currentUserService = currentUserService;
    }

    public void record(String action, String detail) {
        record(null, action, detail);
    }

    public void record(Long userId, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId != null ? userId : resolveUserId());
        log.setAction(action);
        log.setDetail(detail);
        log.setIpAddress(resolveIpAddress());
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private Long resolveUserId() {
        try {
            User user = currentUserService.requireUser();
            return user.getId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

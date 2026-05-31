package com.example.lostandfound.idempotency;

import com.example.lostandfound.common.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Aspect
@Component
public class IdempotentActionAspect {

    private final IdempotencyService idempotencyService;
    private final IdempotencyKeyResolver keyResolver;
    private final ObjectMapper objectMapper;

    public IdempotentActionAspect(IdempotencyService idempotencyService,
                                  IdempotencyKeyResolver keyResolver,
                                  ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.keyResolver = keyResolver;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(idempotentAction)")
    public Object around(ProceedingJoinPoint joinPoint, IdempotentAction idempotentAction) throws Throwable {
        String identity = keyResolver.resolve(idempotentAction.scope());
        String fingerprint = fingerprint(joinPoint);
        String redisKey = idempotentAction.key() + ":" + identity + ":" + fingerprint;
        boolean acquired = idempotencyService.tryAcquire(redisKey, Duration.ofSeconds(idempotentAction.ttlSeconds()));
        if (!acquired) {
            throw new BusinessException(429, idempotentAction.message());
        }

        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            idempotencyService.release(redisKey);
            throw throwable;
        }
    }

    private String fingerprint(ProceedingJoinPoint joinPoint) {
        List<Object> normalizedArgs = new ArrayList<>();
        for (Object arg : joinPoint.getArgs()) {
            Object normalized = normalizeArg(arg);
            if (normalized != null) {
                normalizedArgs.add(normalized);
            }
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Map<String, Object> payload = Map.of(
                "method", signature.toShortString(),
                "args", normalizedArgs
        );
        try {
            return sha256(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            return sha256(payload.toString());
        }
    }

    private Object normalizeArg(Object arg) {
        if (arg == null || arg instanceof ServletRequest || arg instanceof ServletResponse) {
            return null;
        }
        if (arg instanceof MultipartFile file) {
            return Map.of(
                    "type", "multipart",
                    "name", file.getName(),
                    "originalFilename", file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                    "size", file.getSize(),
                    "contentType", file.getContentType() == null ? "" : file.getContentType()
            );
        }
        return arg;
    }

    private String sha256(String raw) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            return Integer.toHexString(raw.hashCode());
        }
    }
}

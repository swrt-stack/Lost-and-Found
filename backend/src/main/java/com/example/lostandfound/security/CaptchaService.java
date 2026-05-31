package com.example.lostandfound.security;

import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.config.CaptchaProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    private static final char[] CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final StringRedisTemplate stringRedisTemplate;
    private final CaptchaProperties properties;
    private final MeterRegistry meterRegistry;

    public CaptchaService(StringRedisTemplate stringRedisTemplate,
                          CaptchaProperties properties,
                          MeterRegistry meterRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public CaptchaPayload generate() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode(properties.getLength());
        String key = CAPTCHA_KEY_PREFIX + captchaId;
        try {
            stringRedisTemplate.opsForValue().set(key, code, Duration.ofSeconds(properties.getTtlSeconds()));
            recordMetric("generate", "success");
        } catch (Exception exception) {
            recordMetric("generate", "error");
            log.warn("Failed to write captcha {}: {}", key, exception.getMessage());
            throw new BusinessException(500, "Failed to generate captcha");
        }
        String svg = buildSvg(code, properties.getWidth(), properties.getHeight());
        String imageData = "data:image/svg+xml;base64," + Base64.getEncoder()
                .encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new CaptchaPayload(captchaId, imageData, properties.getTtlSeconds());
    }

    public void validateAndConsume(String captchaId, String captchaCode) {
        if (!properties.isEnabled()) {
            return;
        }
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            recordMetric("validate", "missing");
            throw new BusinessException(400, "Captcha is required");
        }
        String key = CAPTCHA_KEY_PREFIX + captchaId.trim();
        try {
            String expected = stringRedisTemplate.opsForValue().get(key);
            stringRedisTemplate.delete(key);
            if (expected == null || expected.isBlank()) {
                recordMetric("validate", "expired");
                throw new BusinessException(400, "Captcha has expired, please refresh");
            }
            if (!expected.equalsIgnoreCase(captchaCode.trim())) {
                recordMetric("validate", "mismatch");
                throw new BusinessException(400, "Captcha is incorrect");
            }
            recordMetric("validate", "success");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            recordMetric("validate", "error");
            log.warn("Failed to validate captcha {}: {}", key, exception.getMessage());
            throw new BusinessException(500, "Failed to validate captcha");
        }
    }

    private String randomCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            builder.append(CAPTCHA_CHARS[random.nextInt(CAPTCHA_CHARS.length)]);
        }
        return builder.toString();
    }

    private String buildSvg(String code, int width, int height) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder builder = new StringBuilder();
        builder.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width)
                .append("' height='").append(height).append("' viewBox='0 0 ")
                .append(width).append(' ').append(height).append("'>");
        builder.append("<rect width='100%' height='100%' rx='10' fill='#f7f4ea'/>");
        for (int i = 0; i < 7; i++) {
            builder.append("<line x1='").append(random.nextInt(width)).append("' y1='").append(random.nextInt(height))
                    .append("' x2='").append(random.nextInt(width)).append("' y2='").append(random.nextInt(height))
                    .append("' stroke='").append(randomColor(0.18)).append("' stroke-width='1.4'/>");
        }
        for (int i = 0; i < 18; i++) {
            builder.append("<circle cx='").append(random.nextInt(width)).append("' cy='").append(random.nextInt(height))
                    .append("' r='").append(0.8 + random.nextDouble() * 1.8)
                    .append("' fill='").append(randomColor(0.25)).append("'/>");
        }
        int step = width / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            int x = step * (i + 1);
            int y = 28 + random.nextInt(8);
            int rotate = random.nextInt(-18, 19);
            int fontSize = 24 + random.nextInt(5);
            builder.append("<text x='").append(x).append("' y='").append(y)
                    .append("' text-anchor='middle' font-family='Verdana, Arial, sans-serif' font-size='")
                    .append(fontSize).append("' font-weight='700' transform='rotate(").append(rotate)
                    .append(' ').append(x).append(' ').append(y).append(")' fill='")
                    .append(randomColor(0.85)).append("'>").append(code.charAt(i)).append("</text>");
        }
        builder.append("</svg>");
        return builder.toString();
    }

    private String randomColor(double alpha) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "rgba(" + random.nextInt(30, 180) + "," + random.nextInt(30, 180) + ","
                + random.nextInt(30, 180) + "," + String.format(Locale.ROOT, "%.2f", alpha) + ")";
    }

    private void recordMetric(String action, String outcome) {
        Counter.builder("app_captcha_operations_total")
                .description("Captcha generation and validation operations")
                .tag("action", action)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    public record CaptchaPayload(String captchaId, String imageData, long expiresInSeconds) {
    }
}

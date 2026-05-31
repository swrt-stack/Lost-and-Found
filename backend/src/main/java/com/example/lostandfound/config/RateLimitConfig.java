package com.example.lostandfound.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RateLimitProperties.class, LoginProtectionProperties.class, CaptchaProperties.class})
public class RateLimitConfig {
}

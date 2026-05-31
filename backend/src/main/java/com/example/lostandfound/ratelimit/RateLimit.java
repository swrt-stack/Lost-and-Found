package com.example.lostandfound.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key();

    long rate() default -1;

    long interval() default -1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    RateLimitTarget target() default RateLimitTarget.USER_OR_IP;

    String message() default "Too many requests, please try again later";
}

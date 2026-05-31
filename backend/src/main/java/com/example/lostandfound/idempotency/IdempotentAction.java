package com.example.lostandfound.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentAction {

    String key();

    int ttlSeconds() default 10;

    IdempotentScope scope() default IdempotentScope.USER;

    String message() default "Duplicate submission detected, please try again later";
}

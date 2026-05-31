package com.example.lostandfound.idempotency;

public enum IdempotentScope {
    USER,
    IP,
    USER_OR_IP,
    GLOBAL
}

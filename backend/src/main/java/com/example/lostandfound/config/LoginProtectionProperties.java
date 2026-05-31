package com.example.lostandfound.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.login-protection")
public class LoginProtectionProperties {

    private boolean enabled = true;
    private long maxFailures = 5;
    private long failureWindowSeconds = 300;
    private long lockSeconds = 600;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getMaxFailures() {
        return maxFailures;
    }

    public void setMaxFailures(long maxFailures) {
        this.maxFailures = maxFailures;
    }

    public long getFailureWindowSeconds() {
        return failureWindowSeconds;
    }

    public void setFailureWindowSeconds(long failureWindowSeconds) {
        this.failureWindowSeconds = failureWindowSeconds;
    }

    public long getLockSeconds() {
        return lockSeconds;
    }

    public void setLockSeconds(long lockSeconds) {
        this.lockSeconds = lockSeconds;
    }
}

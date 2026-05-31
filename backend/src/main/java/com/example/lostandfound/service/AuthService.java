package com.example.lostandfound.service;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(String username, String password, String captchaId, String captchaCode);

    Map<String, Object> register(String username, String phone, String password, String captchaId, String captchaCode);

    Map<String, Object> logout(String token);

    Map<String, Object> captcha();
}

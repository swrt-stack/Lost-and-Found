package com.example.lostandfound.dto;

public class LogDTO {

    public record LogVO(String action, String detail, String operator, String ipAddress, String time) {
    }
}

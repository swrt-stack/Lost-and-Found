package com.example.lostandfound.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class ConfigDTO {

    public record SystemConfigVO(String siteName, Boolean reviewEnabled, Integer maxImageSize, Boolean noticeEnabled) {
    }

    @Data
    public static class UpdateSystemConfigRequest {
        @NotBlank(message = "Site name is required")
        private String siteName;
        private Boolean reviewEnabled;
        @Min(value = 1, message = "Max image size must be at least 1")
        @Max(value = 20, message = "Max image size must be no more than 20")
        private Integer maxImageSize;
        private Boolean noticeEnabled;
    }
}

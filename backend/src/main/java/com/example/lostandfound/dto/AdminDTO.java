package com.example.lostandfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

public class AdminDTO {

    public record DashboardVO(Integer publishedCount, String retrievalRate, Map<String, Integer> categoryDistribution,
                              Map<String, Integer> statusDistribution, Map<String, Integer> typeDistribution) {
    }

    public record ReviewVO(String id, String title, String type, String category, String publisher,
                           String publisherAvatarUrl, String status, String description, String location, String time) {
    }

    public record ReviewActionVO(String id, String action, String result) {
    }

    public record ReviewHistoryVO(String action, String detail, String reviewer, String createdAt) {
    }

    public record AnnouncementVO(Long id, String title, String content, String status, String createdAt) {
    }

    public record ClaimRecordVO(Long id, String itemId, String itemTitle, String applicant, String applicantAvatarUrl,
                                String publisher, String publisherAvatarUrl, String message, String status,
                                String reviewRemark, String createdAt) {
    }

    public record ClaimActionVO(Long id, String result, String status) {
    }

    public record ReportVO(Long id, String itemId, String itemType, String reporter, String reason, String status,
                           String reviewRemark, String createdAt, String itemTitle, String itemStatus,
                           String itemPublisher, String itemLocation, String itemDescription) {
    }

    @Data
    public static class AnnouncementRequest {
        @NotBlank(message = "Title is required")
        private String title;
        @NotBlank(message = "Content is required")
        private String content;
        private Integer status;
    }

    @Data
    public static class ReportReviewRequest {
        private String remark;
    }

    public record AnnouncementActionVO(Long id, String result) {
    }

    public record ReportActionVO(Long id, String result) {
    }
}

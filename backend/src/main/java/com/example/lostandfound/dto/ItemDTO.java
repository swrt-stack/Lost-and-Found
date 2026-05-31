package com.example.lostandfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

public class ItemDTO {

    @Data
    public static class CreateItemRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Description is required")
        private String description;

        @NotNull(message = "Category is required")
        private Long categoryId;

        @NotBlank(message = "Location is required")
        private String location;

        @NotBlank(message = "Event time is required")
        private String eventTime;

        private String contact;

        private String pickupMethod;

        private String images;
    }

    @Data
    public static class UpdateItemRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Description is required")
        private String description;

        @NotNull(message = "Category is required")
        private Long categoryId;

        @NotBlank(message = "Location is required")
        private String location;

        @NotBlank(message = "Event time is required")
        private String eventTime;

        private String contact;

        private String pickupMethod;

        private String images;
    }

    @Data
    public static class ClaimRequest {
        @NotBlank(message = "Claim message is required")
        private String message;
    }

    @Data
    public static class ReviewActionRequest {
        private String remark;
    }

    @Data
    public static class ClaimReviewRequest {
        private String remark;
    }

    @Data
    public static class ReportRequest {
        @NotBlank(message = "Report reason is required")
        private String reason;
    }

    public record ItemSummaryVO(String id, String title, String type, String category, String location, String time,
                                String publisher, String publisherAvatarUrl, String status, String description, String contact,
                                String pickupMethod, String images) {
    }

    public record PublishResultVO(String result, String itemId, String status) {
    }

    public record ItemActionVO(String itemId, String result, String status) {
    }

    public record MyItemVO(String id, String title, String type, String category, String location, String time,
                           String publisher, String publisherAvatarUrl, String status, String reviewRemark, String description, String contact,
                           String pickupMethod, String images) {
    }

    public record ClaimVO(Long id, String itemId, String itemTitle, String owner, String applicant, String message,
                          String status, String reviewRemark, String createdAt) {
    }

    public record MyClaimsVO(List<ClaimVO> sentClaims, List<ClaimVO> receivedClaims) {
    }
}

package com.example.lostandfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

public class ChatDTO {

    @Data
    public static class SendMessageRequest {
        private Long counterpartUserId;

        @NotBlank(message = "Message content is required")
        private String content;
    }

    public record ChatContactVO(Long counterpartUserId, String counterpartName, String counterpartLabel,
                                String counterpartAvatarUrl, String lastMessage, String lastTime, Integer unreadCount) {
    }

    public record ChatConversationVO(String itemId, String itemTitle, Long counterpartUserId, String counterpartName,
                                     String counterpartLabel, String counterpartAvatarUrl, String lastMessage, String lastTime, Integer unreadCount) {
    }

    public record ChatSummaryVO(Integer totalCount, Integer unreadCount) {
    }

    public record ChatMessageVO(Long id, Long senderUserId, String senderName, String senderAvatarUrl, Boolean mine,
                                String content, Boolean read, String time) {
    }

    public record ChatThreadVO(String itemId, String itemTitle, Long counterpartUserId, String counterpartName,
                               String counterpartAvatarUrl, Boolean ownerView, List<ChatMessageVO> messages) {
    }

    public record ChatActionVO(String itemId, Long counterpartUserId, String result) {
    }
}

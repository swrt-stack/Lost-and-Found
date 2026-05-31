package com.example.lostandfound.dto;

public class MessageDTO {

    public record MessageVO(Long id, String type, String content, String targetPath, Boolean read, String time) {
    }

    public record MessageSummaryVO(Integer totalCount, Integer unreadCount) {
    }

    public record MessageActionVO(Long id, String result) {
    }

    public record BatchActionVO(Integer affectedCount, String result) {
    }
}

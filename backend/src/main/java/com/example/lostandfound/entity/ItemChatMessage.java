package com.example.lostandfound.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("item_chat_message")
public class ItemChatMessage {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private String itemId;
    private Long senderUserId;
    private Long receiverUserId;
    private String content;
    private Integer readFlag;
    private LocalDateTime createdAt;
}

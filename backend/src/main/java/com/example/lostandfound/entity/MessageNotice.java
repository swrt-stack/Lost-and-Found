package com.example.lostandfound.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("message_notice")
public class MessageNotice {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long userId;
    private String messageType;
    private String content;
    private String targetPath;
    private Integer readFlag;
    private LocalDateTime createdAt;
}

package com.example.lostandfound.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("item_report")
public class ItemReport {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private String itemId;
    private String itemType;
    private Long reporterUserId;
    private String reason;
    private Integer status;
    private String reviewRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

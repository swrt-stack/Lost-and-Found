package com.example.lostandfound.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("lost_item")
public class LostItem {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private String description;
    private String location;
    private LocalDateTime lostTime;
    private String contact;
    private String images;
    private Integer status;
    private LocalDateTime createdAt;
}

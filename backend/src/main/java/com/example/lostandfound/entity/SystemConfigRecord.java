package com.example.lostandfound.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("system_config")
public class SystemConfigRecord {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private String siteName;
    private Integer reviewEnabled;
    private Integer maxImageSize;
    private Integer noticeEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

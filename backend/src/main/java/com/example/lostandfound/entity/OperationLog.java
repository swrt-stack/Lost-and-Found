package com.example.lostandfound.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("operation_log")
public class OperationLog {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long userId;
    private String action;
    private String detail;
    private String ipAddress;
    private LocalDateTime createdAt;
}

package com.example.lostandfound.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("claim_application")
public class ClaimApplication {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long foundItemId;
    private Long applicantUserId;
    private Long ownerUserId;
    private String message;
    private Integer status;
    private String reviewRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

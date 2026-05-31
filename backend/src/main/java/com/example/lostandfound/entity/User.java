package com.example.lostandfound.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("sys_user")
public class User {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String phone;
    private String avatarUrl;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

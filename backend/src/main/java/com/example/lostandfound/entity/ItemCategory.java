package com.example.lostandfound.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

@Data
@Table("item_category")
public class ItemCategory {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private String name;
}

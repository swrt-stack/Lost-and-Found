package com.example.lostandfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class CategoryDTO {

    public record CategoryVO(Long id, String name) {
    }

    @Data
    public static class CreateCategoryRequest {
        @NotBlank(message = "Category name is required")
        private String name;
    }

    public record CategoryActionVO(Long id, String result) {
    }
}

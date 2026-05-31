package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.dto.CategoryDTO;
import com.example.lostandfound.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<Object> list() {
        return ApiResponse.ok(categoryService.listCategories());
    }

    @PostMapping
    public ApiResponse<Object> create(@Valid @RequestBody CategoryDTO.CreateCategoryRequest request) {
        return ApiResponse.ok(categoryService.createCategory(request.getName()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Object> delete(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.deleteCategory(id));
    }
}

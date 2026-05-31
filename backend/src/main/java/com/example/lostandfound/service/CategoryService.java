package com.example.lostandfound.service;

import com.example.lostandfound.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {
    List<CategoryDTO.CategoryVO> listCategories();

    CategoryDTO.CategoryActionVO createCategory(String name);

    CategoryDTO.CategoryActionVO deleteCategory(Long id);
}

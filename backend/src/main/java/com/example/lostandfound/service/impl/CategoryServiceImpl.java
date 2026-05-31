package com.example.lostandfound.service.impl;

import com.example.lostandfound.cache.CacheKeys;
import com.example.lostandfound.cache.RedisJsonCacheService;
import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.dto.CategoryDTO;
import com.example.lostandfound.entity.ItemCategory;
import com.example.lostandfound.mapper.FoundItemMapper;
import com.example.lostandfound.mapper.ItemCategoryMapper;
import com.example.lostandfound.mapper.LostItemMapper;
import com.example.lostandfound.service.CategoryService;
import com.example.lostandfound.service.support.AuditLogSupport;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Duration CATEGORY_LIST_TTL = Duration.ofMinutes(30);

    private final ItemCategoryMapper itemCategoryMapper;
    private final LostItemMapper lostItemMapper;
    private final FoundItemMapper foundItemMapper;
    private final AuditLogSupport auditLogSupport;
    private final RedisJsonCacheService cacheService;

    public CategoryServiceImpl(ItemCategoryMapper itemCategoryMapper, LostItemMapper lostItemMapper,
                               FoundItemMapper foundItemMapper, AuditLogSupport auditLogSupport,
                               RedisJsonCacheService cacheService) {
        this.itemCategoryMapper = itemCategoryMapper;
        this.lostItemMapper = lostItemMapper;
        this.foundItemMapper = foundItemMapper;
        this.auditLogSupport = auditLogSupport;
        this.cacheService = cacheService;
    }

    @Override
    public List<CategoryDTO.CategoryVO> listCategories() {
        return cacheService.getOrLoad(
                CacheKeys.SYSTEM_CATEGORIES,
                CATEGORY_LIST_TTL,
                new com.fasterxml.jackson.core.type.TypeReference<List<CategoryDTO.CategoryVO>>() {
                },
                () -> itemCategoryMapper.selectAll().stream()
                        .sorted(Comparator.comparing(ItemCategory::getId))
                        .map(item -> new CategoryDTO.CategoryVO(item.getId(), item.getName()))
                        .toList()
        );
    }

    @Override
    public CategoryDTO.CategoryActionVO createCategory(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(400, "Category name is required");
        }
        ItemCategory existed = itemCategoryMapper.selectOneByQuery(QueryWrapper.create().where("name = ?", normalized));
        if (existed != null) {
            throw new BusinessException(400, "Category already exists");
        }
        ItemCategory category = new ItemCategory();
        category.setName(normalized);
        itemCategoryMapper.insert(category);
        cacheService.delete(CacheKeys.SYSTEM_CATEGORIES, CacheKeys.SYSTEM_DICT);
        auditLogSupport.record("CREATE_CATEGORY", "Created category " + normalized);
        return new CategoryDTO.CategoryActionVO(category.getId(), "Category created");
    }

    @Override
    public CategoryDTO.CategoryActionVO deleteCategory(Long id) {
        ItemCategory category = itemCategoryMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (category == null) {
            throw new BusinessException(404, "Category not found");
        }
        long lostCount = lostItemMapper.selectCountByQuery(QueryWrapper.create().where("category_id = ?", id));
        long foundCount = foundItemMapper.selectCountByQuery(QueryWrapper.create().where("category_id = ?", id));
        if (lostCount > 0 || foundCount > 0) {
            throw new BusinessException(400, "Category is already in use");
        }
        itemCategoryMapper.deleteById(id);
        cacheService.delete(CacheKeys.SYSTEM_CATEGORIES, CacheKeys.SYSTEM_DICT);
        auditLogSupport.record("DELETE_CATEGORY", "Deleted category " + category.getName());
        return new CategoryDTO.CategoryActionVO(id, "Category deleted");
    }
}

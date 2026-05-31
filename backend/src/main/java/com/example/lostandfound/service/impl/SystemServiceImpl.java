package com.example.lostandfound.service.impl;

import com.example.lostandfound.cache.CacheKeys;
import com.example.lostandfound.cache.RedisJsonCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.example.lostandfound.dto.SystemDTO;
import com.example.lostandfound.entity.Announcement;
import com.example.lostandfound.entity.FoundItem;
import com.example.lostandfound.entity.ItemCategory;
import com.example.lostandfound.entity.LostItem;
import com.example.lostandfound.mapper.AnnouncementMapper;
import com.example.lostandfound.mapper.FoundItemMapper;
import com.example.lostandfound.mapper.ItemCategoryMapper;
import com.example.lostandfound.mapper.LostItemMapper;
import com.example.lostandfound.service.SystemService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemServiceImpl implements SystemService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Duration SYSTEM_DICT_TTL = Duration.ofMinutes(30);
    private static final Duration SYSTEM_OVERVIEW_TTL = Duration.ofMinutes(3);
    private static final Duration SYSTEM_ANNOUNCEMENTS_TTL = Duration.ofMinutes(10);

    private final ItemCategoryMapper itemCategoryMapper;
    private final LostItemMapper lostItemMapper;
    private final FoundItemMapper foundItemMapper;
    private final AnnouncementMapper announcementMapper;
    private final RedisJsonCacheService cacheService;

    public SystemServiceImpl(ItemCategoryMapper itemCategoryMapper, LostItemMapper lostItemMapper,
                             FoundItemMapper foundItemMapper, AnnouncementMapper announcementMapper,
                             RedisJsonCacheService cacheService) {
        this.itemCategoryMapper = itemCategoryMapper;
        this.lostItemMapper = lostItemMapper;
        this.foundItemMapper = foundItemMapper;
        this.announcementMapper = announcementMapper;
        this.cacheService = cacheService;
    }

    @Override
    public SystemDTO.DictVO getDict() {
        return cacheService.getOrLoad(
                CacheKeys.SYSTEM_DICT,
                SYSTEM_DICT_TTL,
                SystemDTO.DictVO.class,
                () -> {
                    List<String> categories = itemCategoryMapper.selectAll().stream().map(ItemCategory::getName).toList();
                    return new SystemDTO.DictVO(
                            categories,
                            List.of("PENDING", "APPROVED", "REJECTED", "COMPLETED"),
                            List.of("PUBLISH", "REVIEW", "MATCH", "CLAIM")
                    );
                }
        );
    }

    @Override
    public SystemDTO.OverviewVO getOverview() {
        return cacheService.getOrLoad(
                CacheKeys.SYSTEM_OVERVIEW,
                SYSTEM_OVERVIEW_TTL,
                SystemDTO.OverviewVO.class,
                () -> {
                    long lostCount = lostItemMapper.selectCountByQuery(QueryWrapper.create());
                    long foundCount = foundItemMapper.selectCountByQuery(QueryWrapper.create());
                    int publishedCount = Math.toIntExact(lostCount + foundCount);
                    long approvedCount = lostItemMapper.selectCountByQuery(QueryWrapper.create().where("status = ?", 1))
                            + foundItemMapper.selectCountByQuery(QueryWrapper.create().where("status = ?", 1));
                    String approvalRate = publishedCount == 0 ? "0%" : Math.round(approvedCount * 100.0 / publishedCount) + "%";

                    List<LostItem> lostItems = lostItemMapper.selectListByQuery(QueryWrapper.create());
                    List<FoundItem> foundItems = foundItemMapper.selectListByQuery(QueryWrapper.create());
                    Map<Long, String> categoryMap = itemCategoryMapper.selectAll().stream()
                            .collect(Collectors.toMap(ItemCategory::getId, ItemCategory::getName, (left, right) -> left));
                    Map<String, Integer> categoryDistribution = new LinkedHashMap<>();
                    Map<String, Integer> statusDistribution = new LinkedHashMap<>();

                    lostItems.forEach(item -> {
                        increment(categoryDistribution, categoryMap.getOrDefault(item.getCategoryId(), "Unknown"));
                        increment(statusDistribution, statusText(item.getStatus()));
                    });
                    foundItems.forEach(item -> {
                        increment(categoryDistribution, categoryMap.getOrDefault(item.getCategoryId(), "Unknown"));
                        increment(statusDistribution, statusText(item.getStatus()));
                    });

                    return new SystemDTO.OverviewVO(
                            publishedCount,
                            approvalRate,
                            categoryDistribution,
                            statusDistribution,
                            latestAnnouncements(5)
                    );
                }
        );
    }

    @Override
    public List<SystemDTO.AnnouncementVO> publicAnnouncements() {
        return cacheService.getOrLoad(
                CacheKeys.SYSTEM_ANNOUNCEMENTS,
                SYSTEM_ANNOUNCEMENTS_TTL,
                new TypeReference<List<SystemDTO.AnnouncementVO>>() {
                },
                () -> latestAnnouncements(Integer.MAX_VALUE)
        );
    }

    @Override
    public List<SystemDTO.AnnouncementVO> latestAnnouncements(int limit) {
        return announcementMapper.selectListByQuery(
                        QueryWrapper.create()
                                .where("status = ?", 1)
                                .orderBy("created_at desc")
                ).stream()
                .limit(limit)
                .map(item -> new SystemDTO.AnnouncementVO(
                        item.getId(),
                        item.getTitle(),
                        item.getContent(),
                        item.getCreatedAt() == null ? "" : item.getCreatedAt().format(FORMATTER)
                ))
                .toList();
    }

    private void increment(Map<String, Integer> map, String key) {
        map.put(key, map.getOrDefault(key, 0) + 1);
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "PENDING";
            case 1 -> "APPROVED";
            case 2 -> "REJECTED";
            case 3 -> "COMPLETED";
            case 4 -> "OFFLINE";
            default -> "UNKNOWN";
        };
    }
}

package com.example.lostandfound.dto;

import java.util.List;
import java.util.Map;

public class SystemDTO {
    public record DictVO(List<String> categories, List<String> itemStatus, List<String> messageTypes) {
    }

    public record AnnouncementVO(Long id, String title, String content, String createdAt) {
    }

    public record OverviewVO(Integer publishedCount, String approvalRate, Map<String, Integer> categoryDistribution,
                             Map<String, Integer> statusDistribution, List<AnnouncementVO> announcements) {
    }
}

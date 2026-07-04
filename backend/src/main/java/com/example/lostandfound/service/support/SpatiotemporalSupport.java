package com.example.lostandfound.service.support;

import com.example.lostandfound.dto.AiDTO;
import com.example.lostandfound.entity.LostItem;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import com.example.lostandfound.dto.AdminDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SpatiotemporalSupport {

    public static final int LOCATION_VOCAB = 2048;
    public static final int TIME_BUCKET_VOCAB = 48;
    public static final int ITEM_TYPE_VOCAB = 8;
    public static final int MAX_SEQ_LEN = 32;

    private SpatiotemporalSupport() {
    }

    public static List<AiDTO.TrajectoryEvent> buildHistory(List<LostItem> items, Map<Integer, String> locationLabels) {
        List<LostItem> ordered = items.stream()
                .filter(item -> item.getLostTime() != null)
                .sorted((left, right) -> left.getLostTime().compareTo(right.getLostTime()))
                .toList();

        int start = Math.max(0, ordered.size() - MAX_SEQ_LEN);
        List<AiDTO.TrajectoryEvent> history = new ArrayList<>();
        for (int index = start; index < ordered.size(); index++) {
            LostItem item = ordered.get(index);
            int locationId = encodeLocationId(item.getLocation());
            int timeBucket = encodeTimeBucket(item.getLostTime());
            int weekday = encodeWeekday(item.getLostTime());
            int itemTypeId = encodeItemTypeId(item.getCategoryId());

            locationLabels.putIfAbsent(locationId, normalizeLocationLabel(item.getLocation()));

            AiDTO.TrajectoryEvent event = new AiDTO.TrajectoryEvent();
            event.setLocationId(locationId);
            event.setTimeBucket(timeBucket);
            event.setWeekday(weekday);
            event.setItemTypeId(itemTypeId);
            history.add(event);
        }
        return history;
    }

    public static Map<Integer, String> buildLocationLabelIndex(List<LostItem> items) {
        Map<Integer, String> labels = new LinkedHashMap<>();
        for (LostItem item : items) {
            String label = normalizeLocationLabel(item.getLocation());
            labels.putIfAbsent(encodeLocationId(item.getLocation()), label);
        }
        return labels;
    }

    public static List<AdminDTO.SpatiotemporalPredictionItemVO> resolveLocationPredictions(
            List<AiDTO.PredictionCandidateVO> modelTopK,
            Map<Integer, String> locationLabels,
            Map<String, Integer> heatmap,
            int topK
    ) {
        List<AdminDTO.SpatiotemporalPredictionItemVO> results = new ArrayList<>();
        Set<Integer> usedIds = new LinkedHashSet<>();

        if (modelTopK != null) {
            for (AiDTO.PredictionCandidateVO item : modelTopK) {
                if (results.size() >= topK) {
                    break;
                }
                String label = locationLabels.get(item.id());
                if (label == null || !usedIds.add(item.id())) {
                    continue;
                }
                results.add(new AdminDTO.SpatiotemporalPredictionItemVO(item.id(), item.score(), label));
            }
        }

        if (results.size() < topK && heatmap != null && !heatmap.isEmpty()) {
            boolean modelMatchedAny = !results.isEmpty();
            int total = heatmap.values().stream().mapToInt(Integer::intValue).sum();
            List<Map.Entry<String, Integer>> ranked = heatmap.entrySet().stream()
                    .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                    .toList();
            for (Map.Entry<String, Integer> entry : ranked) {
                if (results.size() >= topK) {
                    break;
                }
                int locationId = encodeLocationId(entry.getKey());
                if (!usedIds.add(locationId)) {
                    continue;
                }
                double score = total > 0 ? entry.getValue() * 1.0 / total : 0;
                String label = modelMatchedAny ? entry.getKey() + "（历史参考）" : entry.getKey();
                results.add(new AdminDTO.SpatiotemporalPredictionItemVO(locationId, score, label));
            }
        }

        return results;
    }

    public static Map<String, Integer> buildLocationHeatmap(List<LostItem> items) {
        Map<String, Integer> heatmap = new LinkedHashMap<>();
        for (LostItem item : items) {
            String label = normalizeLocationLabel(item.getLocation());
            heatmap.merge(label, 1, Integer::sum);
        }
        return heatmap;
    }

    public static int encodeLocationId(String location) {
        String normalized = location == null ? "" : location.trim();
        if (normalized.isEmpty()) {
            return 0;
        }
        return Math.floorMod(normalized.hashCode(), LOCATION_VOCAB);
    }

    public static int encodeTimeBucket(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        int half = dateTime.getMinute() >= 30 ? 1 : 0;
        int bucket = hour * 2 + half;
        return Math.min(bucket, TIME_BUCKET_VOCAB - 1);
    }

    public static int encodeWeekday(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        return dayOfWeek.getValue() - 1;
    }

    public static int encodeItemTypeId(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return 0;
        }
        return (int) (categoryId % ITEM_TYPE_VOCAB);
    }

    public static String timeBucketLabel(int bucketId) {
        int safe = Math.max(0, Math.min(bucketId, TIME_BUCKET_VOCAB - 1));
        int hour = safe / 2;
        if (safe % 2 == 0) {
            return String.format("%02d:00-%02d:29", hour, hour);
        }
        return String.format("%02d:30-%02d:59", hour, hour);
    }

    public static String weekdayLabel(int weekday) {
        return switch (weekday) {
            case 0 -> "周一";
            case 1 -> "周二";
            case 2 -> "周三";
            case 3 -> "周四";
            case 4 -> "周五";
            case 5 -> "周六";
            case 6 -> "周日";
            default -> "未知";
        };
    }

    private static String normalizeLocationLabel(String location) {
        String normalized = location == null ? "" : location.trim();
        return normalized.isEmpty() ? "未填写地点" : normalized;
    }
}

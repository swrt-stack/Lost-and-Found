package com.example.lostandfound.service.impl;

import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.dto.AiDTO;
import com.example.lostandfound.entity.FoundItem;
import com.example.lostandfound.entity.ItemCategory;
import com.example.lostandfound.entity.LostItem;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.FoundItemMapper;
import com.example.lostandfound.mapper.ItemCategoryMapper;
import com.example.lostandfound.mapper.LostItemMapper;
import com.example.lostandfound.mapper.UserMapper;
import com.example.lostandfound.service.AiService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements AiService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final LostItemMapper lostItemMapper;
    private final FoundItemMapper foundItemMapper;
    private final UserMapper userMapper;
    private final ItemCategoryMapper itemCategoryMapper;

    @Value("${app.ai.image-search.base-url:http://127.0.0.1:8090}")
    private String imageSearchBaseUrl;

    @Value("${app.ai.spatiotemporal.base-url:http://127.0.0.1:8091}")
    private String spatiotemporalBaseUrl;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public AiServiceImpl(RestTemplate restTemplate, LostItemMapper lostItemMapper, FoundItemMapper foundItemMapper,
                         UserMapper userMapper, ItemCategoryMapper itemCategoryMapper) {
        this.restTemplate = restTemplate;
        this.lostItemMapper = lostItemMapper;
        this.foundItemMapper = foundItemMapper;
        this.userMapper = userMapper;
        this.itemCategoryMapper = itemCategoryMapper;
    }

    @Override
    public AiDTO.AiGatewayStatusVO gatewayStatus() {
        return new AiDTO.AiGatewayStatusVO(
                probe(() -> imageSearchStatus()),
                probe(() -> spatiotemporalStatus())
        );
    }

    @Override
    public AiDTO.ImageSearchStatusVO imageSearchStatus() {
        return getForObject(imageSearchBaseUrl + "/index/status", AiDTO.ImageSearchStatusVO.class, "Image search service");
    }

    @Override
    public AiDTO.ImageSearchStatusVO rebuildImageIndex(String imageRoot) {
        Map<String, Object> body = imageRoot == null || imageRoot.isBlank()
                ? Map.of()
                : Map.of("image_root", imageRoot.trim());
        return postJson(imageSearchBaseUrl + "/index/rebuild", body, AiDTO.ImageSearchStatusVO.class, "Image search service");
    }

    @Override
    public AiDTO.ImageSearchResponseVO searchImageByPath(String imagePath, Integer topK) {
        Map<String, Object> body = Map.of(
                "image_path", imagePath,
                "top_k", topK == null ? 10 : topK
        );
        return postJson(imageSearchBaseUrl + "/search/by-path", body, AiDTO.ImageSearchResponseVO.class, "Image search service");
    }

    @Override
    public AiDTO.ImageSearchResponseVO searchImageByUpload(MultipartFile file, Integer topK) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            String url = imageSearchBaseUrl + "/search/by-upload?top_k=" + (topK == null ? 10 : topK);
            ResponseEntity<AiDTO.ImageSearchResponseVO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    AiDTO.ImageSearchResponseVO.class
            );
            return response.getBody();
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to read uploaded image");
        } catch (RestClientException exception) {
            throw new BusinessException(503, "Image search service is unavailable");
        }
    }

    @Override
    public AiDTO.ImageSearchResponseVO searchImageByText(String text, Integer topK) {
        Map<String, Object> body = Map.of(
                "text", text,
                "top_k", topK == null ? 10 : topK
        );
        return postJson(imageSearchBaseUrl + "/search/by-text", body, AiDTO.ImageSearchResponseVO.class, "Image search service");
    }

    @Override
    public AiDTO.SmartMatchResponseVO smartMatch(MultipartFile file, String description, Integer topK) {
        String queryText = description == null ? "" : description.trim();
        if ((file == null || file.isEmpty()) && queryText.isBlank()) {
            throw new BusinessException(400, "Image or description is required");
        }

        List<LostItem> lostItems = lostItemMapper.selectListByQuery(QueryWrapper.create().where("status = ?", 1));
        List<FoundItem> foundItems = foundItemMapper.selectListByQuery(QueryWrapper.create().where("status = ?", 1));
        if (lostItems.isEmpty() && foundItems.isEmpty()) {
            return new AiDTO.SmartMatchResponseVO(queryText, 0, List.of());
        }

        Set<Long> userIds = new LinkedHashSet<>();
        Set<Long> categoryIds = new LinkedHashSet<>();
        lostItems.forEach(item -> {
            userIds.add(item.getUserId());
            if (item.getCategoryId() != null) {
                categoryIds.add(item.getCategoryId());
            }
        });
        foundItems.forEach(item -> {
            userIds.add(item.getUserId());
            if (item.getCategoryId() != null) {
                categoryIds.add(item.getCategoryId());
            }
        });
        Map<Long, String> users = usernamesByIds(userIds);
        Map<Long, String> categories = categoryNamesByIds(categoryIds);

        List<Map<String, Object>> candidates = new ArrayList<>();
        for (LostItem item : lostItems) {
            candidates.add(buildMatchCandidate(
                    "LOST-" + item.getId(),
                    item.getTitle(),
                    item.getCategoryId(),
                    item.getDescription(),
                    item.getUserId(),
                    item.getLocation(),
                    item.getLostTime() == null ? "" : item.getLostTime().format(FORMATTER),
                    item.getImages(),
                    users,
                    categories
            ));
        }
        for (FoundItem item : foundItems) {
            candidates.add(buildMatchCandidate(
                    "FOUND-" + item.getId(),
                    item.getTitle(),
                    item.getCategoryId(),
                    item.getDescription(),
                    item.getUserId(),
                    item.getLocation(),
                    item.getFoundTime() == null ? "" : item.getFoundTime().format(FORMATTER),
                    item.getImages(),
                    users,
                    categories
            ));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("query_text", queryText);
            body.add("top_k", topK == null ? 10 : topK);
            body.add("candidates_json", objectToJson(candidates));

            if (file != null && !file.isEmpty()) {
                ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                };
                body.add("file", resource);
            }

            ResponseEntity<Map> response = restTemplate.exchange(
                    imageSearchBaseUrl + "/match/found-items",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            return convertSmartMatchResponse(response.getBody(), queryText);
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to read uploaded image");
        } catch (RestClientException exception) {
            throw new BusinessException(503, "Image search service is unavailable");
        }
    }

    @Override
    public AiDTO.SpatiotemporalStatusVO spatiotemporalStatus() {
        return getForObject(spatiotemporalBaseUrl + "/model/status", AiDTO.SpatiotemporalStatusVO.class, "Spatiotemporal service");
    }

    @Override
    public AiDTO.SpatiotemporalPredictionVO predictSpatiotemporal(AiDTO.SpatiotemporalPredictRequest request) {
        List<Map<String, Object>> history = request.getHistory().stream()
                .map(item -> Map.<String, Object>of(
                        "location_id", item.getLocationId(),
                        "time_bucket", item.getTimeBucket(),
                        "weekday", item.getWeekday(),
                        "item_type_id", item.getItemTypeId() == null ? 0 : item.getItemTypeId()
                ))
                .toList();

        Map<String, Object> body = Map.of(
                "history", history,
                "top_k", request.getTopK() == null ? 5 : request.getTopK()
        );
        return postJson(spatiotemporalBaseUrl + "/predict", body, AiDTO.SpatiotemporalPredictionVO.class, "Spatiotemporal service");
    }

    private <T> T getForObject(String url, Class<T> type, String serviceName) {
        try {
            return restTemplate.getForObject(url, type);
        } catch (RestClientException exception) {
            throw new BusinessException(503, serviceName + " is unavailable");
        }
    }

    private <T> T postJson(String url, Object body, Class<T> type, String serviceName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, type);
            return response.getBody();
        } catch (RestClientException exception) {
            throw new BusinessException(503, serviceName + " is unavailable");
        }
    }

    private AiDTO.ServiceStatusVO probe(ProbeSupplier supplier) {
        try {
            Object detail = supplier.get();
            return new AiDTO.ServiceStatusVO(true, "UP", detail);
        } catch (BusinessException exception) {
            return new AiDTO.ServiceStatusVO(false, exception.getMessage(), null);
        } catch (Exception exception) {
            return new AiDTO.ServiceStatusVO(false, "Service probe failed", null);
        }
    }

    @FunctionalInterface
    private interface ProbeSupplier {
        Object get();
    }

    private Map<String, Object> buildMatchCandidate(String itemId, String title, Long categoryId, String description,
                                                    Long userId, String location, String time, String images,
                                                    Map<Long, String> users, Map<Long, String> categories) {
        return Map.of(
                "item_id", itemId,
                "title", appendCategory(title, categories.get(categoryId)),
                "description", description == null ? "" : description,
                "publisher", users.getOrDefault(userId, "unknown"),
                "location", location == null ? "" : location,
                "time", time,
                "image_paths", resolveImagePaths(images),
                "image_url", firstImageUrl(images)
        );
    }

    private String appendCategory(String title, String category) {
        if (category == null || category.isBlank()) {
            return title == null ? "" : title;
        }
        return (title == null ? "" : title) + " [" + category + "]";
    }

    private String toStorageRelativePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }
        String path = rawPath.trim();
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex >= 0) {
            path = path.substring(0, fragmentIndex);
        }
        return path.startsWith("/uploads/") ? path.substring("/uploads/".length()) : path;
    }

    private List<String> resolveImagePaths(String rawImages) {
        if (rawImages == null || rawImages.isBlank()) {
            return List.of();
        }
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        List<String> paths = new ArrayList<>();
        for (String item : rawImages.split(",")) {
            String relative = toStorageRelativePath(item);
            if (relative.isBlank()) {
                continue;
            }
            paths.add(root.resolve(relative).normalize().toString());
        }
        return paths;
    }

    private String firstImageUrl(String rawImages) {
        if (rawImages == null || rawImages.isBlank()) {
            return "";
        }
        for (String item : rawImages.split(",")) {
            String normalized = item == null ? "" : item.trim();
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String objectToJson(Object object) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(object);
        } catch (Exception exception) {
            throw new BusinessException(500, "Failed to serialize smart match payload");
        }
    }

    private Map<Long, String> usernamesByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (left, right) -> left));
    }

    private Map<Long, String> categoryNamesByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return itemCategoryMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(ItemCategory::getId, ItemCategory::getName, (left, right) -> left));
    }

    private QueryWrapper whereIn(String column, Set<Long> ids) {
        String placeholders = String.join(", ", Collections.nCopies(ids.size(), "?"));
        return QueryWrapper.create().where(column + " in (" + placeholders + ")", ids.toArray());
    }

    @SuppressWarnings("unchecked")
    private AiDTO.SmartMatchResponseVO convertSmartMatchResponse(Map<String, Object> response, String fallbackQueryText) {
        if (response == null) {
            throw new BusinessException(500, "Empty smart match response");
        }
        List<Map<String, Object>> rawResults = (List<Map<String, Object>>) response.getOrDefault("results", List.of());
        List<AiDTO.SmartMatchCandidateVO> results = rawResults.stream()
                .map(item -> new AiDTO.SmartMatchCandidateVO(
                        (String) item.get("item_id"),
                        (String) item.get("title"),
                        (String) item.get("description"),
                        (String) item.get("publisher"),
                        (String) item.get("location"),
                        (String) item.get("time"),
                        (String) item.get("image_url"),
                        toDouble(item.get("final_score")),
                        toDouble(item.get("image_to_image_score")),
                        toDouble(item.get("image_to_text_score")),
                        toDouble(item.get("text_to_image_score")),
                        toDouble(item.get("text_to_text_score"))
                ))
                .toList();
        return new AiDTO.SmartMatchResponseVO(
                (String) response.getOrDefault("query_text", fallbackQueryText),
                toInteger(response.get("total_candidates")),
                results
        );
    }

    private Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private Integer toInteger(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}

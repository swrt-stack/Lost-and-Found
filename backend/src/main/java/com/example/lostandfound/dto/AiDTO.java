package com.example.lostandfound.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

public class AiDTO {

    @Data
    public static class ImageIndexRebuildRequest {
        private String imageRoot;
    }

    @Data
    public static class ImageSearchByPathRequest {
        @NotBlank(message = "Image path is required")
        private String imagePath;

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 100, message = "topK must be at most 100")
        private Integer topK = 10;
    }

    @Data
    public static class ImageSearchByTextRequest {
        @NotBlank(message = "Search text is required")
        private String text;

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 100, message = "topK must be at most 100")
        private Integer topK = 10;
    }

    @Data
    public static class SmartMatchRequest {
        private String description;

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK must be at most 20")
        private Integer topK = 10;
    }

    @Data
    public static class SpatiotemporalPredictRequest {
        @Valid
        @NotEmpty(message = "History cannot be empty")
        private List<TrajectoryEvent> history;

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK must be at most 20")
        private Integer topK = 5;
    }

    @Data
    public static class TrajectoryEvent {
        @NotNull(message = "locationId is required")
        private Integer locationId;

        @NotNull(message = "timeBucket is required")
        private Integer timeBucket;

        @NotNull(message = "weekday is required")
        private Integer weekday;

        private Integer itemTypeId = 0;
    }

    public record ServiceStatusVO(Boolean available, String message, Object detail) {
    }

    public record AiGatewayStatusVO(ServiceStatusVO imageSearch, ServiceStatusVO spatiotemporal) {
    }

    public record ImageSearchStatusVO(
            @JsonProperty("model_dir") String modelDir,
            @JsonProperty("image_root") String imageRoot,
            @JsonProperty("indexed_count") Integer indexedCount,
            @JsonProperty("ready") Boolean ready) {
    }

    public record ImageSearchResultItemVO(
            @JsonProperty("rank") Integer rank,
            @JsonProperty("score") Double score,
            @JsonProperty("filename") String filename,
            @JsonProperty("absolute_path") String absolutePath,
            @JsonProperty("relative_path") String relativePath,
            @JsonProperty("url_path") String urlPath) {
    }

    public record ImageSearchResponseVO(
            @JsonProperty("query") String query,
            @JsonProperty("total_indexed") Integer totalIndexed,
            @JsonProperty("results") List<ImageSearchResultItemVO> results) {
    }

    public record SmartMatchCandidateVO(
            String itemId,
            String title,
            String description,
            String publisher,
            String location,
            String time,
            String imageUrl,
            Double finalScore,
            Double imageToImageScore,
            Double imageToTextScore,
            Double textToImageScore,
            Double textToTextScore) {
    }

    public record SmartMatchResponseVO(
            String queryText,
            Integer totalCandidates,
            List<SmartMatchCandidateVO> results) {
    }

    public record SpatiotemporalStatusVO(
            @JsonProperty("checkpoint_path") String checkpointPath,
            @JsonProperty("checkpoint_loaded") Boolean checkpointLoaded,
            @JsonProperty("device") String device,
            @JsonProperty("max_seq_len") Integer maxSeqLen,
            @JsonProperty("location_vocab") Integer locationVocab,
            @JsonProperty("time_bucket_vocab") Integer timeBucketVocab,
            @JsonProperty("weekday_vocab") Integer weekdayVocab,
            @JsonProperty("item_type_vocab") Integer itemTypeVocab) {
    }

    public record PredictionCandidateVO(
            @JsonProperty("id") Integer id,
            @JsonProperty("score") Double score) {
    }

    public record SpatiotemporalPredictionVO(
            @JsonProperty("history_length") Integer historyLength,
            @JsonProperty("model_ready") Boolean modelReady,
            @JsonProperty("checkpoint_loaded") Boolean checkpointLoaded,
            @JsonProperty("next_location_topk") List<PredictionCandidateVO> nextLocationTopk,
            @JsonProperty("next_time_bucket_topk") List<PredictionCandidateVO> nextTimeBucketTopk) {
    }
}

package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.dto.AiDTO;
import com.example.lostandfound.ratelimit.RateLimit;
import com.example.lostandfound.ratelimit.RateLimitTarget;
import com.example.lostandfound.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/status")
    public ApiResponse<Object> gatewayStatus() {
        return ApiResponse.ok(aiService.gatewayStatus());
    }

    @GetMapping("/image-search/status")
    public ApiResponse<Object> imageSearchStatus() {
        return ApiResponse.ok(aiService.imageSearchStatus());
    }

    @PostMapping("/image-search/rebuild")
    @RateLimit(key = "ai:image-rebuild",
            target = RateLimitTarget.GLOBAL, message = "Image index rebuild is already running too frequently")
    public ApiResponse<Object> rebuildImageIndex(@RequestBody(required = false) AiDTO.ImageIndexRebuildRequest request) {
        return ApiResponse.ok(aiService.rebuildImageIndex(request == null ? null : request.getImageRoot()));
    }

    @PostMapping("/image-search/by-path")
    @RateLimit(key = "ai:image-by-path",
            message = "Image search requests are too frequent, please try again later")
    public ApiResponse<Object> searchByPath(@Valid @RequestBody AiDTO.ImageSearchByPathRequest request) {
        return ApiResponse.ok(aiService.searchImageByPath(request.getImagePath(), request.getTopK()));
    }

    @PostMapping(value = "/image-search/by-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(key = "ai:image-by-upload",
            message = "Image upload search requests are too frequent, please try again later")
    public ApiResponse<Object> searchByUpload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "topK", required = false) Integer topK) {
        return ApiResponse.ok(aiService.searchImageByUpload(file, topK));
    }

    @PostMapping("/image-search/by-text")
    @RateLimit(key = "ai:image-by-text",
            message = "Text image search requests are too frequent, please try again later")
    public ApiResponse<Object> searchByText(@Valid @RequestBody AiDTO.ImageSearchByTextRequest request) {
        return ApiResponse.ok(aiService.searchImageByText(request.getText(), request.getTopK()));
    }

    @PostMapping(value = "/smart-match", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(key = "ai:smart-match",
            message = "Smart match requests are too frequent, please try again later")
    public ApiResponse<Object> smartMatch(@RequestParam(value = "file", required = false) MultipartFile file,
                                          @RequestParam(value = "description", required = false) String description,
                                          @RequestParam(value = "topK", required = false) Integer topK) {
        return ApiResponse.ok(aiService.smartMatch(file, description, topK));
    }

    @GetMapping("/spatiotemporal/status")
    public ApiResponse<Object> spatiotemporalStatus() {
        return ApiResponse.ok(aiService.spatiotemporalStatus());
    }

    @PostMapping("/spatiotemporal/predict")
    @RateLimit(key = "ai:spatiotemporal-predict",
            message = "Prediction requests are too frequent, please try again later")
    public ApiResponse<Object> predictSpatiotemporal(@Valid @RequestBody AiDTO.SpatiotemporalPredictRequest request) {
        return ApiResponse.ok(aiService.predictSpatiotemporal(request));
    }
}

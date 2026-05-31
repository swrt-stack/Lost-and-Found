package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.dto.ConfigDTO;
import com.example.lostandfound.idempotency.IdempotentAction;
import com.example.lostandfound.ratelimit.RateLimit;
import com.example.lostandfound.service.ConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ConfigService configService;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public UploadController(ConfigService configService) {
        this.configService = configService;
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(key = "upload:image",
            message = "Image upload is too frequent, please try again later")
    @IdempotentAction(key = "upload:image", ttlSeconds = 8,
            message = "Duplicate image upload detected, please wait a moment")
    public ApiResponse<Map<String, Object>> uploadImage(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "Uploaded file cannot be empty");
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename());
        String lowerName = filename.toLowerCase(Locale.ROOT);
        if (!(lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png"))) {
            throw new BusinessException(400, "Only jpg, jpeg and png images are supported");
        }

        ConfigDTO.SystemConfigVO config = configService.getSystemConfig();
        long maxBytes = (long) config.maxImageSize() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(400, "Image size exceeds configured limit");
        }

        String extension = lowerName.substring(lowerName.lastIndexOf('.'));
        String storedName = FORMATTER.format(LocalDateTime.now()) + "-" + UUID.randomUUID().toString().replace("-", "") + extension;
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = uploadPath.resolve(storedName);

        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to store image");
        }

        return ApiResponse.ok(Map.of(
                "filename", storedName,
                "url", "/uploads/" + storedName,
                "size", file.getSize()
        ));
    }
}

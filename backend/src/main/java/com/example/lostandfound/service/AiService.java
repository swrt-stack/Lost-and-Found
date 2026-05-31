package com.example.lostandfound.service;

import com.example.lostandfound.dto.AiDTO;
import org.springframework.web.multipart.MultipartFile;

public interface AiService {
    AiDTO.AiGatewayStatusVO gatewayStatus();

    AiDTO.ImageSearchStatusVO imageSearchStatus();

    AiDTO.ImageSearchStatusVO rebuildImageIndex(String imageRoot);

    AiDTO.ImageSearchResponseVO searchImageByPath(String imagePath, Integer topK);

    AiDTO.ImageSearchResponseVO searchImageByUpload(MultipartFile file, Integer topK);

    AiDTO.ImageSearchResponseVO searchImageByText(String text, Integer topK);

    AiDTO.SmartMatchResponseVO smartMatch(MultipartFile file, String description, Integer topK);

    AiDTO.SpatiotemporalStatusVO spatiotemporalStatus();

    AiDTO.SpatiotemporalPredictionVO predictSpatiotemporal(AiDTO.SpatiotemporalPredictRequest request);
}

package com.example.lostandfound.service;

import com.example.lostandfound.dto.ConfigDTO;

public interface ConfigService {
    ConfigDTO.SystemConfigVO getSystemConfig();

    ConfigDTO.SystemConfigVO updateSystemConfig(ConfigDTO.UpdateSystemConfigRequest request);
}

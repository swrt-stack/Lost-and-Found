package com.example.lostandfound.service.impl;

import com.example.lostandfound.cache.CacheKeys;
import com.example.lostandfound.cache.RedisJsonCacheService;
import com.example.lostandfound.dto.ConfigDTO;
import com.example.lostandfound.entity.SystemConfigRecord;
import com.example.lostandfound.mapper.SystemConfigMapper;
import com.example.lostandfound.service.ConfigService;
import com.example.lostandfound.service.support.AuditLogSupport;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ConfigServiceImpl implements ConfigService {

    private static final long DEFAULT_ID = 1L;
    private static final Duration SYSTEM_CONFIG_TTL = Duration.ofMinutes(30);

    private final SystemConfigMapper systemConfigMapper;
    private final AuditLogSupport auditLogSupport;
    private final RedisJsonCacheService cacheService;

    public ConfigServiceImpl(SystemConfigMapper systemConfigMapper,
                             AuditLogSupport auditLogSupport,
                             RedisJsonCacheService cacheService) {
        this.systemConfigMapper = systemConfigMapper;
        this.auditLogSupport = auditLogSupport;
        this.cacheService = cacheService;
    }

    @Override
    public ConfigDTO.SystemConfigVO getSystemConfig() {
        return cacheService.getOrLoad(
                CacheKeys.SYSTEM_CONFIG,
                SYSTEM_CONFIG_TTL,
                ConfigDTO.SystemConfigVO.class,
                () -> toVO(ensureConfig())
        );
    }

    @Override
    public ConfigDTO.SystemConfigVO updateSystemConfig(ConfigDTO.UpdateSystemConfigRequest request) {
        SystemConfigRecord config = ensureConfig();
        config.setSiteName(request.getSiteName().trim());
        config.setReviewEnabled(Boolean.TRUE.equals(request.getReviewEnabled()) ? 1 : 0);
        config.setMaxImageSize(request.getMaxImageSize());
        config.setNoticeEnabled(Boolean.TRUE.equals(request.getNoticeEnabled()) ? 1 : 0);
        systemConfigMapper.update(config);
        cacheService.delete(CacheKeys.SYSTEM_CONFIG);
        auditLogSupport.record("UPDATE_CONFIG", "Updated system configuration");
        return toVO(config);
    }

    private SystemConfigRecord ensureConfig() {
        SystemConfigRecord config = systemConfigMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", DEFAULT_ID));
        if (config != null) {
            return config;
        }
        config = new SystemConfigRecord();
        LocalDateTime now = LocalDateTime.now();
        config.setId(DEFAULT_ID);
        config.setSiteName("Campus Lost and Found");
        config.setReviewEnabled(1);
        config.setMaxImageSize(5);
        config.setNoticeEnabled(1);
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        systemConfigMapper.insert(config);
        return config;
    }

    private ConfigDTO.SystemConfigVO toVO(SystemConfigRecord config) {
        return new ConfigDTO.SystemConfigVO(
                config.getSiteName(),
                config.getReviewEnabled() != null && config.getReviewEnabled() == 1,
                config.getMaxImageSize(),
                config.getNoticeEnabled() != null && config.getNoticeEnabled() == 1
        );
    }
}

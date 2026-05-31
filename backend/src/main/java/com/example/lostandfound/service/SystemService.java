package com.example.lostandfound.service;

import com.example.lostandfound.dto.SystemDTO;

import java.util.List;

public interface SystemService {
    SystemDTO.DictVO getDict();

    SystemDTO.OverviewVO getOverview();

    List<SystemDTO.AnnouncementVO> publicAnnouncements();

    List<SystemDTO.AnnouncementVO> latestAnnouncements(int limit);
}

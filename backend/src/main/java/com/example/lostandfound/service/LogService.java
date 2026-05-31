package com.example.lostandfound.service;

import com.example.lostandfound.dto.LogDTO;

import java.util.List;

public interface LogService {
    List<LogDTO.LogVO> listLogs();
}

package com.example.lostandfound.service.impl;

import com.example.lostandfound.dto.LogDTO;
import com.example.lostandfound.entity.OperationLog;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.OperationLogMapper;
import com.example.lostandfound.mapper.UserMapper;
import com.example.lostandfound.service.LogService;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LogServiceImpl implements LogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    public LogServiceImpl(OperationLogMapper operationLogMapper, UserMapper userMapper) {
        this.operationLogMapper = operationLogMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<LogDTO.LogVO> listLogs() {
        Map<Long, String> users = userMapper.selectAll().stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (left, right) -> left));
        return operationLogMapper.selectListByQuery(
                        com.mybatisflex.core.query.QueryWrapper.create().orderBy("created_at desc")
                ).stream()
                .map(item -> new LogDTO.LogVO(
                        item.getAction(),
                        item.getDetail(),
                        item.getUserId() == null ? "system" : users.getOrDefault(item.getUserId(), "unknown"),
                        item.getIpAddress(),
                        item.getCreatedAt() == null ? "" : item.getCreatedAt().format(FORMATTER)
                ))
                .toList();
    }
}

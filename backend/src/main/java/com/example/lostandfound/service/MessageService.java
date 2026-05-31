package com.example.lostandfound.service;

import com.example.lostandfound.dto.MessageDTO;

import java.util.List;

public interface MessageService {
    List<MessageDTO.MessageVO> listMessages();

    MessageDTO.MessageSummaryVO summary();

    MessageDTO.MessageActionVO markRead(Long id);

    MessageDTO.BatchActionVO markAllRead();
}

package com.example.lostandfound.service;

import com.example.lostandfound.dto.ChatDTO;

import java.util.List;

public interface ChatService {
    List<ChatDTO.ChatConversationVO> listConversations();

    ChatDTO.ChatSummaryVO summary();

    List<ChatDTO.ChatContactVO> listContacts(String itemId);

    ChatDTO.ChatThreadVO getThread(String itemId, Long counterpartUserId);

    ChatDTO.ChatActionVO sendMessage(String itemId, Long counterpartUserId, String content);
}

package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.service.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ApiResponse<Object> conversations() {
        return ApiResponse.ok(chatService.listConversations());
    }

    @GetMapping("/summary")
    public ApiResponse<Object> summary() {
        return ApiResponse.ok(chatService.summary());
    }
}

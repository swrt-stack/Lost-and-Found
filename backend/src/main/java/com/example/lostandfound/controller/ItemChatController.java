package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.dto.ChatDTO;
import com.example.lostandfound.ratelimit.RateLimit;
import com.example.lostandfound.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items/{itemId}/chats")
public class ItemChatController {

    private final ChatService chatService;

    public ItemChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/contacts")
    public ApiResponse<Object> contacts(@PathVariable String itemId) {
        return ApiResponse.ok(chatService.listContacts(itemId));
    }

    @GetMapping("/messages")
    public ApiResponse<Object> thread(@PathVariable String itemId,
                                      @RequestParam(required = false) Long counterpartUserId) {
        return ApiResponse.ok(chatService.getThread(itemId, counterpartUserId));
    }

    @PostMapping("/messages")
    @RateLimit(key = "chat:send-message",
            message = "Messages are being sent too frequently, please slow down")
    public ApiResponse<Object> send(@PathVariable String itemId,
                                    @Valid @RequestBody ChatDTO.SendMessageRequest request) {
        return ApiResponse.ok(chatService.sendMessage(itemId, request.getCounterpartUserId(), request.getContent()));
    }
}

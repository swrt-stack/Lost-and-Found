package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<Object> list() {
        return ApiResponse.ok(messageService.listMessages());
    }

    @GetMapping("/summary")
    public ApiResponse<Object> summary() {
        return ApiResponse.ok(messageService.summary());
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Object> markRead(@PathVariable Long id) {
        return ApiResponse.ok(messageService.markRead(id));
    }

    @PostMapping("/read-all")
    public ApiResponse<Object> markAllRead() {
        return ApiResponse.ok(messageService.markAllRead());
    }
}

package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.dto.ItemDTO;
import com.example.lostandfound.idempotency.IdempotentAction;
import com.example.lostandfound.ratelimit.RateLimit;
import com.example.lostandfound.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping("/lost")
    @RateLimit(key = "item:create-lost",
            message = "Lost item submissions are too frequent, please try again later")
    @IdempotentAction(key = "item:create-lost", ttlSeconds = 10,
            message = "Duplicate lost item submission detected, please do not submit repeatedly")
    public ApiResponse<Object> createLost(@Valid @RequestBody ItemDTO.CreateItemRequest request) {
        return ApiResponse.ok(itemService.createLost(request));
    }

    @PostMapping("/found")
    @RateLimit(key = "item:create-found",
            message = "Found item submissions are too frequent, please try again later")
    @IdempotentAction(key = "item:create-found", ttlSeconds = 10,
            message = "Duplicate found item submission detected, please do not submit repeatedly")
    public ApiResponse<Object> createFound(@Valid @RequestBody ItemDTO.CreateItemRequest request) {
        return ApiResponse.ok(itemService.createFound(request));
    }

    @GetMapping("/search")
    public ApiResponse<Object> search(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String location,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(itemService.search(keyword, location, type, categoryId));
    }

    @GetMapping("/keyword-search")
    public ApiResponse<Object> keywordSearch(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(itemService.keywordSearch(keyword, type, categoryId));
    }

    @GetMapping("/mine")
    public ApiResponse<Object> mine() {
        return ApiResponse.ok(itemService.myItems());
    }

    @PutMapping("/{itemId}")
    public ApiResponse<Object> updateItem(@PathVariable String itemId, @Valid @RequestBody ItemDTO.UpdateItemRequest request) {
        return ApiResponse.ok(itemService.updateItem(itemId, request));
    }

    @GetMapping("/claims")
    public ApiResponse<Object> myClaims() {
        return ApiResponse.ok(itemService.myClaims());
    }

    @PostMapping("/{itemId}/offline")
    public ApiResponse<Object> offline(@PathVariable String itemId) {
        return ApiResponse.ok(itemService.offlineItem(itemId));
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Object> deleteItem(@PathVariable String itemId) {
        return ApiResponse.ok(itemService.deleteItem(itemId));
    }

    @PostMapping("/{itemId}/report")
    @RateLimit(key = "item:report",
            message = "Reports are too frequent, please try again later")
    @IdempotentAction(key = "item:report", ttlSeconds = 10,
            message = "Duplicate report submission detected, please do not submit repeatedly")
    public ApiResponse<Object> report(@PathVariable String itemId, @Valid @RequestBody ItemDTO.ReportRequest request) {
        return ApiResponse.ok(itemService.reportItem(itemId, request.getReason()));
    }

    @PostMapping("/{itemId}/claim")
    @RateLimit(key = "item:claim",
            message = "Claim requests are too frequent, please try again later")
    @IdempotentAction(key = "item:claim", ttlSeconds = 10,
            message = "Duplicate claim submission detected, please do not submit repeatedly")
    public ApiResponse<Object> claim(@PathVariable String itemId, @Valid @RequestBody ItemDTO.ClaimRequest request) {
        return ApiResponse.ok(itemService.claimFoundItem(itemId, request.getMessage()));
    }

    @PostMapping("/claims/{claimId}/approve")
    public ApiResponse<Object> approveClaim(@PathVariable Long claimId, @RequestBody(required = false) ItemDTO.ClaimReviewRequest request) {
        return ApiResponse.ok(itemService.approveClaim(claimId, request == null ? null : request.getRemark()));
    }

    @PostMapping("/claims/{claimId}/reject")
    public ApiResponse<Object> rejectClaim(@PathVariable Long claimId, @RequestBody(required = false) ItemDTO.ClaimReviewRequest request) {
        return ApiResponse.ok(itemService.rejectClaim(claimId, request == null ? null : request.getRemark()));
    }

    @PostMapping("/{itemId}/complete")
    public ApiResponse<Object> complete(@PathVariable String itemId) {
        return ApiResponse.ok(itemService.completeItem(itemId));
    }
}

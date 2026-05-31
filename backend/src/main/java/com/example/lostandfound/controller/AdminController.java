package com.example.lostandfound.controller;

import com.example.lostandfound.common.ApiResponse;
import com.example.lostandfound.dto.AdminDTO;
import com.example.lostandfound.dto.ItemDTO;
import com.example.lostandfound.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Object> dashboard() {
        return ApiResponse.ok(adminService.getDashboard());
    }

    @GetMapping("/reviews")
    public ApiResponse<Object> reviews() {
        return ApiResponse.ok(adminService.reviewList());
    }

    @GetMapping("/review-history")
    public ApiResponse<Object> reviewHistory() {
        return ApiResponse.ok(adminService.reviewHistory());
    }

    @GetMapping("/claims")
    public ApiResponse<Object> claims() {
        return ApiResponse.ok(adminService.claims());
    }

    @GetMapping("/lost-items")
    public ApiResponse<Object> lostItems(@org.springframework.web.bind.annotation.RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminService.lostItems(keyword));
    }

    @GetMapping("/found-items")
    public ApiResponse<Object> foundItems(@org.springframework.web.bind.annotation.RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminService.foundItems(keyword));
    }

    @GetMapping("/items/{id}")
    public ApiResponse<Object> itemDetail(@PathVariable String id) {
        return ApiResponse.ok(adminService.itemDetail(id));
    }

    @PostMapping("/claims/{id}/approve")
    public ApiResponse<Object> approveClaim(@PathVariable Long id, @RequestBody(required = false) AdminDTO.ReportReviewRequest request) {
        return ApiResponse.ok(adminService.approveClaim(id, request == null ? null : request.getRemark()));
    }

    @PostMapping("/claims/{id}/reject")
    public ApiResponse<Object> rejectClaim(@PathVariable Long id, @RequestBody(required = false) AdminDTO.ReportReviewRequest request) {
        return ApiResponse.ok(adminService.rejectClaim(id, request == null ? null : request.getRemark()));
    }

    @PostMapping("/reviews/{id}/approve")
    public ApiResponse<Object> approve(@PathVariable String id, @RequestBody(required = false) ItemDTO.ReviewActionRequest request) {
        return ApiResponse.ok(adminService.approve(id, request == null ? null : request.getRemark()));
    }

    @PostMapping("/reviews/{id}/reject")
    public ApiResponse<Object> reject(@PathVariable String id, @RequestBody(required = false) ItemDTO.ReviewActionRequest request) {
        return ApiResponse.ok(adminService.reject(id, request == null ? null : request.getRemark()));
    }

    @PostMapping("/reviews/{id}/delete")
    public ApiResponse<Object> delete(@PathVariable String id, @RequestBody(required = false) ItemDTO.ReviewActionRequest request) {
        return ApiResponse.ok(adminService.remove(id, request == null ? null : request.getRemark()));
    }

    @GetMapping("/announcements")
    public ApiResponse<Object> announcements() {
        return ApiResponse.ok(adminService.announcements());
    }

    @PostMapping("/announcements")
    public ApiResponse<Object> createAnnouncement(@Valid @RequestBody AdminDTO.AnnouncementRequest request) {
        return ApiResponse.ok(adminService.createAnnouncement(request));
    }

    @PutMapping("/announcements/{id}")
    public ApiResponse<Object> updateAnnouncement(@PathVariable Long id, @Valid @RequestBody AdminDTO.AnnouncementRequest request) {
        return ApiResponse.ok(adminService.updateAnnouncement(id, request));
    }

    @DeleteMapping("/announcements/{id}")
    public ApiResponse<Object> deleteAnnouncement(@PathVariable Long id) {
        return ApiResponse.ok(adminService.deleteAnnouncement(id));
    }

    @GetMapping("/reports")
    public ApiResponse<Object> reports() {
        return ApiResponse.ok(adminService.reports());
    }

    @PostMapping("/reports/{id}/resolve")
    public ApiResponse<Object> resolveReport(@PathVariable Long id, @RequestBody(required = false) AdminDTO.ReportReviewRequest request) {
        return ApiResponse.ok(adminService.resolveReport(id, request == null ? null : request.getRemark()));
    }

    @PostMapping("/reports/{id}/reject")
    public ApiResponse<Object> rejectReport(@PathVariable Long id, @RequestBody(required = false) AdminDTO.ReportReviewRequest request) {
        return ApiResponse.ok(adminService.rejectReport(id, request == null ? null : request.getRemark()));
    }
}

package com.example.lostandfound.service;

import com.example.lostandfound.dto.AdminDTO;
import com.example.lostandfound.dto.ItemDTO;

import java.util.List;

public interface AdminService {
    AdminDTO.DashboardVO getDashboard();

    AdminDTO.SpatiotemporalAnalysisVO getSpatiotemporalAnalysis(Integer topK);

    List<AdminDTO.ReviewVO> reviewList();

    List<AdminDTO.ReviewHistoryVO> reviewHistory();

    List<AdminDTO.ClaimRecordVO> claims();

    List<ItemDTO.ItemSummaryVO> lostItems(String keyword);

    List<ItemDTO.ItemSummaryVO> foundItems(String keyword);

    ItemDTO.ItemSummaryVO itemDetail(String itemId);

    AdminDTO.ClaimActionVO approveClaim(Long claimId, String remark);

    AdminDTO.ClaimActionVO rejectClaim(Long claimId, String remark);

    AdminDTO.ReviewActionVO approve(String id, String remark);

    AdminDTO.ReviewActionVO reject(String id, String remark);

    AdminDTO.ReviewActionVO remove(String id, String remark);

    List<AdminDTO.AnnouncementVO> announcements();

    AdminDTO.AnnouncementActionVO createAnnouncement(AdminDTO.AnnouncementRequest request);

    AdminDTO.AnnouncementActionVO updateAnnouncement(Long id, AdminDTO.AnnouncementRequest request);

    AdminDTO.AnnouncementActionVO deleteAnnouncement(Long id);

    List<AdminDTO.ReportVO> reports();

    AdminDTO.ReportActionVO resolveReport(Long id, String remark);

    AdminDTO.ReportActionVO rejectReport(Long id, String remark);
}

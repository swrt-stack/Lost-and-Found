package com.example.lostandfound.service.impl;

import com.example.lostandfound.cache.CacheKeys;
import com.example.lostandfound.cache.RedisJsonCacheService;
import com.example.lostandfound.cache.UnreadCounterService;
import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.dto.AdminDTO;
import com.example.lostandfound.dto.ItemDTO;
import com.example.lostandfound.entity.Announcement;
import com.example.lostandfound.entity.ClaimApplication;
import com.example.lostandfound.entity.FoundItem;
import com.example.lostandfound.entity.ItemCategory;
import com.example.lostandfound.entity.ItemReport;
import com.example.lostandfound.entity.LostItem;
import com.example.lostandfound.entity.MessageNotice;
import com.example.lostandfound.entity.OperationLog;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.AnnouncementMapper;
import com.example.lostandfound.mapper.ClaimApplicationMapper;
import com.example.lostandfound.mapper.FoundItemMapper;
import com.example.lostandfound.mapper.ItemCategoryMapper;
import com.example.lostandfound.mapper.ItemReportMapper;
import com.example.lostandfound.mapper.LostItemMapper;
import com.example.lostandfound.mapper.MessageNoticeMapper;
import com.example.lostandfound.mapper.OperationLogMapper;
import com.example.lostandfound.mapper.UserMapper;
import com.example.lostandfound.service.AdminService;
import com.example.lostandfound.service.support.AuditLogSupport;
import com.example.lostandfound.service.support.MatchNoticeSupport;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnnouncementMapper announcementMapper;
    private final ClaimApplicationMapper claimApplicationMapper;
    private final LostItemMapper lostItemMapper;
    private final FoundItemMapper foundItemMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final ItemReportMapper itemReportMapper;
    private final UserMapper userMapper;
    private final MessageNoticeMapper messageNoticeMapper;
    private final OperationLogMapper operationLogMapper;
    private final AuditLogSupport auditLogSupport;
    private final MatchNoticeSupport matchNoticeSupport;
    private final RedisJsonCacheService cacheService;
    private final UnreadCounterService unreadCounterService;

    public AdminServiceImpl(AnnouncementMapper announcementMapper, ClaimApplicationMapper claimApplicationMapper,
                            LostItemMapper lostItemMapper, FoundItemMapper foundItemMapper,
                            ItemCategoryMapper itemCategoryMapper, ItemReportMapper itemReportMapper, UserMapper userMapper,
                            MessageNoticeMapper messageNoticeMapper, OperationLogMapper operationLogMapper, AuditLogSupport auditLogSupport,
                            MatchNoticeSupport matchNoticeSupport, RedisJsonCacheService cacheService,
                            UnreadCounterService unreadCounterService) {
        this.announcementMapper = announcementMapper;
        this.claimApplicationMapper = claimApplicationMapper;
        this.lostItemMapper = lostItemMapper;
        this.foundItemMapper = foundItemMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.itemReportMapper = itemReportMapper;
        this.userMapper = userMapper;
        this.messageNoticeMapper = messageNoticeMapper;
        this.operationLogMapper = operationLogMapper;
        this.auditLogSupport = auditLogSupport;
        this.matchNoticeSupport = matchNoticeSupport;
        this.cacheService = cacheService;
        this.unreadCounterService = unreadCounterService;
    }

    @Override
    public AdminDTO.DashboardVO getDashboard() {
        long lostCount = lostItemMapper.selectCountByQuery(QueryWrapper.create());
        long foundCount = foundItemMapper.selectCountByQuery(QueryWrapper.create());
        int publishedCount = Math.toIntExact(lostCount + foundCount);
        long approvedCount = lostItemMapper.selectCountByQuery(QueryWrapper.create().where("status = ?", 1))
                + foundItemMapper.selectCountByQuery(QueryWrapper.create().where("status = ?", 1));
        String retrievalRate = publishedCount == 0 ? "0%" : Math.round(approvedCount * 100.0 / publishedCount) + "%";

        List<LostItem> lostItems = lostItemMapper.selectListByQuery(QueryWrapper.create().select("category_id", "status"));
        List<FoundItem> foundItems = foundItemMapper.selectListByQuery(QueryWrapper.create().select("category_id", "status"));
        Map<Long, String> categoryMap = itemCategoryMapper.selectAll().stream()
                .collect(Collectors.toMap(ItemCategory::getId, ItemCategory::getName, (left, right) -> left));
        Map<String, Integer> distribution = new LinkedHashMap<>();
        Map<String, Integer> statusDistribution = new LinkedHashMap<>();
        Map<String, Integer> typeDistribution = new LinkedHashMap<>();
        lostItems.forEach(item -> increment(distribution, categoryMap.getOrDefault(item.getCategoryId(), "Unknown")));
        foundItems.forEach(item -> increment(distribution, categoryMap.getOrDefault(item.getCategoryId(), "Unknown")));
        lostItems.forEach(item -> {
            increment(statusDistribution, statusText(item.getStatus()));
            increment(typeDistribution, "lost");
        });
        foundItems.forEach(item -> {
            increment(statusDistribution, statusText(item.getStatus()));
            increment(typeDistribution, "found");
        });
        return new AdminDTO.DashboardVO(publishedCount, retrievalRate, distribution, statusDistribution, typeDistribution);
    }

    @Override
    public List<AdminDTO.ReviewVO> reviewList() {
        List<AdminDTO.ReviewVO> reviews = new ArrayList<>();
        List<LostItem> lostItems = lostItemMapper.selectListByQuery(
                QueryWrapper.create().where("status = ?", 0).orderBy("lost_time desc")
        );
        List<FoundItem> foundItems = foundItemMapper.selectListByQuery(
                QueryWrapper.create().where("status = ?", 0).orderBy("found_time desc")
        );
        Set<Long> categoryIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();
        lostItems.forEach(item -> {
            categoryIds.add(item.getCategoryId());
            userIds.add(item.getUserId());
        });
        foundItems.forEach(item -> {
            categoryIds.add(item.getCategoryId());
            userIds.add(item.getUserId());
        });
        Map<Long, String> categoryMap = categoryNamesByIds(categoryIds);
        Map<Long, User> userMap = usersByIds(userIds);

        lostItems.forEach(item ->
                reviews.add(new AdminDTO.ReviewVO(
                        "LOST-" + item.getId(),
                        item.getTitle(),
                        "lost",
                        categoryMap.getOrDefault(item.getCategoryId(), "Unknown"),
                        displayName(userMap.get(item.getUserId())),
                        avatarUrl(userMap.get(item.getUserId())),
                        statusText(item.getStatus()),
                        item.getDescription(),
                        item.getLocation(),
                        formatTime(item.getLostTime())
                )));
        foundItems.forEach(item ->
                reviews.add(new AdminDTO.ReviewVO(
                        "FOUND-" + item.getId(),
                        item.getTitle(),
                        "found",
                        categoryMap.getOrDefault(item.getCategoryId(), "Unknown"),
                        displayName(userMap.get(item.getUserId())),
                        avatarUrl(userMap.get(item.getUserId())),
                        statusText(item.getStatus()),
                        item.getDescription(),
                        item.getLocation(),
                        formatTime(item.getFoundTime())
                )));

        return reviews.stream()
                .sorted(Comparator.comparing(AdminDTO.ReviewVO::time, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<AdminDTO.ReviewHistoryVO> reviewHistory() {
        List<OperationLog> logs = operationLogMapper.selectListByQuery(
                QueryWrapper.create()
                        .where("action in (?, ?, ?)", "APPROVE_REVIEW", "REJECT_REVIEW", "DELETE_REVIEW")
                        .orderBy("created_at desc")
        );
        Set<Long> userIds = logs.stream()
                .map(OperationLog::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, User> users = usersByIds(userIds);

        return logs.stream()
                .map(item -> new AdminDTO.ReviewHistoryVO(
                        item.getAction(),
                        item.getDetail(),
                        item.getUserId() == null ? "system" : displayName(users.get(item.getUserId())),
                        item.getCreatedAt() == null ? "" : item.getCreatedAt().format(FORMATTER)
                ))
                .toList();
    }

    @Override
    public List<AdminDTO.ClaimRecordVO> claims() {
        List<ClaimApplication> claims = claimApplicationMapper.selectListByQuery(
                QueryWrapper.create().orderBy("created_at desc")
        );
        Set<Long> foundItemIds = claims.stream()
                .map(com.example.lostandfound.entity.ClaimApplication::getFoundItemId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> userIds = new LinkedHashSet<>();
        claims.forEach(item -> {
            userIds.add(item.getApplicantUserId());
            userIds.add(item.getOwnerUserId());
        });
        Map<Long, FoundItem> foundItems = foundItemsByIds(foundItemIds);
        Map<Long, User> users = usersByIds(userIds);

        return claims.stream()
                .map(item -> {
                    FoundItem foundItem = foundItems.get(item.getFoundItemId());
                    return new AdminDTO.ClaimRecordVO(
                            item.getId(),
                            foundItem == null ? "FOUND-" + item.getFoundItemId() : "FOUND-" + foundItem.getId(),
                            foundItem == null ? "Unknown item" : foundItem.getTitle(),
                            displayName(users.get(item.getApplicantUserId())),
                            avatarUrl(users.get(item.getApplicantUserId())),
                            displayName(users.get(item.getOwnerUserId())),
                            avatarUrl(users.get(item.getOwnerUserId())),
                            item.getMessage(),
                            claimStatusText(item.getStatus()),
                            item.getReviewRemark(),
                            item.getCreatedAt() == null ? "" : item.getCreatedAt().format(FORMATTER)
                    );
                })
                .toList();
    }

    @Override
    public List<ItemDTO.ItemSummaryVO> lostItems(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Map<Long, String> categories = categoryNamesByIds(itemCategoryMapper.selectAll().stream()
                .map(ItemCategory::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Long, User> users = usersByIds(userMapper.selectAll().stream()
                .map(User::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        QueryWrapper wrapper = QueryWrapper.create().orderBy("lost_time desc");
        if (!normalizedKeyword.isBlank()) {
            wrapper.where("title like ?", "%" + normalizedKeyword + "%");
        }

        return lostItemMapper.selectListByQuery(wrapper).stream()
                .map(item -> new ItemDTO.ItemSummaryVO(
                        "LOST-" + item.getId(),
                        item.getTitle(),
                        "lost",
                        categories.getOrDefault(item.getCategoryId(), "Unknown"),
                        item.getLocation(),
                        formatTime(item.getLostTime()),
                        displayName(users.get(item.getUserId())),
                        avatarUrl(users.get(item.getUserId())),
                        statusText(item.getStatus()),
                        item.getDescription(),
                        item.getContact(),
                        null,
                        item.getImages()
                ))
                .toList();
    }

    @Override
    public List<ItemDTO.ItemSummaryVO> foundItems(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        Map<Long, String> categories = categoryNamesByIds(itemCategoryMapper.selectAll().stream()
                .map(ItemCategory::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Long, User> users = usersByIds(userMapper.selectAll().stream()
                .map(User::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        QueryWrapper wrapper = QueryWrapper.create().orderBy("found_time desc");
        if (!normalizedKeyword.isBlank()) {
            wrapper.where("title like ?", "%" + normalizedKeyword + "%");
        }

        return foundItemMapper.selectListByQuery(wrapper).stream()
                .map(item -> new ItemDTO.ItemSummaryVO(
                        "FOUND-" + item.getId(),
                        item.getTitle(),
                        "found",
                        categories.getOrDefault(item.getCategoryId(), "Unknown"),
                        item.getLocation(),
                        formatTime(item.getFoundTime()),
                        displayName(users.get(item.getUserId())),
                        avatarUrl(users.get(item.getUserId())),
                        statusText(item.getStatus()),
                        item.getDescription(),
                        null,
                        item.getPickupMethod(),
                        item.getImages()
                ))
                .toList();
    }

    @Override
    public ItemDTO.ItemSummaryVO itemDetail(String itemId) {
        ReviewTarget target = parseTarget(itemId);
        Map<Long, String> categories = categoryNamesByIds(itemCategoryMapper.selectAll().stream()
                .map(ItemCategory::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Long, User> users = usersByIds(userMapper.selectAll().stream()
                .map(User::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        if ("LOST".equals(target.type())) {
            LostItem item = requireLost(target.numericId());
            return new ItemDTO.ItemSummaryVO(
                    "LOST-" + item.getId(),
                    item.getTitle(),
                    "lost",
                    categories.getOrDefault(item.getCategoryId(), "Unknown"),
                    item.getLocation(),
                    formatTime(item.getLostTime()),
                    displayName(users.get(item.getUserId())),
                    avatarUrl(users.get(item.getUserId())),
                    statusText(item.getStatus()),
                    item.getDescription(),
                    item.getContact(),
                    null,
                    item.getImages()
            );
        }

        FoundItem item = requireFound(target.numericId());
        return new ItemDTO.ItemSummaryVO(
                "FOUND-" + item.getId(),
                item.getTitle(),
                "found",
                categories.getOrDefault(item.getCategoryId(), "Unknown"),
                item.getLocation(),
                formatTime(item.getFoundTime()),
                displayName(users.get(item.getUserId())),
                avatarUrl(users.get(item.getUserId())),
                statusText(item.getStatus()),
                item.getDescription(),
                null,
                item.getPickupMethod(),
                item.getImages()
        );
    }

    @Override
    public AdminDTO.ClaimActionVO approveClaim(Long claimId, String remark) {
        ClaimApplication claim = requireClaim(claimId);
        if (claim.getStatus() != null && claim.getStatus() != 0) {
            throw new BusinessException(400, "Claim is already processed");
        }
        FoundItem item = requireFound(claim.getFoundItemId());
        claim.setStatus(1);
        claim.setReviewRemark(blankToNull(remark));
        claimApplicationMapper.update(claim);
        item.setStatus(3);
        foundItemMapper.update(item);
        invalidateOverviewCache();
        notifyUser(claim.getApplicantUserId(), "CLAIM", "管理员已通过你对物品【" + item.getTitle() + "】的认领申请",
                "/item-detail?id=FOUND-" + item.getId());
        notifyUser(claim.getOwnerUserId(), "CLAIM", "管理员已通过你物品【" + item.getTitle() + "】的一条认领申请",
                "/item-detail?id=FOUND-" + item.getId());
        auditLogSupport.record("APPROVE_CLAIM", "Approved claim " + claimId + suffixRemark(remark));
        return new AdminDTO.ClaimActionVO(claimId, "认领申请已通过", claimStatusText(claim.getStatus()));
    }

    @Override
    public AdminDTO.ClaimActionVO rejectClaim(Long claimId, String remark) {
        ClaimApplication claim = requireClaim(claimId);
        if (claim.getStatus() != null && claim.getStatus() != 0) {
            throw new BusinessException(400, "Claim is already processed");
        }
        FoundItem item = requireFound(claim.getFoundItemId());
        claim.setStatus(2);
        claim.setReviewRemark(blankToNull(remark));
        claimApplicationMapper.update(claim);
        notifyUser(claim.getApplicantUserId(), "CLAIM", "管理员已驳回你对物品【" + item.getTitle() + "】的认领申请"
                + suffixRemark(remark), "/item-detail?id=FOUND-" + item.getId());
        notifyUser(claim.getOwnerUserId(), "CLAIM", "管理员已驳回你物品【" + item.getTitle() + "】的一条认领申请"
                + suffixRemark(remark), "/item-detail?id=FOUND-" + item.getId());
        auditLogSupport.record("REJECT_CLAIM", "Rejected claim " + claimId + suffixRemark(remark));
        return new AdminDTO.ClaimActionVO(claimId, "认领申请已驳回", claimStatusText(claim.getStatus()));
    }

    @Override
    public AdminDTO.ReviewActionVO approve(String id, String remark) {
        ReviewTarget target = parseTarget(id);
        if ("LOST".equals(target.type())) {
            LostItem item = requireLost(target.numericId());
            item.setStatus(1);
            lostItemMapper.update(item);
            invalidateOverviewCache();
            notifyUser(item.getUserId(), "REVIEW", "你的遗失物品【" + item.getTitle() + "】审核已通过" + suffixRemark(remark),
                    "/item-detail?id=LOST-" + item.getId());
            matchNoticeSupport.onLostApproved(item);
        } else {
            FoundItem item = requireFound(target.numericId());
            item.setStatus(1);
            foundItemMapper.update(item);
            invalidateOverviewCache();
            notifyUser(item.getUserId(), "REVIEW", "你的招领物品【" + item.getTitle() + "】审核已通过" + suffixRemark(remark),
                    "/item-detail?id=FOUND-" + item.getId());
            matchNoticeSupport.onFoundApproved(item);
        }
        auditLogSupport.record("APPROVE_REVIEW", "Approved review " + id);
        return new AdminDTO.ReviewActionVO(id, "approve", "审核已通过");
    }

    @Override
    public AdminDTO.ReviewActionVO reject(String id, String remark) {
        ReviewTarget target = parseTarget(id);
        if ("LOST".equals(target.type())) {
            LostItem item = requireLost(target.numericId());
            item.setStatus(2);
            lostItemMapper.update(item);
            invalidateOverviewCache();
            notifyUser(item.getUserId(), "REVIEW", "你的遗失物品【" + item.getTitle() + "】审核未通过" + suffixRemark(remark),
                    "/item-detail?id=LOST-" + item.getId());
        } else {
            FoundItem item = requireFound(target.numericId());
            item.setStatus(2);
            foundItemMapper.update(item);
            invalidateOverviewCache();
            notifyUser(item.getUserId(), "REVIEW", "你的招领物品【" + item.getTitle() + "】审核未通过" + suffixRemark(remark),
                    "/item-detail?id=FOUND-" + item.getId());
        }
        auditLogSupport.record("REJECT_REVIEW", "Rejected review " + id);
        return new AdminDTO.ReviewActionVO(id, "reject", "审核已驳回");
    }

    @Override
    public AdminDTO.ReviewActionVO remove(String id, String remark) {
        ReviewTarget target = parseTarget(id);
        if ("LOST".equals(target.type())) {
            LostItem item = requireLost(target.numericId());
            lostItemMapper.deleteById(item.getId());
            invalidateOverviewCache();
            notifyUser(item.getUserId(), "REVIEW", "你的遗失物品【" + item.getTitle() + "】已被管理员移除" + suffixRemark(remark),
                    "/user-center");
        } else {
            FoundItem item = requireFound(target.numericId());
            foundItemMapper.deleteById(item.getId());
            invalidateOverviewCache();
            notifyUser(item.getUserId(), "REVIEW", "你的招领物品【" + item.getTitle() + "】已被管理员移除" + suffixRemark(remark),
                    "/user-center");
        }
        auditLogSupport.record("DELETE_REVIEW", "Deleted review " + id);
        return new AdminDTO.ReviewActionVO(id, "delete", "记录已删除");
    }

    @Override
    public List<AdminDTO.AnnouncementVO> announcements() {
        return announcementMapper.selectListByQuery(QueryWrapper.create().orderBy("created_at desc")).stream()
                .map(item -> new AdminDTO.AnnouncementVO(
                        item.getId(),
                        item.getTitle(),
                        item.getContent(),
                        item.getStatus() != null && item.getStatus() == 1 ? "PUBLISHED" : "DRAFT",
                        item.getCreatedAt() == null ? "" : item.getCreatedAt().format(FORMATTER)
                ))
                .toList();
    }

    @Override
    public AdminDTO.AnnouncementActionVO createAnnouncement(AdminDTO.AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        LocalDateTime now = LocalDateTime.now();
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcement.setStatus(request.getStatus() != null && request.getStatus() == 1 ? 1 : 0);
        announcement.setCreatedAt(now);
        announcement.setUpdatedAt(now);
        announcementMapper.insert(announcement);
        if (announcement.getStatus() != null && announcement.getStatus() == 1) {
            broadcastAnnouncement(announcement);
        }
        invalidateAnnouncementCaches();
        auditLogSupport.record("CREATE_ANNOUNCEMENT", "Created announcement " + announcement.getTitle());
        return new AdminDTO.AnnouncementActionVO(announcement.getId(), "Announcement created");
    }

    @Override
    public AdminDTO.AnnouncementActionVO updateAnnouncement(Long id, AdminDTO.AnnouncementRequest request) {
        Announcement announcement = requireAnnouncement(id);
        Integer oldStatus = announcement.getStatus();
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcement.setStatus(request.getStatus() != null && request.getStatus() == 1 ? 1 : 0);
        announcementMapper.update(announcement);
        if ((oldStatus == null || oldStatus != 1) && announcement.getStatus() != null && announcement.getStatus() == 1) {
            broadcastAnnouncement(announcement);
        }
        invalidateAnnouncementCaches();
        auditLogSupport.record("UPDATE_ANNOUNCEMENT", "Updated announcement " + announcement.getTitle());
        return new AdminDTO.AnnouncementActionVO(id, "Announcement updated");
    }

    @Override
    public AdminDTO.AnnouncementActionVO deleteAnnouncement(Long id) {
        Announcement announcement = requireAnnouncement(id);
        announcementMapper.deleteById(id);
        invalidateAnnouncementCaches();
        auditLogSupport.record("DELETE_ANNOUNCEMENT", "Deleted announcement " + announcement.getTitle());
        return new AdminDTO.AnnouncementActionVO(id, "Announcement deleted");
    }

    @Override
    public List<AdminDTO.ReportVO> reports() {
        List<ItemReport> reports = itemReportMapper.selectListByQuery(QueryWrapper.create().orderBy("created_at desc"));
        Set<Long> reporterIds = reports.stream()
                .map(ItemReport::getReporterUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> lostIds = new LinkedHashSet<>();
        Set<Long> foundIds = new LinkedHashSet<>();
        reports.stream()
                .map(ItemReport::getItemId)
                .map(this::parseTarget)
                .forEach(target -> {
                    if ("LOST".equals(target.type())) {
                        lostIds.add(target.numericId());
                    } else {
                        foundIds.add(target.numericId());
                    }
                });

        Map<Long, LostItem> lostItems = lostItemsByIds(lostIds);
        Map<Long, FoundItem> foundItems = foundItemsByIds(foundIds);
        Set<Long> itemOwnerIds = new LinkedHashSet<>();
        lostItems.values().forEach(item -> itemOwnerIds.add(item.getUserId()));
        foundItems.values().forEach(item -> itemOwnerIds.add(item.getUserId()));
        reporterIds.addAll(itemOwnerIds);
        Map<Long, String> users = usernamesByIds(reporterIds);

        return reports.stream()
                .map(item -> toReportVO(item, users, lostItems, foundItems))
                .toList();
    }

    @Override
    public AdminDTO.ReportActionVO resolveReport(Long id, String remark) {
        ItemReport report = requireReport(id);
        report.setStatus(1);
        report.setReviewRemark(remark == null || remark.isBlank() ? null : remark.trim());
        itemReportMapper.update(report);
        auditLogSupport.record("RESOLVE_REPORT", "Resolved report " + id);
        return new AdminDTO.ReportActionVO(id, "Report resolved");
    }

    @Override
    public AdminDTO.ReportActionVO rejectReport(Long id, String remark) {
        ItemReport report = requireReport(id);
        report.setStatus(2);
        report.setReviewRemark(remark == null || remark.isBlank() ? null : remark.trim());
        itemReportMapper.update(report);
        auditLogSupport.record("REJECT_REPORT", "Rejected report " + id);
        return new AdminDTO.ReportActionVO(id, "Report rejected");
    }

    private LostItem requireLost(Long id) {
        LostItem item = lostItemMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (item == null) {
            throw new BusinessException(404, "Lost item not found");
        }
        return item;
    }

    private FoundItem requireFound(Long id) {
        FoundItem item = foundItemMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (item == null) {
            throw new BusinessException(404, "Found item not found");
        }
        return item;
    }

    private ReviewTarget parseTarget(String id) {
        if (id == null || !id.contains("-")) {
            throw new BusinessException(400, "Invalid review id");
        }
        String[] parts = id.split("-", 2);
        try {
            return new ReviewTarget(parts[0].toUpperCase(), Long.parseLong(parts[1]));
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, "Invalid review id");
        }
    }

    private void notifyUser(Long userId, String type, String content, String targetPath) {
        MessageNotice notice = new MessageNotice();
        notice.setUserId(userId);
        notice.setMessageType(type);
        notice.setContent(content);
        notice.setTargetPath(targetPath);
        notice.setReadFlag(0);
        notice.setCreatedAt(LocalDateTime.now());
        messageNoticeMapper.insert(notice);
        unreadCounterService.incrementMessageUnread(userId);
    }

    private void broadcastAnnouncement(Announcement announcement) {
        List<User> users = userMapper.selectListByQuery(QueryWrapper.create().where("status = ?", 1));
        String content = "系统公告：" + announcement.getTitle() + " - " + announcement.getContent();
        for (User user : users) {
            notifyUser(user.getId(), "ANNOUNCEMENT", content, "/");
        }
    }

    private void increment(Map<String, Integer> distribution, String key) {
        distribution.put(key, distribution.getOrDefault(key, 0) + 1);
    }

    private AdminDTO.ReportVO toReportVO(ItemReport report, Map<Long, String> users,
                                         Map<Long, LostItem> lostItems, Map<Long, FoundItem> foundItems) {
        ReportItemDetail detail = findReportItemDetail(report.getItemId(), users, lostItems, foundItems);
        return new AdminDTO.ReportVO(
                report.getId(),
                report.getItemId(),
                report.getItemType(),
                users.getOrDefault(report.getReporterUserId(), "unknown"),
                report.getReason(),
                reportStatusText(report.getStatus()),
                report.getReviewRemark(),
                report.getCreatedAt() == null ? "" : report.getCreatedAt().format(FORMATTER),
                detail.title(),
                detail.status(),
                detail.publisher(),
                detail.location(),
                detail.description()
        );
    }

    private ReportItemDetail findReportItemDetail(String itemId, Map<Long, String> users,
                                                  Map<Long, LostItem> lostItems, Map<Long, FoundItem> foundItems) {
        ReviewTarget target = parseTarget(itemId);
        if ("LOST".equals(target.type())) {
            LostItem item = lostItems.get(target.numericId());
            if (item == null) {
                return new ReportItemDetail("Removed item", "DELETED", "-", "-", "-");
            }
            return new ReportItemDetail(
                    item.getTitle(),
                    statusText(item.getStatus()),
                    users.getOrDefault(item.getUserId(), "unknown"),
                    item.getLocation(),
                    item.getDescription()
            );
        }
        FoundItem item = foundItems.get(target.numericId());
        if (item == null) {
            return new ReportItemDetail("Removed item", "DELETED", "-", "-", "-");
        }
        return new ReportItemDetail(
                item.getTitle(),
                statusText(item.getStatus()),
                users.getOrDefault(item.getUserId(), "unknown"),
                item.getLocation(),
                item.getDescription()
        );
    }

    private Announcement requireAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (announcement == null) {
            throw new BusinessException(404, "Announcement not found");
        }
        return announcement;
    }

    private ClaimApplication requireClaim(Long id) {
        ClaimApplication claim = claimApplicationMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (claim == null) {
            throw new BusinessException(404, "Claim application not found");
        }
        return claim;
    }

    private ItemReport requireReport(Long id) {
        ItemReport report = itemReportMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (report == null) {
            throw new BusinessException(404, "Report not found");
        }
        return report;
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "PENDING";
            case 1 -> "APPROVED";
            case 2 -> "REJECTED";
            case 3 -> "COMPLETED";
            case 4 -> "OFFLINE";
            default -> "UNKNOWN";
        };
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(FORMATTER);
    }

    private String suffixRemark(String remark) {
        return remark == null || remark.isBlank() ? "" : "，备注：" + remark.trim();
    }

    private String reportStatusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "PENDING";
            case 1 -> "RESOLVED";
            case 2 -> "REJECTED";
            default -> "UNKNOWN";
        };
    }

    private String claimStatusText(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case 0 -> "PENDING";
            case 1 -> "APPROVED";
            case 2 -> "REJECTED";
            default -> "UNKNOWN";
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<Long, String> categoryNamesByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return itemCategoryMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(ItemCategory::getId, ItemCategory::getName, (left, right) -> left));
    }

    private Map<Long, String> usernamesByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (left, right) -> left));
    }

    private Map<Long, User> usersByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, LostItem> lostItemsByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return lostItemMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(LostItem::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, FoundItem> foundItemsByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return foundItemMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(FoundItem::getId, Function.identity(), (left, right) -> left));
    }

    private QueryWrapper whereIn(String column, Set<Long> ids) {
        String placeholders = String.join(", ", java.util.Collections.nCopies(ids.size(), "?"));
        return QueryWrapper.create().where(column + " in (" + placeholders + ")", ids.toArray());
    }

    private String displayName(User user) {
        if (user == null) {
            return "unknown";
        }
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getUsername();
    }

    private String avatarUrl(User user) {
        return user == null ? null : user.getAvatarUrl();
    }

    private void invalidateOverviewCache() {
        cacheService.delete(CacheKeys.SYSTEM_OVERVIEW);
    }

    private void invalidateAnnouncementCaches() {
        cacheService.delete(CacheKeys.SYSTEM_ANNOUNCEMENTS, CacheKeys.SYSTEM_OVERVIEW);
    }

    private record ReviewTarget(String type, Long numericId) {
    }

    private record ReportItemDetail(String title, String status, String publisher, String location, String description) {
    }
}

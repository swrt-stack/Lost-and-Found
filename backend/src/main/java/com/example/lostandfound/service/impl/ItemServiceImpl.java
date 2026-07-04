package com.example.lostandfound.service.impl;

import com.example.lostandfound.cache.CacheKeys;
import com.example.lostandfound.cache.RedisJsonCacheService;
import com.example.lostandfound.cache.UnreadCounterService;
import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.dto.ConfigDTO;
import com.example.lostandfound.dto.ItemDTO;
import com.example.lostandfound.entity.ClaimApplication;
import com.example.lostandfound.entity.FoundItem;
import com.example.lostandfound.entity.ItemCategory;
import com.example.lostandfound.entity.ItemReport;
import com.example.lostandfound.entity.LostItem;
import com.example.lostandfound.entity.MessageNotice;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.ClaimApplicationMapper;
import com.example.lostandfound.mapper.FoundItemMapper;
import com.example.lostandfound.mapper.ItemCategoryMapper;
import com.example.lostandfound.mapper.ItemReportMapper;
import com.example.lostandfound.mapper.LostItemMapper;
import com.example.lostandfound.mapper.MessageNoticeMapper;
import com.example.lostandfound.mapper.UserMapper;
import com.example.lostandfound.security.CurrentUserService;
import com.example.lostandfound.service.ConfigService;
import com.example.lostandfound.service.ItemService;
import com.example.lostandfound.service.support.AuditLogSupport;
import com.example.lostandfound.service.support.MatchNoticeSupport;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LostItemMapper lostItemMapper;
    private final FoundItemMapper foundItemMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final UserMapper userMapper;
    private final MessageNoticeMapper messageNoticeMapper;
    private final ClaimApplicationMapper claimApplicationMapper;
    private final ItemReportMapper itemReportMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogSupport auditLogSupport;
    private final ConfigService configService;
    private final MatchNoticeSupport matchNoticeSupport;
    private final RedisJsonCacheService cacheService;
    private final UnreadCounterService unreadCounterService;

    public ItemServiceImpl(LostItemMapper lostItemMapper, FoundItemMapper foundItemMapper,
                           ItemCategoryMapper itemCategoryMapper, UserMapper userMapper,
                           MessageNoticeMapper messageNoticeMapper, ClaimApplicationMapper claimApplicationMapper,
                           ItemReportMapper itemReportMapper, CurrentUserService currentUserService,
                           AuditLogSupport auditLogSupport, ConfigService configService,
                           MatchNoticeSupport matchNoticeSupport, RedisJsonCacheService cacheService,
                           UnreadCounterService unreadCounterService) {
        this.lostItemMapper = lostItemMapper;
        this.foundItemMapper = foundItemMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.userMapper = userMapper;
        this.messageNoticeMapper = messageNoticeMapper;
        this.claimApplicationMapper = claimApplicationMapper;
        this.itemReportMapper = itemReportMapper;
        this.currentUserService = currentUserService;
        this.auditLogSupport = auditLogSupport;
        this.configService = configService;
        this.matchNoticeSupport = matchNoticeSupport;
        this.cacheService = cacheService;
        this.unreadCounterService = unreadCounterService;
    }

    @Override
    public ItemDTO.PublishResultVO createLost(ItemDTO.CreateItemRequest request) {
        User user = currentUserService.requireUser();
        ensureCategoryExists(request.getCategoryId());
        LostItem item = new LostItem();
        item.setCreatedAt(LocalDateTime.now());
        item.setUserId(user.getId());
        applyLostFields(item, request);
        item.setStatus(reviewEnabled() ? 0 : 1);
        lostItemMapper.insert(item);
        invalidateOverviewCache();
        auditLogSupport.record(user.getId(), "PUBLISH_LOST", "Published lost item " + item.getTitle());
        if (item.getStatus() == 1) {
            matchNoticeSupport.onLostApproved(item);
        }
        return new ItemDTO.PublishResultVO(
                item.getStatus() == 1 ? "Lost item submitted and approved" : "Lost item submitted",
                "LOST-" + item.getId(),
                statusText(item.getStatus())
        );
    }

    @Override
    public ItemDTO.PublishResultVO createFound(ItemDTO.CreateItemRequest request) {
        User user = currentUserService.requireUser();
        ensureCategoryExists(request.getCategoryId());
        FoundItem item = new FoundItem();
        item.setCreatedAt(LocalDateTime.now());
        item.setUserId(user.getId());
        applyFoundFields(item, request);
        item.setStatus(reviewEnabled() ? 0 : 1);
        foundItemMapper.insert(item);
        invalidateOverviewCache();
        auditLogSupport.record(user.getId(), "PUBLISH_FOUND", "Published found item " + item.getTitle());
        if (item.getStatus() == 1) {
            matchNoticeSupport.onFoundApproved(item);
        }
        return new ItemDTO.PublishResultVO(
                item.getStatus() == 1 ? "Found item submitted and approved" : "Found item submitted",
                "FOUND-" + item.getId(),
                statusText(item.getStatus())
        );
    }

    @Override
    public List<ItemDTO.ItemSummaryVO> search(String keyword, String location, String type, Long categoryId) {
        String normalizedKeyword = normalize(keyword);
        String normalizedLocation = normalize(location);
        String normalizedType = normalize(type);
        Map<Long, String> categories = categoryNameMap();
        Map<Long, User> users = userMap();

        List<ItemDTO.ItemSummaryVO> results = new ArrayList<>();
        if (!"found".equalsIgnoreCase(normalizedType)) {
            lostItemMapper.selectListByQuery(buildLostSearchQuery(normalizedKeyword, normalizedLocation, categoryId))
                    .forEach(item -> results.add(new ItemDTO.ItemSummaryVO(
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
                    )));
        }
        if (!"lost".equalsIgnoreCase(normalizedType)) {
            foundItemMapper.selectListByQuery(buildFoundSearchQuery(normalizedKeyword, normalizedLocation, categoryId))
                    .forEach(item -> results.add(new ItemDTO.ItemSummaryVO(
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
                    )));
        }
        return results.stream()
                .sorted(Comparator.comparing(ItemDTO.ItemSummaryVO::time, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<ItemDTO.ItemSummaryVO> keywordSearch(String keyword, String type, Long categoryId) {
        String normalizedKeyword = normalize(keyword);
        String normalizedType = normalize(type);
        Map<Long, String> categories = categoryNameMap();
        Map<Long, User> users = userMap();

        List<ItemDTO.ItemSummaryVO> results = new ArrayList<>();
        if (!"found".equalsIgnoreCase(normalizedType)) {
            lostItemMapper.selectListByQuery(buildLostKeywordSearchQuery(normalizedKeyword, categoryId))
                    .forEach(item -> results.add(new ItemDTO.ItemSummaryVO(
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
                    )));
        }
        if (!"lost".equalsIgnoreCase(normalizedType)) {
            foundItemMapper.selectListByQuery(buildFoundKeywordSearchQuery(normalizedKeyword, categoryId))
                    .forEach(item -> results.add(new ItemDTO.ItemSummaryVO(
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
                    )));
        }
        return results.stream()
                .sorted(Comparator.comparing(ItemDTO.ItemSummaryVO::time, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<ItemDTO.MyItemVO> myItems() {
        User user = currentUserService.requireUser();
        Map<Long, String> categories = categoryNameMap();
        String publisher = displayName(user);
        String publisherAvatarUrl = user.getAvatarUrl();
        List<ItemDTO.MyItemVO> items = new ArrayList<>();
        lostItemMapper.selectListByQuery(QueryWrapper.create().where("user_id = ?", user.getId())).forEach(item ->
                items.add(new ItemDTO.MyItemVO(
                        "LOST-" + item.getId(),
                        item.getTitle(),
                        "lost",
                        categories.getOrDefault(item.getCategoryId(), "Unknown"),
                        item.getLocation(),
                        formatTime(item.getLostTime()),
                        publisher,
                        publisherAvatarUrl,
                        statusText(item.getStatus()),
                        null,
                        item.getDescription(),
                        item.getContact(),
                        null,
                        item.getImages()
                )));
        foundItemMapper.selectListByQuery(QueryWrapper.create().where("user_id = ?", user.getId())).forEach(item ->
                items.add(new ItemDTO.MyItemVO(
                        "FOUND-" + item.getId(),
                        item.getTitle(),
                        "found",
                        categories.getOrDefault(item.getCategoryId(), "Unknown"),
                        item.getLocation(),
                        formatTime(item.getFoundTime()),
                        publisher,
                        publisherAvatarUrl,
                        statusText(item.getStatus()),
                        null,
                        item.getDescription(),
                        null,
                        item.getPickupMethod(),
                        item.getImages()
                )));
        return items.stream()
                .sorted(Comparator.comparing(ItemDTO.MyItemVO::time, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public ItemDTO.ItemActionVO updateItem(String itemId, ItemDTO.UpdateItemRequest request) {
        User user = currentUserService.requireUser();
        ensureCategoryExists(request.getCategoryId());
        ReviewTarget target = parseTarget(itemId);
        if ("LOST".equals(target.type())) {
            LostItem item = requireLost(target.numericId());
            ensureOwner(user, item.getUserId());
            ensureEditable(item.getStatus());
            applyLostFields(item, request);
            item.setStatus(reviewEnabled() ? 0 : 1);
            lostItemMapper.update(item);
            invalidateOverviewCache();
            auditLogSupport.record(user.getId(), "UPDATE_ITEM", "Updated " + itemId);
            return new ItemDTO.ItemActionVO(itemId, "Item updated", statusText(item.getStatus()));
        }
        FoundItem item = requireFound(target.numericId());
        ensureOwner(user, item.getUserId());
        ensureEditable(item.getStatus());
        applyFoundFields(item, request);
        item.setStatus(reviewEnabled() ? 0 : 1);
        foundItemMapper.update(item);
        invalidateOverviewCache();
        auditLogSupport.record(user.getId(), "UPDATE_ITEM", "Updated " + itemId);
        return new ItemDTO.ItemActionVO(itemId, "Item updated", statusText(item.getStatus()));
    }

    @Override
    public ItemDTO.ItemActionVO offlineItem(String itemId) {
        User user = currentUserService.requireUser();
        ReviewTarget target = parseTarget(itemId);
        if ("LOST".equals(target.type())) {
            LostItem item = requireLost(target.numericId());
            ensureOwner(user, item.getUserId());
            item.setStatus(4);
            lostItemMapper.update(item);
            invalidateOverviewCache();
            auditLogSupport.record(user.getId(), "OFFLINE_ITEM", "Offlined " + itemId);
            return new ItemDTO.ItemActionVO(itemId, "Item taken offline", statusText(item.getStatus()));
        }
        FoundItem item = requireFound(target.numericId());
        ensureOwner(user, item.getUserId());
        item.setStatus(4);
        foundItemMapper.update(item);
        invalidateOverviewCache();
        auditLogSupport.record(user.getId(), "OFFLINE_ITEM", "Offlined " + itemId);
        return new ItemDTO.ItemActionVO(itemId, "Item taken offline", statusText(item.getStatus()));
    }

    @Override
    public ItemDTO.ItemActionVO deleteItem(String itemId) {
        User user = currentUserService.requireUser();
        ReviewTarget target = parseTarget(itemId);
        itemReportMapper.deleteByQuery(QueryWrapper.create().where("item_id = ?", itemId));
        if ("LOST".equals(target.type())) {
            LostItem item = requireLost(target.numericId());
            ensureOwner(user, item.getUserId());
            lostItemMapper.deleteById(item.getId());
            invalidateOverviewCache();
            auditLogSupport.record(user.getId(), "DELETE_ITEM", "Deleted " + itemId);
            return new ItemDTO.ItemActionVO(itemId, "Item deleted", "DELETED");
        }
        FoundItem item = requireFound(target.numericId());
        ensureOwner(user, item.getUserId());
        claimApplicationMapper.deleteByQuery(QueryWrapper.create().where("found_item_id = ?", item.getId()));
        foundItemMapper.deleteById(item.getId());
        invalidateOverviewCache();
        auditLogSupport.record(user.getId(), "DELETE_ITEM", "Deleted " + itemId);
        return new ItemDTO.ItemActionVO(itemId, "Item deleted", "DELETED");
    }

    @Override
    public ItemDTO.ItemActionVO reportItem(String itemId, String reason) {
        User user = currentUserService.requireUser();
        ReviewTarget target = parseTarget(itemId);
        if (itemOwnerId(target).equals(user.getId())) {
            throw new BusinessException(400, "You cannot report your own item");
        }
        ItemReport existed = itemReportMapper.selectOneByQuery(QueryWrapper.create()
                .where("item_id = ? and reporter_user_id = ? and status = ?", itemId, user.getId(), 0));
        if (existed != null) {
            throw new BusinessException(400, "You already have a pending report for this item");
        }
        ItemReport report = new ItemReport();
        LocalDateTime now = LocalDateTime.now();
        report.setItemId(itemId);
        report.setItemType(target.type());
        report.setReporterUserId(user.getId());
        report.setReason(reason.trim());
        report.setStatus(0);
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        itemReportMapper.insert(report);
        auditLogSupport.record(user.getId(), "REPORT_ITEM", "Reported " + itemId);
        return new ItemDTO.ItemActionVO(itemId, "Report submitted", "PENDING");
    }

    @Override
    public ItemDTO.ItemActionVO claimFoundItem(String itemId, String message) {
        User user = currentUserService.requireUser();
        ReviewTarget target = parseTarget(itemId);
        if (!"FOUND".equals(target.type())) {
            throw new BusinessException(400, "??????????????");
        }
        FoundItem item = requireFound(target.numericId());
        if (item.getStatus() == null || item.getStatus() != 1) {
            throw new BusinessException(400, "??????????????????");
        }
        if (user.getId().equals(item.getUserId())) {
            throw new BusinessException(400, "?????????????");
        }
        ClaimApplication existed = claimApplicationMapper.selectOneByQuery(QueryWrapper.create()
                .where("found_item_id = ? and applicant_user_id = ? and status = ?", item.getId(), user.getId(), 0));
        if (existed != null) {
            throw new BusinessException(400, "??????????????????");
        }
        ClaimApplication claim = new ClaimApplication();
        LocalDateTime now = LocalDateTime.now();
        claim.setFoundItemId(item.getId());
        claim.setApplicantUserId(user.getId());
        claim.setOwnerUserId(item.getUserId());
        claim.setMessage(message.trim());
        claim.setStatus(0);
        claim.setCreatedAt(now);
        claim.setUpdatedAt(now);
        claimApplicationMapper.insert(claim);
        createNotice(
                item.getUserId(),
                "CLAIM",
                "???????" + item.getTitle() + "????????????",
                "/item-detail?id=FOUND-" + item.getId()
        );
        auditLogSupport.record(user.getId(), "CLAIM_FOUND_ITEM", "Submitted claim for " + itemId);
        return new ItemDTO.ItemActionVO(itemId, "???????", "PENDING");
    }

    @Override
    public ItemDTO.MyClaimsVO myClaims() {
        User user = currentUserService.requireUser();
        List<ClaimApplication> sentClaimEntities = claimApplicationMapper
                .selectListByQuery(QueryWrapper.create()
                        .where("applicant_user_id = ?", user.getId())
                        .orderBy("created_at desc"));

        List<ClaimApplication> receivedClaimEntities = claimApplicationMapper
                .selectListByQuery(QueryWrapper.create()
                        .where("owner_user_id = ?", user.getId())
                        .orderBy("created_at desc"));

        Set<Long> foundItemIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();
        sentClaimEntities.forEach(claim -> {
            foundItemIds.add(claim.getFoundItemId());
            userIds.add(claim.getApplicantUserId());
            userIds.add(claim.getOwnerUserId());
        });
        receivedClaimEntities.forEach(claim -> {
            foundItemIds.add(claim.getFoundItemId());
            userIds.add(claim.getApplicantUserId());
            userIds.add(claim.getOwnerUserId());
        });

        Map<Long, FoundItem> foundItems = foundItemsByIds(foundItemIds);
        Map<Long, String> users = usernamesByIds(userIds);
        List<ItemDTO.ClaimVO> sentClaims = sentClaimEntities.stream()
                .map(claim -> toClaimVO(claim, foundItems, users))
                .toList();
        List<ItemDTO.ClaimVO> receivedClaims = receivedClaimEntities.stream()
                .map(claim -> toClaimVO(claim, foundItems, users))
                .toList();

        return new ItemDTO.MyClaimsVO(sentClaims, receivedClaims);
    }

    @Override
    public ItemDTO.ItemActionVO approveClaim(Long claimId, String remark) {
        User user = currentUserService.requireUser();
        ClaimApplication claim = requireClaim(claimId);
        if (!user.getId().equals(claim.getOwnerUserId())) {
            throw new BusinessException(403, "???????????");
        }
        if (claim.getStatus() != null && claim.getStatus() != 0) {
            throw new BusinessException(400, "?????????");
        }
        FoundItem item = requireFound(claim.getFoundItemId());
        claim.setStatus(1);
        claim.setReviewRemark(blankToNull(remark));
        claimApplicationMapper.update(claim);
        item.setStatus(3);
        foundItemMapper.update(item);
        invalidateOverviewCache();
        createNotice(
                claim.getApplicantUserId(),
                "CLAIM",
                "?????" + item.getTitle() + "?????????",
                "/item-detail?id=FOUND-" + item.getId()
        );
        createNotice(
                item.getUserId(),
                "CLAIM",
                "???????" + item.getTitle() + "????????",
                "/item-detail?id=FOUND-" + item.getId()
        );
        auditLogSupport.record(user.getId(), "APPROVE_CLAIM", "Approved claim " + claimId);
        return new ItemDTO.ItemActionVO("FOUND-" + item.getId(), "???????", statusText(item.getStatus()));
    }

    @Override
    public ItemDTO.ItemActionVO rejectClaim(Long claimId, String remark) {
        User user = currentUserService.requireUser();
        ClaimApplication claim = requireClaim(claimId);
        if (!user.getId().equals(claim.getOwnerUserId())) {
            throw new BusinessException(403, "???????????");
        }
        if (claim.getStatus() != null && claim.getStatus() != 0) {
            throw new BusinessException(400, "?????????");
        }
        claim.setStatus(2);
        claim.setReviewRemark(blankToNull(remark));
        claimApplicationMapper.update(claim);
        FoundItem item = requireFound(claim.getFoundItemId());
        createNotice(
                claim.getApplicantUserId(),
                "CLAIM",
                "?????" + item.getTitle() + "??????????" + suffixRemark(remark),
                "/item-detail?id=FOUND-" + item.getId()
        );
        auditLogSupport.record(user.getId(), "REJECT_CLAIM", "Rejected claim " + claimId);
        return new ItemDTO.ItemActionVO("FOUND-" + item.getId(), "???????", statusText(item.getStatus()));
    }

    @Override
    public ItemDTO.ItemActionVO completeItem(String itemId) {
        User user = currentUserService.requireUser();
        ReviewTarget target = parseTarget(itemId);
        if ("LOST".equals(target.type())) {
            LostItem item = requireLost(target.numericId());
            ensureOwner(user, item.getUserId());
            item.setStatus(3);
            lostItemMapper.update(item);
            invalidateOverviewCache();
            auditLogSupport.record(user.getId(), "COMPLETE_ITEM", "Completed " + itemId);
            return new ItemDTO.ItemActionVO(itemId, "Lost item marked as completed", statusText(item.getStatus()));
        }
        FoundItem item = requireFound(target.numericId());
        ensureOwner(user, item.getUserId());
        item.setStatus(3);
        foundItemMapper.update(item);
        invalidateOverviewCache();
        auditLogSupport.record(user.getId(), "COMPLETE_ITEM", "Completed " + itemId);
        return new ItemDTO.ItemActionVO(itemId, "Found item marked as completed", statusText(item.getStatus()));
    }

    private void invalidateOverviewCache() {
        cacheService.delete(CacheKeys.SYSTEM_OVERVIEW);
    }

    private void applyLostFields(LostItem item, ItemDTO.CreateItemRequest request) {
        item.setCategoryId(request.getCategoryId());
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setLocation(request.getLocation().trim());
        item.setLostTime(parseTime(request.getEventTime()));
        item.setContact(blankToNull(request.getContact()));
        item.setImages(blankToNull(request.getImages()));
    }

    private void applyLostFields(LostItem item, ItemDTO.UpdateItemRequest request) {
        item.setCategoryId(request.getCategoryId());
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setLocation(request.getLocation().trim());
        item.setLostTime(parseTime(request.getEventTime()));
        item.setContact(blankToNull(request.getContact()));
        item.setImages(blankToNull(request.getImages()));
    }

    private void applyFoundFields(FoundItem item, ItemDTO.CreateItemRequest request) {
        item.setCategoryId(request.getCategoryId());
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setLocation(request.getLocation().trim());
        item.setFoundTime(parseTime(request.getEventTime()));
        item.setPickupMethod(blankToNull(request.getPickupMethod()));
        item.setImages(blankToNull(request.getImages()));
    }

    private void applyFoundFields(FoundItem item, ItemDTO.UpdateItemRequest request) {
        item.setCategoryId(request.getCategoryId());
        item.setTitle(request.getTitle().trim());
        item.setDescription(request.getDescription().trim());
        item.setLocation(request.getLocation().trim());
        item.setFoundTime(parseTime(request.getEventTime()));
        item.setPickupMethod(blankToNull(request.getPickupMethod()));
        item.setImages(blankToNull(request.getImages()));
    }

    private void ensureCategoryExists(Long categoryId) {
        if (itemCategoryMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", categoryId)) == null) {
            throw new BusinessException(400, "Category does not exist");
        }
    }

    private void ensureEditable(Integer status) {
        if (status != null && (status == 3 || status == 4)) {
            throw new BusinessException(400, "Completed or offline items cannot be edited");
        }
    }

    private LocalDateTime parseTime(String rawTime) {
        try {
            return LocalDateTime.parse(rawTime, FORMATTER);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(rawTime);
            } catch (Exception ex) {
                throw new BusinessException(400, "Invalid event time format");
            }
        }
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(FORMATTER);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private QueryWrapper buildLostSearchQuery(String keyword, String location, Long categoryId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where("status in (?, ?)", 1, 3);
        if (!keyword.isBlank()) {
            wrapper.and("title like ?", "%" + keyword + "%");
        }
        if (!location.isBlank()) {
            wrapper.and("location like ?", "%" + location + "%");
        }
        if (categoryId != null) {
            wrapper.and("category_id = ?", categoryId);
        }
        return wrapper;
    }

    private QueryWrapper buildFoundSearchQuery(String keyword, String location, Long categoryId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where("status in (?, ?)", 1, 3);
        if (!keyword.isBlank()) {
            wrapper.and("title like ?", "%" + keyword + "%");
        }
        if (!location.isBlank()) {
            wrapper.and("location like ?", "%" + location + "%");
        }
        if (categoryId != null) {
            wrapper.and("category_id = ?", categoryId);
        }
        return wrapper;
    }

    private QueryWrapper buildLostKeywordSearchQuery(String keyword, Long categoryId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where("status in (?, ?)", 1, 3);
        if (!keyword.isBlank()) {
            String pattern = "%" + keyword + "%";
            wrapper.and("(title like ? or location like ? or description like ?)", pattern, pattern, pattern);
        }
        if (categoryId != null) {
            wrapper.and("category_id = ?", categoryId);
        }
        return wrapper;
    }

    private QueryWrapper buildFoundKeywordSearchQuery(String keyword, Long categoryId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where("status in (?, ?)", 1, 3);
        if (!keyword.isBlank()) {
            String pattern = "%" + keyword + "%";
            wrapper.and("(title like ? or location like ? or description like ?)", pattern, pattern, pattern);
        }
        if (categoryId != null) {
            wrapper.and("category_id = ?", categoryId);
        }
        return wrapper;
    }

    private Map<Long, FoundItem> foundItemsByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return foundItemMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(FoundItem::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, String> usernamesByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (left, right) -> left));
    }

    private QueryWrapper whereIn(String column, Set<Long> ids) {
        String placeholders = String.join(", ", java.util.Collections.nCopies(ids.size(), "?"));
        return QueryWrapper.create().where(column + " in (" + placeholders + ")", ids.toArray());
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

    private boolean reviewEnabled() {
        ConfigDTO.SystemConfigVO config = configService.getSystemConfig();
        return Boolean.TRUE.equals(config.reviewEnabled());
    }

    private Map<Long, String> categoryNameMap() {
        return itemCategoryMapper.selectAll().stream()
                .collect(Collectors.toMap(ItemCategory::getId, ItemCategory::getName, (left, right) -> left));
    }

    private Map<Long, User> userMap() {
        return userMapper.selectAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
    }

    private String displayName(User user) {
        if (user == null) {
            return "????";
        }
        return user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname()
                : user.getUsername();
    }

    private String avatarUrl(User user) {
        return user == null ? null : user.getAvatarUrl();
    }

    private ItemDTO.ClaimVO toClaimVO(ClaimApplication claim, Map<Long, FoundItem> foundItems, Map<Long, String> users) {
        FoundItem item = foundItems.get(claim.getFoundItemId());
        return new ItemDTO.ClaimVO(
                claim.getId(),
                item == null ? "FOUND-" + claim.getFoundItemId() : "FOUND-" + item.getId(),
                item == null ? "????" : item.getTitle(),
                users.getOrDefault(claim.getOwnerUserId(), "????"),
                users.getOrDefault(claim.getApplicantUserId(), "????"),
                claim.getMessage(),
                claimStatusText(claim.getStatus()),
                claim.getReviewRemark(),
                formatTime(claim.getCreatedAt())
        );
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

    private ReviewTarget parseTarget(String id) {
        if (id == null || !id.contains("-")) {
            throw new BusinessException(400, "??????");
        }
        String[] parts = id.split("-", 2);
        try {
            return new ReviewTarget(parts[0].toUpperCase(), Long.parseLong(parts[1]));
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, "??????");
        }
    }

    private Long itemOwnerId(ReviewTarget target) {
        if ("LOST".equals(target.type())) {
            return requireLost(target.numericId()).getUserId();
        }
        return requireFound(target.numericId()).getUserId();
    }

    private LostItem requireLost(Long id) {
        LostItem item = lostItemMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (item == null) {
            throw new BusinessException(404, "???????");
        }
        return item;
    }

    private FoundItem requireFound(Long id) {
        FoundItem item = foundItemMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (item == null) {
            throw new BusinessException(404, "???????");
        }
        return item;
    }

    private ClaimApplication requireClaim(Long id) {
        ClaimApplication claim = claimApplicationMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", id));
        if (claim == null) {
            throw new BusinessException(404, "???????");
        }
        return claim;
    }

    private void createNotice(Long userId, String type, String content, String targetPath) {
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

    private void ensureOwner(User user, Long ownerId) {
        if (!user.getId().equals(ownerId)) {
            throw new BusinessException(403, "???????????");
        }
    }

    private String suffixRemark(String remark) {
        return remark == null || remark.isBlank() ? "" : "????" + remark.trim();
    }

    private record ReviewTarget(String type, Long numericId) {
    }
}

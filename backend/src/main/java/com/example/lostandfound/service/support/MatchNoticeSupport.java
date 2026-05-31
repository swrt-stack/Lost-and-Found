package com.example.lostandfound.service.support;

import com.example.lostandfound.cache.UnreadCounterService;
import com.example.lostandfound.dto.ConfigDTO;
import com.example.lostandfound.entity.FoundItem;
import com.example.lostandfound.entity.ItemCategory;
import com.example.lostandfound.entity.LostItem;
import com.example.lostandfound.entity.MessageNotice;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.FoundItemMapper;
import com.example.lostandfound.mapper.ItemCategoryMapper;
import com.example.lostandfound.mapper.LostItemMapper;
import com.example.lostandfound.mapper.MessageNoticeMapper;
import com.example.lostandfound.mapper.UserMapper;
import com.example.lostandfound.service.ConfigService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MatchNoticeSupport {

    private final LostItemMapper lostItemMapper;
    private final FoundItemMapper foundItemMapper;
    private final MessageNoticeMapper messageNoticeMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final UserMapper userMapper;
    private final ConfigService configService;
    private final UnreadCounterService unreadCounterService;

    public MatchNoticeSupport(LostItemMapper lostItemMapper, FoundItemMapper foundItemMapper,
                              MessageNoticeMapper messageNoticeMapper, ItemCategoryMapper itemCategoryMapper,
                              UserMapper userMapper, ConfigService configService,
                              UnreadCounterService unreadCounterService) {
        this.lostItemMapper = lostItemMapper;
        this.foundItemMapper = foundItemMapper;
        this.messageNoticeMapper = messageNoticeMapper;
        this.itemCategoryMapper = itemCategoryMapper;
        this.userMapper = userMapper;
        this.configService = configService;
        this.unreadCounterService = unreadCounterService;
    }

    public void onLostApproved(LostItem lostItem) {
        if (!noticeEnabled()) {
            return;
        }
        List<FoundItem> foundItems = foundItemMapper.selectListByQuery(buildOppositeItemQuery(lostItem.getCategoryId()));
        Map<Long, User> userMap = loadUsers(foundItems.stream()
                .map(FoundItem::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new)), lostItem.getUserId());
        Map<Long, ItemCategory> categoryMap = loadCategories(foundItems.stream()
                .map(FoundItem::getCategoryId)
                .collect(Collectors.toCollection(LinkedHashSet::new)), lostItem.getCategoryId());
        for (FoundItem foundItem : foundItems) {
            if (isPotentialMatch(lostItem, foundItem)) {
                notifyBothSides(lostItem, foundItem, userMap, categoryMap);
            }
        }
    }

    public void onFoundApproved(FoundItem foundItem) {
        if (!noticeEnabled()) {
            return;
        }
        List<LostItem> lostItems = lostItemMapper.selectListByQuery(buildOppositeItemQuery(foundItem.getCategoryId()));
        Map<Long, User> userMap = loadUsers(lostItems.stream()
                .map(LostItem::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new)), foundItem.getUserId());
        Map<Long, ItemCategory> categoryMap = loadCategories(lostItems.stream()
                .map(LostItem::getCategoryId)
                .collect(Collectors.toCollection(LinkedHashSet::new)), foundItem.getCategoryId());
        for (LostItem lostItem : lostItems) {
            if (isPotentialMatch(lostItem, foundItem)) {
                notifyBothSides(lostItem, foundItem, userMap, categoryMap);
            }
        }
    }

    private boolean noticeEnabled() {
        ConfigDTO.SystemConfigVO config = configService.getSystemConfig();
        return Boolean.TRUE.equals(config.noticeEnabled());
    }

    private boolean isPotentialMatch(LostItem lostItem, FoundItem foundItem) {
        if (lostItem.getCategoryId() != null && foundItem.getCategoryId() != null
                && !lostItem.getCategoryId().equals(foundItem.getCategoryId())) {
            return false;
        }
        int score = 0;
        if (textOverlap(lostItem.getTitle(), foundItem.getTitle())) {
            score += 2;
        }
        if (textOverlap(lostItem.getLocation(), foundItem.getLocation())) {
            score += 1;
        }
        if (textOverlap(lostItem.getDescription(), foundItem.getDescription())) {
            score += 1;
        }
        return score >= 2;
    }

    private boolean textOverlap(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        String[] tokens = left.toLowerCase().split("[\\s,.;:!?\u3001\uff0c\uff1b\uff1a]+");
        String target = right.toLowerCase();
        for (String token : tokens) {
            if (token.length() >= 2 && target.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private void notifyBothSides(LostItem lostItem, FoundItem foundItem,
                                 Map<Long, User> userMap, Map<Long, ItemCategory> categoryMap) {
        String categoryName = categoryMap.containsKey(lostItem.getCategoryId())
                ? categoryMap.get(lostItem.getCategoryId()).getName()
                : "未知分类";

        String lostMessage = "系统为你的遗失物品【" + lostItem.getTitle() + "】匹配到一条可能相关的招领信息【"
                + foundItem.getTitle() + "】，分类为【" + categoryName + "】。可与 " + usernameOf(userMap, foundItem.getUserId()) + " 进一步核实。";
        String foundMessage = "系统为你的招领物品【" + foundItem.getTitle() + "】匹配到一条可能相关的遗失信息【"
                + lostItem.getTitle() + "】，分类为【" + categoryName + "】。可与 " + usernameOf(userMap, lostItem.getUserId()) + " 进一步核实。";

        createMessageIfAbsent(lostItem.getUserId(), lostMessage, "/item-detail?id=FOUND-" + foundItem.getId());
        createMessageIfAbsent(foundItem.getUserId(), foundMessage, "/item-detail?id=LOST-" + lostItem.getId());
    }

    private void createMessageIfAbsent(Long userId, String content, String targetPath) {
        MessageNotice existed = messageNoticeMapper.selectOneByQuery(
                QueryWrapper.create().where("user_id = ? and message_type = ? and content = ?", userId, "MATCH", content)
        );
        if (existed != null) {
            return;
        }
        MessageNotice notice = new MessageNotice();
        notice.setUserId(userId);
        notice.setMessageType("MATCH");
        notice.setContent(content);
        notice.setTargetPath(targetPath);
        notice.setReadFlag(0);
        notice.setCreatedAt(LocalDateTime.now());
        messageNoticeMapper.insert(notice);
        unreadCounterService.incrementMessageUnread(userId);
    }

    private QueryWrapper buildOppositeItemQuery(Long categoryId) {
        QueryWrapper query = QueryWrapper.create().where("status = ?", 1);
        if (categoryId != null) {
            query.and("category_id = ?", categoryId);
        }
        return query;
    }

    private Map<Long, User> loadUsers(Set<Long> ids, Long extraUserId) {
        if (extraUserId != null) {
            ids.add(extraUserId);
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, ItemCategory> loadCategories(Set<Long> ids, Long extraCategoryId) {
        if (extraCategoryId != null) {
            ids.add(extraCategoryId);
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        return itemCategoryMapper.selectListByQuery(whereIn("id", ids)).stream()
                .collect(Collectors.toMap(ItemCategory::getId, Function.identity(), (left, right) -> left));
    }

    private QueryWrapper whereIn(String column, Set<Long> ids) {
        String placeholders = String.join(", ", Collections.nCopies(ids.size(), "?"));
        return QueryWrapper.create().where(column + " in (" + placeholders + ")", ids.toArray());
    }

    private String usernameOf(Map<Long, User> userMap, Long userId) {
        User user = userMap.get(userId);
        return user == null ? "另一位用户" : user.getUsername();
    }
}

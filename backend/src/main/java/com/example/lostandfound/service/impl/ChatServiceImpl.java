package com.example.lostandfound.service.impl;

import com.example.lostandfound.cache.UnreadCounterService;
import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.dto.ChatDTO;
import com.example.lostandfound.entity.FoundItem;
import com.example.lostandfound.entity.ItemChatMessage;
import com.example.lostandfound.entity.LostItem;
import com.example.lostandfound.entity.MessageNotice;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.FoundItemMapper;
import com.example.lostandfound.mapper.ItemChatMessageMapper;
import com.example.lostandfound.mapper.LostItemMapper;
import com.example.lostandfound.mapper.MessageNoticeMapper;
import com.example.lostandfound.mapper.UserMapper;
import com.example.lostandfound.security.CurrentUserService;
import com.example.lostandfound.service.ChatService;
import com.example.lostandfound.service.support.AuditLogSupport;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ItemChatMessageMapper itemChatMessageMapper;
    private final LostItemMapper lostItemMapper;
    private final FoundItemMapper foundItemMapper;
    private final UserMapper userMapper;
    private final MessageNoticeMapper messageNoticeMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogSupport auditLogSupport;
    private final UnreadCounterService unreadCounterService;

    public ChatServiceImpl(ItemChatMessageMapper itemChatMessageMapper,
                           LostItemMapper lostItemMapper,
                           FoundItemMapper foundItemMapper,
                           UserMapper userMapper,
                           MessageNoticeMapper messageNoticeMapper,
                           CurrentUserService currentUserService,
                           AuditLogSupport auditLogSupport,
                           UnreadCounterService unreadCounterService) {
        this.itemChatMessageMapper = itemChatMessageMapper;
        this.lostItemMapper = lostItemMapper;
        this.foundItemMapper = foundItemMapper;
        this.userMapper = userMapper;
        this.messageNoticeMapper = messageNoticeMapper;
        this.currentUserService = currentUserService;
        this.auditLogSupport = auditLogSupport;
        this.unreadCounterService = unreadCounterService;
    }

    @Override
    public List<ChatDTO.ChatConversationVO> listConversations() {
        User currentUser = currentUserService.requireUser();
        List<ItemChatMessage> messages = itemChatMessageMapper.selectListByQuery(QueryWrapper.create()
                .where("sender_user_id = ? or receiver_user_id = ?", currentUser.getId(), currentUser.getId())
                .orderBy("created_at asc"));
        if (messages.isEmpty()) {
            return List.of();
        }

        Set<String> itemIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        Map<String, List<ItemChatMessage>> grouped = new LinkedHashMap<>();
        for (ItemChatMessage message : messages) {
            Long counterpartId = currentUser.getId().equals(message.getSenderUserId())
                    ? message.getReceiverUserId()
                    : message.getSenderUserId();
            String key = message.getItemId() + "::" + counterpartId;
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(message);
            itemIds.add(message.getItemId());
            userIds.add(counterpartId);
        }

        Map<Long, User> users = usersByIds(userIds);
        Map<String, String> itemTitles = itemTitleMap(itemIds);

        return grouped.entrySet().stream()
                .map(entry -> {
                    String[] keyParts = entry.getKey().split("::", 2);
                    String itemId = keyParts[0];
                    Long counterpartId = Long.parseLong(keyParts[1]);
                    User counterpart = users.get(counterpartId);
                    List<ItemChatMessage> thread = entry.getValue();
                    ItemChatMessage lastMessage = thread.get(thread.size() - 1);
                    int unreadCount = (int) thread.stream()
                            .filter(message -> currentUser.getId().equals(message.getReceiverUserId()) && !isRead(message))
                            .count();
                    return new ChatDTO.ChatConversationVO(
                            itemId,
                            itemTitles.getOrDefault(itemId, itemId),
                            counterpartId,
                            counterpart == null ? "-" : counterpart.getUsername(),
                            counterpart == null ? "-" : displayName(counterpart),
                            counterpart == null ? null : counterpart.getAvatarUrl(),
                            lastMessage.getContent(),
                            formatTime(lastMessage.getCreatedAt()),
                            unreadCount
                    );
                })
                .sorted(Comparator.comparing(ChatDTO.ChatConversationVO::lastTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public ChatDTO.ChatSummaryVO summary() {
        User currentUser = currentUserService.requireUser();
        List<ChatDTO.ChatConversationVO> conversations = listConversations();
        int unreadCount = unreadCounterService.getOrLoadChatUnreadCount(currentUser.getId(), () -> conversations.stream()
                .map(ChatDTO.ChatConversationVO::unreadCount)
                .filter(count -> count != null)
                .mapToInt(Integer::intValue)
                .sum());
        return new ChatDTO.ChatSummaryVO(conversations.size(), unreadCount);
    }

    @Override
    public List<ChatDTO.ChatContactVO> listContacts(String itemId) {
        User currentUser = currentUserService.requireUser();
        ItemContext context = requireItemContext(itemId);
        if (!currentUser.getId().equals(context.ownerUserId())) {
            ensurePublicChatAvailable(context);
        }

        List<ItemChatMessage> messages = itemChatMessageMapper.selectListByQuery(QueryWrapper.create()
                .where("item_id = ? and (sender_user_id = ? or receiver_user_id = ?)", itemId, currentUser.getId(), currentUser.getId())
                .orderBy("created_at asc"));

        if (!currentUser.getId().equals(context.ownerUserId())) {
            User owner = requireUser(context.ownerUserId());
            ItemChatMessage lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);
            int unreadCount = (int) messages.stream()
                    .filter(entry -> currentUser.getId().equals(entry.getReceiverUserId()) && !isRead(entry))
                    .count();
            return List.of(new ChatDTO.ChatContactVO(
                    owner.getId(),
                    owner.getUsername(),
                    displayName(owner),
                    owner.getAvatarUrl(),
                    lastMessage == null ? "" : lastMessage.getContent(),
                    lastMessage == null ? "" : formatTime(lastMessage.getCreatedAt()),
                    unreadCount
            ));
        }

        Map<Long, List<ItemChatMessage>> grouped = new LinkedHashMap<>();
        for (ItemChatMessage message : messages) {
            Long counterpartId = currentUser.getId().equals(message.getSenderUserId())
                    ? message.getReceiverUserId()
                    : message.getSenderUserId();
            grouped.computeIfAbsent(counterpartId, key -> new ArrayList<>()).add(message);
        }

        Map<Long, User> users = usersByIds(grouped.keySet());
        return grouped.entrySet().stream()
                .map(entry -> {
                    List<ItemChatMessage> thread = entry.getValue();
                    ItemChatMessage lastMessage = thread.get(thread.size() - 1);
                    int unreadCount = (int) thread.stream()
                            .filter(message -> currentUser.getId().equals(message.getReceiverUserId()) && !isRead(message))
                            .count();
                    User counterpart = users.get(entry.getKey());
                    return new ChatDTO.ChatContactVO(
                            entry.getKey(),
                            counterpart == null ? "-" : counterpart.getUsername(),
                            counterpart == null ? "-" : displayName(counterpart),
                            counterpart == null ? null : counterpart.getAvatarUrl(),
                            lastMessage.getContent(),
                            formatTime(lastMessage.getCreatedAt()),
                            unreadCount
                    );
                })
                .sorted(Comparator.comparing(ChatDTO.ChatContactVO::lastTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public ChatDTO.ChatThreadVO getThread(String itemId, Long counterpartUserId) {
        User currentUser = currentUserService.requireUser();
        ItemContext context = requireItemContext(itemId);
        Long resolvedCounterpartUserId = resolveCounterpartUserId(currentUser, context, counterpartUserId);
        User counterpart = requireUser(resolvedCounterpartUserId);

        List<ItemChatMessage> messages = loadThreadMessages(itemId, currentUser.getId(), resolvedCounterpartUserId);
        boolean changed = false;
        int readCount = 0;
        for (ItemChatMessage message : messages) {
            if (currentUser.getId().equals(message.getReceiverUserId()) && !isRead(message)) {
                message.setReadFlag(1);
                itemChatMessageMapper.update(message);
                changed = true;
                readCount++;
            }
        }
        if (readCount > 0) {
            unreadCounterService.decrementChatUnread(currentUser.getId(), readCount);
        }
        if (changed) {
            messages = loadThreadMessages(itemId, currentUser.getId(), resolvedCounterpartUserId);
        }

        return new ChatDTO.ChatThreadVO(
                itemId,
                context.itemTitle(),
                counterpart.getId(),
                displayName(counterpart),
                counterpart.getAvatarUrl(),
                currentUser.getId().equals(context.ownerUserId()),
                messages.stream()
                        .map(message -> new ChatDTO.ChatMessageVO(
                                message.getId(),
                                message.getSenderUserId(),
                                displayName(message.getSenderUserId(), currentUser, counterpart),
                                avatarUrlOf(message.getSenderUserId(), currentUser, counterpart),
                                currentUser.getId().equals(message.getSenderUserId()),
                                message.getContent(),
                                isRead(message),
                                formatTime(message.getCreatedAt())
                        ))
                        .toList()
        );
    }

    @Override
    public ChatDTO.ChatActionVO sendMessage(String itemId, Long counterpartUserId, String content) {
        User currentUser = currentUserService.requireUser();
        ItemContext context = requireItemContext(itemId);
        Long resolvedCounterpartUserId = resolveCounterpartUserId(currentUser, context, counterpartUserId);
        if (currentUser.getId().equals(resolvedCounterpartUserId)) {
            throw new BusinessException(400, "不能给自己发送消息");
        }

        User counterpart = requireUser(resolvedCounterpartUserId);
        ItemChatMessage message = new ItemChatMessage();
        message.setItemId(itemId);
        message.setSenderUserId(currentUser.getId());
        message.setReceiverUserId(counterpart.getId());
        message.setContent(content.trim());
        message.setReadFlag(0);
        message.setCreatedAt(LocalDateTime.now());
        itemChatMessageMapper.insert(message);
        unreadCounterService.incrementChatUnread(counterpart.getId());

        createNotice(
                counterpart.getId(),
                "CHAT",
                displayName(currentUser) + " 就「" + context.itemTitle() + "」给你发来一条新消息",
                "/messages?itemId=" + itemId + "&counterpartUserId=" + currentUser.getId()
        );
        auditLogSupport.record(currentUser.getId(), "SEND_CHAT_MESSAGE", "Sent chat message for " + itemId);
        return new ChatDTO.ChatActionVO(itemId, counterpart.getId(), "消息已发送");
    }

    private Long resolveCounterpartUserId(User currentUser, ItemContext context, Long counterpartUserId) {
        if (!currentUser.getId().equals(context.ownerUserId())) {
            ensurePublicChatAvailable(context);
            return context.ownerUserId();
        }
        if (counterpartUserId == null) {
            throw new BusinessException(400, "请指定沟通对象");
        }
        if (!hasExistingConversation(context.itemId(), currentUser.getId(), counterpartUserId)) {
            throw new BusinessException(403, "对方尚未发起沟通，暂不能主动回复");
        }
        return counterpartUserId;
    }

    private boolean hasExistingConversation(String itemId, Long ownerUserId, Long counterpartUserId) {
        ItemChatMessage existed = itemChatMessageMapper.selectOneByQuery(QueryWrapper.create()
                .where("item_id = ? and ((sender_user_id = ? and receiver_user_id = ?) or (sender_user_id = ? and receiver_user_id = ?))",
                        itemId, ownerUserId, counterpartUserId, counterpartUserId, ownerUserId));
        return existed != null;
    }

    private List<ItemChatMessage> loadThreadMessages(String itemId, Long currentUserId, Long counterpartUserId) {
        return itemChatMessageMapper.selectListByQuery(QueryWrapper.create()
                .where("item_id = ? and ((sender_user_id = ? and receiver_user_id = ?) or (sender_user_id = ? and receiver_user_id = ?))",
                        itemId, currentUserId, counterpartUserId, counterpartUserId, currentUserId)
                .orderBy("created_at asc"));
    }

    private void ensurePublicChatAvailable(ItemContext context) {
        if (!isPublicChatStatus(context.status())) {
            throw new BusinessException(403, "当前物品状态暂不支持在线沟通");
        }
    }

    private boolean isPublicChatStatus(Integer status) {
        return status != null && (status == 1 || status == 3);
    }

    private ItemContext requireItemContext(String itemId) {
        ReviewTarget target = parseTarget(itemId);
        if ("LOST".equals(target.type())) {
            LostItem item = lostItemMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", target.numericId()));
            if (item == null) {
                throw new BusinessException(404, "物品不存在");
            }
            return new ItemContext(itemId, item.getTitle(), item.getUserId(), item.getStatus());
        }

        FoundItem item = foundItemMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", target.numericId()));
        if (item == null) {
            throw new BusinessException(404, "物品不存在");
        }
        return new ItemContext(itemId, item.getTitle(), item.getUserId(), item.getStatus());
    }

    private ReviewTarget parseTarget(String id) {
        if (id == null || !id.contains("-")) {
            throw new BusinessException(400, "物品编号无效");
        }
        String[] parts = id.split("-", 2);
        try {
            return new ReviewTarget(parts[0].toUpperCase(), Long.parseLong(parts[1]));
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, "物品编号无效");
        }
    }

    private Map<Long, User> usersByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectListByQuery(whereIn("id", ids.stream().map(String::valueOf).toList())).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));
    }

    private Map<String, String> itemTitleMap(Set<String> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> lostIds = new HashSet<>();
        Set<Long> foundIds = new HashSet<>();
        for (String itemId : itemIds) {
            ReviewTarget target = parseTarget(itemId);
            if ("LOST".equals(target.type())) {
                lostIds.add(target.numericId());
            } else if ("FOUND".equals(target.type())) {
                foundIds.add(target.numericId());
            }
        }

        Map<String, String> result = new LinkedHashMap<>();
        if (!lostIds.isEmpty()) {
            lostItemMapper.selectListByQuery(whereIn("id", lostIds.stream().map(String::valueOf).toList()))
                    .forEach(item -> result.put("LOST-" + item.getId(), item.getTitle()));
        }
        if (!foundIds.isEmpty()) {
            foundItemMapper.selectListByQuery(whereIn("id", foundIds.stream().map(String::valueOf).toList()))
                    .forEach(item -> result.put("FOUND-" + item.getId(), item.getTitle()));
        }
        itemIds.forEach(itemId -> result.putIfAbsent(itemId, itemId));
        return result;
    }

    private QueryWrapper whereIn(String column, List<String> values) {
        if (values.isEmpty()) {
            return QueryWrapper.create().where("1 = 0");
        }
        String placeholders = String.join(", ", Collections.nCopies(values.size(), "?"));
        return QueryWrapper.create().where(column + " in (" + placeholders + ")", values.toArray());
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectOneByQuery(QueryWrapper.create().where("id = ?", userId));
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
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

    private String displayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private String displayName(Long userId, User currentUser, User counterpart) {
        if (currentUser.getId().equals(userId)) {
            return displayName(currentUser);
        }
        if (counterpart.getId().equals(userId)) {
            return displayName(counterpart);
        }
        return "-";
    }

    private String avatarUrlOf(Long userId, User currentUser, User counterpart) {
        if (currentUser.getId().equals(userId)) {
            return currentUser.getAvatarUrl();
        }
        if (counterpart.getId().equals(userId)) {
            return counterpart.getAvatarUrl();
        }
        return null;
    }

    private boolean isRead(ItemChatMessage message) {
        return message.getReadFlag() != null && message.getReadFlag() == 1;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(FORMATTER);
    }

    private record ReviewTarget(String type, Long numericId) {
    }

    private record ItemContext(String itemId, String itemTitle, Long ownerUserId, Integer status) {
    }
}

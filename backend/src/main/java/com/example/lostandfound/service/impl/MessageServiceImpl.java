package com.example.lostandfound.service.impl;

import com.example.lostandfound.cache.UnreadCounterService;
import com.example.lostandfound.common.BusinessException;
import com.example.lostandfound.dto.MessageDTO;
import com.example.lostandfound.entity.MessageNotice;
import com.example.lostandfound.entity.User;
import com.example.lostandfound.mapper.MessageNoticeMapper;
import com.example.lostandfound.security.CurrentUserService;
import com.example.lostandfound.service.MessageService;
import com.example.lostandfound.service.support.AuditLogSupport;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MessageNoticeMapper messageNoticeMapper;
    private final CurrentUserService currentUserService;
    private final AuditLogSupport auditLogSupport;
    private final UnreadCounterService unreadCounterService;

    public MessageServiceImpl(MessageNoticeMapper messageNoticeMapper, CurrentUserService currentUserService,
                              AuditLogSupport auditLogSupport, UnreadCounterService unreadCounterService) {
        this.messageNoticeMapper = messageNoticeMapper;
        this.currentUserService = currentUserService;
        this.auditLogSupport = auditLogSupport;
        this.unreadCounterService = unreadCounterService;
    }

    @Override
    public List<MessageDTO.MessageVO> listMessages() {
        User user = currentUserService.requireUser();
        return messageNoticeMapper.selectListByQuery(QueryWrapper.create().where("user_id = ?", user.getId())).stream()
                .sorted(Comparator.comparing(MessageNotice::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> new MessageDTO.MessageVO(
                        item.getId(),
                        item.getMessageType(),
                        item.getContent(),
                        item.getTargetPath(),
                        item.getReadFlag() != null && item.getReadFlag() == 1,
                        item.getCreatedAt() == null ? "" : item.getCreatedAt().format(FORMATTER)
                ))
                .toList();
    }

    @Override
    public MessageDTO.MessageSummaryVO summary() {
        User user = currentUserService.requireUser();
        List<MessageNotice> notices = messageNoticeMapper.selectListByQuery(QueryWrapper.create().where("user_id = ?", user.getId()));
        int unreadCount = unreadCounterService.getOrLoadMessageUnreadCount(user.getId(), () -> (int) notices.stream()
                .filter(item -> item.getReadFlag() == null || item.getReadFlag() == 0)
                .count());
        return new MessageDTO.MessageSummaryVO(notices.size(), unreadCount);
    }

    @Override
    public MessageDTO.MessageActionVO markRead(Long id) {
        User user = currentUserService.requireUser();
        MessageNotice notice = messageNoticeMapper.selectOneByQuery(
                QueryWrapper.create().where("id = ? and user_id = ?", id, user.getId())
        );
        if (notice == null) {
            throw new BusinessException(404, "Message not found");
        }
        boolean unread = notice.getReadFlag() == null || notice.getReadFlag() == 0;
        notice.setReadFlag(1);
        messageNoticeMapper.update(notice);
        if (unread) {
            unreadCounterService.decrementMessageUnread(user.getId(), 1);
        }
        auditLogSupport.record(user.getId(), "READ_MESSAGE", "Read message " + id);
        return new MessageDTO.MessageActionVO(id, "Message marked as read");
    }

    @Override
    public MessageDTO.BatchActionVO markAllRead() {
        User user = currentUserService.requireUser();
        List<MessageNotice> notices = messageNoticeMapper.selectListByQuery(QueryWrapper.create().where("user_id = ?", user.getId()));
        int affectedCount = 0;
        for (MessageNotice notice : notices) {
            if (notice.getReadFlag() != null && notice.getReadFlag() == 1) {
                continue;
            }
            notice.setReadFlag(1);
            messageNoticeMapper.update(notice);
            affectedCount++;
        }
        unreadCounterService.resetMessageUnread(user.getId());
        auditLogSupport.record(user.getId(), "READ_ALL_MESSAGES", "Read all messages");
        return new MessageDTO.BatchActionVO(affectedCount, "All messages marked as read");
    }
}

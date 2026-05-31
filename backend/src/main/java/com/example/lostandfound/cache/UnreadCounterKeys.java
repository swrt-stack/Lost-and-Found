package com.example.lostandfound.cache;

public final class UnreadCounterKeys {

    private static final String MESSAGE_UNREAD_PREFIX = "counter:unread:message:";
    private static final String CHAT_UNREAD_PREFIX = "counter:unread:chat:";

    private UnreadCounterKeys() {
    }

    public static String messageUnread(Long userId) {
        return MESSAGE_UNREAD_PREFIX + userId;
    }

    public static String chatUnread(Long userId) {
        return CHAT_UNREAD_PREFIX + userId;
    }
}

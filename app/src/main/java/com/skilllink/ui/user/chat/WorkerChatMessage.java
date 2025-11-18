package com.skilllink.ui.user.chat;

final class WorkerChatMessage {

    private final boolean isUser;
    private final String content;
    private final String timestamp;

    private WorkerChatMessage(boolean isUser, String content, String timestamp) {
        this.isUser = isUser;
        this.content = content;
        this.timestamp = timestamp;
    }

    static WorkerChatMessage user(String content, String timestamp) {
        return new WorkerChatMessage(true, content, timestamp);
    }

    static WorkerChatMessage worker(String content, String timestamp) {
        return new WorkerChatMessage(false, content, timestamp);
    }

    boolean isUser() {
        return isUser;
    }

    String getContent() {
        return content;
    }

    String getTimestamp() {
        return timestamp;
    }
}

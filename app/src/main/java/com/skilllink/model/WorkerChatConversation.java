package com.skilllink.model;

import androidx.annotation.Nullable;

public class WorkerChatConversation {

    private final String conversationId;
    private final String userId;
    private final String userName;
    private final String userContact;
    private final String userAvatarUri;
    private final String serviceId;
    private final String serviceName;
    private final String serviceCategory;
    private final String lastMessage;
    private final long lastMessageTimestamp;
    private final boolean hasUnread;

    public WorkerChatConversation(String conversationId,
                                  String userId,
                                  String userName,
                                  String userContact,
                                  String userAvatarUri,
                                  String serviceId,
                                  String serviceName,
                                  String serviceCategory,
                                  String lastMessage,
                                  long lastMessageTimestamp,
                                  boolean hasUnread) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.userName = userName;
        this.userContact = userContact;
        this.userAvatarUri = userAvatarUri;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.serviceCategory = serviceCategory;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.hasUnread = hasUnread;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    @Nullable
    public String getUserContact() {
        return userContact;
    }

    @Nullable
    public String getUserAvatarUri() {
        return userAvatarUri;
    }

    @Nullable
    public String getServiceId() {
        return serviceId;
    }

    @Nullable
    public String getServiceName() {
        return serviceName;
    }

    @Nullable
    public String getServiceCategory() {
        return serviceCategory;
    }

    @Nullable
    public String getLastMessage() {
        return lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public boolean hasUnread() {
        return hasUnread;
    }
}

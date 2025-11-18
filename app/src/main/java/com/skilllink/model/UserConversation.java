package com.skilllink.model;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class UserConversation {

    private final String id;
    private final String workerId;
    private final String title;
    private final String subtitle;
    private final String serviceId;
    private final String serviceName;
    private final String serviceCategory;
    private final String workerImageUri;
    private final String lastMessage;
    private final long lastMessageTimestamp;
    private final boolean hasUnread;

    public UserConversation(String id,
                            String workerId,
                            String title,
                            String subtitle,
                            String serviceId,
                            String serviceName,
                            String serviceCategory,
                            String workerImageUri,
                            String lastMessage,
                            long lastMessageTimestamp,
                            boolean hasUnread) {
        this.id = id;
        this.workerId = workerId;
        this.title = title;
        this.subtitle = subtitle;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.serviceCategory = serviceCategory;
        this.workerImageUri = workerImageUri;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.hasUnread = hasUnread;
    }

    public static UserConversation fromJson(JSONObject object) throws JSONException {
        if (object == null) {
            throw new JSONException("Conversation payload cannot be null");
        }
        return new UserConversation(
                object.optString("id", null),
                object.optString("workerId", null),
                object.optString("title", null),
                object.optString("subtitle", null),
                object.optString("serviceId", null),
                object.optString("serviceName", null),
                object.optString("serviceCategory", null),
                object.optString("workerImageUri", null),
                object.optString("lastMessage", null),
                object.optLong("lastMessageTimestamp", System.currentTimeMillis()),
                object.optBoolean("hasUnread", false)
        );
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("workerId", workerId);
        object.put("title", title);
        object.put("subtitle", subtitle);
        object.put("serviceId", serviceId);
        object.put("serviceName", serviceName);
        object.put("serviceCategory", serviceCategory);
        object.put("workerImageUri", workerImageUri);
        object.put("lastMessage", lastMessage);
        object.put("lastMessageTimestamp", lastMessageTimestamp);
        object.put("hasUnread", hasUnread);
        return object;
    }

    public UserConversation withLastMessage(String message, long timestamp, boolean unread) {
        return new UserConversation(
                id,
                workerId,
                title,
                subtitle,
                serviceId,
                serviceName,
                serviceCategory,
                workerImageUri,
                message,
                timestamp,
                unread
        );
    }

    public String getId() {
        return id;
    }

    @Nullable
    public String getWorkerId() {
        return workerId;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
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
    public String getWorkerImageUri() {
        return workerImageUri;
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

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("workerId", workerId);
        map.put("title", title);
        map.put("subtitle", subtitle);
        map.put("serviceId", serviceId);
        map.put("serviceName", serviceName);
        map.put("serviceCategory", serviceCategory);
        map.put("workerImageUri", workerImageUri);
        map.put("lastMessage", lastMessage);
        map.put("lastMessageTimestamp", lastMessageTimestamp);
        map.put("hasUnread", hasUnread);
        return map;
    }

    public static UserConversation fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        return new UserConversation(
                doc.getString("id"),
                doc.getString("workerId"),
                doc.getString("title"),
                doc.getString("subtitle"),
                doc.getString("serviceId"),
                doc.getString("serviceName"),
                doc.getString("serviceCategory"),
                doc.getString("workerImageUri"),
                doc.getString("lastMessage"),
                doc.getLong("lastMessageTimestamp") != null ? doc.getLong("lastMessageTimestamp") : System.currentTimeMillis(),
                doc.getBoolean("hasUnread") != null ? doc.getBoolean("hasUnread") : false
        );
    }
}

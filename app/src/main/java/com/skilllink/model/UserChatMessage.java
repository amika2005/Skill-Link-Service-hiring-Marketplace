package com.skilllink.model;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserChatMessage {

    private final String id;
    private final String conversationId;
    private final String senderId;
    private final String senderName;
    private final boolean fromUser;
    private final String content;
    private final long timestamp;

    public UserChatMessage(String id,
                           String conversationId,
                           boolean fromUser,
                           String content,
                           long timestamp) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.senderId = null;
        this.senderName = null;
        this.fromUser = fromUser;
        this.content = content;
        this.timestamp = timestamp;
    }

    public UserChatMessage(String id,
                           String conversationId,
                           String senderId,
                           String senderName,
                           String content,
                           long timestamp,
                           boolean fromUser) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.fromUser = fromUser;
        this.content = content;
        this.timestamp = timestamp;
    }

    public static UserChatMessage user(String conversationId, String content, long timestamp) {
        return new UserChatMessage(null, conversationId, true, content, timestamp);
    }

    public static UserChatMessage worker(String conversationId, String content, long timestamp) {
        return new UserChatMessage(null, conversationId, false, content, timestamp);
    }

    @NonNull
    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("conversationId", conversationId);
        object.put("fromUser", fromUser);
        object.put("content", content);
        object.put("timestamp", timestamp);
        return object;
    }

    public static UserChatMessage fromJson(JSONObject object) throws JSONException {
        if (object == null) {
            throw new JSONException("Message payload cannot be null");
        }
        return new UserChatMessage(
                object.optString("id", null),
                object.optString("conversationId", null),
                object.optBoolean("fromUser", false),
                object.optString("content", null),
                object.optLong("timestamp", System.currentTimeMillis())
        );
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public boolean isFromUser() {
        return fromUser;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getMessageId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getMessageText() {
        return content;
    }

    public String getSenderName() {
        return senderName;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("conversationId", conversationId);
        map.put("fromUser", fromUser);
        map.put("content", content);
        map.put("timestamp", timestamp);
        return map;
    }

    public static UserChatMessage fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        return new UserChatMessage(
                doc.getString("id"),
                doc.getString("conversationId"),
                doc.getBoolean("fromUser") != null ? doc.getBoolean("fromUser") : false,
                doc.getString("content"),
                doc.getLong("timestamp") != null ? doc.getLong("timestamp") : System.currentTimeMillis()
        );
    }
}

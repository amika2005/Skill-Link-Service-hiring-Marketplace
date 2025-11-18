package com.skilllink.data.firebase;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.skilllink.BuildConfig;
import com.skilllink.model.UserChatMessage;
import com.skilllink.model.UserConversation;
import com.skilllink.model.WorkerChatConversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseChatStore {

    private static final String COLLECTION_CONVERSATIONS = "conversations";
    private static final String SUBCOLLECTION_MESSAGES = "messages";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String FIELD_USER_UNREAD = "userUnread";
    private static final String FIELD_WORKER_UNREAD = "workerUnread";

    private final FirebaseFirestore firestore;
    private final boolean enabled;

    private FirebaseChatStore() {
        enabled = BuildConfig.FIRESTORE_ENABLED;
        firestore = enabled ? FirebaseFirestore.getInstance() : null;
    }

    private static class Holder {
        private static final FirebaseChatStore INSTANCE = new FirebaseChatStore();
    }

    public static FirebaseChatStore getInstance() {
        return Holder.INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public ListenerRegistration listenToUserConversations(@Nullable String userId,
                                                          @NonNull ConversationListener listener) {
        if (!enabled) {
            listener.onConversations(Collections.<UserConversation>emptyList());
            return null;
        }

        if (TextUtils.isEmpty(userId)) {
            listener.onConversations(Collections.<UserConversation>emptyList());
            return null;
        }

        return firestore.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> handleConversationSnapshot(false, snapshots, error, listener, null));
    }

    @Nullable
    public ListenerRegistration listenToWorkerConversations(@Nullable String workerId,
                                                            @NonNull WorkerConversationListener listener) {
        if (!enabled) {
            listener.onConversations(Collections.<WorkerChatConversation>emptyList());
            return null;
        }

        if (TextUtils.isEmpty(workerId)) {
            listener.onConversations(Collections.<WorkerChatConversation>emptyList());
            return null;
        }

        return firestore.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("workerId", workerId)
                .addSnapshotListener((snapshots, error) -> handleConversationSnapshot(true, snapshots, error, null, listener));
    }

    private void handleConversationSnapshot(boolean forWorker,
                                            @Nullable QuerySnapshot snapshots,
                                            @Nullable Exception error,
                                            @Nullable ConversationListener userListener,
                                            @Nullable WorkerConversationListener workerListener) {
        if (error != null) {
            if (userListener != null) {
                userListener.onError(error);
            }
            if (workerListener != null) {
                workerListener.onError(error);
            }
            return;
        }

        if (snapshots == null) {
            if (userListener != null) {
                userListener.onConversations(Collections.<UserConversation>emptyList());
            }
            if (workerListener != null) {
                workerListener.onConversations(Collections.<WorkerChatConversation>emptyList());
            }
            return;
        }

        if (forWorker) {
            List<WorkerChatConversation> conversations = new ArrayList<>();
            for (DocumentSnapshot document : snapshots.getDocuments()) {
                WorkerChatConversation conversation = toWorkerConversation(document);
                if (conversation != null) {
                    conversations.add(conversation);
                }
            }
            Collections.sort(conversations, (o1, o2) -> Long.compare(o2.getLastMessageTimestamp(), o1.getLastMessageTimestamp()));
            if (workerListener != null) {
                workerListener.onConversations(conversations);
            }
        } else {
            List<UserConversation> conversations = new ArrayList<>();
            for (DocumentSnapshot document : snapshots.getDocuments()) {
                UserConversation conversation = toUserConversation(document);
                if (conversation != null) {
                    conversations.add(conversation);
                }
            }
            Collections.sort(conversations, (o1, o2) -> Long.compare(o2.getLastMessageTimestamp(), o1.getLastMessageTimestamp()));
            if (userListener != null) {
                userListener.onConversations(conversations);
            }
        }
    }

    @Nullable
    private UserConversation toUserConversation(@Nullable DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        String conversationId = document.getId();
        String workerId = document.getString("workerId");
        String workerName = document.getString("workerName");
        String workerOccupation = document.getString("workerOccupation");
        String serviceId = document.getString("serviceId");
        String serviceName = document.getString("serviceName");
        String serviceCategory = document.getString("serviceCategory");
        String workerImageUri = document.getString("workerImageUri");
        String lastMessage = document.getString("lastMessage");
        Long timestampValue = document.getLong("lastMessageTimestamp");
        boolean hasUnread = Boolean.TRUE.equals(document.getBoolean(FIELD_USER_UNREAD));

        long timestamp = timestampValue != null ? timestampValue : 0L;

        return new UserConversation(
                conversationId,
                workerId,
                workerName,
                workerOccupation,
                serviceId,
                serviceName,
                serviceCategory,
                workerImageUri,
                lastMessage,
                timestamp,
                hasUnread
        );
    }

    @Nullable
    private WorkerChatConversation toWorkerConversation(@Nullable DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        String conversationId = document.getId();
        String userId = document.getString("userId");
        String userName = document.getString("userName");
        String userContact = document.getString("userContact");
        String userAvatarUri = document.getString("userAvatarUri");
        String serviceId = document.getString("serviceId");
        String serviceName = document.getString("serviceName");
        String serviceCategory = document.getString("serviceCategory");
        String lastMessage = document.getString("lastMessage");
        Long timestampValue = document.getLong("lastMessageTimestamp");
        boolean hasUnread = Boolean.TRUE.equals(document.getBoolean(FIELD_WORKER_UNREAD));

        long timestamp = timestampValue != null ? timestampValue : 0L;

        return new WorkerChatConversation(
                conversationId,
                userId,
                userName,
                userContact,
                userAvatarUri,
                serviceId,
                serviceName,
                serviceCategory,
                lastMessage,
                timestamp,
                hasUnread
        );
    }

    @Nullable
    public ListenerRegistration listenToMessages(@Nullable String conversationId,
                                                 @NonNull MessageListener listener) {
        if (!enabled) {
            listener.onMessages(Collections.<UserChatMessage>emptyList());
            return null;
        }

        if (TextUtils.isEmpty(conversationId)) {
            listener.onMessages(Collections.<UserChatMessage>emptyList());
            return null;
        }

        return firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(SUBCOLLECTION_MESSAGES)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots == null) {
                        listener.onMessages(Collections.<UserChatMessage>emptyList());
                        return;
                    }

                    List<UserChatMessage> messages = new ArrayList<>();
                    for (DocumentSnapshot document : snapshots.getDocuments()) {
                        UserChatMessage message = toChatMessage(document);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    listener.onMessages(messages);
                });
    }

    @Nullable
    private UserChatMessage toChatMessage(@Nullable DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        String id = document.getId();
        String conversationId = document.getString("conversationId");
        if (TextUtils.isEmpty(conversationId)) {
            conversationId = document.getReference().getParent().getParent() != null
                    ? document.getReference().getParent().getParent().getId()
                    : null;
        }
        String content = document.getString("content");
        String senderType = document.getString("senderType");
        Long timestampValue = document.getLong(FIELD_TIMESTAMP);

        long timestamp = timestampValue != null ? timestampValue : System.currentTimeMillis();
        boolean fromUser = "USER".equalsIgnoreCase(senderType);

        return new UserChatMessage(id, conversationId, fromUser, content != null ? content : "", timestamp);
    }

    public void sendMessage(@NonNull MessagePayload payload, @Nullable CompletionListener listener) {
        if (!enabled) {
            if (listener != null) {
                listener.onSuccess();
            }
            return;
        }

        if (TextUtils.isEmpty(payload.getConversationId()) || TextUtils.isEmpty(payload.getContent())) {
            if (listener != null) {
                listener.onError(new IllegalArgumentException("Conversation id and content are required"));
            }
            return;
        }

        DocumentReference conversationRef = firestore.collection(COLLECTION_CONVERSATIONS)
                .document(payload.getConversationId());
        DocumentReference messageRef = conversationRef
                .collection(SUBCOLLECTION_MESSAGES)
                .document();

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", messageRef.getId());
        messageData.put("conversationId", payload.getConversationId());
        messageData.put("content", payload.getContent());
        messageData.put("senderId", payload.getSenderId());
        messageData.put("senderType", payload.getSenderType().name());
        messageData.put(FIELD_TIMESTAMP, payload.getTimestamp());

        Map<String, Object> conversationData = new HashMap<>();
        conversationData.put("id", payload.getConversationId());
        conversationData.put("userId", payload.getUserId());
        conversationData.put("userName", payload.getUserName());
        conversationData.put("userContact", payload.getUserContact());
        conversationData.put("userAvatarUri", payload.getUserAvatarUri());
        conversationData.put("workerId", payload.getWorkerId());
        conversationData.put("workerName", payload.getWorkerName());
        conversationData.put("workerOccupation", payload.getWorkerOccupation());
        conversationData.put("workerImageUri", payload.getWorkerImageUri());
        conversationData.put("serviceId", payload.getServiceId());
        conversationData.put("serviceName", payload.getServiceName());
        conversationData.put("serviceCategory", payload.getServiceCategory());
        conversationData.put("lastMessage", payload.getContent());
        conversationData.put("lastMessageSender", payload.getSenderType().name());
        conversationData.put("lastMessageTimestamp", payload.getTimestamp());
        conversationData.put(FIELD_UPDATED_AT, payload.getTimestamp());
        conversationData.put(FIELD_USER_UNREAD, payload.getSenderType() == SenderType.WORKER);
        conversationData.put(FIELD_WORKER_UNREAD, payload.getSenderType() == SenderType.USER);

        List<String> participants = new ArrayList<>(2);
        if (!TextUtils.isEmpty(payload.getUserId())) {
            participants.add(payload.getUserId());
        }
        if (!TextUtils.isEmpty(payload.getWorkerId())) {
            participants.add(payload.getWorkerId());
        }
        conversationData.put("participants", participants);

        WriteBatch batch = firestore.batch();
        batch.set(messageRef, messageData);
        batch.set(conversationRef, conversationData, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(error -> {
                    if (listener != null) {
                        listener.onError(error);
                    }
                });
    }

    public void markConversationAsRead(@NonNull String conversationId, boolean forWorker) {
        if (!enabled) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(forWorker ? FIELD_WORKER_UNREAD : FIELD_USER_UNREAD, false);

        firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .set(updates, SetOptions.merge());
    }

    public static String buildConversationId(@NonNull String userId, @NonNull String workerId) {
        return userId + "_" + workerId;
    }

    public enum SenderType {
        USER,
        WORKER
    }

    public interface ConversationListener {
        void onConversations(List<UserConversation> conversations);

        void onError(Exception exception);
    }

    public interface WorkerConversationListener {
        void onConversations(List<WorkerChatConversation> conversations);

        void onError(Exception exception);
    }

    public interface MessageListener {
        void onMessages(List<UserChatMessage> messages);

        void onError(Exception exception);
    }

    private static final class NoopRegistration implements ListenerRegistration {
        @Override
        public void remove() {
            // no-op
        }
    }

    public interface CompletionListener {
        void onSuccess();

        void onError(Exception exception);
    }

    public static class MessagePayload {
        private final String conversationId;
        private final String content;
        private final long timestamp;
        private final SenderType senderType;
        private final String senderId;
        private final String userId;
        private final String userName;
        private final String userContact;
        private final String userAvatarUri;
        private final String workerId;
        private final String workerName;
        private final String workerOccupation;
        private final String workerImageUri;
        private final String serviceId;
        private final String serviceName;
        private final String serviceCategory;

        private MessagePayload(Builder builder) {
            this.conversationId = builder.conversationId;
            this.content = builder.content;
            this.timestamp = builder.timestamp;
            this.senderType = builder.senderType;
            this.senderId = builder.senderId;
            this.userId = builder.userId;
            this.userName = builder.userName;
            this.userContact = builder.userContact;
            this.userAvatarUri = builder.userAvatarUri;
            this.workerId = builder.workerId;
            this.workerName = builder.workerName;
            this.workerOccupation = builder.workerOccupation;
            this.workerImageUri = builder.workerImageUri;
            this.serviceId = builder.serviceId;
            this.serviceName = builder.serviceName;
            this.serviceCategory = builder.serviceCategory;
        }

        public String getConversationId() {
            return conversationId;
        }

        public String getContent() {
            return content;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public SenderType getSenderType() {
            return senderType;
        }

        public String getSenderId() {
            return senderId;
        }

        public String getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }

        public String getUserContact() {
            return userContact;
        }

        public String getUserAvatarUri() {
            return userAvatarUri;
        }

        public String getWorkerId() {
            return workerId;
        }

        public String getWorkerName() {
            return workerName;
        }

        public String getWorkerOccupation() {
            return workerOccupation;
        }

        public String getWorkerImageUri() {
            return workerImageUri;
        }

        public String getServiceId() {
            return serviceId;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getServiceCategory() {
            return serviceCategory;
        }

        public static class Builder {
            private String conversationId;
            private String content;
            private long timestamp;
            private SenderType senderType = SenderType.USER;
            private String senderId;
            private String userId;
            private String userName;
            private String userContact;
            private String userAvatarUri;
            private String workerId;
            private String workerName;
            private String workerOccupation;
            private String workerImageUri;
            private String serviceId;
            private String serviceName;
            private String serviceCategory;

            public Builder setConversationId(String conversationId) {
                this.conversationId = conversationId;
                return this;
            }

            public Builder setContent(String content) {
                this.content = content;
                return this;
            }

            public Builder setTimestamp(long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder setSenderType(SenderType senderType) {
                this.senderType = senderType;
                return this;
            }

            public Builder setSenderId(String senderId) {
                this.senderId = senderId;
                return this;
            }

            public Builder setUserId(String userId) {
                this.userId = userId;
                return this;
            }

            public Builder setUserName(String userName) {
                this.userName = userName;
                return this;
            }

            public Builder setUserContact(String userContact) {
                this.userContact = userContact;
                return this;
            }

            public Builder setUserAvatarUri(String userAvatarUri) {
                this.userAvatarUri = userAvatarUri;
                return this;
            }

            public Builder setWorkerId(String workerId) {
                this.workerId = workerId;
                return this;
            }

            public Builder setWorkerName(String workerName) {
                this.workerName = workerName;
                return this;
            }

            public Builder setWorkerOccupation(String workerOccupation) {
                this.workerOccupation = workerOccupation;
                return this;
            }

            public Builder setWorkerImageUri(String workerImageUri) {
                this.workerImageUri = workerImageUri;
                return this;
            }

            public Builder setServiceId(String serviceId) {
                this.serviceId = serviceId;
                return this;
            }

            public Builder setServiceName(String serviceName) {
                this.serviceName = serviceName;
                return this;
            }

            public Builder setServiceCategory(String serviceCategory) {
                this.serviceCategory = serviceCategory;
                return this;
            }

            public MessagePayload build() {
                if (timestamp == 0L) {
                    timestamp = System.currentTimeMillis();
                }
                return new MessagePayload(this);
            }
        }
    }
}

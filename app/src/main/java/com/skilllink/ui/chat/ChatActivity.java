package com.skilllink.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.skilllink.R;
import com.skilllink.model.UserChatMessage;
import com.skilllink.model.UserConversation;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_PARTICIPANT_NAME = "participant_name";
    public static final String EXTRA_PARTICIPANT_ID = "participant_id";
    public static final String EXTRA_IS_USER_INITIATED = "is_user_initiated";

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private EditText inputMessage;
    private ImageButton buttonSend;
    private CircularProgressIndicator loadingIndicator;
    private View emptyStateView;

    private ChatAdapter chatAdapter;
    private FirebaseFirestore firestore;
    private SessionManager sessionManager;
    private String conversationId;
    private String participantId;
    private String participantName;
    private String currentUserId;
    private String currentUserName;
    private boolean isUserInitiated;

    private final List<UserChatMessage> messages = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initializeViews();
        setupToolbar();
        initializeData();
        setupRecyclerView();
        setupClickListeners();
        setupMessageInput();
        
        if (conversationId != null) {
            loadMessages();
            listenForMessages();
        } else {
            createNewConversation();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view_chat);
        inputMessage = findViewById(R.id.input_message);
        buttonSend = findViewById(R.id.button_send);
        loadingIndicator = findViewById(R.id.loading_indicator);
        emptyStateView = findViewById(R.id.empty_state_view);

        firestore = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        currentUserName = sessionManager.getUserName();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(participantName != null ? participantName : "Chat");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeData() {
        Intent intent = getIntent();
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID);
        participantId = intent.getStringExtra(EXTRA_PARTICIPANT_ID);
        participantName = intent.getStringExtra(EXTRA_PARTICIPANT_NAME);
        isUserInitiated = intent.getBooleanExtra(EXTRA_IS_USER_INITIATED, true);

        if (TextUtils.isEmpty(participantId) || TextUtils.isEmpty(currentUserId)) {
            showToast("Invalid chat parameters");
            finish();
            return;
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messages, currentUserId, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
        
        // Scroll to bottom when new messages are added
        recyclerView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                recyclerView.post(() -> recyclerView.scrollToPosition(messages.size() - 1));
            }
        });
    }

    private void setupClickListeners() {
        buttonSend.setOnClickListener(v -> sendMessage());
    }

    private void setupMessageInput() {
        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonSend.setEnabled(!TextUtils.isEmpty(s.toString().trim()));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        buttonSend.setEnabled(false);
    }

    private void createNewConversation() {
        showLoading(true);
        
        // Create a new conversation document
        String newConversationId = firestore.collection("conversations").document().getId();
        
        UserConversation conversation = new UserConversation(
            newConversationId,
            participantId,
            participantName,
            currentUserName,
            null, // serviceId
            null, // serviceName
            null, // serviceCategory
            null, // workerImageUri
            null, // lastMessage
            new Date().getTime(),
            false // hasUnread
        );

        firestore.collection("conversations")
                .document(newConversationId)
                .set(conversation)
                .addOnSuccessListener(aVoid -> {
                    conversationId = newConversationId;
                    loadMessages();
                    listenForMessages();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to create conversation");
                    showLoading(false);
                    finish();
                });
    }

    private void loadMessages() {
        if (conversationId == null) return;

        showLoading(true);

        firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    messages.clear();
                    for (var document : queryDocumentSnapshots.getDocuments()) {
                        UserChatMessage message = document.toObject(UserChatMessage.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    chatAdapter.notifyDataSetChanged();
                    scrollToBottom();
                    showLoading(false);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load messages");
                    showLoading(false);
                });
    }

    private void listenForMessages() {
        if (conversationId == null) return;

        firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                UserChatMessage message = dc.getDocument().toObject(UserChatMessage.class);
                                messages.add(message);
                                chatAdapter.notifyItemInserted(messages.size() - 1);
                                scrollToBottom();
                                updateEmptyState();
                            }
                        }
                    }
                });
    }

    private void sendMessage() {
        String messageText = inputMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText) || TextUtils.isEmpty(conversationId)) {
            return;
        }

        UserChatMessage message = new UserChatMessage(
            firestore.collection("conversations").document(conversationId)
                    .collection("messages").document().getId(),
            conversationId,
            currentUserId,
            currentUserName,
            messageText,
            new Date().getTime(),
            isUserInitiated
        );

        inputMessage.setText("");
        buttonSend.setEnabled(false);

        firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(message.getMessageId())
                .set(message)
                .addOnSuccessListener(aVoid -> {
                    // Update conversation's last message and timestamp
                    updateConversationLastMessage(messageText, new Date());
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to send message");
                    buttonSend.setEnabled(true);
                });
    }

    private void updateConversationLastMessage(String lastMessage, Date timestamp) {
        if (conversationId == null) return;

        firestore.collection("conversations")
                .document(conversationId)
                .update("lastMessage", lastMessage,
                       "lastMessageTime", timestamp,
                       "unreadCount", 1)
                .addOnFailureListener(e -> {
                    // Ignore failure to update conversation metadata
                });
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) {
            recyclerView.post(() -> recyclerView.scrollToPosition(messages.size() - 1));
        }
    }

    private void updateEmptyState() {
        if (emptyStateView != null) {
            emptyStateView.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        }
        recyclerView.setVisibility(messages.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Adapter for chat messages
        private static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<UserChatMessage> messages;
        private final String currentUserId;
        private final Context context;
        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;

        public ChatAdapter(List<UserChatMessage> messages, String currentUserId, Context context) {
            this.messages = messages;
            this.currentUserId = currentUserId;
            this.context = context;
        }

        @Override
        public int getItemViewType(int position) {
            UserChatMessage message = messages.get(position);
            return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == VIEW_TYPE_SENT) {
                View view = inflater.inflate(R.layout.item_chat_message_sent, parent, false);
                return new SentMessageViewHolder(view, context);
            } else {
                View view = inflater.inflate(R.layout.item_chat_message_received, parent, false);
                return new ReceivedMessageViewHolder(view, context);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            UserChatMessage message = messages.get(position);
            if (holder instanceof SentMessageViewHolder) {
                ((SentMessageViewHolder) holder).bind(message);
            } else {
                ((ReceivedMessageViewHolder) holder).bind(message);
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        private static class SentMessageViewHolder extends RecyclerView.ViewHolder {
            private final TextView messageText;
            private final TextView timeText;
            private final Context context;

            public SentMessageViewHolder(@NonNull View itemView, Context context) {
                super(itemView);
                this.context = context;
                messageText = itemView.findViewById(R.id.text_message);
                timeText = itemView.findViewById(R.id.text_time);
            }

            public void bind(UserChatMessage message) {
                messageText.setText(message.getMessageText());
                timeText.setText(formatTime(context, new Date(message.getTimestamp())));
            }
        }

        private static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
            private final TextView messageText;
            private final TextView timeText;
            private final TextView senderNameText;
            private final Context context;

            public ReceivedMessageViewHolder(@NonNull View itemView, Context context) {
                super(itemView);
                this.context = context;
                messageText = itemView.findViewById(R.id.text_message);
                timeText = itemView.findViewById(R.id.text_time);
                senderNameText = itemView.findViewById(R.id.text_sender_name);
            }

            public void bind(UserChatMessage message) {
                messageText.setText(message.getMessageText());
                timeText.setText(formatTime(context, new Date(message.getTimestamp())));
                senderNameText.setText(message.getSenderName());
            }
        }

        private static String formatTime(Context context, Date date) {
            if (date == null) return "";
            // Simple time formatting - you can enhance this
            return android.text.format.DateFormat.getTimeFormat(context).format(date);
        }
    }
}

package com.skilllink.ui.user.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.skilllink.R;
import com.skilllink.data.firebase.FirebaseChatStore;
import com.skilllink.model.UserChatMessage;
import com.skilllink.model.WorkerService;
import com.skilllink.util.ImageLoader;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class WorkerChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";
    public static final String EXTRA_SERVICE_ID = "extra_service_id";
    public static final String EXTRA_SERVICE_NAME = "extra_service_name";
    public static final String EXTRA_SERVICE_CATEGORY = "extra_service_category";
    public static final String EXTRA_WORKER_ID = "extra_worker_id";
    public static final String EXTRA_WORKER_NAME = "extra_worker_name";
    public static final String EXTRA_WORKER_OCCUPATION = "extra_worker_occupation";
    public static final String EXTRA_WORKER_IMAGE_URI = "extra_worker_image_uri";
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USER_NAME = "extra_user_name";
    public static final String EXTRA_USER_CONTACT = "extra_user_contact";
    public static final String EXTRA_USER_IMAGE_URI = "extra_user_image_uri";

    private final List<UserChatMessage> messages = new ArrayList<>();

    private SessionManager sessionManager;
    private FirebaseChatStore chatStore;
    private boolean chatEnabled;
    private WorkerChatAdapter adapter;
    private TextInputEditText inputMessage;
    private MaterialButton buttonSend;
    private RecyclerView recyclerView;
    private ListenerRegistration messageRegistration;

    private boolean actingAsWorker;

    private String conversationId;
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

    private de.hdodenhof.circleimageview.CircleImageView imagePeerAvatar;
    private TextView textPeerName;
    private TextView textPeerSubtitle;
    private TextView textPresence;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_chat);

        sessionManager = new SessionManager(this);
        chatStore = FirebaseChatStore.getInstance();
        chatEnabled = chatStore != null && chatStore.isEnabled();
        actingAsWorker = "worker".equalsIgnoreCase(sessionManager.getUserRole());

        extractIntentExtras();
        resolveIdentityDefaults();
        boolean conversationResolved = ensureConversationId();
        setupToolbar();
        setupRecyclerView();
        setupComposer();
        if (conversationResolved && chatEnabled) {
            startListeningToMessages();
        } else if (!chatEnabled) {
            Toast.makeText(this, R.string.chat_feature_unavailable, Toast.LENGTH_LONG).show();
            if (buttonSend != null) {
                buttonSend.setEnabled(false);
                buttonSend.setAlpha(0.5f);
            }
            if (inputMessage != null) {
                inputMessage.setEnabled(false);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        markConversationAsRead();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageRegistration != null) {
            messageRegistration.remove();
            messageRegistration = null;
        }
    }

    private void extractIntentExtras() {
        Intent intent = getIntent();
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID);
        serviceId = intent.getStringExtra(EXTRA_SERVICE_ID);
        serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME);
        serviceCategory = intent.getStringExtra(EXTRA_SERVICE_CATEGORY);
        workerId = intent.getStringExtra(EXTRA_WORKER_ID);
        workerName = intent.getStringExtra(EXTRA_WORKER_NAME);
        workerOccupation = intent.getStringExtra(EXTRA_WORKER_OCCUPATION);
        workerImageUri = intent.getStringExtra(EXTRA_WORKER_IMAGE_URI);
        userId = intent.getStringExtra(EXTRA_USER_ID);
        userName = intent.getStringExtra(EXTRA_USER_NAME);
        userContact = intent.getStringExtra(EXTRA_USER_CONTACT);
        userAvatarUri = intent.getStringExtra(EXTRA_USER_IMAGE_URI);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        imagePeerAvatar = toolbar.findViewById(R.id.imagePeerAvatar);
        textPeerName = toolbar.findViewById(R.id.textPeerName);
        textPeerSubtitle = toolbar.findViewById(R.id.textPeerSubtitle);
        textPresence = toolbar.findViewById(R.id.textPresence);

        updateToolbarContent();
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerMessages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkerChatAdapter(messages, !actingAsWorker);
        recyclerView.setAdapter(adapter);
    }

    private void setupComposer() {
        inputMessage = findViewById(R.id.inputMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonSend.setOnClickListener(v -> sendMessage());
        inputMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
        updateComposerEnabledState();
    }

    private void sendMessage() {
        if (!chatEnabled) {
            Toast.makeText(this, R.string.chat_feature_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        String content = inputMessage.getText() != null ? inputMessage.getText().toString().trim() : "";
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.worker_chat_empty_state, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!canParticipateInConversation()) {
            Toast.makeText(this, R.string.worker_chat_missing_participants, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hadConversationId = !TextUtils.isEmpty(conversationId);
        boolean conversationResolved = ensureConversationId();
        if (TextUtils.isEmpty(conversationId)) {
            Toast.makeText(this, R.string.worker_chat_missing_participants, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hadConversationId && conversationResolved) {
            startListeningToMessages();
        }

        FirebaseChatStore.MessagePayload payload = new FirebaseChatStore.MessagePayload.Builder()
                .setConversationId(conversationId)
                .setContent(content)
                .setTimestamp(System.currentTimeMillis())
                .setSenderType(actingAsWorker ? FirebaseChatStore.SenderType.WORKER : FirebaseChatStore.SenderType.USER)
                .setSenderId(actingAsWorker ? workerId : userId)
                .setUserId(userId)
                .setUserName(userName)
                .setUserContact(userContact)
                .setUserAvatarUri(userAvatarUri)
                .setWorkerId(workerId)
                .setWorkerName(workerName)
                .setWorkerOccupation(workerOccupation)
                .setWorkerImageUri(workerImageUri)
                .setServiceId(serviceId)
                .setServiceName(serviceName)
                .setServiceCategory(serviceCategory)
                .build();

        buttonSend.setEnabled(false);

        inputMessage.setText(null);
        chatStore.sendMessage(payload, new FirebaseChatStore.CompletionListener() {
            @Override
            public void onSuccess() {
                buttonSend.setEnabled(true);
            }

            @Override
            public void onError(Exception exception) {
                buttonSend.setEnabled(true);
                Toast.makeText(WorkerChatActivity.this, R.string.worker_chat_send_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markConversationAsRead() {
        if (!chatEnabled || TextUtils.isEmpty(conversationId)) {
            return;
        }
        chatStore.markConversationAsRead(conversationId, actingAsWorker);
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) {
            recyclerView.scrollToPosition(messages.size() - 1);
        }
    }

    private void startListeningToMessages() {
        if (!chatEnabled || TextUtils.isEmpty(conversationId)) {
            return;
        }

        if (messageRegistration != null) {
            messageRegistration.remove();
        }

        messageRegistration = chatStore.listenToMessages(conversationId, new FirebaseChatStore.MessageListener() {
            @Override
            public void onMessages(List<UserChatMessage> incoming) {
                messages.clear();
                messages.addAll(incoming);
                adapter.notifyDataSetChanged();
                scrollToBottom();
                markConversationAsRead();
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(WorkerChatActivity.this, R.string.worker_chat_load_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resolveIdentityDefaults() {
        if (!actingAsWorker) {
            if (TextUtils.isEmpty(userId)) {
                userId = sessionManager.getOrCreateUserDocumentId();
            }
            if (TextUtils.isEmpty(userName)) {
                userName = sessionManager.getUserName();
            }
            if (TextUtils.isEmpty(userContact)) {
                String phone = sessionManager.getUserPhone();
                userContact = !TextUtils.isEmpty(phone) ? phone : sessionManager.getUserEmail();
            }
            if (TextUtils.isEmpty(userAvatarUri)) {
                userAvatarUri = sessionManager.getUserAvatarUri();
            }
        } else {
            if (TextUtils.isEmpty(workerId)) {
                workerId = sessionManager.getOrCreateWorkerDocumentId();
            }
            if (TextUtils.isEmpty(workerName)) {
                workerName = sessionManager.getUserName();
            }
            if (TextUtils.isEmpty(workerImageUri)) {
                workerImageUri = sessionManager.getUserAvatarUri();
            }
        }

        if (!TextUtils.isEmpty(serviceId)) {
            WorkerService matchedService = sessionManager.findWorkerServiceById(serviceId);
            if (matchedService != null) {
                if (TextUtils.isEmpty(workerId)) {
                    workerId = matchedService.getOwnerId();
                }
                if (TextUtils.isEmpty(workerName)) {
                    workerName = matchedService.getOwnerName();
                }
                if (TextUtils.isEmpty(workerOccupation)) {
                    workerOccupation = matchedService.getCategory();
                }
                if (TextUtils.isEmpty(workerImageUri)) {
                    workerImageUri = matchedService.getImageUri();
                }
            }
        }

        if (actingAsWorker) {
            if (TextUtils.isEmpty(userName)) {
                userName = getString(R.string.worker_chat_customer_fallback);
            }
            if (TextUtils.isEmpty(userContact)) {
                userContact = sessionManager.getUserEmail();
            }
            if (TextUtils.isEmpty(userAvatarUri)) {
                userAvatarUri = sessionManager.getUserAvatarUri();
            }
        }

        if (TextUtils.isEmpty(workerOccupation) && !TextUtils.isEmpty(serviceCategory)) {
            workerOccupation = serviceCategory;
        }

        inferParticipantsFromConversationId();
    }

    private void inferParticipantsFromConversationId() {
        if (!TextUtils.isEmpty(conversationId) && TextUtils.isEmpty(userId) && !TextUtils.isEmpty(workerId)) {
            String suffix = "_" + workerId;
            if (conversationId.endsWith(suffix)) {
                userId = conversationId.substring(0, conversationId.length() - suffix.length());
            }
        }
        if (!TextUtils.isEmpty(conversationId) && TextUtils.isEmpty(workerId) && !TextUtils.isEmpty(userId)) {
            String prefix = userId + "_";
            if (conversationId.startsWith(prefix)) {
                workerId = conversationId.substring(prefix.length());
            }
        }
    }

    private boolean ensureConversationId() {
        if (!TextUtils.isEmpty(conversationId)) {
            return true;
        }
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(workerId)) {
            return false;
        }
        conversationId = FirebaseChatStore.buildConversationId(userId, workerId);
        return true;
    }

    private boolean canParticipateInConversation() {
        return !TextUtils.isEmpty(userId) && !TextUtils.isEmpty(workerId);
    }

    private void updateComposerEnabledState() {
        boolean enabled = chatEnabled && canParticipateInConversation();
        if (buttonSend != null) {
            buttonSend.setEnabled(enabled);
        }
        if (inputMessage != null) {
            inputMessage.setEnabled(enabled);
        }
    }

    private void updateToolbarContent() {
        if (textPeerName == null || textPeerSubtitle == null) {
            return;
        }

        if (actingAsWorker) {
            textPeerName.setText(!TextUtils.isEmpty(userName) ? userName : getString(R.string.worker_chat_customer_fallback));
            textPeerSubtitle.setText(!TextUtils.isEmpty(userContact) ? userContact : getString(R.string.worker_chat_customer_subtitle_fallback));
            ImageLoader.loadUriInto(this, imagePeerAvatar, userAvatarUri, R.drawable.ic_user, R.color.primary_color);
        } else {
            textPeerName.setText(!TextUtils.isEmpty(workerName) ? workerName : getString(R.string.worker_service_detail_title));
            String subtitle = !TextUtils.isEmpty(workerOccupation) ? workerOccupation : (!TextUtils.isEmpty(serviceCategory) ? serviceCategory : getString(R.string.worker_chat_subtitle_fallback));
            textPeerSubtitle.setText(subtitle);
            ImageLoader.loadUriInto(this, imagePeerAvatar, workerImageUri, R.drawable.ic_user, R.color.primary_color);
        }

        if (textPresence != null) {
            if (!TextUtils.isEmpty(serviceName)) {
                textPresence.setVisibility(View.VISIBLE);
                textPresence.setText(serviceName);
            } else {
                textPresence.setVisibility(View.GONE);
            }
        }
    }
}

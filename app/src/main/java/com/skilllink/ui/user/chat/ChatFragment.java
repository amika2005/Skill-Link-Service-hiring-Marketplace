package com.skilllink.ui.user.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;
import com.skilllink.R;
import com.skilllink.data.firebase.FirebaseChatStore;
import com.skilllink.databinding.FragmentUserChatBinding;
import com.skilllink.model.UserConversation;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private FragmentUserChatBinding binding;
    private SessionManager sessionManager;
    private FirebaseChatStore chatStore;
    private boolean chatEnabled;
    private UserConversationAdapter adapter;
    private final List<UserConversation> conversations = new ArrayList<>();
    private ListenerRegistration conversationRegistration;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserChatBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());
        chatStore = FirebaseChatStore.getInstance();
        chatEnabled = chatStore != null && chatStore.isEnabled();
        userId = sessionManager.getOrCreateUserDocumentId();

        setupRecyclerView();
        setupActions();
        if (chatEnabled) {
            startConversationListener();
        } else {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.cardConversations.setVisibility(View.GONE);
            binding.newMessageFab.setEnabled(false);
            binding.newMessageFab.setAlpha(0.5f);
            binding.buttonEmptyNewMessage.setEnabled(false);
            Toast.makeText(requireContext(), R.string.chat_feature_unavailable, Toast.LENGTH_LONG).show();
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (chatEnabled && conversationRegistration == null) {
            startConversationListener();
        }
    }

    private void setupRecyclerView() {
        binding.recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserConversationAdapter(conversations);
        adapter.setListener(this::openConversation);
        binding.recyclerViewChats.setAdapter(adapter);
    }

    private void setupActions() {
        View.OnClickListener comingSoon = v ->
                Toast.makeText(requireContext(), R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();

        binding.newMessageFab.setOnClickListener(comingSoon);
        binding.buttonEmptyNewMessage.setOnClickListener(comingSoon);
    }

    private void startConversationListener() {
        if (!chatEnabled || chatStore == null || TextUtils.isEmpty(userId)) {
            return;
        }

        if (conversationRegistration != null) {
            conversationRegistration.remove();
        }

        conversationRegistration = chatStore.listenToUserConversations(userId, new FirebaseChatStore.ConversationListener() {
            @Override
            public void onConversations(List<UserConversation> latest) {
                conversations.clear();
                conversations.addAll(latest);
                adapter.notifyDataSetChanged();
                boolean hasConversations = !conversations.isEmpty();
                binding.emptyState.setVisibility(hasConversations ? View.GONE : View.VISIBLE);
                binding.cardConversations.setVisibility(hasConversations ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception exception) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.worker_chat_load_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openConversation(@NonNull UserConversation conversation) {
        if (chatEnabled) {
            chatStore.markConversationAsRead(conversation.getId(), false);
        }
        Intent intent = new Intent(requireContext(), WorkerChatActivity.class);
        intent.putExtra(WorkerChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
        if (conversation.getWorkerId() != null) {
            intent.putExtra(WorkerChatActivity.EXTRA_WORKER_ID, conversation.getWorkerId());
        }
        intent.putExtra(WorkerChatActivity.EXTRA_WORKER_NAME, conversation.getTitle());
        intent.putExtra(WorkerChatActivity.EXTRA_WORKER_OCCUPATION, conversation.getSubtitle());
        if (conversation.getWorkerImageUri() != null) {
            intent.putExtra(WorkerChatActivity.EXTRA_WORKER_IMAGE_URI, conversation.getWorkerImageUri());
        }
        if (conversation.getServiceId() != null) {
            intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_ID, conversation.getServiceId());
        }
        intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_NAME, conversation.getServiceName());
        intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_CATEGORY, conversation.getServiceCategory());
        intent.putExtra(WorkerChatActivity.EXTRA_USER_ID, userId);
        intent.putExtra(WorkerChatActivity.EXTRA_USER_NAME, sessionManager.getUserName());
        String userContact = !TextUtils.isEmpty(sessionManager.getUserPhone())
                ? sessionManager.getUserPhone()
                : sessionManager.getUserEmail();
        intent.putExtra(WorkerChatActivity.EXTRA_USER_CONTACT, userContact);
        intent.putExtra(WorkerChatActivity.EXTRA_USER_IMAGE_URI, sessionManager.getUserAvatarUri());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (conversationRegistration != null) {
            conversationRegistration.remove();
            conversationRegistration = null;
        }
        binding = null;
        adapter = null;
    }
}
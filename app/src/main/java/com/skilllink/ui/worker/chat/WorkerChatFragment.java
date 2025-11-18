package com.skilllink.ui.worker.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.skilllink.R;
import com.skilllink.data.firebase.FirebaseChatStore;
import com.skilllink.databinding.FragmentWorkerChatBinding;
import com.skilllink.model.WorkerChatConversation;
import com.skilllink.ui.user.chat.WorkerChatActivity;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class WorkerChatFragment extends Fragment {

    private FragmentWorkerChatBinding binding;
    private SessionManager sessionManager;
    private FirebaseChatStore chatStore;
    private boolean chatEnabled;
    private WorkerConversationAdapter adapter;
    private final List<WorkerChatConversation> conversations = new ArrayList<>();
    private ListenerRegistration conversationRegistration;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkerChatBinding.inflate(inflater, container, false);
        sessionManager = new SessionManager(requireContext());
        chatStore = FirebaseChatStore.getInstance();
        chatEnabled = chatStore != null && chatStore.isEnabled();

        setupRecyclerView();
        if (chatEnabled) {
            startConversationListener();
        } else {
            binding.emptyState.setVisibility(View.VISIBLE);
            Toast.makeText(requireContext(), R.string.chat_feature_unavailable, Toast.LENGTH_LONG).show();
        }

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        binding.recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WorkerConversationAdapter(conversations);
        adapter.setListener(this::openConversation);
        binding.recyclerViewChats.setAdapter(adapter);
    }

    private void startConversationListener() {
        if (!chatEnabled) {
            return;
        }

        String workerId = sessionManager.getOrCreateWorkerDocumentId();
        if (TextUtils.isEmpty(workerId)) {
            binding.emptyState.setVisibility(View.VISIBLE);
            return;
        }

        if (conversationRegistration != null) {
            conversationRegistration.remove();
        }

        conversationRegistration = chatStore.listenToWorkerConversations(workerId, new FirebaseChatStore.WorkerConversationListener() {
            @Override
            public void onConversations(List<WorkerChatConversation> latest) {
                conversations.clear();
                conversations.addAll(latest);
                adapter.notifyDataSetChanged();
                boolean hasConversations = !conversations.isEmpty();
                binding.emptyState.setVisibility(hasConversations ? View.GONE : View.VISIBLE);
                binding.recyclerViewChats.setVisibility(hasConversations ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception exception) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.worker_chat_load_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openConversation(@NonNull WorkerChatConversation conversation) {
        if (chatEnabled) {
            chatStore.markConversationAsRead(conversation.getConversationId(), true);
        }

        Intent intent = new Intent(requireContext(), WorkerChatActivity.class);
        intent.putExtra(WorkerChatActivity.EXTRA_CONVERSATION_ID, conversation.getConversationId());
        intent.putExtra(WorkerChatActivity.EXTRA_USER_ID, conversation.getUserId());
        intent.putExtra(WorkerChatActivity.EXTRA_USER_NAME, conversation.getUserName());
        intent.putExtra(WorkerChatActivity.EXTRA_USER_CONTACT, conversation.getUserContact());
        intent.putExtra(WorkerChatActivity.EXTRA_USER_IMAGE_URI, conversation.getUserAvatarUri());
        intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_ID, conversation.getServiceId());
        intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_NAME, conversation.getServiceName());
        intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_CATEGORY, conversation.getServiceCategory());
        intent.putExtra(WorkerChatActivity.EXTRA_WORKER_ID, sessionManager.getOrCreateWorkerDocumentId());
        intent.putExtra(WorkerChatActivity.EXTRA_WORKER_NAME, sessionManager.getUserName());
        intent.putExtra(WorkerChatActivity.EXTRA_WORKER_IMAGE_URI, sessionManager.getUserAvatarUri());
        intent.putExtra(WorkerChatActivity.EXTRA_WORKER_OCCUPATION, sessionManager.getUserBio());
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
    }
}
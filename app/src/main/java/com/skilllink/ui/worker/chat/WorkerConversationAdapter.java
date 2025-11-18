package com.skilllink.ui.worker.chat;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skilllink.R;
import com.skilllink.model.WorkerChatConversation;
import com.skilllink.util.ImageLoader;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

class WorkerConversationAdapter extends RecyclerView.Adapter<WorkerConversationAdapter.ViewHolder> {

    interface Listener {
        void onConversationSelected(@NonNull WorkerChatConversation conversation);
    }

    private final List<WorkerChatConversation> conversations;
    private Listener listener;

    WorkerConversationAdapter(List<WorkerChatConversation> conversations) {
        this.conversations = conversations;
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worker_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(conversations.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView imageAvatar;
        private final TextView textName;
        private final TextView textContact;
        private final TextView textService;
        private final TextView textLastMessage;
        private final TextView textTimestamp;
        private final View viewUnread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            textName = itemView.findViewById(R.id.textName);
            textContact = itemView.findViewById(R.id.textContact);
            textService = itemView.findViewById(R.id.textService);
            textLastMessage = itemView.findViewById(R.id.textLastMessage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            viewUnread = itemView.findViewById(R.id.viewUnread);
        }

        void bind(WorkerChatConversation conversation, Listener listener) {
            textName.setText(!TextUtils.isEmpty(conversation.getUserName())
                    ? conversation.getUserName()
                    : itemView.getContext().getString(R.string.worker_chat_customer_fallback));

            if (TextUtils.isEmpty(conversation.getUserContact())) {
                textContact.setVisibility(View.GONE);
            } else {
                textContact.setVisibility(View.VISIBLE);
                textContact.setText(conversation.getUserContact());
            }

            if (!TextUtils.isEmpty(conversation.getServiceName())) {
                textService.setVisibility(View.VISIBLE);
                textService.setText(conversation.getServiceName());
            } else if (!TextUtils.isEmpty(conversation.getServiceCategory())) {
                textService.setVisibility(View.VISIBLE);
                textService.setText(conversation.getServiceCategory());
            } else {
                textService.setVisibility(View.GONE);
            }

            CharSequence preview = !TextUtils.isEmpty(conversation.getLastMessage())
                    ? conversation.getLastMessage()
                    : itemView.getContext().getString(R.string.worker_chat_input_hint);
            textLastMessage.setText(preview);

            long timestamp = conversation.getLastMessageTimestamp();
            if (timestamp > 0) {
                CharSequence relative = DateUtils.getRelativeTimeSpanString(
                        timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                );
                textTimestamp.setText(relative);
            } else {
                textTimestamp.setText("-");
            }

            viewUnread.setVisibility(conversation.hasUnread() ? View.VISIBLE : View.GONE);

            ImageLoader.loadUriInto(itemView.getContext(), imageAvatar, conversation.getUserAvatarUri(), R.drawable.ic_user, R.color.primary_color);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationSelected(conversation);
                }
            });
        }
    }
}

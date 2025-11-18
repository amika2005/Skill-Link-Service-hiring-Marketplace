package com.skilllink.ui.user.chat;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skilllink.R;
import com.skilllink.model.UserConversation;
import com.skilllink.util.ImageLoader;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

class UserConversationAdapter extends RecyclerView.Adapter<UserConversationAdapter.ConversationViewHolder> {

    interface Listener {
        void onConversationSelected(@NonNull UserConversation conversation);
    }

    private final List<UserConversation> conversations;
    private Listener listener;

    UserConversationAdapter(List<UserConversation> conversations) {
        this.conversations = conversations;
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(conversations.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView workerImage;
        private final TextView workerName;
        private final TextView occupation;
        private final TextView lastMessage;
        private final TextView timestamp;
        private final View unreadIndicator;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            workerImage = itemView.findViewById(R.id.worker_image);
            workerName = itemView.findViewById(R.id.worker_name);
            occupation = itemView.findViewById(R.id.occupation);
            lastMessage = itemView.findViewById(R.id.last_message);
            timestamp = itemView.findViewById(R.id.timestamp);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }

        void bind(UserConversation conversation, Listener listener) {
            Context context = itemView.getContext();

            workerName.setText(!TextUtils.isEmpty(conversation.getTitle())
                    ? conversation.getTitle()
                    : context.getString(R.string.worker_service_detail_title));

            if (TextUtils.isEmpty(conversation.getSubtitle())) {
                occupation.setVisibility(View.GONE);
            } else {
                occupation.setVisibility(View.VISIBLE);
                occupation.setText(conversation.getSubtitle());
            }

            CharSequence preview = !TextUtils.isEmpty(conversation.getLastMessage())
                    ? conversation.getLastMessage()
                    : context.getString(R.string.worker_chat_input_hint);
            lastMessage.setText(preview);

            long timestampValue = conversation.getLastMessageTimestamp();
            if (timestampValue > 0) {
                CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                        timestampValue,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                );
                timestamp.setText(relativeTime);
            } else {
                timestamp.setText("-");
            }

            unreadIndicator.setVisibility(conversation.hasUnread() ? View.VISIBLE : View.GONE);

            ImageLoader.loadUriInto(context, workerImage, conversation.getWorkerImageUri(), R.drawable.ic_user, R.color.primary_color);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationSelected(conversation);
                }
            });
        }
    }
}

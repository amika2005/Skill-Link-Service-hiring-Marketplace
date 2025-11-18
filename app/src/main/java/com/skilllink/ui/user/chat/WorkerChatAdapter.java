package com.skilllink.ui.user.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.skilllink.R;
import com.skilllink.model.UserChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class WorkerChatAdapter extends RecyclerView.Adapter<WorkerChatAdapter.ChatViewHolder> {

    private final List<UserChatMessage> messages;
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private final boolean currentUserIsConsumer;

    WorkerChatAdapter(List<UserChatMessage> messages) {
        this(messages, true);
    }

    WorkerChatAdapter(List<UserChatMessage> messages, boolean currentUserIsConsumer) {
        this.messages = messages;
        this.currentUserIsConsumer = currentUserIsConsumer;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worker_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(messages.get(position), timeFormatter, currentUserIsConsumer);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout container;
        private final MaterialCardView cardBubble;
        private final TextView textMessage;
        private final TextView textTimestamp;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            container = (LinearLayout) itemView;
            cardBubble = itemView.findViewById(R.id.cardBubble);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
        }

        void bind(UserChatMessage message, SimpleDateFormat timeFormatter, boolean currentUserIsConsumer) {
            textMessage.setText(message.getContent());
            textTimestamp.setText(timeFormatter.format(new Date(message.getTimestamp())));

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardBubble.getLayoutParams();
            boolean isFromCurrentUser = (message.isFromUser() && currentUserIsConsumer)
                    || (!message.isFromUser() && !currentUserIsConsumer);

            if (isFromCurrentUser) {
                container.setGravity(android.view.Gravity.END);
                cardBubble.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.primary_color));
                textMessage.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), android.R.color.white));
                textTimestamp.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.worker_home_hero_muted));
                params.gravity = android.view.Gravity.END;
                params.setMarginStart(80);
                params.setMarginEnd(0);
            } else {
                container.setGravity(android.view.Gravity.START);
                cardBubble.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.primary_chip_surface));
                textMessage.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.worker_home_text_primary));
                textTimestamp.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.worker_home_text_secondary));
                params.gravity = android.view.Gravity.START;
                params.setMarginStart(0);
                params.setMarginEnd(80);
            }
            cardBubble.setLayoutParams(params);
        }
    }
}

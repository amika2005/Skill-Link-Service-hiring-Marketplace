package com.skilllink.ui.common;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.skilllink.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.widget.TextView;

public class ModernNotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.notifications_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.recycler_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new NotificationsAdapter(buildSampleNotifications()));
    }

    private List<NotificationItem> buildSampleNotifications() {
        List<NotificationItem> items = new ArrayList<>();
        long now = System.currentTimeMillis();
        items.add(new NotificationItem(
                getString(R.string.notifications_booking_confirmed_title),
                getString(R.string.notifications_booking_confirmed_body),
                now - 45 * 60 * 1000L,
                getString(R.string.notifications_tag_booking)));
        items.add(new NotificationItem(
                getString(R.string.notifications_promo_title),
                getString(R.string.notifications_promo_body),
                now - 3 * 60 * 60 * 1000L,
                getString(R.string.notifications_tag_offers)));
        items.add(new NotificationItem(
                getString(R.string.notifications_service_update_title),
                getString(R.string.notifications_service_update_body),
                now - 23 * 60 * 60 * 1000L,
                getString(R.string.notifications_tag_services)));
        items.add(new NotificationItem(
                getString(R.string.notifications_support_title),
                getString(R.string.notifications_support_body),
                now - 2 * 24 * 60 * 60 * 1000L,
                getString(R.string.notifications_tag_support)));
        return items;
    }

    private static class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

        private final List<NotificationItem> notifications;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

        NotificationsAdapter(@NonNull List<NotificationItem> notifications) {
            this.notifications = notifications;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(notifications.get(position), dateFormat);
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            private final TextView title;
            private final TextView body;
            private final TextView time;
            private final TextView tag;
            private final MaterialCardView container;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.text_notification_title);
                body = itemView.findViewById(R.id.text_notification_body);
                time = itemView.findViewById(R.id.text_notification_time);
                tag = itemView.findViewById(R.id.text_notification_tag);
                container = (MaterialCardView) itemView;
            }

            void bind(@NonNull NotificationItem item, @NonNull SimpleDateFormat formatter) {
                title.setText(item.title);
                body.setText(item.body);
                time.setText(formatRelativeTime(item.timestamp, formatter));
                if (TextUtils.isEmpty(item.tag)) {
                    tag.setVisibility(View.GONE);
                } else {
                    tag.setVisibility(View.VISIBLE);
                    tag.setText(item.tag);
                }
                container.setStrokeColor(container.getContext().getColor(R.color.primary_soft_stroke));
            }

            private String formatRelativeTime(long timestamp, SimpleDateFormat formatter) {
                long delta = System.currentTimeMillis() - timestamp;
                long minutes = delta / (60 * 1000);
                if (minutes < 60) {
                    return itemView.getContext().getResources().getQuantityString(R.plurals.notifications_minutes_ago, (int) Math.max(1, minutes), Math.max(1, minutes));
                }
                long hours = minutes / 60;
                if (hours < 24) {
                    return itemView.getContext().getResources().getQuantityString(R.plurals.notifications_hours_ago, (int) hours, (int) hours);
                }
                return formatter.format(new Date(timestamp));
            }
        }
    }

    private static class NotificationItem {
        final String title;
        final String body;
        final long timestamp;
        final String tag;

        NotificationItem(String title, String body, long timestamp, String tag) {
            this.title = title;
            this.body = body;
            this.timestamp = timestamp;
            this.tag = tag;
        }
    }
}

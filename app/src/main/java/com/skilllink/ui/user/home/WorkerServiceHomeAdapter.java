package com.skilllink.ui.user.home;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.skilllink.R;
import com.skilllink.model.WorkerService;
import com.skilllink.ui.chat.ChatActivity;
import com.skilllink.util.ImageLoader;
import com.skilllink.util.ServiceCategoryRegistry;

import java.util.ArrayList;
import java.util.List;

public class WorkerServiceHomeAdapter extends RecyclerView.Adapter<WorkerServiceHomeAdapter.ViewHolder> {

    public interface Listener {
        void onServiceSelected(@NonNull WorkerService service);
        void onChatClicked(@NonNull WorkerService service);
    }

    private final List<WorkerService> services;
    private final Listener listener;

    public WorkerServiceHomeAdapter(@NonNull List<WorkerService> services, @NonNull Listener listener) {
        this.services = services;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_worker_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(services.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView imageService;
        private final TextView textServiceName;
        private final TextView textWorkerName;
        private final TextView textCategory;
        private final TextView textMeta;
        private final TextView textHighlights;
        private final TextView textBio;
        private final MaterialButton buttonBook;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageService = itemView.findViewById(R.id.imageService);
            textServiceName = itemView.findViewById(R.id.textServiceName);
            textWorkerName = itemView.findViewById(R.id.textWorkerName);
            textCategory = itemView.findViewById(R.id.textCategory);
            textMeta = itemView.findViewById(R.id.textMeta);
            textHighlights = itemView.findViewById(R.id.textHighlights);
            textBio = itemView.findViewById(R.id.textBio);
            buttonBook = itemView.findViewById(R.id.buttonBook);
        }

        void bind(@NonNull WorkerService service, @NonNull Listener listener) {
            textServiceName.setText(!TextUtils.isEmpty(service.getName())
                    ? service.getName()
                    : itemView.getContext().getString(R.string.manage_services_field_name));

            String displayCategory = ServiceCategoryRegistry.getDisplayNameOrDefault(service.getCategory());
            if (TextUtils.isEmpty(displayCategory)) {
                textCategory.setVisibility(View.GONE);
            } else {
                textCategory.setVisibility(View.VISIBLE);
                textCategory.setText(displayCategory);
            }

            if (TextUtils.isEmpty(service.getBio())) {
                textBio.setVisibility(View.GONE);
            } else {
                textBio.setVisibility(View.VISIBLE);
                textBio.setText(service.getBio());
            }

            // Display worker name
            String workerName = service.getOwnerName();
            if (TextUtils.isEmpty(workerName)) {
                textWorkerName.setVisibility(View.GONE);
            } else {
                textWorkerName.setVisibility(View.VISIBLE);
                textWorkerName.setText(itemView.getContext().getString(R.string.worker_service_by_format, workerName));
            }

            bindMetaDetails(service);

            ImageLoader.loadUriInto(
                    itemView.getContext(),
                    imageService,
                    service.getImageUri(),
                    R.drawable.ic_service_manage,
                    R.color.primary_color
            );

            itemView.setOnClickListener(v -> listener.onServiceSelected(service));

            if (buttonBook != null) {
                buttonBook.setOnClickListener(v -> listener.onServiceSelected(service));
            }

        }

        private void bindMetaDetails(@NonNull WorkerService service) {
            int hash = Math.abs(service.getId().hashCode());
            float rating = 4.3f + ((hash % 7) * 0.1f);
            int jobsCount = 45 + (hash % 90);

            if (textMeta != null) {
                textMeta.setVisibility(View.VISIBLE);
                textMeta.setText(itemView.getContext().getString(
                        R.string.home_service_meta_format,
                        rating,
                        jobsCount));
            }

            if (textHighlights != null) {
                List<String> highlights = new ArrayList<>();
                String[] pool = new String[] {
                        itemView.getContext().getString(R.string.worker_service_detail_highlight_verified),
                        itemView.getContext().getString(R.string.worker_service_detail_highlight_guarantee),
                        itemView.getContext().getString(R.string.worker_service_detail_highlight_eco),
                        itemView.getContext().getString(R.string.home_worker_tag_trusted)
                };

                for (int i = 0; i < pool.length && highlights.size() < 3; i++) {
                    String candidate = pool[(hash + i) % pool.length];
                    if (!highlights.contains(candidate)) {
                        highlights.add(candidate);
                    }
                }

                if (highlights.isEmpty()) {
                    textHighlights.setVisibility(View.GONE);
                } else {
                    textHighlights.setVisibility(View.VISIBLE);
                    textHighlights.setText(TextUtils.join(" • ", highlights));
                }
            }
        }
    }
}

package com.skilllink.ui.user.home;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.skilllink.R;
import com.skilllink.model.RecommendedWorker;
import com.skilllink.util.ImageLoader;

import java.util.List;
import java.util.Locale;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder> {

    public interface Listener {
        void onWorkerSelected(@NonNull RecommendedWorker worker);

        void onChatRequested(@NonNull RecommendedWorker worker);
    }

    private final List<RecommendedWorker> workers;
    private final Listener listener;

    public WorkerAdapter(@NonNull List<RecommendedWorker> workers, @NonNull Listener listener) {
        this.workers = workers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_worker, parent, false);
        return new WorkerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        holder.bind(workers.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return workers.size();
    }

    static class WorkerViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView imageAvatar;
        private final TextView workerName;
        private final TextView workerOccupation;
        private final TextView workerRating;
        private final TextView workerReviews;
        private final TextView availability;
        private final TextView specialties;
        private final TextView workerPrice;
        private final TextView workerDistance;
        private final LinearLayout actionRow;
        private final MaterialButton buttonChat;
        private final MaterialButton buttonViewProfile;

        WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.worker_image);
            workerName = itemView.findViewById(R.id.worker_name);
            workerOccupation = itemView.findViewById(R.id.worker_occupation);
            workerRating = itemView.findViewById(R.id.worker_rating);
            workerReviews = itemView.findViewById(R.id.worker_reviews);
            availability = itemView.findViewById(R.id.availability_text);
            specialties = itemView.findViewById(R.id.worker_specialties);
            workerPrice = itemView.findViewById(R.id.worker_price);
            workerDistance = itemView.findViewById(R.id.worker_distance);
            actionRow = itemView.findViewById(R.id.action_row);
            buttonChat = itemView.findViewById(R.id.button_chat);
            buttonViewProfile = itemView.findViewById(R.id.button_view_profile);
        }

        void bind(@NonNull RecommendedWorker worker, @NonNull Listener listener) {
            workerName.setText(worker.getName());
            workerOccupation.setText(worker.getOccupation());
            workerRating.setText(String.format(Locale.getDefault(), "%.1f★", worker.getRating()));
            workerReviews.setText(itemView.getContext().getString(R.string.home_worker_reviews_format, worker.getReviewCount()));

            if (TextUtils.isEmpty(worker.getAvailability())) {
                availability.setVisibility(View.GONE);
            } else {
                availability.setVisibility(View.VISIBLE);
                availability.setText(worker.getAvailability());
            }

            if (worker.getSpecialties().isEmpty()) {
                specialties.setVisibility(View.GONE);
            } else {
                specialties.setVisibility(View.VISIBLE);
                specialties.setText(TextUtils.join(" • ", worker.getSpecialties()));
            }

            if (TextUtils.isEmpty(worker.getPriceDisplay())) {
                workerPrice.setVisibility(View.GONE);
            } else {
                workerPrice.setVisibility(View.VISIBLE);
                workerPrice.setText(worker.getPriceDisplay());
            }

            if (TextUtils.isEmpty(worker.getDistanceDisplay())) {
                workerDistance.setVisibility(View.GONE);
            } else {
                workerDistance.setVisibility(View.VISIBLE);
                workerDistance.setText(worker.getDistanceDisplay());
            }

            ImageLoader.loadUriInto(
                    itemView.getContext(),
                    imageAvatar,
                    worker.getImageUri(),
                    R.drawable.ic_worker,
                    R.color.primary_color
            );

            View.OnClickListener openProfileListener = v -> {
                if (!TextUtils.isEmpty(worker.getServiceId())) {
                    listener.onWorkerSelected(worker);
                } else {
                    listener.onChatRequested(worker);
                }
            };

            itemView.setOnClickListener(openProfileListener);

            buttonChat.setOnClickListener(v -> listener.onChatRequested(worker));

            if (TextUtils.isEmpty(worker.getServiceId())) {
                buttonViewProfile.setVisibility(View.GONE);
            } else {
                buttonViewProfile.setVisibility(View.VISIBLE);
                buttonViewProfile.setOnClickListener(v -> listener.onWorkerSelected(worker));
            }

            if (actionRow != null) {
                actionRow.setVisibility(View.VISIBLE);
            }
        }
    }
}
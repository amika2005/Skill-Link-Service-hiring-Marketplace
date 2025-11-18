package com.skilllink.ui.user.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.skilllink.R;

import java.util.List;

public class ServiceCategoryAdapter extends RecyclerView.Adapter<ServiceCategoryAdapter.ViewHolder> {

    private final List<HomeFragment.ServiceCategory> services;
    private final OnServiceClickListener listener;

    public interface OnServiceClickListener {
        void onServiceClick(HomeFragment.ServiceCategory service);
    }

    public ServiceCategoryAdapter(List<HomeFragment.ServiceCategory> services, OnServiceClickListener listener) {
        this.services = services;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeFragment.ServiceCategory service = services.get(position);
        holder.bind(service, listener);
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView serviceImage;
        private final TextView serviceName;
        private final TextView workersCount;
        private final CardView serviceCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            serviceImage = itemView.findViewById(R.id.service_image);
            serviceName = itemView.findViewById(R.id.service_name);
            workersCount = itemView.findViewById(R.id.workers_count);
            serviceCard = itemView.findViewById(R.id.service_card);
        }

        public void bind(HomeFragment.ServiceCategory service, OnServiceClickListener listener) {
            Context context = itemView.getContext();
            serviceName.setText(service.getName());
            workersCount.setText(context.getString(R.string.home_service_workers_format, service.getWorkersCount()));

            // Set image based on service type
            serviceImage.setImageResource(service.getImageResource());
            serviceImage.setImageTintList(null);
            serviceImage.clearColorFilter();

            serviceCard.setOnClickListener(v -> listener.onServiceClick(service));
        }
    }
}

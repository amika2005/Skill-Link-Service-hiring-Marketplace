package com.skilllink.ui.worker.account;

import android.content.Context;
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
import com.skilllink.util.ImageLoader;
import com.skilllink.util.ServiceCategoryRegistry;

import java.util.List;

class WorkerServicesAdapter extends RecyclerView.Adapter<WorkerServicesAdapter.ViewHolder> {

    interface Listener {
        void onEditService(@NonNull WorkerService service);

        void onDeleteService(@NonNull WorkerService service);
    }

    private final List<WorkerService> services;
    private final Listener listener;

    WorkerServicesAdapter(@NonNull List<WorkerService> services,
                          @NonNull Listener listener) {
        this.services = services;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_worker_service, parent, false);
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
        private final TextView textCategory;
        private final TextView textPrice;
        private final TextView textServiceArea;
        private final TextView textBio;
        private final MaterialButton buttonEdit;
        private final MaterialButton buttonDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageService = itemView.findViewById(R.id.imageService);
            textServiceName = itemView.findViewById(R.id.textServiceName);
            textCategory = itemView.findViewById(R.id.textCategory);
            textPrice = itemView.findViewById(R.id.textPrice);
            textServiceArea = itemView.findViewById(R.id.textServiceArea);
            textBio = itemView.findViewById(R.id.textBio);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }

        void bind(@NonNull WorkerService service, @NonNull Listener listener) {
            Context context = itemView.getContext();

            textServiceName.setText(!TextUtils.isEmpty(service.getName())
                    ? service.getName()
                    : context.getString(R.string.manage_services_field_name));

            String displayCategory = ServiceCategoryRegistry.getDisplayNameOrDefault(service.getCategory());
            if (TextUtils.isEmpty(displayCategory)) {
                textCategory.setVisibility(View.GONE);
            } else {
                textCategory.setVisibility(View.VISIBLE);
                textCategory.setText(displayCategory);
            }

            String priceValue = service.getPriceValue();
            if (TextUtils.isEmpty(priceValue)) {
                textPrice.setVisibility(View.GONE);
            } else {
                textPrice.setVisibility(View.VISIBLE);
                if (service.isHourlyPricing()) {
                    textPrice.setText(context.getString(R.string.home_service_price_hourly_format, priceValue));
                } else {
                    textPrice.setText(context.getString(R.string.home_service_price_custom_format, priceValue));
                }
            }

            String serviceArea = service.getServiceArea();
            double radius = service.getCoverageRadiusKm();
            if (TextUtils.isEmpty(serviceArea)) {
                textServiceArea.setVisibility(View.GONE);
            } else {
                textServiceArea.setVisibility(View.VISIBLE);
                double effectiveRadius = radius > 0 ? radius : 20d;
                int roundedRadius = (int) Math.round(effectiveRadius);
                String radiusLabel = context.getString(R.string.manage_services_field_radius_value_format, roundedRadius);
                textServiceArea.setText(context.getString(R.string.manage_services_service_area_format, serviceArea, radiusLabel));
            }

            if (TextUtils.isEmpty(service.getBio())) {
                textBio.setVisibility(View.GONE);
            } else {
                textBio.setVisibility(View.VISIBLE);
                textBio.setText(service.getBio());
            }

            ImageLoader.loadUriInto(
                    itemView.getContext(),
                    imageService,
                    service.getImageUri(),
                    R.drawable.ic_service_manage,
                    R.color.primary_color
            );

            buttonEdit.setOnClickListener(v -> listener.onEditService(service));
            buttonDelete.setOnClickListener(v -> listener.onDeleteService(service));
        }

    }
}

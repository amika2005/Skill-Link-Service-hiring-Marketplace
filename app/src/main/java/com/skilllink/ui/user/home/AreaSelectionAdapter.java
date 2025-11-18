package com.skilllink.ui.user.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skilllink.R;
import com.skilllink.model.ServiceArea;

import java.util.ArrayList;
import java.util.List;

class AreaSelectionAdapter extends RecyclerView.Adapter<AreaSelectionAdapter.AreaViewHolder> {

    interface Listener {
        void onAreaSelected(@NonNull ServiceArea area);
    }

    private final List<ServiceArea> areas = new ArrayList<>();
    private final Listener listener;

    AreaSelectionAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    void submitList(@NonNull List<ServiceArea> data) {
        areas.clear();
        areas.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AreaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_area_option, parent, false);
        return new AreaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AreaViewHolder holder, int position) {
        ServiceArea area = areas.get(position);
        holder.bind(area);
    }

    @Override
    public int getItemCount() {
        return areas.size();
    }

    class AreaViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final TextView textDistrict;

        AreaViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_name);
            textDistrict = itemView.findViewById(R.id.text_district);
        }

        void bind(@NonNull ServiceArea area) {
            textName.setText(area.getName());
            String district = area.getDistrict();
            if (district == null || district.trim().isEmpty()) {
                textDistrict.setVisibility(View.GONE);
            } else {
                textDistrict.setVisibility(View.VISIBLE);
                textDistrict.setText(district);
            }
            itemView.setOnClickListener(v -> listener.onAreaSelected(area));
        }
    }
}

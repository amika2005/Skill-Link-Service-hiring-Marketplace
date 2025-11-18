package com.skilllink.ui.user.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.skilllink.R;
import com.skilllink.model.ServiceArea;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AreaSelectionBottomSheet extends BottomSheetDialogFragment implements AreaSelectionAdapter.Listener {

    public static final String REQUEST_KEY = "area_selection_request";
    public static final String RESULT_AREA_NAME = "result_area_name";
    public static final String RESULT_AREA_DISTRICT = "result_area_district";
    public static final String RESULT_AREA_LATITUDE = "result_area_latitude";
    public static final String RESULT_AREA_LONGITUDE = "result_area_longitude";

    private SessionManager sessionManager;
    private final List<ServiceArea> allAreas = new ArrayList<>();
    private final List<ServiceArea> filteredAreas = new ArrayList<>();
    private AreaSelectionAdapter adapter;
    private ChipGroup recentChipGroup;
    private View recentContainer;
    private ChipGroup popularChipGroup;
    private View popularContainer;
    private View emptyState;
    private TextInputEditText searchInput;

    public static AreaSelectionBottomSheet newInstance() {
        return new AreaSelectionBottomSheet();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        allAreas.addAll(sessionManager.getServiceAreas());
        Collections.sort(allAreas, Comparator.comparing(ServiceArea::getName, String.CASE_INSENSITIVE_ORDER));
        filteredAreas.addAll(allAreas);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_area_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        searchInput = view.findViewById(R.id.input_search);
        recentContainer = view.findViewById(R.id.container_recent);
        popularContainer = view.findViewById(R.id.container_popular);
        recentChipGroup = view.findViewById(R.id.group_recent);
        popularChipGroup = view.findViewById(R.id.group_popular);
        emptyState = view.findViewById(R.id.text_empty);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_results);

        adapter = new AreaSelectionAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        adapter.submitList(new ArrayList<>(filteredAreas));

        populateRecentChips();
        populatePopularChips();
        updateEmptyState();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterAreas(s != null ? s.toString() : "");
            }
        });
    }

    private void populateRecentChips() {
        List<String> recentNames = sessionManager.getRecentServiceAreas();
        recentChipGroup.removeAllViews();
        if (recentNames.isEmpty()) {
            recentContainer.setVisibility(View.GONE);
            return;
        }
        recentContainer.setVisibility(View.VISIBLE);
        for (String name : recentNames) {
            ServiceArea area = findAreaByName(name);
            if (area == null) {
                continue;
            }
            Chip chip = createAreaChip(recentChipGroup, area.getName());
            chip.setOnClickListener(v -> handleSelection(area));
            recentChipGroup.addView(chip);
        }
    }

    private void populatePopularChips() {
        popularChipGroup.removeAllViews();
        List<ServiceArea> popularAreas = new ArrayList<>();
        for (ServiceArea area : allAreas) {
            if (area.isPopular()) {
                popularAreas.add(area);
            }
        }
        if (popularAreas.isEmpty()) {
            popularContainer.setVisibility(View.GONE);
            return;
        }
        popularContainer.setVisibility(View.VISIBLE);
        for (ServiceArea area : popularAreas) {
            Chip chip = createAreaChip(popularChipGroup, area.getName());
            chip.setOnClickListener(v -> handleSelection(area));
            popularChipGroup.addView(chip);
        }
    }

    private Chip createAreaChip(@NonNull ChipGroup parent, @NonNull String label) {
        Chip chip = (Chip) LayoutInflater.from(requireContext()).inflate(R.layout.view_filter_chip, parent, false);
        chip.setText(label);
        chip.setCheckable(false);
        return chip;
    }

    private void filterAreas(@NonNull String query) {
        String trimmed = query.trim();
        filteredAreas.clear();
        if (trimmed.isEmpty()) {
            filteredAreas.addAll(allAreas);
        } else {
            String lower = trimmed.toLowerCase(Locale.getDefault());
            for (ServiceArea area : allAreas) {
                String name = area.getName();
                String district = area.getDistrict();
                if ((name != null && name.toLowerCase(Locale.getDefault()).contains(lower)) ||
                        (district != null && district.toLowerCase(Locale.getDefault()).contains(lower))) {
                    filteredAreas.add(area);
                }
            }
        }
        adapter.submitList(new ArrayList<>(filteredAreas));
        updateEmptyState();
    }

    private void updateEmptyState() {
        emptyState.setVisibility(filteredAreas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private ServiceArea findAreaByName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (ServiceArea area : allAreas) {
            if (name.equalsIgnoreCase(area.getName())) {
                return area;
            }
        }
        return null;
    }

    private void handleSelection(@NonNull ServiceArea area) {
        sessionManager.addRecentServiceArea(area.getName());
        Bundle result = new Bundle();
        result.putString(RESULT_AREA_NAME, area.getName());
        if (!TextUtils.isEmpty(area.getDistrict())) {
            result.putString(RESULT_AREA_DISTRICT, area.getDistrict());
        }
        if (!Double.isNaN(area.getLatitude())) {
            result.putDouble(RESULT_AREA_LATITUDE, area.getLatitude());
        }
        if (!Double.isNaN(area.getLongitude())) {
            result.putDouble(RESULT_AREA_LONGITUDE, area.getLongitude());
        }
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismissAllowingStateLoss();
    }

    @Override
    public void onAreaSelected(@NonNull ServiceArea area) {
        handleSelection(area);
    }
}

package com.skilllink.ui.user.home;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentResultListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skilllink.R;
import com.skilllink.model.WorkerService;
import com.skilllink.util.ServiceCategoryRegistry;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class NearbyProfessionalsMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MaterialToolbar toolbar;
    private AutoCompleteTextView inputCategory;
    private TextInputLayout inputLayoutArea;
    private TextInputEditText inputArea;
    private Slider sliderRadius;
    private TextView textRadiusValue;
    private MaterialButton buttonSearch;
    private MaterialButton buttonReset;
    private TextView textResultsSummary;
    private GoogleMap googleMap;
    private BitmapDescriptor markerDescriptor;
    private SessionManager sessionManager;
    private final List<WorkerService> allServices = new ArrayList<>();
    private final List<Marker> activeMarkers = new ArrayList<>();
    private String selectedAreaName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_professionals_map);

        toolbar = findViewById(R.id.toolbar);
        inputCategory = findViewById(R.id.input_category);
        inputLayoutArea = findViewById(R.id.input_layout_area);
        inputArea = findViewById(R.id.input_area);
        sliderRadius = findViewById(R.id.slider_radius);
        textRadiusValue = findViewById(R.id.text_radius_value);
        buttonSearch = findViewById(R.id.button_search);
        buttonReset = findViewById(R.id.button_reset);
        textResultsSummary = findViewById(R.id.text_results_summary);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        sessionManager = new SessionManager(this);
        allServices.addAll(sessionManager.getWorkerServices());

        configureCategoryDropdown();
        configureAreaSelector();
        configureRadiusSlider();
        configureButtons();

        getSupportFragmentManager().setFragmentResultListener(AreaSelectionBottomSheet.REQUEST_KEY, this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (!AreaSelectionBottomSheet.REQUEST_KEY.equals(requestKey)) {
                    return;
                }
                String area = result.getString(AreaSelectionBottomSheet.RESULT_AREA_NAME);
                selectedAreaName = area;
                if (!TextUtils.isEmpty(area)) {
                    inputArea.setText(area);
                }
                performSearch();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void configureCategoryDropdown() {
        List<String> categories = new ArrayList<>();
        categories.add(getString(R.string.map_filters_category_hint));
        for (ServiceCategoryRegistry.Category category : ServiceCategoryRegistry.getCategories()) {
            categories.add(category.getDisplayName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        inputCategory.setAdapter(adapter);
        inputCategory.setText(categories.get(0), false);
    }

    private void configureAreaSelector() {
        inputArea.setText(null);
        inputArea.setLongClickable(false);
        inputArea.setKeyListener(null);

        View.OnClickListener listener = v -> AreaSelectionBottomSheet.newInstance()
                .show(getSupportFragmentManager(), "area_selection_sheet");
        inputArea.setOnClickListener(listener);
        inputArea.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                listener.onClick(v);
            }
        });
        inputLayoutArea.setOnClickListener(listener);
        inputLayoutArea.setStartIconOnClickListener(listener);
    }

    private void configureRadiusSlider() {
        updateRadiusLabel(sliderRadius.getValue());
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> updateRadiusLabel(value));
    }

    private void configureButtons() {
        buttonSearch.setOnClickListener(v -> performSearch());
        buttonReset.setOnClickListener(v -> {
            inputCategory.setText(getString(R.string.map_filters_category_hint), false);
            selectedAreaName = null;
            inputArea.setText(null);
            sliderRadius.setValue(20f);
            performSearch();
        });
    }

    private void updateRadiusLabel(float value) {
        int rounded = Math.round(value);
        textRadiusValue.setText(getString(R.string.manage_services_field_radius_value_format, rounded));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setTrafficEnabled(false);

        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_sri_lanka));
        } catch (Resources.NotFoundException ignored) {
        }

        float density = getResources().getDisplayMetrics().density;
        int topPadding = (int) (density * 60f);
        int bottomPadding = (int) (density * 260f);
        googleMap.setPadding(0, topPadding, 0, bottomPadding);

        performSearch();
    }

    private void performSearch() {
        if (googleMap == null) {
            return;
        }

        allServices.clear();
        allServices.addAll(sessionManager.getWorkerServices());

        String selectedCategory = inputCategory.getText() != null ? inputCategory.getText().toString().trim() : "";
        String selectedArea = selectedAreaName != null ? selectedAreaName.trim() : "";
        float targetRadius = sliderRadius.getValue();
        String allCategoryLabel = getString(R.string.map_filters_category_hint);
        boolean filterByArea = !TextUtils.isEmpty(selectedArea);

        List<WorkerService> filtered = new ArrayList<>();
        for (WorkerService service : allServices) {
            if (service == null) {
                continue;
            }
            if (Double.isNaN(service.getLatitude()) || Double.isNaN(service.getLongitude())) {
                continue;
            }
            if (!allCategoryLabel.equalsIgnoreCase(selectedCategory)) {
                String displayName = ServiceCategoryRegistry.getDisplayNameOrDefault(service.getCategory());
                if (TextUtils.isEmpty(displayName) || !selectedCategory.equalsIgnoreCase(displayName)) {
                    continue;
                }
            }
            if (filterByArea) {
                String area = service.getServiceArea();
                if (TextUtils.isEmpty(area) || !selectedArea.equalsIgnoreCase(area.trim())) {
                    continue;
                }
            }
            double coverageRadius = service.getCoverageRadiusKm();
            if (coverageRadius > 0 && coverageRadius + 0.5 < targetRadius) {
                continue;
            }
            filtered.add(service);
        }

        updateMapMarkers(filtered);

        if (filtered.isEmpty()) {
            String summary = filterByArea
                    ? getString(R.string.map_filters_no_results_area, selectedArea)
                    : getString(R.string.map_filters_no_results);
            textResultsSummary.setText(summary);
            String toastMessage = filterByArea
                    ? getString(R.string.map_filters_no_results_area, selectedArea)
                    : getString(R.string.map_filters_no_results);
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        } else {
            String summary = filterByArea
                    ? getString(R.string.map_results_summary_area, filtered.size(), selectedArea)
                    : getString(R.string.map_results_summary, filtered.size());
            textResultsSummary.setText(summary);

            String toastMessage = filterByArea
                    ? getString(R.string.map_filters_apply_toast_area, filtered.size(), selectedArea, (double) targetRadius)
                    : getString(R.string.map_filters_apply_toast, filtered.size(), (double) targetRadius);
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMapMarkers(@NonNull List<WorkerService> services) {
        googleMap.clear();
        activeMarkers.clear();

        if (services.isEmpty()) {
            centerOnSriLanka();
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        BitmapDescriptor descriptor = obtainMarkerDescriptor();

        for (WorkerService service : services) {
            LatLng position = new LatLng(service.getLatitude(), service.getLongitude());
            MarkerOptions options = new MarkerOptions()
                    .position(position)
                    .title(!TextUtils.isEmpty(service.getName()) ? service.getName() : getString(R.string.map_toolbar_title))
                    .snippet(buildMarkerSnippet(service));
            if (descriptor != null) {
                options.icon(descriptor);
            }
            Marker marker = googleMap.addMarker(options);
            if (marker != null) {
                activeMarkers.add(marker);
            }
            builder.include(position);
        }

        LatLngBounds bounds = builder.build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, (int) (getResources().getDisplayMetrics().widthPixels * 0.15f)));
    }

    @NonNull
    private String buildMarkerSnippet(@NonNull WorkerService service) {
        String priceValue = service.getPriceValue();
        String priceDisplay;
        if (!TextUtils.isEmpty(priceValue)) {
            if (service.isHourlyPricing()) {
                priceDisplay = getString(R.string.home_service_price_hourly_format, priceValue);
            } else {
                priceDisplay = getString(R.string.home_service_price_custom_format, priceValue);
            }
        } else {
            priceDisplay = getString(R.string.map_marker_price_unknown);
        }
        double coverage = service.getCoverageRadiusKm();
        int roundedCoverage = (int) Math.round(coverage > 0 ? coverage : 20d);
        String radiusLabel = getString(R.string.manage_services_field_radius_value_format, roundedCoverage);
        return getString(R.string.map_marker_snippet, priceDisplay, radiusLabel);
    }

    private void centerOnSriLanka() {
        LatLng center = new LatLng(7.873054, 80.771797);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 6.5f));
    }

    @Nullable
    private BitmapDescriptor obtainMarkerDescriptor() {
        if (markerDescriptor != null) {
            return markerDescriptor;
        }
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_map_marker_worker);
        if (drawable == null) {
            return null;
        }
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        markerDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        return markerDescriptor;
    }
}

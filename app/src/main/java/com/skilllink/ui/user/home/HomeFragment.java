package com.skilllink.ui.user.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.firebase.firestore.ListenerRegistration;
import com.skilllink.R;
import com.skilllink.data.firebase.FirebaseServiceStore;
import com.skilllink.data.RealTimeServiceManager;
import com.skilllink.model.RecommendedWorker;
import com.skilllink.model.WorkerService;
import com.skilllink.services.ServiceCategoryActivity;
import com.skilllink.ui.chat.ChatActivity;
import com.skilllink.ui.common.ModernNotificationsActivity;
import com.skilllink.ui.user.services.AllServicesActivity;
import com.skilllink.util.NameFormatter;
import com.skilllink.util.ServiceCategoryRegistry;
import com.skilllink.util.SessionManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private static final int MAX_HOME_SERVICES = 2;
    private static final int MAX_POPULAR_CATEGORIES = 8;
    private static final String MAP_VIEW_BUNDLE_KEY = "home_map_view_state";

    private RecyclerView categoriesRecycler;
    private RecyclerView workerServicesRecycler;
    private NestedScrollView homeScroll;

    private View heroCard;
    private View searchCard;
    private View metricsGroup;
    private View buttonUpdateLocation;
    private View buttonNewBooking;
    private View buttonServicesAll;
    private View buttonWorkerServicesAll;
    private View buttonMap;
    private View buttonEmergencyRequest;
    private View buttonEmergencyCall;
    private View buttonMapCta;
    private View notificationButton;
    private View buttonVoiceSearch;
    private View buttonFilterSearch;
    private View workerServicesEmptyState;
    private View workerServicesHeader;
    private View mapPreviewCard;

    private TextView greetingText;
    private TextView locationText;
    private TextView metricServicesValue;
    private TextView metricProfessionalsValue;
    private TextView metricRatingValue;
    private TextView searchResultsText;
    private TextView workerServicesPreviewInfo;
    private TextInputEditText searchInput;
    private MapView mapPreview;
    private CircularProgressIndicator mapPreviewLoading;

    private boolean entryAnimationPlayed;
    private GoogleMap mapPreviewMap;
    private boolean mapReady;
    private final List<Marker> mapPreviewMarkers = new ArrayList<>();
    private BitmapDescriptor mapMarkerDescriptor;

    private ServiceCategoryAdapter serviceCategoryAdapter;
    private WorkerServiceHomeAdapter workerServiceAdapter;
    private SessionManager sessionManager;
    private final List<ServiceCategory> filteredServiceCategories = new ArrayList<>();
    private final List<WorkerService> workerServices = new ArrayList<>();
    private final List<WorkerService> workerServicePreview = new ArrayList<>();
    private final List<ServiceCategory> serviceCategories = new ArrayList<>();
    private final List<RecommendedWorker> recommendedWorkers = new ArrayList<>();
    private FirebaseServiceStore firebaseServiceStore;
    private ListenerRegistration servicesRegistration;
    private boolean servicesSyncErrorShown;
    private BroadcastReceiver servicesUpdateReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MaterialFadeThrough fadeThrough = new MaterialFadeThrough();
        fadeThrough.setDuration(220);
        setEnterTransition(fadeThrough);
        setExitTransition(fadeThrough);

        initializeCategories();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_user_home_new, container, false);

        initializeViews(root);
        setupRecyclerViews();
        setupClickListeners();
        initializeMapView(savedInstanceState);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Ensure sample services exist for demo
        ensureSampleServicesExist();
        
        bindHeaderDetails();
        playEntryAnimations();
        setupSearchInteractions();
        refreshWorkerServices();
        refreshRecommendedWorkers();
        startServicesListener();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapPreview != null) {
            mapPreview.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapPreview != null) {
            mapPreview.onResume();
        }
        
        // Start real-time service updates
        RealTimeServiceManager realTimeManager = RealTimeServiceManager.getInstance(requireContext());
        realTimeManager.startListening();
        
        // Register broadcast receiver for service updates first
        registerServicesUpdateReceiver();
        
        // Then refresh data
        refreshWorkerServices();
        refreshRecommendedWorkers();
    }

    @Override
    public void onPause() {
        if (mapPreview != null) {
            mapPreview.onPause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (mapPreview != null) {
            mapPreview.onStop();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapPreview != null) {
            mapPreview.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapPreview != null) {
            Bundle mapBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
            if (mapBundle == null) {
                mapBundle = new Bundle();
                outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapBundle);
            }
            mapPreview.onSaveInstanceState(mapBundle);
        }
    }

    private void initializeViews(View root) {
        homeScroll = root.findViewById(R.id.home_scroll);
        categoriesRecycler = root.findViewById(R.id.categories_recycler);
        workerServicesRecycler = root.findViewById(R.id.worker_services_recycler);
        workerServicesHeader = root.findViewById(R.id.worker_services_header);
        heroCard = root.findViewById(R.id.hero_card);
        searchCard = root.findViewById(R.id.search_card);
        metricsGroup = root.findViewById(R.id.metrics_group);
        buttonUpdateLocation = root.findViewById(R.id.button_update_location);
        buttonNewBooking = root.findViewById(R.id.button_new_booking);
        buttonServicesAll = root.findViewById(R.id.button_services_all);
        buttonWorkerServicesAll = root.findViewById(R.id.button_worker_services_all);
        buttonMap = root.findViewById(R.id.button_map);
        buttonEmergencyRequest = root.findViewById(R.id.button_emergency_request);
        buttonEmergencyCall = root.findViewById(R.id.button_emergency_call);
        buttonMapCta = root.findViewById(R.id.button_map_cta);
        notificationButton = root.findViewById(R.id.button_notifications);
        buttonVoiceSearch = root.findViewById(R.id.button_voice_search);
        buttonFilterSearch = root.findViewById(R.id.button_filter_search);
        greetingText = root.findViewById(R.id.text_greeting);
        locationText = root.findViewById(R.id.text_location);
        metricServicesValue = root.findViewById(R.id.metric_services_value);
        metricProfessionalsValue = root.findViewById(R.id.metric_professionals_value);
        metricRatingValue = root.findViewById(R.id.metric_rating_value);
        searchResultsText = root.findViewById(R.id.text_search_results);
        searchInput = root.findViewById(R.id.input_search_services);
        workerServicesEmptyState = root.findViewById(R.id.worker_services_empty_state);
        workerServicesPreviewInfo = root.findViewById(R.id.worker_services_preview_info);
        mapPreviewCard = root.findViewById(R.id.map_preview_card);
        mapPreview = root.findViewById(R.id.map_preview);
        mapPreviewLoading = root.findViewById(R.id.map_preview_loading);

        sessionManager = new SessionManager(requireContext());
    }

    private void setupRecyclerViews() {
        filteredServiceCategories.clear();
        filteredServiceCategories.addAll(serviceCategories);

        serviceCategoryAdapter = new ServiceCategoryAdapter(filteredServiceCategories, this::onServiceClick);
        RecyclerView.LayoutManager categoriesLayoutManager = new GridLayoutManager(getContext(), 4, RecyclerView.VERTICAL, false);
        categoriesRecycler.setLayoutManager(categoriesLayoutManager);
        categoriesRecycler.setHasFixedSize(true);

        if (getContext() != null) {
            LayoutAnimationController categoriesAnimation = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_animation_fall_down);
            categoriesRecycler.setLayoutAnimation(categoriesAnimation);
        }

        categoriesRecycler.setAdapter(serviceCategoryAdapter);
        categoriesRecycler.scheduleLayoutAnimation();

        workerServiceAdapter = new WorkerServiceHomeAdapter(workerServicePreview, new WorkerServiceHomeAdapter.Listener() {
            @Override
            public void onServiceSelected(@NonNull WorkerService service) {
                onWorkerServiceClick(service);
            }

            @Override
            public void onChatClicked(@NonNull WorkerService service) {
                startChatWithWorker(service);
            }
        });
        workerServicesRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        workerServicesRecycler.setAdapter(workerServiceAdapter);
    }

    private void setupClickListeners() {
        View.OnClickListener comingSoonListener = v -> showComingSoonToast();

        if (buttonNewBooking != null) {
            buttonNewBooking.setOnClickListener(v -> navigateToProfessionalServices());
        }

        if (buttonUpdateLocation != null) {
            buttonUpdateLocation.setOnClickListener(v -> showLocationUpdateDialog());
        }

        if (buttonServicesAll != null) {
            buttonServicesAll.setOnClickListener(v -> openAllServices());
        }

        if (buttonWorkerServicesAll != null) {
            buttonWorkerServicesAll.setOnClickListener(v -> openAllServices());
        }

        if (buttonEmergencyRequest != null) {
            buttonEmergencyRequest.setOnClickListener(v -> openAllServices());
        }

        if (buttonEmergencyCall != null) {
            buttonEmergencyCall.setOnClickListener(v -> Toast.makeText(getContext(), R.string.home_emergency_contact_toast, Toast.LENGTH_SHORT).show());
        }

        if (buttonMap != null) {
            buttonMap.setOnClickListener(v -> openNearbyMap());
        }

        if (buttonMapCta != null) {
            buttonMapCta.setOnClickListener(v -> openNearbyMap());
        }

        if (mapPreviewCard != null) {
            mapPreviewCard.setOnClickListener(v -> openNearbyMap());
        }

        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> openNotifications());
        }

        if (buttonVoiceSearch != null) {
            buttonVoiceSearch.setOnClickListener(comingSoonListener);
        }

        if (buttonFilterSearch != null) {
            buttonFilterSearch.setOnClickListener(comingSoonListener);
        }

    }

    private void initializeMapView(@Nullable Bundle savedInstanceState) {
        if (mapPreview == null) {
            return;
        }

        Bundle mapBundle = null;
        if (savedInstanceState != null) {
            mapBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapPreview.onCreate(mapBundle);
        mapPreview.getMapAsync(this);
        if (mapPreviewLoading != null) {
            mapPreviewLoading.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        if (!isAdded()) {
            return;
        }
        MapsInitializer.initialize(requireContext().getApplicationContext());
        mapPreviewMap = googleMap;
        mapReady = true;
        if (mapPreviewLoading != null) {
            mapPreviewLoading.setVisibility(View.GONE);
        }

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.setBuildingsEnabled(false);
        googleMap.setTrafficEnabled(false);
        googleMap.setMinZoomPreference(5.5f);
        googleMap.setMaxZoomPreference(18f);

        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_sri_lanka));
        } catch (Exception ignored) {
            // ignore
        }

        updateMapMarkers();
    }

    private void bindHeaderDetails() {
        if (greetingText == null || locationText == null) {
            return;
        }

        if (sessionManager == null) {
            sessionManager = new SessionManager(requireContext());
        }

        NameFormatter.Parts parts = NameFormatter.resolve(sessionManager.getUserName(), sessionManager.getUserEmail());

        String greetingName = getString(R.string.home_greeting_fallback_name);
        if (parts != null && !TextUtils.isEmpty(parts.getFirstName())) {
            greetingName = parts.getFirstName();
        }

        greetingText.setText(getString(R.string.home_greeting_title, greetingName));

        String location = sessionManager.getUserLocation();
        if (TextUtils.isEmpty(location)) {
            location = getString(R.string.home_location_placeholder);
        }
        locationText.setText(location);

        updateServiceMetric(workerServices.size());
        updateProfessionalMetric();
        updateRatingMetric();
    }

    private void playEntryAnimations() {
        if (entryAnimationPlayed) {
            return;
        }
        entryAnimationPlayed = true;

        animateView(heroCard, 0);
        animateView(searchCard, 70);
        animateView(metricsGroup, 140);
    }

    private void setupSearchInteractions() {
        if (searchInput == null) {
            return;
        }

        applySearchQuery("");

        searchInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(textView);
                return true;
            }
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchQuery(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void refreshWorkerServices() {
        if (!isAdded()) {
            return;
        }

        if (sessionManager == null) {
            sessionManager = new SessionManager(requireContext());
        }
        
        // Load from local cache first
        List<WorkerService> storedServices = sessionManager.getWorkerServices();

        workerServices.clear();
        if (storedServices != null) {
            workerServices.addAll(storedServices);
        }

        workerServicePreview.clear();
        if (!workerServices.isEmpty()) {
            List<WorkerService> sorted = new ArrayList<>(workerServices);
            Collections.sort(sorted, (a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
            int limit = Math.min(MAX_HOME_SERVICES, sorted.size());
            workerServicePreview.addAll(sorted.subList(0, limit));
        }

        updateCategoryCounts();

        if (workerServiceAdapter != null) {
            workerServiceAdapter.notifyDataSetChanged();
        }

        boolean hasPreview = !workerServicePreview.isEmpty();
        if (workerServicesRecycler != null) {
            workerServicesRecycler.setVisibility(hasPreview ? View.VISIBLE : View.GONE);
        }
        if (workerServicesEmptyState != null) {
            workerServicesEmptyState.setVisibility(workerServices.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (workerServicesPreviewInfo != null) {
            if (workerServices.size() > workerServicePreview.size() && hasPreview) {
                workerServicesPreviewInfo.setVisibility(View.VISIBLE);
                workerServicesPreviewInfo.setText(getString(R.string.home_worker_services_preview_info,
                        workerServicePreview.size(), workerServices.size()));
            } else {
                workerServicesPreviewInfo.setVisibility(View.GONE);
            }
        }

        updateServiceMetric(workerServices.size());
        updateMapMarkers();
        
        // Also refresh from Firebase to get latest data
        refreshFromFirebase();
    }

    private void refreshFromFirebase() {
        if (firebaseServiceStore == null) {
            firebaseServiceStore = FirebaseServiceStore.getInstance();
        }

        if (firebaseServiceStore == null || !firebaseServiceStore.isEnabled()) {
            return;
        }

        firebaseServiceStore.refreshAllServices(new FirebaseServiceStore.ServiceListCallback() {
            @Override
            public void onSuccess(List<WorkerService> services) {
                if (sessionManager != null) {
                    sessionManager.saveWorkerServices(services);
                }
                
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        workerServices.clear();
                        if (services != null) {
                            workerServices.addAll(services);
                        }

                        workerServicePreview.clear();
                        if (!workerServices.isEmpty()) {
                            List<WorkerService> sorted = new ArrayList<>(workerServices);
                            Collections.sort(sorted, (a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
                            int limit = Math.min(MAX_HOME_SERVICES, sorted.size());
                            workerServicePreview.addAll(sorted.subList(0, limit));
                        }

                        updateCategoryCounts();

                        if (workerServiceAdapter != null) {
                            workerServiceAdapter.notifyDataSetChanged();
                        }

                        boolean hasPreview = !workerServicePreview.isEmpty();
                        if (workerServicesRecycler != null) {
                            workerServicesRecycler.setVisibility(hasPreview ? View.VISIBLE : View.GONE);
                        }
                        if (workerServicesEmptyState != null) {
                            workerServicesEmptyState.setVisibility(workerServices.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                        if (workerServicesPreviewInfo != null) {
                            if (workerServices.size() > workerServicePreview.size() && hasPreview) {
                                workerServicesPreviewInfo.setVisibility(View.VISIBLE);
                                workerServicesPreviewInfo.setText(getString(R.string.home_worker_services_preview_info,
                                        workerServicePreview.size(), workerServices.size()));
                            } else {
                                workerServicesPreviewInfo.setVisibility(View.GONE);
                            }
                        }

                        updateServiceMetric(workerServices.size());
                        updateMapMarkers();
                    });
                }
            }

            @Override
            public void onError(Exception exception) {
                // Continue with local data if Firebase fails
            }
        });
    }

    private void updateServiceMetric(int workerServiceCount) {
        if (metricServicesValue == null) {
            return;
        }

        if (workerServiceCount > 0) {
            metricServicesValue.setText(String.valueOf(workerServiceCount));
        } else {
            metricServicesValue.setText(String.valueOf(serviceCategories.size()));
        }
    }

    private void refreshRecommendedWorkers() {
        if (!isAdded()) {
            return;
        }

        if (sessionManager == null) {
            sessionManager = new SessionManager(requireContext());
        }

        List<RecommendedWorker> storedWorkers = sessionManager.getRecommendedWorkers();

        recommendedWorkers.clear();
        if (storedWorkers != null) {
            recommendedWorkers.addAll(storedWorkers);
        }

        updateProfessionalMetric();
        updateRatingMetric();
        updateMapMarkers();
    }

    private void startServicesListener() {
        if (!isAdded()) {
            return;
        }

        if (firebaseServiceStore == null) {
            firebaseServiceStore = FirebaseServiceStore.getInstance();
        }

        if (firebaseServiceStore == null || !firebaseServiceStore.isEnabled()) {
            return;
        }

        if (servicesRegistration != null) {
            servicesRegistration.remove();
        }

        servicesSyncErrorShown = false;

        // Initial refresh to get latest data
        firebaseServiceStore.refreshAllServices(new FirebaseServiceStore.ServiceListCallback() {
            @Override
            public void onSuccess(List<WorkerService> services) {
                applyServiceUpdates(services);
            }

            @Override
            public void onError(Exception exception) {
                handleServiceSyncError();
            }
        });

        // Set up real-time listener for continuous updates
        servicesRegistration = firebaseServiceStore.listenToAllServices(new FirebaseServiceStore.ServiceListener() {
            @Override
            public void onServicesChanged(List<WorkerService> services) {
                applyServiceUpdates(services);
            }

            @Override
            public void onError(Exception exception) {
                handleServiceSyncError();
            }
        });
    }

    private void applyServiceUpdates(@NonNull List<WorkerService> services) {
        if (sessionManager == null && isAdded()) {
            sessionManager = new SessionManager(requireContext());
        }

        if (sessionManager != null) {
            sessionManager.saveWorkerServices(services);
        }

        if (!isAdded()) {
            return;
        }

        requireActivity().runOnUiThread(() -> {
            servicesSyncErrorShown = false;
            refreshWorkerServices();
        });
    }

    private void handleServiceSyncError() {
        if (!isAdded() || servicesSyncErrorShown) {
            return;
        }
        servicesSyncErrorShown = true;
        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), R.string.home_services_sync_failed, Toast.LENGTH_SHORT).show());
    }

    private void updateProfessionalMetric() {
        if (metricProfessionalsValue == null) {
            return;
        }

        int totalProfessionals = recommendedWorkers.isEmpty()
                ? calculateFallbackProfessionals()
                : recommendedWorkers.size();
        metricProfessionalsValue.setText(String.valueOf(totalProfessionals));
    }

    private void updateRatingMetric() {
        if (metricRatingValue == null) {
            return;
        }

        double rating = getAverageRating();
        metricRatingValue.setText(String.format(Locale.getDefault(), "%.1f★", rating));
    }

    private void initializeCategories() {
        serviceCategories.clear();
        int count = 0;
        for (ServiceCategoryRegistry.Category meta : ServiceCategoryRegistry.getCategories()) {
            ServiceCategory category = new ServiceCategory(
                    meta.getDisplayName(),
                    meta.getKey(),
                    meta.getDefaultWorkersCount(),
                    meta.getIconResId());
            serviceCategories.add(category);
            count++;
            if (count >= MAX_POPULAR_CATEGORIES) {
                break;
            }
        }
    }

    private void updateCategoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (WorkerService service : workerServices) {
            String key = ServiceCategoryRegistry.resolveKey(service.getCategory());
            if (TextUtils.isEmpty(key)) {
                continue;
            }
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        for (ServiceCategory category : serviceCategories) {
            int dynamicCount = counts.getOrDefault(category.getType(), 0);
            if (dynamicCount > 0) {
                category.setWorkersCount(dynamicCount);
            } else {
                category.setWorkersCount(category.getDefaultWorkersCount());
            }
        }

        if (serviceCategoryAdapter != null) {
            serviceCategoryAdapter.notifyDataSetChanged();
        }

        if (recommendedWorkers.isEmpty()) {
            updateProfessionalMetric();
        }
    }

    private void applySearchQuery(String query) {
        String trimmed = query != null ? query.trim() : "";
        filteredServiceCategories.clear();

        if (trimmed.isEmpty()) {
            filteredServiceCategories.addAll(serviceCategories);
            updateSearchResultsInfo(null, serviceCategories.size());
        } else {
            String lower = trimmed.toLowerCase(Locale.getDefault());
            for (ServiceCategory category : serviceCategories) {
                if (category.getName().toLowerCase(Locale.getDefault()).contains(lower)) {
                    filteredServiceCategories.add(category);
                }
            }
            updateSearchResultsInfo(trimmed, filteredServiceCategories.size());
        }

        if (serviceCategoryAdapter != null) {
            serviceCategoryAdapter.notifyDataSetChanged();
        }
    }

    private void updateSearchResultsInfo(@Nullable String query, int count) {
        if (searchResultsText == null) {
            return;
        }

        if (TextUtils.isEmpty(query)) {
            searchResultsText.setVisibility(View.GONE);
            return;
        }

        if (count == 0) {
            searchResultsText.setText(getString(R.string.home_search_no_results, query));
        } else {
            searchResultsText.setText(getString(R.string.home_search_results_count, count, query));
        }
        searchResultsText.setVisibility(View.VISIBLE);
    }

    private void openAllServices() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, AllServicesActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        if (servicesRegistration != null) {
            servicesRegistration.remove();
            servicesRegistration = null;
        }
        if (mapPreview != null) {
            mapPreview.onDestroy();
            mapPreview = null;
        }
        
        // Stop real-time service updates
        RealTimeServiceManager realTimeManager = RealTimeServiceManager.getInstance(requireContext());
        realTimeManager.stopListening();
        
        // Unregister broadcast receiver
        unregisterServicesUpdateReceiver();
        
        super.onDestroyView();
        mapPreviewMap = null;
        mapReady = false;
        mapPreviewMarkers.clear();
    }

    private void hideKeyboard(@Nullable View view) {
        if (view == null) {
            return;
        }
        Context context = view.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void animateView(View target, long delay) {
        if (target == null) {
            return;
        }

        target.setAlpha(0f);
        target.setTranslationY(40f);
        target.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(420)
            .setInterpolator(new FastOutSlowInInterpolator())
            .start();
    }

    private int getTotalProfessionals() {
        if (!recommendedWorkers.isEmpty()) {
            return recommendedWorkers.size();
        }
        return calculateFallbackProfessionals();
    }

    private int calculateFallbackProfessionals() {
        int total = 0;
        for (ServiceCategory category : serviceCategories) {
            total += category.getWorkersCount();
        }
        return total;
    }

    private double getAverageRating() {
        if (recommendedWorkers.isEmpty()) {
            return 4.8d;
        }

        double sum = 0d;
        for (RecommendedWorker worker : recommendedWorkers) {
            sum += worker.getRating();
        }
        return sum / recommendedWorkers.size();
    }

    private void updateMapMarkers() {
        if (!mapReady || mapPreviewMap == null || !isAdded()) {
            return;
        }

        mapPreviewMap.clear();
        mapPreviewMarkers.clear();

        List<WorkerService> servicesWithLocation = new ArrayList<>();
        for (WorkerService service : workerServices) {
            if (service == null) {
                continue;
            }
            double lat = service.getLatitude();
            double lng = service.getLongitude();
            if (Double.isNaN(lat) || Double.isNaN(lng)) {
                continue;
            }
            servicesWithLocation.add(service);
        }

        if (!servicesWithLocation.isEmpty()) {
            renderServiceMarkers(servicesWithLocation);
            return;
        }

        List<RecommendedWorker> fallbackWorkers = new ArrayList<>();
        for (RecommendedWorker worker : recommendedWorkers) {
            if (worker == null) {
                continue;
            }
            double lat = worker.getLatitude();
            double lng = worker.getLongitude();
            if (Double.isNaN(lat) || Double.isNaN(lng)) {
                continue;
            }
            fallbackWorkers.add(worker);
        }

        if (fallbackWorkers.isEmpty()) {
            centerMapOnSriLanka();
        } else {
            renderRecommendedMarkers(fallbackWorkers);
        }
    }

    private void renderServiceMarkers(@NonNull List<WorkerService> services) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        BitmapDescriptor descriptor = getMarkerIcon();
        for (WorkerService service : services) {
            LatLng position = new LatLng(service.getLatitude(), service.getLongitude());
            String title = !TextUtils.isEmpty(service.getName())
                    ? service.getName()
                    : ServiceCategoryRegistry.getDisplayNameOrDefault(service.getCategory());
            MarkerOptions options = new MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet(buildServiceSnippet(service));
            if (descriptor != null) {
                options.icon(descriptor);
            }
            Marker marker = mapPreviewMap.addMarker(options);
            if (marker != null) {
                mapPreviewMarkers.add(marker);
            }
            boundsBuilder.include(position);
        }
        animateToBounds(boundsBuilder);
    }

    private void renderRecommendedMarkers(@NonNull List<RecommendedWorker> workers) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        BitmapDescriptor descriptor = getMarkerIcon();
        for (RecommendedWorker worker : workers) {
            LatLng position = new LatLng(worker.getLatitude(), worker.getLongitude());
            MarkerOptions options = new MarkerOptions()
                    .position(position)
                    .title(worker.getName())
                    .snippet(buildRecommendedSnippet(worker));
            if (descriptor != null) {
                options.icon(descriptor);
            }
            Marker marker = mapPreviewMap.addMarker(options);
            if (marker != null) {
                mapPreviewMarkers.add(marker);
            }
            boundsBuilder.include(position);
        }
        animateToBounds(boundsBuilder);
    }

    private void animateToBounds(@NonNull LatLngBounds.Builder builder) {
        int padding = (int) (getResources().getDisplayMetrics().widthPixels * 0.12f);
        LatLngBounds bounds = builder.build();
        if (mapPreview != null) {
            mapPreview.post(() -> mapPreviewMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding)));
        } else {
            mapPreviewMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }

    @NonNull
    private String buildServiceSnippet(@NonNull WorkerService service) {
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

    @NonNull
    private String buildRecommendedSnippet(@NonNull RecommendedWorker worker) {
        String priceDisplay = worker.getPriceDisplay();
        if (TextUtils.isEmpty(priceDisplay)) {
            priceDisplay = getString(R.string.map_marker_price_unknown);
        }
        double coverage = worker.getServiceRadiusKm();
        int roundedCoverage = (int) Math.round(coverage > 0 ? coverage : 20d);
        String radiusLabel = getString(R.string.manage_services_field_radius_value_format, roundedCoverage);
        return getString(R.string.map_marker_snippet, priceDisplay, radiusLabel);
    }

    private void centerMapOnSriLanka() {
        if (mapPreviewMap == null) {
            return;
        }
        LatLng center = new LatLng(7.873054, 80.771797);
        mapPreviewMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 6.5f));
    }

    @Nullable
    private BitmapDescriptor getMarkerIcon() {
        if (mapMarkerDescriptor != null) {
            return mapMarkerDescriptor;
        }
        if (!isAdded()) {
            return null;
        }
        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker_worker);
        if (drawable == null) {
            return null;
        }
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        mapMarkerDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        return mapMarkerDescriptor;
    }

    private void navigateToProfessionalServices() {
        openAllServices();
    }

    private void openNearbyMap() {
        if (!isAdded()) {
            return;
        }
        Intent intent = new Intent(requireContext(), NearbyProfessionalsMapActivity.class);
        startActivity(intent);
    }

    private void openNotifications() {
        if (!isAdded()) {
            return;
        }
        Intent intent = new Intent(requireContext(), ModernNotificationsActivity.class);
        startActivity(intent);
    }

    private void showLocationUpdateDialog() {
        if (!isAdded()) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_location, null, false);
        TextInputEditText inputField = dialogView.findViewById(R.id.input_location);

        String savedLocation = sessionManager != null ? sessionManager.getUserLocation() : null;
        if (!TextUtils.isEmpty(savedLocation) && inputField != null) {
            inputField.setText(savedLocation);
            inputField.setSelection(savedLocation.length());
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_location_dialog_title)
                .setView(dialogView)
                .setNegativeButton(R.string.home_location_dialog_negative, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.home_location_dialog_positive, null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton == null || inputField == null) {
                return;
            }
            inputField.setError(null);
            positiveButton.setOnClickListener(v -> {
                CharSequence value = inputField.getText();
                String location = value != null ? value.toString().trim() : "";
                if (location.isEmpty()) {
                    inputField.setError(getString(R.string.home_location_dialog_error));
                    return;
                }

                if (sessionManager == null) {
                    sessionManager = new SessionManager(requireContext());
                }
                sessionManager.setUserLocation(location);
                if (locationText != null) {
                    locationText.setText(location);
                }
                dialog.dismiss();
                Toast.makeText(requireContext(), R.string.home_location_updated, Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
    }

    private void showComingSoonToast() {
        if (getContext() == null) {
            return;
        }
        Toast.makeText(getContext(), R.string.feature_coming_soon, Toast.LENGTH_SHORT).show();
    }

    private void onServiceClick(ServiceCategory service) {
        Intent intent = new Intent(getContext(), ServiceCategoryActivity.class);
        intent.putExtra("service_name", service.getName());
        intent.putExtra("service_type", service.getType());
        intent.putExtra("workers_count", service.getWorkersCount());
        startActivity(intent);
    }

    private void onWorkerServiceClick(@NonNull WorkerService service) {
        if (getContext() == null) {
            return;
        }
        Intent intent = new Intent(getContext(), WorkerServiceDetailActivity.class);
        intent.putExtra(WorkerServiceDetailActivity.EXTRA_SERVICE_ID, service.getId());
        startActivity(intent);
    }

    private void startChatWithWorker(@NonNull WorkerService service) {
        if (getContext() == null) {
            return;
        }
        
        String workerId = service.getOwnerId();
        String workerName = service.getOwnerName();
        
        if (TextUtils.isEmpty(workerId) || TextUtils.isEmpty(workerName)) {
            Toast.makeText(getContext(), "Worker information not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_PARTICIPANT_ID, workerId);
        intent.putExtra(ChatActivity.EXTRA_PARTICIPANT_NAME, workerName);
        intent.putExtra(ChatActivity.EXTRA_IS_USER_INITIATED, true);
        // Don't set conversation ID - let the chat activity create a new one
        startActivity(intent);
    }

    private void registerServicesUpdateReceiver() {
        if (servicesUpdateReceiver == null) {
            servicesUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if ("com.skilllink.SERVICES_UPDATED".equals(intent.getAction())) {
                        // Refresh worker services when notified
                        refreshWorkerServices();
                    }
                }
            };
        }
        
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(servicesUpdateReceiver, new IntentFilter("com.skilllink.SERVICES_UPDATED"));
    }

    private void unregisterServicesUpdateReceiver() {
        if (servicesUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(servicesUpdateReceiver);
            servicesUpdateReceiver = null;
        }
    }

    private void ensureSampleServicesExist() {
        if (sessionManager == null) {
            sessionManager = new SessionManager(requireContext());
        }
        
        List<WorkerService> services = sessionManager.getWorkerServices();
        if (services == null || services.isEmpty()) {
            // Create sample services for demo
            List<WorkerService> sampleServices = new ArrayList<>();
            
            // Sample service 1: Plumbing
            WorkerService service1 = WorkerService.create(
                "worker1", "John Doe", "john@example.com",
                "plumbing", "Plumbing Services", 
                "Expert plumbing repairs and installations", 
                WorkerService.PRICE_TYPE_HOURLY, "50",
                null
            );
            service1 = service1.withLocation("Colombo", 6.9271, 79.8612, 15.0);
            sampleServices.add(service1);
            
            // Sample service 2: Electrical
            WorkerService service2 = WorkerService.create(
                "worker2", "Jane Smith", "jane@example.com", 
                "electrical", "Electrical Services",
                "Professional electrical work and repairs",
                WorkerService.PRICE_TYPE_HOURLY, "60",
                null
            );
            service2 = service2.withLocation("Kandy", 7.2906, 80.6337, 20.0);
            sampleServices.add(service2);
            
            // Sample service 3: Cleaning
            WorkerService service3 = WorkerService.create(
                "worker3", "Mike Wilson", "mike@example.com",
                "cleaning", "House Cleaning Services",
                "Professional home and office cleaning services",
                WorkerService.PRICE_TYPE_HOURLY, "40",
                null
            );
            service3 = service3.withLocation("Galle", 6.0535, 80.2200, 25.0);
            sampleServices.add(service3);
            
            // Sample service 4: Painting
            WorkerService service4 = WorkerService.create(
                "worker4", "Sarah Brown", "sarah@example.com",
                "painting", "Painting Services",
                "Interior and exterior painting services",
                WorkerService.PRICE_TYPE_CUSTOM, "15000",
                null
            );
            service4 = service4.withLocation("Negombo", 7.2083, 79.8358, 30.0);
            sampleServices.add(service4);
            
            // Sample service 5: Carpentry
            WorkerService service5 = WorkerService.create(
                "worker5", "Tom Harris", "tom@example.com",
                "carpentry", "Carpentry Services",
                "Custom furniture making and woodwork",
                WorkerService.PRICE_TYPE_HOURLY, "55",
                null
            );
            service5 = service5.withLocation("Kurunegala", 6.9754, 80.7540, 35.0);
            sampleServices.add(service5);
            
            // Sample service 6: Landscaping
            WorkerService service6 = WorkerService.create(
                "worker6", "Lisa Chen", "lisa@example.com",
                "landscaping", "Landscaping Services",
                "Garden design and maintenance services",
                WorkerService.PRICE_TYPE_CUSTOM, "20000",
                null
            );
            service6 = service6.withLocation("Jaffna", 9.6615, 80.0255, 40.0);
            sampleServices.add(service6);
            
            // Save sample services
            sessionManager.saveWorkerServices(sampleServices);
            
            // Log for debugging
            Log.d("HomeFragment", "Created " + sampleServices.size() + " sample worker services");
        }
    }

    // Data model for service categories
    public static class ServiceCategory {
        private final String name;
        private final String type;
        private final int imageResource;
        private final int defaultWorkersCount;
        private int workersCount;

        public ServiceCategory(String name, String type, int defaultWorkersCount, int imageResource) {
            this.name = name;
            this.type = type;
            this.imageResource = imageResource;
            this.defaultWorkersCount = defaultWorkersCount;
            this.workersCount = defaultWorkersCount;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public int getWorkersCount() { return workersCount; }
        public int getImageResource() { return imageResource; }
        public int getDefaultWorkersCount() { return defaultWorkersCount; }
        public void setWorkersCount(int workersCount) { this.workersCount = workersCount; }
    }
}

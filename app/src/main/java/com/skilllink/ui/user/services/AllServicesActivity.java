package com.skilllink.ui.user.services;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.skilllink.R;
import com.skilllink.model.WorkerService;
import com.skilllink.ui.user.home.WorkerServiceDetailActivity;
import com.skilllink.ui.user.home.WorkerServiceHomeAdapter;
import com.skilllink.util.ServiceCategoryRegistry;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AllServicesActivity extends AppCompatActivity implements WorkerServiceHomeAdapter.Listener {

    public static final String EXTRA_HIGHLIGHT_SERVICE_ID = "extra_highlight_service_id";

    private SessionManager sessionManager;
    private WorkerServiceHomeAdapter adapter;
    private final List<WorkerService> allServices = new ArrayList<>();
    private final List<WorkerService> filteredServices = new ArrayList<>();

    private TextInputEditText inputSearch;
    private MaterialAutoCompleteTextView inputCategory;
    private ImageButton buttonBack;
    private TextView textResultsCount;
    private View layoutEmptyState;
    private RecyclerView recyclerServices;
    private final List<String> categoryDisplayValues = new ArrayList<>();
    private final List<String> categoryKeyValues = new ArrayList<>();

    private String highlightServiceId;
    private boolean highlightHandled;

    private String selectedCategoryKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_services);

        sessionManager = new SessionManager(this);
        highlightServiceId = getIntent().getStringExtra(EXTRA_HIGHLIGHT_SERVICE_ID);
        highlightHandled = TextUtils.isEmpty(highlightServiceId);

        initializeViews();
        setupRecycler();
        setupCategoryDropdown();
        setupFilterInputs();

        loadServices();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadServices();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent == null) {
            return;
        }
        String newHighlightId = intent.getStringExtra(EXTRA_HIGHLIGHT_SERVICE_ID);
        if (!TextUtils.isEmpty(newHighlightId)) {
            highlightServiceId = newHighlightId;
            highlightHandled = false;
            loadServices();
        }
    }

    private void initializeViews() {
        inputSearch = findViewById(R.id.input_search);
        inputCategory = findViewById(R.id.input_category);
        buttonBack = findViewById(R.id.button_back);
        textResultsCount = findViewById(R.id.text_results_count);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        recyclerServices = findViewById(R.id.recycler_all_services);
        MaterialButton buttonClearFilters = findViewById(R.id.button_clear_filters);

        if (buttonClearFilters != null) {
            buttonClearFilters.setOnClickListener(v -> clearFilters());
        }

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecycler() {
        adapter = new WorkerServiceHomeAdapter(filteredServices, this);
        recyclerServices.setLayoutManager(new LinearLayoutManager(this));
        recyclerServices.setAdapter(adapter);
        recyclerServices.setItemAnimator(null);
    }

    private void setupCategoryDropdown() {
        if (inputCategory == null) {
            return;
        }

        categoryDisplayValues.clear();
        categoryKeyValues.clear();

        categoryDisplayValues.add(getString(R.string.all_services_category_all));
        categoryKeyValues.add(null);

        for (ServiceCategoryRegistry.Category category : ServiceCategoryRegistry.getCategories()) {
            categoryDisplayValues.add(category.getDisplayName());
            categoryKeyValues.add(category.getKey());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                categoryDisplayValues);
        inputCategory.setAdapter(adapter);
        inputCategory.setText(categoryDisplayValues.get(0), false);
        inputCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategoryKey = categoryKeyValues.get(position);
            applyFilters();
        });
    }

    private void setupFilterInputs() {
        if (inputSearch != null) {
            inputSearch.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilters();
                }
            });
        }
    }

    private void clearFilters() {
        if (inputSearch != null) {
            inputSearch.setText(null);
        }
        if (inputCategory != null && !categoryDisplayValues.isEmpty()) {
            inputCategory.setText(categoryDisplayValues.get(0), false);
        }
        selectedCategoryKey = null;
        applyFilters();
    }

    private void loadServices() {
        allServices.clear();
        List<WorkerService> storedServices = sessionManager.getWorkerServices();
        if (storedServices != null) {
            allServices.addAll(storedServices);
        }

        Collections.sort(allServices, new Comparator<WorkerService>() {
            @Override
            public int compare(WorkerService o1, WorkerService o2) {
                return Long.compare(o2.getUpdatedAt(), o1.getUpdatedAt());
            }
        });

        applyFilters();
        
        // Always try to refresh from Firebase to get latest services
        // This ensures services remain visible even when workers log out
        refreshFromFirebase();
    }

    private void refreshFromFirebase() {
        try {
            Class<?> firebaseClass = Class.forName("com.skilllink.data.firebase.FirebaseServiceStore");
            Object firebaseInstance = firebaseClass.getMethod("getInstance").invoke(null);
            Boolean enabled = (Boolean) firebaseClass.getMethod("isEnabled").invoke(firebaseInstance);
            
            if (enabled) {
                firebaseClass.getMethod("refreshAllServices", 
                    Class.forName("com.skilllink.data.firebase.FirebaseServiceStore$ServiceListCallback"))
                    .invoke(firebaseInstance, new Object[] { new FirebaseServiceCallback() });
            }
        } catch (Exception e) {
            // Firebase not available, continue with local data
        }
    }

    private class FirebaseServiceCallback {
        public void onSuccess(List<WorkerService> services) {
            runOnUiThread(() -> {
                if (services != null) {
                    sessionManager.saveWorkerServices(services);
                    allServices.clear();
                    allServices.addAll(services);
                    Collections.sort(allServices, (o1, o2) -> 
                        Long.compare(o2.getUpdatedAt(), o1.getUpdatedAt()));
                    applyFilters();
                }
            });
        }

        public void onError(Exception exception) {
            // Continue with local data if Firebase fails
        }
    }

    private void applyFilters() {
        String query = inputSearch != null && inputSearch.getText() != null
                ? inputSearch.getText().toString().trim().toLowerCase(Locale.getDefault())
                : "";

        filteredServices.clear();
        for (WorkerService service : allServices) {
            if (!matchesCategory(service)) {
                continue;
            }
            if (!matchesQuery(service, query)) {
                continue;
            }
            filteredServices.add(service);
        }

        adapter.notifyDataSetChanged();
        updateResultsState();
        maybeScrollToHighlight();
    }

    private void maybeScrollToHighlight() {
        if (TextUtils.isEmpty(highlightServiceId) || highlightHandled || recyclerServices == null) {
            return;
        }
        int position = -1;
        for (int i = 0; i < filteredServices.size(); i++) {
            WorkerService service = filteredServices.get(i);
            if (service != null && TextUtils.equals(highlightServiceId, service.getId())) {
                position = i;
                break;
            }
        }
        if (position < 0) {
            return;
        }
        final int targetPosition = position;
        recyclerServices.post(() -> recyclerServices.smoothScrollToPosition(targetPosition));
        highlightHandled = true;
    }

    private boolean matchesCategory(@NonNull WorkerService service) {
        if (TextUtils.isEmpty(selectedCategoryKey)) {
            return true;
        }
        String key = ServiceCategoryRegistry.resolveKey(service.getCategory());
        return TextUtils.equals(selectedCategoryKey, key);
    }

    private boolean matchesQuery(@NonNull WorkerService service, @NonNull String query) {
        if (query.isEmpty()) {
            return true;
        }

        String name = service.getName() != null ? service.getName().toLowerCase(Locale.getDefault()) : "";
        String bio = service.getBio() != null ? service.getBio().toLowerCase(Locale.getDefault()) : "";
        String category = ServiceCategoryRegistry.getDisplayNameOrDefault(service.getCategory())
                .toLowerCase(Locale.getDefault());

        return name.contains(query) || bio.contains(query) || category.contains(query);
    }

    private void updateResultsState() {
        int count = filteredServices.size();

        if (textResultsCount != null) {
            if (count > 0) {
                textResultsCount.setVisibility(View.VISIBLE);
                textResultsCount.setText(getResources().getQuantityString(
                        R.plurals.all_services_results_count,
                        count,
                        count));
            } else {
                textResultsCount.setVisibility(View.GONE);
            }
        }

        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        }

        if (recyclerServices != null) {
            recyclerServices.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onServiceSelected(@NonNull WorkerService service) {
        Intent intent = new Intent(this, WorkerServiceDetailActivity.class);
        intent.putExtra(WorkerServiceDetailActivity.EXTRA_SERVICE_ID, service.getId());
        startActivity(intent);
    }

    @Override
    public void onChatClicked(@NonNull WorkerService service) {
        Intent chatIntent = new Intent(this, com.skilllink.ui.chat.ChatActivity.class);
        chatIntent.putExtra(com.skilllink.ui.chat.ChatActivity.EXTRA_PARTICIPANT_ID, service.getOwnerId());
        chatIntent.putExtra(com.skilllink.ui.chat.ChatActivity.EXTRA_PARTICIPANT_NAME, service.getOwnerName());
        chatIntent.putExtra(com.skilllink.ui.chat.ChatActivity.EXTRA_IS_USER_INITIATED, true);
        startActivity(chatIntent);
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}

package com.skilllink.services;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.skilllink.R;
import com.skilllink.model.WorkerService;
import com.skilllink.ui.user.home.WorkerServiceDetailActivity;
import com.skilllink.ui.user.home.WorkerServiceHomeAdapter;
import com.skilllink.util.ServiceCategoryRegistry;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ServiceCategoryActivity extends AppCompatActivity {

    private TextView serviceTitle;
    private TextView workersCount;
    private ImageView serviceIcon;
    private RecyclerView workersRecycler;
    private Button bookNowButton;

    private String serviceName;
    private String categoryKey;
    private int workersCountValue;
    private boolean isEmergency;
    private final List<WorkerService> categoryServices = new ArrayList<>();
    private WorkerServiceHomeAdapter workerServiceAdapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_category);

        // Get data from intent
        Intent intent = getIntent();
        serviceName = intent.getStringExtra("service_name");
        String serviceType = intent.getStringExtra("service_type");
        isEmergency = intent.getBooleanExtra("is_emergency", false);

        String resolvedKey = ServiceCategoryRegistry.resolveKey(serviceType);
        if (TextUtils.isEmpty(resolvedKey)) {
            resolvedKey = ServiceCategoryRegistry.resolveKey(serviceName);
        }
        categoryKey = resolvedKey;
        workersCountValue = 0;

        initializeViews();
        setupData();
        setupRecyclerView();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWorkerServices();
    }

    private void initializeViews() {
        serviceTitle = findViewById(R.id.service_title);
        workersCount = findViewById(R.id.workers_count);
        serviceIcon = findViewById(R.id.service_icon);
        workersRecycler = findViewById(R.id.workers_recycler);
        bookNowButton = findViewById(R.id.book_now_button);
        sessionManager = new SessionManager(this);
    }

    private void setupData() {
        boolean appliedMeta = false;
        if (!TextUtils.isEmpty(categoryKey)) {
            ServiceCategoryRegistry.Category meta = ServiceCategoryRegistry.findByKey(categoryKey);
            if (meta != null) {
                serviceTitle.setText(meta.getDisplayName());
                if (meta.getIconResId() != 0) {
                    serviceIcon.setImageResource(meta.getIconResId());
                }
                appliedMeta = true;
            }
        }

        if (!appliedMeta) {
            serviceTitle.setText(!TextUtils.isEmpty(serviceName) ? serviceName : "Service");
        }

        updateWorkersCountText();

        // Update UI for emergency services
        if (isEmergency) {
            bookNowButton.setText("Book Emergency Service");
            bookNowButton.setBackgroundColor(getColor(android.R.color.holo_red_dark));
        }
    }

    private void setupRecyclerView() {
        workerServiceAdapter = new WorkerServiceHomeAdapter(categoryServices, new WorkerServiceHomeAdapter.Listener() {
            @Override
            public void onServiceSelected(@NonNull WorkerService service) {
                onWorkerServiceSelected(service);
            }

            @Override
            public void onChatClicked(@NonNull WorkerService service) {
                onWorkerServiceChatClicked(service);
            }
        });
        workersRecycler.setLayoutManager(new LinearLayoutManager(this));
        workersRecycler.setAdapter(workerServiceAdapter);
        loadWorkerServices();
    }

    private void setupClickListeners() {
        bookNowButton.setOnClickListener(v -> {
            // TODO: Navigate to booking flow
            Toast.makeText(this, "Booking functionality coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadWorkerServices() {
        List<WorkerService> storedServices = sessionManager.getWorkerServices();

        categoryServices.clear();
        if (storedServices != null) {
            for (WorkerService service : storedServices) {
                String key = ServiceCategoryRegistry.resolveKey(service.getCategory());
                if (TextUtils.isEmpty(categoryKey) || (!TextUtils.isEmpty(key) && key.equals(categoryKey))) {
                    categoryServices.add(service);
                }
            }
        }

        workersCountValue = categoryServices.size();
        updateWorkersCountText();

        if (workerServiceAdapter != null) {
            workerServiceAdapter.notifyDataSetChanged();
        }
        
        // Also try to refresh from Firebase for real-time updates
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
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (services != null) {
                        sessionManager.saveWorkerServices(services);
                        // Reload services for this category
                        loadWorkerServices();
                    }
                }
            });
        }

        public void onError(Exception exception) {
            // Continue with local data if Firebase fails
        }
    }

    private void updateWorkersCountText() {
        if (workersCount == null) {
            return;
        }

        if (workersCountValue <= 0) {
            workersCount.setText("No workers available yet");
        } else if (workersCountValue == 1) {
            workersCount.setText("1 worker available");
        } else {
            workersCount.setText(workersCountValue + " workers available");
        }
    }

    private void onWorkerServiceSelected(@NonNull WorkerService service) {
        Intent detailIntent = new Intent(this, WorkerServiceDetailActivity.class);
        detailIntent.putExtra(WorkerServiceDetailActivity.EXTRA_SERVICE_ID, service.getId());
        startActivity(detailIntent);
    }

    private void onWorkerServiceChatClicked(@NonNull WorkerService service) {
        Intent chatIntent = new Intent(this, com.skilllink.ui.chat.ChatActivity.class);
        chatIntent.putExtra(com.skilllink.ui.chat.ChatActivity.EXTRA_PARTICIPANT_ID, service.getOwnerId());
        chatIntent.putExtra(com.skilllink.ui.chat.ChatActivity.EXTRA_PARTICIPANT_NAME, service.getOwnerName());
        chatIntent.putExtra(com.skilllink.ui.chat.ChatActivity.EXTRA_IS_USER_INITIATED, true);
        startActivity(chatIntent);
    }

}

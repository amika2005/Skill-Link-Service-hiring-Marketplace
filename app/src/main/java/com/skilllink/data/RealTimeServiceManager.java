package com.skilllink.data;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.skilllink.data.firebase.FirebaseServiceStore;
import com.skilllink.model.WorkerService;
import com.skilllink.util.SessionManager;

import java.util.List;

/**
 * Real-time service manager that handles live updates when workers add services
 * This ensures that users see new services immediately without needing to refresh
 */
public class RealTimeServiceManager {
    
    private static final String ACTION_SERVICES_UPDATED = "com.skilllink.SERVICES_UPDATED";
    private static final String EXTRA_SERVICE_CATEGORY = "service_category";
    private static final String EXTRA_SERVICE_ID = "service_id";
    
    private static RealTimeServiceManager instance;
    private final Context context;
    private final SessionManager sessionManager;
    private final FirebaseServiceStore firebaseServiceStore;
    private boolean isListening = false;
    
    private RealTimeServiceManager(Context context) {
        this.context = context.getApplicationContext();
        this.sessionManager = new SessionManager(this.context);
        this.firebaseServiceStore = FirebaseServiceStore.getInstance();
    }
    
    public static synchronized RealTimeServiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new RealTimeServiceManager(context);
        }
        return instance;
    }
    
    /**
     * Start listening for real-time service updates
     */
    public void startListening() {
        if (isListening || firebaseServiceStore == null || !firebaseServiceStore.isEnabled()) {
            return;
        }
        
        isListening = true;
        
        // Initial refresh to get latest data
        firebaseServiceStore.refreshAllServices(new FirebaseServiceStore.ServiceListCallback() {
            @Override
            public void onSuccess(List<WorkerService> services) {
                if (services != null) {
                    sessionManager.saveWorkerServices(services);
                    broadcastServicesUpdate(services);
                }
            }
            
            @Override
            public void onError(Exception exception) {
                // Handle error silently
            }
        });
        
        // Set up real-time listener for continuous updates
        firebaseServiceStore.listenToAllServices(new FirebaseServiceStore.ServiceListener() {
            @Override
            public void onServicesChanged(List<WorkerService> services) {
                if (services != null) {
                    sessionManager.saveWorkerServices(services);
                    broadcastServicesUpdate(services);
                }
            }
            
            @Override
            public void onError(Exception exception) {
                // Handle error silently
            }
        });
    }
    
    /**
     * Stop listening for real-time updates
     */
    public void stopListening() {
        isListening = false;
    }
    
    /**
     * Manually trigger a refresh and broadcast update
     * This is useful when a worker adds a new service
     */
    public void refreshAndNotify() {
        if (firebaseServiceStore == null || !firebaseServiceStore.isEnabled()) {
            return;
        }
        
        firebaseServiceStore.refreshAllServices(new FirebaseServiceStore.ServiceListCallback() {
            @Override
            public void onSuccess(List<WorkerService> services) {
                if (services != null) {
                    sessionManager.saveWorkerServices(services);
                    broadcastServicesUpdate(services);
                }
            }
            
            @Override
            public void onError(Exception exception) {
                // Handle error silently
            }
        });
    }
    
    /**
     * Notify about a specific service addition
     */
    public void notifyServiceAdded(WorkerService service) {
        if (service == null) {
            return;
        }
        
        // First, add the service to the local cache immediately
        List<WorkerService> currentServices = sessionManager.getWorkerServices();
        boolean serviceExists = false;
        
        // Check if service already exists and update it
        for (int i = 0; i < currentServices.size(); i++) {
            WorkerService existing = currentServices.get(i);
            if (existing != null && service.getId().equals(existing.getId())) {
                currentServices.set(i, service);
                serviceExists = true;
                break;
            }
        }
        
        // If service doesn't exist, add it
        if (!serviceExists) {
            currentServices.add(0, service); // Add to front for priority
        }
        
        // Save updated services list
        sessionManager.saveWorkerServices(currentServices);
        
        // Refresh all services from Firebase to ensure consistency
        refreshAndNotify();
        
        // Also send a specific broadcast about this service
        Intent intent = new Intent(ACTION_SERVICES_UPDATED);
        intent.putExtra(EXTRA_SERVICE_CATEGORY, service.getCategory());
        intent.putExtra(EXTRA_SERVICE_ID, service.getId());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    
    /**
     * Broadcast service updates to all activities
     */
    private void broadcastServicesUpdate(List<WorkerService> services) {
        Intent intent = new Intent(ACTION_SERVICES_UPDATED);
        
        // Add category information for targeted updates
        if (services != null && !services.isEmpty()) {
            // Find the most recently added service
            WorkerService latestService = services.stream()
                    .max((s1, s2) -> Long.compare(s1.getUpdatedAt(), s2.getUpdatedAt()))
                    .orElse(null);
            
            if (latestService != null) {
                intent.putExtra(EXTRA_SERVICE_CATEGORY, latestService.getCategory());
                intent.putExtra(EXTRA_SERVICE_ID, latestService.getId());
            }
        }
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    
    /**
     * Check if a specific category was updated
     */
    public static boolean wasCategoryUpdated(Intent intent, String categoryKey) {
        if (intent == null || categoryKey == null) {
            return false;
        }
        String updatedCategory = intent.getStringExtra(EXTRA_SERVICE_CATEGORY);
        return categoryKey.equals(updatedCategory);
    }
    
    /**
     * Get the updated service ID from intent
     */
    public static String getUpdatedServiceId(Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getStringExtra(EXTRA_SERVICE_ID);
    }
    
    /**
     * Check if real-time updates are available
     */
    public boolean isRealTimeEnabled() {
        return firebaseServiceStore != null && firebaseServiceStore.isEnabled();
    }
}

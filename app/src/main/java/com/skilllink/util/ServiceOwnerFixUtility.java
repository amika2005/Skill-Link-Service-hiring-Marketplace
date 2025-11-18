package com.skilllink.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.skilllink.model.WorkerService;
import com.skilllink.model.WorkerJobRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to fix service owner information and ensure job requests are properly routed
 */
public class ServiceOwnerFixUtility {
    
    private static final String TAG = "ServiceOwnerFixUtility";
    
    /**
     * Fixes all services to ensure they have proper owner information
     */
    public static void fixServiceOwnership(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String currentWorkerId = sessionManager.getOrCreateWorkerDocumentId();
        String workerName = sessionManager.getUserName();
        String workerEmail = sessionManager.getUserEmail();
        
        Log.d(TAG, "Fixing service ownership for worker: " + currentWorkerId);
        
        // Fix services
        List<WorkerService> services = sessionManager.getWorkerServices();
        List<WorkerService> updatedServices = new ArrayList<>();
        boolean servicesUpdated = false;
        
        for (WorkerService service : services) {
            if (service == null) continue;
            
            // Check if service needs owner information
            if (TextUtils.isEmpty(service.getOwnerId()) || !currentWorkerId.equals(service.getOwnerId())) {
                Log.d(TAG, "Updating owner for service: " + service.getName() + 
                      " (old owner: " + service.getOwnerId() + ", new owner: " + currentWorkerId + ")");
                
                WorkerService updatedService = service.withOwner(currentWorkerId, workerName, workerEmail);
                updatedServices.add(updatedService);
                servicesUpdated = true;
            } else {
                updatedServices.add(service);
            }
        }
        
        if (servicesUpdated) {
            sessionManager.saveWorkerServices(updatedServices);
            Log.d(TAG, "Updated " + updatedServices.size() + " services with owner information");
        }
        
        Log.d(TAG, "Service ownership fix completed");
    }
    
    /**
     * Debug method to log current state of services and job requests
     */
    public static void debugCurrentState(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String currentWorkerId = sessionManager.getOrCreateWorkerDocumentId();
        
        Log.d(TAG, "=== DEBUG CURRENT STATE ===");
        Log.d(TAG, "Current Worker ID: " + currentWorkerId);
        Log.d(TAG, "Current Worker Name: " + sessionManager.getUserName());
        Log.d(TAG, "Current Worker Email: " + sessionManager.getUserEmail());
        
        // Log services
        List<WorkerService> services = sessionManager.getWorkerServices();
        Log.d(TAG, "Total Services: " + services.size());
        
        for (WorkerService service : services) {
            if (service == null) continue;
            Log.d(TAG, "Service: " + service.getName() + 
                  " (ID: " + service.getId() + 
                  ", Owner: " + service.getOwnerId() + 
                  ", OwnerName: " + service.getOwnerName() + ")");
        }
        
        // Log job requests
        List<WorkerJobRequest> requests = sessionManager.getWorkerJobRequests();
        Log.d(TAG, "Total Job Requests: " + requests.size());
        
        for (WorkerJobRequest request : requests) {
            if (request == null) continue;
            Log.d(TAG, "Job Request: " + request.getServiceName() + 
                  " (ID: " + request.getId() + 
                  ", ServiceOwner: " + request.getServiceOwnerId() + 
                  ", Status: " + request.getStatus() + 
                  ", Customer: " + request.getCustomerName() + ")");
        }
        
        Log.d(TAG, "=== END DEBUG ===");
    }
    
    /**
     * Checks if a job request should be visible to the current worker
     */
    public static boolean isJobRequestForCurrentWorker(Context context, WorkerJobRequest request) {
        SessionManager sessionManager = new SessionManager(context);
        String currentWorkerId = sessionManager.getOrCreateWorkerDocumentId();
        
        if (request == null || TextUtils.isEmpty(currentWorkerId)) {
            return false;
        }
        
        // Check if the request is specifically for the current worker
        if (!TextUtils.isEmpty(request.getServiceOwnerId())) {
            return currentWorkerId.equals(request.getServiceOwnerId());
        }
        
        // Fallback: check if the service belongs to current worker
        String serviceId = request.getServiceId();
        if (!TextUtils.isEmpty(serviceId)) {
            List<WorkerService> workerServices = sessionManager.getWorkerServices();
            for (WorkerService service : workerServices) {
                if (serviceId.equals(service.getId()) && currentWorkerId.equals(service.getOwnerId())) {
                    return true;
                }
            }
        }
        
        return false;
    }
}

package com.skilllink.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.skilllink.R;
import com.skilllink.util.FirebaseWorkerRecoveryUtility;

/**
 * Admin activity to recover deleted worker document fields in Firebase
 * This activity should only be accessible to administrators
 */
public class WorkerDataRecoveryActivity extends AppCompatActivity {
    
    private static final String TAG = "WorkerDataRecovery";
    
    private Button btnValidateAll;
    private Button btnRecoverAll;
    private Button btnRecoverSpecific;
    private TextView txtStatus;
    private TextView txtResults;
    private ProgressBar progressBar;
    
    private FirebaseWorkerRecoveryUtility recoveryUtility;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_data_recovery);
        
        initViews();
        setupClickListeners();
        
        recoveryUtility = new FirebaseWorkerRecoveryUtility();
    }
    
    private void initViews() {
        btnValidateAll = findViewById(R.id.btn_validate_all);
        btnRecoverAll = findViewById(R.id.btn_recover_all);
        btnRecoverSpecific = findViewById(R.id.btn_recover_specific);
        txtStatus = findViewById(R.id.txt_status);
        txtResults = findViewById(R.id.txt_results);
        progressBar = findViewById(R.id.progress_bar);
        
        updateStatus("Ready to validate and recover worker data");
    }
    
    private void setupClickListeners() {
        btnValidateAll.setOnClickListener(v -> validateAllWorkers());
        btnRecoverAll.setOnClickListener(v -> recoverAllWorkers());
        btnRecoverSpecific.setOnClickListener(v -> recoverSpecificWorker());
    }
    
    private void validateAllWorkers() {
        setLoading(true);
        updateStatus("Validating all worker documents...");
        txtResults.setText("");
        
        recoveryUtility.validateWorkerDocuments(new FirebaseWorkerRecoveryUtility.ValidationCallback() {
            @Override
            public void onSuccess(java.util.List<FirebaseWorkerRecoveryUtility.ValidationReport> reports) {
                setLoading(false);
                updateStatus("Validation completed");
                
                StringBuilder results = new StringBuilder();
                results.append("Validation Results:\n\n");
                
                int workersWithIssues = 0;
                for (FirebaseWorkerRecoveryUtility.ValidationReport report : reports) {
                    if (report.hasIssues()) {
                        workersWithIssues++;
                        results.append(report.toString()).append("\n\n");
                    }
                }
                
                if (workersWithIssues == 0) {
                    results.append("✅ All worker documents are valid!");
                } else {
                    results.append("⚠️ Found ").append(workersWithIssues).append(" workers with issues");
                }
                
                txtResults.setText(results.toString());
                Log.d(TAG, "Validation completed: " + workersWithIssues + " workers with issues");
            }
            
            @Override
            public void onError(Exception exception) {
                setLoading(false);
                updateStatus("Validation failed");
                txtResults.setText("Error: " + exception.getMessage());
                Log.e(TAG, "Validation failed", exception);
                Toast.makeText(WorkerDataRecoveryActivity.this, "Validation failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void recoverAllWorkers() {
        setLoading(true);
        updateStatus("Recovering all worker documents...");
        txtResults.setText("");
        
        recoveryUtility.recoverAllWorkerFields(new FirebaseWorkerRecoveryUtility.RecoveryCallback() {
            @Override
            public void onSuccess(String message) {
                setLoading(false);
                updateStatus("Recovery completed");
                txtResults.setText("✅ " + message);
                Log.d(TAG, "Recovery completed: " + message);
                Toast.makeText(WorkerDataRecoveryActivity.this, "Recovery completed: " + message, Toast.LENGTH_LONG).show();
            }
            
            @Override
            public void onError(Exception exception) {
                setLoading(false);
                updateStatus("Recovery failed");
                txtResults.setText("Error: " + exception.getMessage());
                Log.e(TAG, "Recovery failed", exception);
                Toast.makeText(WorkerDataRecoveryActivity.this, "Recovery failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void recoverSpecificWorker() {
        // For now, show a toast. In a real implementation, you might want to show a dialog
        // to input the worker ID
        Toast.makeText(this, "Feature coming soon: Recover specific worker by ID", Toast.LENGTH_SHORT).show();
    }
    
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnValidateAll.setEnabled(!loading);
        btnRecoverAll.setEnabled(!loading);
        btnRecoverSpecific.setEnabled(!loading);
    }
    
    private void updateStatus(String status) {
        txtStatus.setText("Status: " + status);
        Log.d(TAG, "Status: " + status);
    }
}

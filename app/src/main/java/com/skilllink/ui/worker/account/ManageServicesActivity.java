package com.skilllink.ui.worker.account;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.skilllink.R;
import com.skilllink.data.firebase.FirebaseServiceStore;
import com.skilllink.model.ServiceArea;
import com.skilllink.model.WorkerService;
import com.skilllink.util.SessionManager;
import com.skilllink.util.ServiceCategoryRegistry;
import com.skilllink.ui.user.home.AreaSelectionBottomSheet;
import com.skilllink.ui.user.services.AllServicesActivity;
import com.skilllink.data.RealTimeServiceManager;

import java.util.ArrayList;
import java.util.List;

public class ManageServicesActivity extends AppCompatActivity implements WorkerServicesAdapter.Listener {

    private final List<WorkerService> services = new ArrayList<>();
    private WorkerServicesAdapter adapter;
    private View layoutEmptyState;
    private RecyclerView recyclerServices;
    private SessionManager sessionManager;
    private ActivityResultLauncher<String[]> imagePickerLauncher;
    private ImageSelectionCallback pendingImageCallback;
    private FirebaseServiceStore firebaseServiceStore;
    private boolean remoteSyncEnabled;
    private String workerDocumentId;
    private String workerName;
    private String workerEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_services);

        sessionManager = new SessionManager(this);
        firebaseServiceStore = FirebaseServiceStore.getInstance();
        remoteSyncEnabled = firebaseServiceStore != null && firebaseServiceStore.isEnabled();
        workerDocumentId = sessionManager.getOrCreateWorkerDocumentId();
        workerName = sessionManager.getUserName();
        workerEmail = sessionManager.getUserEmail();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        recyclerServices = findViewById(R.id.recyclerServices);
        recyclerServices.setLayoutManager(new LinearLayoutManager(this));
        recyclerServices.setItemAnimator(null);

        adapter = new WorkerServicesAdapter(services, this);
        recyclerServices.setAdapter(adapter);

        ExtendedFloatingActionButton fabAddService = findViewById(R.id.fabAddService);
        fabAddService.setOnClickListener(v -> showServiceDialog(null));

        MaterialButton buttonEmptyAdd = findViewById(R.id.buttonEmptyAdd);
        buttonEmptyAdd.setOnClickListener(v -> showServiceDialog(null));

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (pendingImageCallback != null) {
                        pendingImageCallback.onImageSelected(uri);
                        pendingImageCallback = null;
                    }
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {
                            // Persisting permission failed; continue with granted session permission.
                        }
                    }
                }
        );

        refreshServices();
        synchronizeRemoteServices();
    }

    private void refreshServices() {
        services.clear();
        
        // Ensure we have a valid worker document ID
        if (TextUtils.isEmpty(workerDocumentId)) {
            workerDocumentId = sessionManager.getOrCreateWorkerDocumentId();
        }
        
        List<WorkerService> storedServices = sessionManager.getWorkerServices();
        if (storedServices != null) {
            boolean needsSave = false;
            List<WorkerService> updatedServices = new ArrayList<>();
            
            for (WorkerService service : storedServices) {
                if (service == null) {
                    continue;
                }
                
                // Include services that belong to the current worker OR services without an owner
                // This ensures newly added services are properly associated with the worker
                String serviceOwnerId = service.getOwnerId();
                if (TextUtils.isEmpty(serviceOwnerId) || TextUtils.equals(workerDocumentId, serviceOwnerId)) {
                    // If the service doesn't have an owner, assign it to the current worker
                    if (TextUtils.isEmpty(serviceOwnerId)) {
                        service = service.withOwner(workerDocumentId, workerName, workerEmail);
                        updatedServices.add(service);
                        needsSave = true;
                        android.util.Log.d("ManageServices", "Fixed owner for service: " + service.getName());
                    }
                    services.add(service);
                }
            }
            
            // Save all updated services at once to avoid multiple writes
            if (needsSave) {
                sessionManager.saveWorkerServices(updatedServices);
                android.util.Log.d("ManageServices", "Saved " + updatedServices.size() + " updated services");
            }
        }
        
        // Update the adapter with the filtered services list
        adapter.notifyDataSetChanged();

        // Update UI visibility based on whether we have services
        if (services.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerServices.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerServices.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEditService(@NonNull WorkerService service) {
        showServiceDialog(service);
    }

    @Override
    public void onDeleteService(@NonNull WorkerService service) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.manage_services_delete_title)
                .setMessage(R.string.manage_services_delete_message)
                .setNegativeButton(R.string.manage_services_action_cancel, null)
                .setPositiveButton(R.string.manage_services_delete_confirm, (dialog, which) -> {
                    sessionManager.deleteWorkerService(service.getId());
                    Toast.makeText(this, R.string.manage_services_deleted_toast, Toast.LENGTH_SHORT).show();
                    refreshServices();
                    deleteServiceRemote(service);
                })
                .show();
    }

    private void showServiceDialog(@Nullable WorkerService existing) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_worker_service, null, false);

        MaterialToolbar dialogToolbar = content.findViewById(R.id.toolbarAddService);
        TextInputLayout inputLayoutCategory = content.findViewById(R.id.inputLayoutCategory);
        MaterialAutoCompleteTextView inputCategory = content.findViewById(R.id.inputCategory);
        TextInputLayout inputLayoutName = content.findViewById(R.id.inputLayoutName);
        TextInputEditText inputName = content.findViewById(R.id.inputName);
        TextInputLayout inputLayoutBio = content.findViewById(R.id.inputLayoutBio);
        TextInputEditText inputBio = content.findViewById(R.id.inputBio);
        MaterialButtonToggleGroup togglePriceType = content.findViewById(R.id.togglePriceType);
        TextInputLayout inputLayoutPrice = content.findViewById(R.id.inputLayoutPrice);
        TextInputEditText inputPrice = content.findViewById(R.id.inputPrice);
        TextInputLayout inputLayoutArea = content.findViewById(R.id.inputLayoutArea);
        TextInputEditText inputServiceArea = content.findViewById(R.id.inputServiceArea);
        Slider sliderCoverageRadius = content.findViewById(R.id.sliderCoverageRadius);
        TextView textCoverageValue = content.findViewById(R.id.textCoverageValue);
        ShapeableImageView imagePreview = content.findViewById(R.id.imagePreview);
        TextView textStatus = content.findViewById(R.id.textImageStatus);
        TextView textHelper = content.findViewById(R.id.textImageHelper);
        TextView textFormTitle = content.findViewById(R.id.textFormTitle);
        TextView textFormSubtitle = content.findViewById(R.id.textFormSubtitle);
        MaterialButton buttonSelectImage = content.findViewById(R.id.buttonSelectImage);
        MaterialButton buttonRemoveImage = content.findViewById(R.id.buttonRemoveImage);
        MaterialButton buttonCancel = content.findViewById(R.id.buttonCancel);
        MaterialButton buttonSave = content.findViewById(R.id.buttonSave);

        int toolbarTitleRes = existing == null
                ? R.string.manage_services_dialog_title_add
                : R.string.manage_services_dialog_title_edit;
        dialogToolbar.setTitle(toolbarTitleRes);
        textFormTitle.setText(toolbarTitleRes);
        textFormSubtitle.setText(existing == null
                ? R.string.manage_services_dialog_subtitle_add
                : R.string.manage_services_dialog_subtitle_edit);

        inputCategory.setSimpleItems(ServiceCategoryRegistry.getDisplayNames());

        final String[] imageUriHolder = {existing != null ? existing.getImageUri() : null};
        final String[] selectedAreaName = {existing != null ? existing.getServiceArea() : null};
        final double[] selectedLatitude = {existing != null ? existing.getLatitude() : Double.NaN};
        final double[] selectedLongitude = {existing != null ? existing.getLongitude() : Double.NaN};

        float initialRadius = existing != null ? (float) existing.getCoverageRadiusKm() : 20f;
        if (initialRadius < sliderCoverageRadius.getValueFrom()) {
            initialRadius = sliderCoverageRadius.getValueFrom();
        } else if (initialRadius > sliderCoverageRadius.getValueTo()) {
            initialRadius = sliderCoverageRadius.getValueTo();
        }
        sliderCoverageRadius.setValue(initialRadius);
        textCoverageValue.setText(getString(R.string.manage_services_field_radius_value_format, Math.round(initialRadius)));
        sliderCoverageRadius.addOnChangeListener((slider, value, fromUser) ->
                textCoverageValue.setText(getString(R.string.manage_services_field_radius_value_format, Math.round(value))));

        if (existing != null) {
            if (!TextUtils.isEmpty(existing.getCategory())) {
                ServiceCategoryRegistry.Category categoryMeta = ServiceCategoryRegistry.resolve(existing.getCategory());
                if (categoryMeta != null) {
                    inputCategory.setText(categoryMeta.getDisplayName(), false);
                } else {
                    inputCategory.setText(existing.getCategory(), false);
                }
            }
            if (!TextUtils.isEmpty(existing.getName())) {
                inputName.setText(existing.getName());
            }
            if (!TextUtils.isEmpty(existing.getBio())) {
                inputBio.setText(existing.getBio());
            }
            if (existing.isHourlyPricing()) {
                togglePriceType.check(R.id.buttonPriceHourly);
            } else {
                togglePriceType.check(R.id.buttonPriceCustom);
            }
            if (!TextUtils.isEmpty(existing.getPriceValue())) {
                inputPrice.setText(existing.getPriceValue());
            }
        } else {
            togglePriceType.check(R.id.buttonPriceHourly);
        }

        if (!TextUtils.isEmpty(selectedAreaName[0])) {
            inputServiceArea.setText(selectedAreaName[0]);
            if (Double.isNaN(selectedLatitude[0]) || Double.isNaN(selectedLongitude[0])) {
                ServiceArea linkedArea = sessionManager.findServiceAreaByName(selectedAreaName[0]);
                if (linkedArea != null) {
                    selectedLatitude[0] = linkedArea.getLatitude();
                    selectedLongitude[0] = linkedArea.getLongitude();
                }
            }
        }

        updatePriceInputState(togglePriceType.getCheckedButtonId(), inputLayoutPrice, inputPrice);
        togglePriceType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updatePriceInputState(checkedId, inputLayoutPrice, inputPrice);
            }
        });

        inputServiceArea.setInputType(InputType.TYPE_NULL);
        inputServiceArea.setKeyListener(null);
        inputServiceArea.setFocusable(false);
        inputServiceArea.setFocusableInTouchMode(false);

        View.OnClickListener areaClickListener = v -> {
            AreaSelectionBottomSheet sheet = AreaSelectionBottomSheet.newInstance();
            sheet.show(getSupportFragmentManager(), "area_selection_manage_service");
        };
        inputServiceArea.setOnClickListener(areaClickListener);
        inputServiceArea.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.post(() -> {
                    v.clearFocus();
                    areaClickListener.onClick(v);
                });
            }
        });
        inputLayoutArea.setEndIconVisible(false);
        inputLayoutArea.setOnClickListener(areaClickListener);
        inputLayoutArea.setStartIconOnClickListener(areaClickListener);

        applyImagePreview(imageUriHolder[0], imagePreview, textStatus, textHelper, buttonRemoveImage);

        buttonSelectImage.setOnClickListener(v -> {
            pendingImageCallback = uri -> {
                if (uri != null) {
                    imageUriHolder[0] = uri.toString();
                    applyImagePreview(imageUriHolder[0], imagePreview, textStatus, textHelper, buttonRemoveImage);
                }
            };
            imagePickerLauncher.launch(new String[]{"image/*"});
        });

        buttonRemoveImage.setOnClickListener(v -> {
            imageUriHolder[0] = null;
            applyImagePreview(null, imagePreview, textStatus, textHelper, buttonRemoveImage);
        });
        buttonSave.setText(existing == null
                ? R.string.manage_services_action_save
                : R.string.manage_services_action_update);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setView(content);

        AlertDialog dialog = builder.create();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.setFragmentResultListener(AreaSelectionBottomSheet.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (!AreaSelectionBottomSheet.REQUEST_KEY.equals(requestKey) || bundle == null || !dialog.isShowing()) {
                return;
            }
            String areaName = bundle.getString(AreaSelectionBottomSheet.RESULT_AREA_NAME);
            if (!TextUtils.isEmpty(areaName)) {
                selectedAreaName[0] = areaName;
                inputServiceArea.setText(areaName);
                inputLayoutArea.setError(null);

                double latitude = bundle.containsKey(AreaSelectionBottomSheet.RESULT_AREA_LATITUDE)
                        ? bundle.getDouble(AreaSelectionBottomSheet.RESULT_AREA_LATITUDE, Double.NaN)
                        : Double.NaN;
                double longitude = bundle.containsKey(AreaSelectionBottomSheet.RESULT_AREA_LONGITUDE)
                        ? bundle.getDouble(AreaSelectionBottomSheet.RESULT_AREA_LONGITUDE, Double.NaN)
                        : Double.NaN;

                if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
                    ServiceArea fallback = sessionManager.findServiceAreaByName(areaName);
                    if (fallback != null) {
                        latitude = fallback.getLatitude();
                        longitude = fallback.getLongitude();
                    }
                }

                selectedLatitude[0] = latitude;
                selectedLongitude[0] = longitude;
            }
        });
        dialogToolbar.setNavigationOnClickListener(v -> dialog.dismiss());
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            clearErrors(inputLayoutCategory, inputLayoutName, inputLayoutBio, inputLayoutPrice, inputLayoutArea);

            String categoryRaw = inputCategory.getText() != null
                    ? inputCategory.getText().toString().trim()
                    : "";
            ServiceCategoryRegistry.Category categoryMeta = ServiceCategoryRegistry.resolve(categoryRaw);
            String name = readText(inputName);
            String bio = readText(inputBio);
            String priceValue = readText(inputPrice);
            String priceType = togglePriceType.getCheckedButtonId() == R.id.buttonPriceHourly
                    ? WorkerService.PRICE_TYPE_HOURLY
                    : WorkerService.PRICE_TYPE_CUSTOM;

            boolean valid = true;

            if (categoryMeta == null) {
                inputLayoutCategory.setError(getString(R.string.manage_services_error_category));
                valid = false;
            }

            if (TextUtils.isEmpty(name)) {
                inputLayoutName.setError(getString(R.string.manage_services_error_name));
                valid = false;
            }

            if (TextUtils.isEmpty(bio)) {
                inputLayoutBio.setError(getString(R.string.manage_services_error_bio));
                valid = false;
            }

            if (TextUtils.isEmpty(priceValue)) {
                inputLayoutPrice.setError(getString(R.string.manage_services_error_price));
                valid = false;
            }

            if (TextUtils.isEmpty(selectedAreaName[0])) {
                inputLayoutArea.setError(getString(R.string.manage_services_field_area_error));
                valid = false;
            }

            if (!valid) {
                return;
            }

            String categoryKey = categoryMeta != null ? categoryMeta.getKey() : null;

            WorkerService updated;
            if (existing == null) {
                updated = WorkerService.create(workerDocumentId, workerName, workerEmail, categoryKey, name, bio, priceType, priceValue, imageUriHolder[0]);
            } else {
                updated = existing.withUpdatedDetails(categoryKey, name, bio, priceType, priceValue, imageUriHolder[0]);
            }

            updated = ensureOwnerMetadata(updated);
            double latitude = selectedLatitude[0];
            double longitude = selectedLongitude[0];
            if ((Double.isNaN(latitude) || Double.isNaN(longitude)) && !TextUtils.isEmpty(selectedAreaName[0])) {
                ServiceArea fallback = sessionManager.findServiceAreaByName(selectedAreaName[0]);
                if (fallback != null) {
                    latitude = fallback.getLatitude();
                    longitude = fallback.getLongitude();
                }
            }
            updated = updated.withLocation(selectedAreaName[0], latitude, longitude, sliderCoverageRadius.getValue());

            sessionManager.upsertWorkerService(updated);

            Toast.makeText(this,
                    existing == null
                            ? R.string.manage_services_saved_toast
                            : R.string.manage_services_updated_toast,
                    Toast.LENGTH_SHORT).show();

            refreshServices();
            pushServiceToRemote(updated);
            
            // Notify all activities that services have been updated with the new/updated service
            notifyServicesUpdated(updated);
            
            dialog.dismiss();
            // Don't navigate to AllServices - stay in ManageServices to show the added service
        });

        dialog.setOnShowListener(di -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        dialog.setOnDismissListener(d -> fragmentManager.clearFragmentResultListener(AreaSelectionBottomSheet.REQUEST_KEY));

        dialog.show();
    }

    private void synchronizeRemoteServices() {
        if (!remoteSyncEnabled) {
            // If Firebase is not available, ensure local services are properly saved
            runOnUiThread(this::refreshServices);
            return;
        }
        
        // First, push all local services to Firebase to ensure they're stored globally
        List<WorkerService> localServices = sessionManager.getWorkerServices();
        if (localServices != null && !localServices.isEmpty()) {
            for (WorkerService service : localServices) {
                if (service != null && TextUtils.equals(service.getOwnerId(), workerDocumentId)) {
                    // Push this service to Firebase global collection
                    pushServiceToRemoteSync(service);
                }
            }
        }
        
        // Then refresh from Firebase to get all global services
        firebaseServiceStore.refreshAllServices(new FirebaseServiceStore.ServiceListCallback() {
            @Override
            public void onSuccess(List<WorkerService> remoteServices) {
                // Merge remote services with local services to avoid losing local changes
                List<WorkerService> mergedServices = mergeServices(localServices, remoteServices);
                sessionManager.saveWorkerServices(mergedServices);
                runOnUiThread(ManageServicesActivity.this::refreshServices);
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(ManageServicesActivity.this::refreshServices);
            }
        });
    }

    private List<WorkerService> mergeServices(List<WorkerService> localServices, List<WorkerService> remoteServices) {
        List<WorkerService> merged = new ArrayList<>();
        
        // Add all local services first
        if (localServices != null) {
            merged.addAll(localServices);
        }
        
        // Add remote services that don't exist locally (by ID)
        if (remoteServices != null) {
            for (WorkerService remote : remoteServices) {
                boolean exists = false;
                for (WorkerService local : merged) {
                    if (local.getId().equals(remote.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    merged.add(remote);
                }
            }
        }
        
        return merged;
    }

    private void navigateToAllServices(@NonNull String serviceId) {
        Intent intent = new Intent(this, AllServicesActivity.class);
        intent.putExtra(AllServicesActivity.EXTRA_HIGHLIGHT_SERVICE_ID, serviceId);
        startActivity(intent);
    }

    private void pushServiceToRemote(@NonNull WorkerService service) {
        if (!remoteSyncEnabled || TextUtils.isEmpty(workerDocumentId)) {
            return;
        }
        firebaseServiceStore.upsertService(workerDocumentId, workerName, workerEmail, service, new FirebaseServiceStore.CompletionListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(ManageServicesActivity.this::synchronizeRemoteServices);
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> Toast.makeText(ManageServicesActivity.this, R.string.manage_services_sync_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void pushServiceToRemoteSync(@NonNull WorkerService service) {
        if (!remoteSyncEnabled || TextUtils.isEmpty(workerDocumentId)) {
            return;
        }
        firebaseServiceStore.upsertService(workerDocumentId, workerName, workerEmail, service, new FirebaseServiceStore.CompletionListener() {
            @Override
            public void onSuccess() {
                // Service successfully pushed to Firebase global collection
            }

            @Override
            public void onError(Exception exception) {
                // Log error but don't show toast to avoid spamming during sync
            }
        });
    }

    private void deleteServiceRemote(@NonNull WorkerService service) {
        if (!remoteSyncEnabled || TextUtils.isEmpty(workerDocumentId)) {
            return;
        }
        firebaseServiceStore.deleteService(workerDocumentId, service.getId(), new FirebaseServiceStore.CompletionListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(ManageServicesActivity.this::synchronizeRemoteServices);
            }

            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> Toast.makeText(ManageServicesActivity.this, R.string.manage_services_sync_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private WorkerService ensureOwnerMetadata(@NonNull WorkerService service) {
        if (TextUtils.isEmpty(service.getOwnerId())) {
            return service.withOwner(workerDocumentId, workerName, workerEmail);
        }
        return service;
    }

    private void applyImagePreview(@Nullable String imageUri,
                                   @NonNull ShapeableImageView preview,
                                   @NonNull TextView statusView,
                                   @NonNull TextView helperView,
                                   @NonNull MaterialButton removeButton) {
        helperView.setText(R.string.manage_services_image_helper);
        if (!TextUtils.isEmpty(imageUri)) {
            try {
                preview.setImageURI(null);
                preview.setImageURI(Uri.parse(imageUri));
                ImageViewCompat.setImageTintList(preview, null);
                preview.clearColorFilter();
            } catch (SecurityException exception) {
                preview.setImageResource(R.drawable.ic_service_manage);
                ImageViewCompat.setImageTintList(preview,
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_color)));
            }
            statusView.setText(R.string.manage_services_image_change);
            removeButton.setVisibility(View.VISIBLE);
        } else {
            preview.setImageResource(R.drawable.ic_service_manage);
            ImageViewCompat.setImageTintList(preview,
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_color)));
            statusView.setText(R.string.manage_services_image_add);
            removeButton.setVisibility(View.GONE);
        }
    }

    private void updatePriceInputState(int checkedId,
                                       @NonNull TextInputLayout layout,
                                       @NonNull TextInputEditText input) {
        if (checkedId == R.id.buttonPriceHourly) {
            layout.setHint(getString(R.string.manage_services_field_price_hint_hourly));
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        } else {
            layout.setHint(getString(R.string.manage_services_field_price_hint_custom));
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        input.setSelection(input.getText() != null ? input.getText().length() : 0);
    }

    private void clearErrors(TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) {
            if (layout != null) {
                layout.setError(null);
            }
        }
    }

    @NonNull
    private String readText(@Nullable TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void notifyServicesUpdated(@NonNull WorkerService updatedService) {
        // Use RealTimeServiceManager to notify about the service addition/update
        RealTimeServiceManager realTimeManager = RealTimeServiceManager.getInstance(this);
        
        // Notify about the specific service that was added/updated
        realTimeManager.notifyServiceAdded(updatedService);
        
        // Also send a broadcast to ensure all activities refresh
        Intent intent = new Intent("com.skilllink.SERVICES_UPDATED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private interface ImageSelectionCallback {
        void onImageSelected(@Nullable Uri uri);
    }
}

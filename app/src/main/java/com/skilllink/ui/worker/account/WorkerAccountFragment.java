package com.skilllink.ui.worker.account;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.skilllink.R;
import com.skilllink.RoleSelectionActivity;
import com.skilllink.databinding.FragmentWorkerAccountBinding;
import com.skilllink.ui.legal.LegalDocumentActivity;
import com.skilllink.ui.user.account.AccountSupportActivity;
import com.skilllink.ui.user.account.ManageProfileActivity;
import com.skilllink.util.NameFormatter;
import com.skilllink.util.SessionManager;
import com.skilllink.model.WorkerService;

import java.util.List;

public class WorkerAccountFragment extends Fragment {

    private FragmentWorkerAccountBinding binding;
    private SessionManager sessionManager;
    private ActivityResultLauncher<String[]> avatarPickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        avatarPickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                String previousAvatar = sessionManager.getUserAvatarUri();
                if (!TextUtils.isEmpty(previousAvatar)) {
                    try {
                        requireContext().getContentResolver().releasePersistableUriPermission(
                                Uri.parse(previousAvatar),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException ignored) {
                        // Ignore failures when releasing previous permissions.
                    }
                }

                try {
                    requireContext().getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException ignored) {
                    // Some providers may not support persistable permissions; ignore if it fails.
                }

                sessionManager.setUserAvatarUri(uri.toString());

                if (binding != null) {
                    binding.imageWorkerAvatar.setImageURI(null);
                    binding.imageWorkerAvatar.setImageURI(uri);
                }
            }
        });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkerAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initViews();

        return root;
    }

    private void initViews() {
        populateProfile();
        loadUserAvatar();

        View.OnClickListener changePhotoClickListener = v -> avatarPickerLauncher.launch(new String[]{"image/*"});
        binding.buttonChangePhoto.setOnClickListener(changePhotoClickListener);
        binding.imageWorkerAvatar.setOnClickListener(changePhotoClickListener);

        View.OnClickListener manageProfileClick = v -> {
            Intent intent = new Intent(requireContext(), ManageProfileActivity.class);
            startActivity(intent);
        };

        binding.btnEditProfile.setOnClickListener(manageProfileClick);
        binding.rowManageProfile.setOnClickListener(manageProfileClick);

        binding.rowManageServices.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ManageServicesActivity.class);
            startActivity(intent);
        });

        binding.rowJobs.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_worker_jobs));

        binding.rowMessages.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_worker_chat));

        binding.rowEarnings.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_worker_earnings));

        binding.rowSupport.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AccountSupportActivity.class);
            startActivity(intent);
        });

        binding.rowTerms.setOnClickListener(v -> openLegalDocument(LegalDocumentActivity.DOCUMENT_TERMS));

        binding.rowPrivacy.setOnClickListener(v -> openLegalDocument(LegalDocumentActivity.DOCUMENT_PRIVACY));

        binding.btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();

            Intent intent = new Intent(requireContext(), RoleSelectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            populateProfile();
            loadUserAvatar();
            updateServicesSummary();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private String getAppVersion() {
        try {
            PackageManager packageManager = requireContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(requireContext().getPackageName(), 0);
            return packageInfo.versionName != null ? packageInfo.versionName : "";
        } catch (PackageManager.NameNotFoundException exception) {
            return "";
        }
    }

    private void openLegalDocument(String documentType) {
        Intent intent = LegalDocumentActivity.createIntent(requireContext(), documentType);
        startActivity(intent);
    }

    private void populateProfile() {
        NameFormatter.Parts parts = NameFormatter.resolve(sessionManager.getUserName(), sessionManager.getUserEmail());

        if (parts != null) {
            binding.textWorkerFirstName.setText(parts.getFirstName());
            if (parts.hasLastName()) {
                binding.textWorkerLastName.setText(parts.getLastName());
                binding.textWorkerLastName.setVisibility(View.VISIBLE);
            } else {
                binding.textWorkerLastName.setText("");
                binding.textWorkerLastName.setVisibility(View.GONE);
            }
        } else {
            binding.textWorkerFirstName.setText(getString(R.string.worker_account_default_first_name));
            binding.textWorkerLastName.setText(getString(R.string.worker_account_default_last_name));
            binding.textWorkerLastName.setVisibility(View.VISIBLE);
        }

        binding.textWorkerRating.setText(getString(R.string.worker_account_rating_default));

        String versionName = getAppVersion();
        binding.textFooter.setText(getString(R.string.account_footer_format, versionName));

        updateServicesSummary();
    }

    private void loadUserAvatar() {
        if (binding == null) {
            return;
        }

        String avatarUri = sessionManager.getUserAvatarUri();
        if (!TextUtils.isEmpty(avatarUri)) {
            Uri uri = Uri.parse(avatarUri);
            binding.imageWorkerAvatar.setImageURI(null);
            binding.imageWorkerAvatar.setImageURI(uri);
        } else {
            binding.imageWorkerAvatar.setImageResource(R.drawable.ic_profile);
        }
    }

    private void updateServicesSummary() {
        if (binding == null) {
            return;
        }

        List<WorkerService> services = sessionManager.getWorkerServices();
        String workerId = sessionManager.getOrCreateWorkerDocumentId();
        int count = 0;
        if (services != null) {
            for (WorkerService service : services) {
                if (service == null) {
                    continue;
                }
                if (TextUtils.isEmpty(workerId) || TextUtils.equals(workerId, service.getOwnerId())) {
                    count++;
                }
            }
        }

        if (count == 0) {
            binding.textManageServicesSubtitle.setText(R.string.worker_account_manage_services_subtitle_empty);
        } else {
            binding.textManageServicesSubtitle.setText(
                    getResources().getQuantityString(
                            R.plurals.worker_account_manage_services_count,
                            count,
                            count
                    )
            );
        }
    }
}
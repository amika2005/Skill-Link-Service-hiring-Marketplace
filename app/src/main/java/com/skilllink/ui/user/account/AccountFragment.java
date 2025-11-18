package com.skilllink.ui.user.account;

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
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.skilllink.R;
import com.skilllink.RoleSelectionActivity;
import com.skilllink.databinding.FragmentUserAccountBinding;
import com.skilllink.ui.legal.LegalDocumentActivity;
import com.skilllink.util.NameFormatter;
import com.skilllink.util.SessionManager;

public class AccountFragment extends Fragment {

    private FragmentUserAccountBinding binding;
    private SessionManager sessionManager;
    private ActivityResultLauncher<String[]> avatarPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
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
                        // Ignore if the previous permission can no longer be released.
                    }
                }

                try {
                    requireContext().getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException ignored) {
                    // Some providers might not support persistable permissions; ignore if it fails.
                }

                sessionManager.setUserAvatarUri(uri.toString());

                if (binding != null) {
                    binding.imageUserAvatar.setImageURI(null);
                    binding.imageUserAvatar.setImageURI(uri);
                }
            }
        });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserAccountBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initViews();

        return root;
    }

    private void initViews() {
        loadUserAvatar();

        NameFormatter.Parts parts = NameFormatter.resolve(sessionManager.getUserName(), sessionManager.getUserEmail());

        if (parts != null) {
            binding.textUserFirstName.setText(parts.getFirstName());
            if (parts.hasLastName()) {
                binding.textUserLastName.setText(parts.getLastName());
                binding.textUserLastName.setVisibility(View.VISIBLE);
            } else {
                binding.textUserLastName.setText("");
                binding.textUserLastName.setVisibility(View.GONE);
            }
        } else {
            binding.textUserFirstName.setText(getString(R.string.account_default_first_name));
            binding.textUserLastName.setText(getString(R.string.account_default_last_name));
            binding.textUserLastName.setVisibility(View.VISIBLE);
        }

        binding.textUserRating.setText(getString(R.string.account_rating_default));

        String versionName = getAppVersion();
        binding.textFooter.setText(getString(R.string.account_footer_format, versionName));

        applyPaymentStatus();

        View.OnClickListener changePhotoClickListener = v -> avatarPickerLauncher.launch(new String[]{"image/*"});
        binding.buttonChangePhoto.setOnClickListener(changePhotoClickListener);
        binding.imageUserAvatar.setOnClickListener(changePhotoClickListener);

        View.OnClickListener manageProfileClick = v -> {
            Intent intent = new Intent(requireContext(), ManageProfileActivity.class);
            startActivity(intent);
        };

        binding.btnEditProfile.setOnClickListener(manageProfileClick);
        binding.rowManageProfile.setOnClickListener(manageProfileClick);

        View.OnClickListener managePaymentsClick = v -> {
            Intent intent = new Intent(requireContext(), ManagePaymentsActivity.class);
            startActivity(intent);
        };

        binding.rowPaymentCash.setOnClickListener(managePaymentsClick);
        binding.rowPaymentCards.setOnClickListener(managePaymentsClick);

        binding.rowBookings.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_bookings));

        binding.rowMessages.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.nav_chat));

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
            applyPaymentStatus();
            loadUserAvatar();
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

    private void loadUserAvatar() {
        if (binding == null) {
            return;
        }

        String avatarUri = sessionManager.getUserAvatarUri();

        if (!TextUtils.isEmpty(avatarUri)) {
            Uri uri = Uri.parse(avatarUri);
            binding.imageUserAvatar.setImageURI(null);
            binding.imageUserAvatar.setImageURI(uri);
        } else {
            binding.imageUserAvatar.setImageResource(R.drawable.ic_user);
        }
    }

    private void applyPaymentStatus() {
        if (binding == null) {
            return;
        }

        boolean cashEnabled = sessionManager.isPaymentCashEnabled();
        binding.textPaymentCashStatus.setText(cashEnabled
                ? R.string.account_payment_cash_status_enabled
                : R.string.account_payment_cash_status_disabled);

        String cardLast4 = sessionManager.getPaymentCardLast4();
        if (!TextUtils.isEmpty(cardLast4)) {
            binding.textPaymentCardStatus.setText(getString(R.string.account_payment_cards_status_present, cardLast4));
        } else {
            binding.textPaymentCardStatus.setText(R.string.account_payment_cards_status_missing);
        }
    }
}
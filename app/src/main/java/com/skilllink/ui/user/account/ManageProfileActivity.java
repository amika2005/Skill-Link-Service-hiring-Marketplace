package com.skilllink.ui.user.account;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.skilllink.R;
import com.skilllink.databinding.ActivityManageProfileBinding;
import com.skilllink.util.NameFormatter;
import com.skilllink.util.SessionManager;

import java.util.Locale;

public class ManageProfileActivity extends AppCompatActivity {

    private ActivityManageProfileBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.toolbar.setTitle(R.string.manage_profile_title);

        populateFields();

        binding.buttonSave.setOnClickListener(v -> saveProfile());
    }

    private void populateFields() {
        String email = getValueOrEmpty(sessionManager.getUserEmail());
        NameFormatter.Parts nameParts = NameFormatter.resolve(sessionManager.getUserName(), email);

        String firstName = "";
        String lastName = "";

        if (nameParts != null) {
            firstName = nameParts.getFirstName();
            if (nameParts.hasLastName()) {
                lastName = nameParts.getLastName();
            }
        }

        String phone = getValueOrEmpty(sessionManager.getUserPhone());
        String location = getValueOrEmpty(sessionManager.getUserLocation());
        String bio = getValueOrEmpty(sessionManager.getUserBio());
        String avatarUri = getValueOrEmpty(sessionManager.getUserAvatarUri());

        binding.inputFirstName.setText(firstName);
        binding.inputLastName.setText(lastName);
        binding.inputEmail.setText(email);
        binding.inputPhone.setText(phone);
        binding.inputLocation.setText(location);
        binding.inputBio.setText(bio);

        if (!TextUtils.isEmpty(avatarUri)) {
            binding.imageProfileAvatar.setImageURI(null);
            binding.imageProfileAvatar.setImageURI(Uri.parse(avatarUri));
        } else {
            binding.imageProfileAvatar.setImageResource(R.drawable.ic_user);
        }

        String displayName = buildDisplayName(firstName, lastName);
        binding.textProfileName.setText(displayName);
        binding.textProfileEmail.setText(!TextUtils.isEmpty(email) ? email : getString(R.string.account_default_email));

        int completion = calculateCompletion(firstName, lastName, phone, location, bio);
        binding.progressProfileCompletion.setProgress(completion);
        String completionMessage = String.format(Locale.getDefault(), "%d%% complete • %s", completion, getString(R.string.manage_profile_completion_hint));
        binding.textProfileCompletionHint.setText(completionMessage);
    }

    private void saveProfile() {
        String firstName = readText(binding.inputFirstName);
        String lastName = readText(binding.inputLastName);
        String email = readText(binding.inputEmail);
        String phone = readText(binding.inputPhone);
        String location = readText(binding.inputLocation);
        String bio = readText(binding.inputBio);

        String combinedName = buildCombinedName(firstName, lastName);

        sessionManager.updateUserProfile(combinedName, email, phone, location, bio);

        binding.textProfileName.setText(buildDisplayName(firstName, lastName));
        binding.textProfileEmail.setText(!TextUtils.isEmpty(email) ? email : getString(R.string.account_default_email));

        int completion = calculateCompletion(firstName, lastName, phone, location, bio);
        binding.progressProfileCompletion.setProgress(completion);
        String completionMessage = String.format(Locale.getDefault(), "%d%% complete • %s", completion, getString(R.string.manage_profile_completion_hint));
        binding.textProfileCompletionHint.setText(completionMessage);

        Toast.makeText(this, R.string.manage_profile_saved_toast, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String buildDisplayName(String firstName, String lastName) {
        if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName)) {
            return String.format(Locale.getDefault(), "%s %s", firstName, lastName);
        }

        if (!TextUtils.isEmpty(firstName)) {
            return firstName;
        }

        if (!TextUtils.isEmpty(lastName)) {
            return lastName;
        }

        return String.format(Locale.getDefault(), "%s %s",
                getString(R.string.account_default_first_name),
                getString(R.string.account_default_last_name));
    }

    private String buildCombinedName(String firstName, String lastName) {
        if (TextUtils.isEmpty(firstName) && TextUtils.isEmpty(lastName)) {
            return "";
        }

        if (TextUtils.isEmpty(lastName)) {
            return firstName;
        }

        if (TextUtils.isEmpty(firstName)) {
            return lastName;
        }

        return String.format(Locale.getDefault(), "%s %s", firstName, lastName);
    }

    private int calculateCompletion(String firstName, String lastName, String phone, String location, String bio) {
        int completed = 0;

        if (!TextUtils.isEmpty(firstName)) {
            completed++;
        }

        if (!TextUtils.isEmpty(lastName)) {
            completed++;
        }

        if (!TextUtils.isEmpty(phone)) {
            completed++;
        }

        if (!TextUtils.isEmpty(location)) {
            completed++;
        }

        if (!TextUtils.isEmpty(bio)) {
            completed++;
        }

        return (int) ((completed / 5f) * 100);
    }

    private String readText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getValueOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

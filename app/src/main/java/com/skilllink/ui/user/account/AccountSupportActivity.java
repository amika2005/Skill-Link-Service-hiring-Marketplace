package com.skilllink.ui.user.account;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.skilllink.R;
import com.skilllink.databinding.ActivityAccountSupportBinding;

public class AccountSupportActivity extends AppCompatActivity {

    private ActivityAccountSupportBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountSupportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.toolbar.setTitle(R.string.support_center_title);
        binding.textPageHeading.setText(R.string.support_center_title);

        binding.callCard.setOnClickListener(v -> {
            String phone = getString(R.string.support_phone_raw);
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            startActivity(intent);
        });

        binding.emailCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + getString(R.string.support_email_address)));
            startActivity(Intent.createChooser(intent, getString(R.string.support_center_email)));
        });

        binding.faqCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.support_faq_url)));
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

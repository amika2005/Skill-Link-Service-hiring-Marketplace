package com.skilllink.ui.legal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.skilllink.R;
import com.skilllink.databinding.ActivityLegalDocumentBinding;
import com.skilllink.databinding.ItemLegalSectionBinding;

public class LegalDocumentActivity extends AppCompatActivity {

    public static final String EXTRA_DOCUMENT_TYPE = "document_type";
    public static final String DOCUMENT_TERMS = "terms";
    public static final String DOCUMENT_PRIVACY = "privacy";

    private ActivityLegalDocumentBinding binding;

    public static Intent createIntent(Context context, String documentType) {
        Intent intent = new Intent(context, LegalDocumentActivity.class);
        intent.putExtra(EXTRA_DOCUMENT_TYPE, documentType);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLegalDocumentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String documentType = getIntent().getStringExtra(EXTRA_DOCUMENT_TYPE);
        if (DOCUMENT_PRIVACY.equals(documentType)) {
            showPrivacyPolicy();
        } else {
            showTermsAndConditions();
        }
    }

    private void showTermsAndConditions() {
        binding.toolbar.setTitle(R.string.legal_terms_title);
        binding.textPageHeading.setText(R.string.legal_terms_title);
        binding.textHeroTitle.setText(R.string.legal_terms_hero_title);
        binding.textHeroSubtitle.setText(R.string.legal_terms_hero_subtitle);
        binding.imageHero.setContentDescription(getString(R.string.legal_terms_title));
        binding.textUpdated.setText(R.string.legal_terms_updated);
        binding.textIntro.setText(R.string.legal_terms_intro);
        renderDocumentContent(getString(R.string.legal_terms_content));
    }

    private void showPrivacyPolicy() {
        binding.toolbar.setTitle(R.string.legal_privacy_title);
        binding.textPageHeading.setText(R.string.legal_privacy_title);
        binding.textHeroTitle.setText(R.string.legal_privacy_hero_title);
        binding.textHeroSubtitle.setText(R.string.legal_privacy_hero_subtitle);
        binding.imageHero.setContentDescription(getString(R.string.legal_privacy_title));
        binding.textUpdated.setText(R.string.legal_privacy_updated);
        binding.textIntro.setText(R.string.legal_privacy_intro);
        renderDocumentContent(getString(R.string.legal_privacy_content));
    }

    @SuppressLint("WrongConstant")
    private void renderDocumentContent(String content) {
        binding.contentContainer.removeAllViews();
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        String[] sections = content.split("\\n\\n");

        for (String rawSection : sections) {
            if (rawSection == null) {
                continue;
            }

            String section = rawSection.trim();
            if (section.isEmpty()) {
                continue;
            }

            String[] parts = section.split("\\n", 2);
            String title = parts[0].trim();
            String body = parts.length > 1 ? parts[1].trim() : "";

            ItemLegalSectionBinding sectionBinding = ItemLegalSectionBinding.inflate(inflater, binding.contentContainer, false);
            sectionBinding.textSectionTitle.setText(title);
            sectionBinding.textSectionBody.setText(body);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sectionBinding.textSectionBody.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD);
            }

            binding.contentContainer.addView(sectionBinding.getRoot());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

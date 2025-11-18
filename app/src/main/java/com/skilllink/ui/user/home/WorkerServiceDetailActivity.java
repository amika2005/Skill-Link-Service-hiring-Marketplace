package com.skilllink.ui.user.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.skilllink.R;
import com.skilllink.model.RecommendedWorker;
import com.skilllink.model.WorkerService;
import com.skilllink.util.ServiceCategoryRegistry;
import com.skilllink.ui.user.bookings.ServiceBookingActivity;
import com.skilllink.ui.user.chat.WorkerChatActivity;
import com.skilllink.util.ImageLoader;
import com.skilllink.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkerServiceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SERVICE_ID = "extra_service_id";

    private SessionManager sessionManager;
    private WorkerService service;

    private ImageView imageHero;
    private TextView textServiceName;
    private TextView textHeaderCategory;
    private TextView textDetailSubtitle;
    private TextView textPrice;
    private TextView textPriceSubtitle;
    private TextView textBio;
    private TextView textStatJobs;
    private TextView textStatExperience;
    private TextView textStatRating;
    private ChipGroup chipGroupHighlights;
    private MaterialButton buttonChat;
    private MaterialButton buttonBook;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_service_detail);

        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();

        String serviceId = getIntent().getStringExtra(EXTRA_SERVICE_ID);
        service = sessionManager.findWorkerServiceById(serviceId);
        if (service == null) {
            Toast.makeText(this, R.string.worker_service_detail_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        populateContent();

        buttonBook.setOnClickListener(v -> {
            Intent intent = new Intent(this, ServiceBookingActivity.class);
            intent.putExtra(ServiceBookingActivity.EXTRA_SERVICE_ID, service.getId());
            startActivity(intent);
        });

        buttonChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, WorkerChatActivity.class);
            intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_ID, service.getId());
            intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_NAME, service.getName());
            intent.putExtra(WorkerChatActivity.EXTRA_SERVICE_CATEGORY, service.getCategory());

            RecommendedWorker recommended = sessionManager.findRecommendedWorkerByServiceId(service.getId());
            if (recommended != null) {
                intent.putExtra(WorkerChatActivity.EXTRA_WORKER_ID, recommended.getId());
                intent.putExtra(WorkerChatActivity.EXTRA_WORKER_NAME, recommended.getName());
                intent.putExtra(WorkerChatActivity.EXTRA_WORKER_OCCUPATION, recommended.getOccupation());
                if (!TextUtils.isEmpty(recommended.getImageUri())) {
                    intent.putExtra(WorkerChatActivity.EXTRA_WORKER_IMAGE_URI, recommended.getImageUri());
                }
            }
            startActivity(intent);
        });
    }

    private void bindViews() {
        imageHero = findViewById(R.id.imageHero);
        textServiceName = findViewById(R.id.textServiceName);
        textHeaderCategory = findViewById(R.id.textHeaderCategory);
        textDetailSubtitle = findViewById(R.id.textDetailSubtitle);
        textPrice = findViewById(R.id.textPrice);
        textPriceSubtitle = findViewById(R.id.textPriceSubtitle);
        textBio = findViewById(R.id.textBio);
        textStatJobs = findViewById(R.id.textStatJobs);
        textStatExperience = findViewById(R.id.textStatExperience);
        textStatRating = findViewById(R.id.textStatRating);
        chipGroupHighlights = findViewById(R.id.chipGroupHighlights);
        buttonChat = findViewById(R.id.buttonChat);
        buttonBook = findViewById(R.id.buttonBook);
    }

    private void populateContent() {
        String serviceName = !TextUtils.isEmpty(service.getName())
                ? service.getName()
                : getString(R.string.worker_service_detail_title);

        if (textServiceName != null) {
            textServiceName.setText(serviceName);
        }

        if (textHeaderCategory != null) {
            String displayCategory = ServiceCategoryRegistry.getDisplayNameOrDefault(service.getCategory());
            if (TextUtils.isEmpty(displayCategory)) {
                textHeaderCategory.setVisibility(View.GONE);
                if (textDetailSubtitle != null) {
                    textDetailSubtitle.setVisibility(View.VISIBLE);
                    textDetailSubtitle.setText(R.string.worker_service_detail_subheading);
                }
            } else {
                textHeaderCategory.setVisibility(View.VISIBLE);
                textHeaderCategory.setText(displayCategory);
                if (textDetailSubtitle != null) {
                    textDetailSubtitle.setVisibility(View.VISIBLE);
                    textDetailSubtitle.setText(getString(R.string.worker_service_detail_subtitle_format, displayCategory));
                }
            }
        }

        if (TextUtils.isEmpty(service.getBio())) {
            textBio.setText(R.string.worker_service_detail_no_bio);
        } else {
            textBio.setText(service.getBio());
        }

        if (TextUtils.isEmpty(service.getPriceValue())) {
            textPrice.setText(getString(R.string.worker_service_detail_custom_format, getString(R.string.home_service_price_custom_label)));
            textPriceSubtitle.setVisibility(View.GONE);
        } else if (service.isHourlyPricing()) {
            textPrice.setText(getString(R.string.worker_service_detail_hourly_format, service.getPriceValue()));
            textPriceSubtitle.setVisibility(View.VISIBLE);
            textPriceSubtitle.setText(R.string.worker_service_detail_price_subtitle);
        } else {
            textPrice.setText(getString(R.string.worker_service_detail_custom_format, service.getPriceValue()));
            textPriceSubtitle.setVisibility(View.VISIBLE);
            textPriceSubtitle.setText(R.string.worker_service_detail_price_subtitle);
        }

        ImageLoader.loadUriInto(this, imageHero, service.getImageUri(), R.drawable.ic_service_manage, R.color.primary_color);

        applyHeroBadges();
        applyHighlights();
    }

    private void applyHeroBadges() {
        int hash = Math.abs(service.getId().hashCode());
        int jobsCount = 25 + (hash % 85);
        int experienceYears = 1 + (hash % 6);
        float rating = 4.2f + ((hash % 7) * 0.1f);

        textStatJobs.setText(getString(R.string.worker_service_detail_jobs_value, jobsCount));
        textStatExperience.setText(getString(R.string.worker_service_detail_experience_value, experienceYears));
        textStatRating.setText(getString(R.string.worker_service_detail_rating_value, rating));
    }

    private void applyHighlights() {
        List<String> highlights = new ArrayList<>();
        highlights.add(getString(R.string.worker_service_detail_highlight_verified));
        highlights.add(getString(R.string.worker_service_detail_highlight_eco));
        highlights.add(getString(R.string.worker_service_detail_highlight_guarantee));

        if (chipGroupHighlights == null) {
            return;
        }

        chipGroupHighlights.removeAllViews();
        int[] highlightIcons = {
                R.drawable.ic_shield,
                R.drawable.ic_spark,
                R.drawable.ic_warranty,
                R.drawable.ic_verified
        };

        for (int i = 0; i < highlights.size(); i++) {
            Chip chip = new Chip(this);
            chip.setText(highlights.get(i));
            chip.setCheckable(false);
            chip.setClickable(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipBackgroundColorResource(R.color.primary_chip_surface);
            chip.setChipIconResource(highlightIcons[i % highlightIcons.length]);
            chip.setChipIconTintResource(R.color.primary_color);
            chip.setChipIconSize(getResources().getDimension(R.dimen.chip_icon_size_small));
            chip.setTextColor(ContextCompat.getColor(this, R.color.primary_color));
            chip.setTextSize(13f);
            chip.setRippleColorResource(android.R.color.transparent);
            chipGroupHighlights.addView(chip);
        }
    }

}

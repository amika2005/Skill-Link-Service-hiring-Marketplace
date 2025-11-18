package com.skilllink;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class RoleSelectionActivity extends AppCompatActivity {

    private CardView userCard;
    private CardView workerCard;
    private ImageView userImage;
    private ImageView workerImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        initializeViews();
        loadAnimations();
        setupClickListeners();
        setupTouchListeners();
    }

    private void initializeViews() {
        userCard = findViewById(R.id.user_card);
        workerCard = findViewById(R.id.worker_card);
        userImage = findViewById(R.id.user_image);
        workerImage = findViewById(R.id.worker_image);
    }

    private void loadAnimations() {
        // Load custom animations (staggered)
        Animation slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        Animation slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        
        // Add delays for staggered animations
        slideInLeft.setStartOffset(200);
        slideInRight.setStartOffset(300);
        
        // Apply animations to cards
        userCard.startAnimation(slideInLeft);
        workerCard.startAnimation(slideInRight);
        
    }

    private void setupClickListeners() {
        // Set click listeners for the buttons and cards
        findViewById(R.id.btn_user).setOnClickListener(v -> navigateToLogin("user"));
        findViewById(R.id.btn_worker).setOnClickListener(v -> navigateToLogin("worker"));
        findViewById(R.id.user_card).setOnClickListener(v -> navigateToLogin("user"));
        findViewById(R.id.worker_card).setOnClickListener(v -> navigateToLogin("worker"));
    }

    private void setupTouchListeners() {
        // Add touch feedback for user card
        userCard.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_down));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up));
                    break;
            }
            return false; // Return false to allow click events to still be processed
        });

        // Add touch feedback for worker card
        workerCard.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_down));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up));
                    break;
            }
            return false; // Return false to allow click events to still be processed
        });
    }

    private void navigateToLogin(String role) {
        // Get the card that should be animated based on the role
        CardView selectedCard = role.equals("user") ? userCard : workerCard;
        
        // Add a slight animation effect before navigating
        selectedCard.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    selectedCard.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                    
                    Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
                    intent.putExtra("user_role", role);
                    startActivity(intent);
                    finish();
                })
                .start();
    }
}

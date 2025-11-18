package com.skilllink;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class WorkerSignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, UserSignupActivity.class);
        intent.putExtra("user_role", "worker");
        startActivity(intent);
        finish();
    }
}

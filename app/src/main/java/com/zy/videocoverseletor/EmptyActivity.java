package com.zy.videocoverseletor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EmptyActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty_layout);
    }

    public void toCoverSel(View view){
        Intent intent = new Intent(EmptyActivity.this, CoverSelectorActivity.class);
        startActivity(intent);
    }
}

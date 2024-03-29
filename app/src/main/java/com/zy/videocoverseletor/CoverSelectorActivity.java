package com.zy.videocoverseletor;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.zy.videocoverseletor.view.SelectCoverView;

public class CoverSelectorActivity extends AppCompatActivity {
    private static final String TAG = CoverSelectorActivity.class.getSimpleName();
    private SelectCoverView mUGCKitVideoCut;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover_selector);
        checkPermission();
        initDataObserver();
    }

    public void initDataObserver() {
        mUGCKitVideoCut = findViewById(R.id.video_cutter_layout);
        final ImageView mCoverImg = findViewById(R.id.iv_cover);
        mUGCKitVideoCut.setOnCorpImgListener(new SelectCoverView.OnCorpImgListener() {
            @Override
            public void OnCrop(Bitmap bitmap) {
                mCoverImg.setImageBitmap(bitmap);
            }
        });

        findViewById(R.id.btn_select).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUGCKitVideoCut.cropVideoCover();
            }
        });
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(CoverSelectorActivity.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(CoverSelectorActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(CoverSelectorActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CoverSelectorActivity.this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE
                    }, 0);
        }
    }


}

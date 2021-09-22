package com.cakes.democamera2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cakes.democamera2.utils.LogUtil;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private final int PREVIEW_WIDTH = 480;
    private final int PREVIEW_HEIGHT = 640;

    private TextureView textureView;

    private CameraHelper cameraHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LogUtil.d(TAG, "onCreate -- 111111");
        textureView = findViewById(R.id.main_texture_view);

        textureView.setSurfaceTextureListener(surfaceTextureListener);
        checkPermissions();
    }

    private boolean checkPermissions() {
        boolean allGranted = true;
        allGranted &= ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 1);
        }
        return allGranted;
    }

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {

            LogUtil.d(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureAvailable()");
            useCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };

    private void useCamera() {
        cameraHelper = new CameraHelper(this, textureView, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        cameraHelper.useCamera();
    }


    public void takePhoto(View view) {
        if (null != cameraHelper) {
            cameraHelper.takePhoto();
        }
    }
}
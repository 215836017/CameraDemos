package com.cakes.democamera2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;

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

        textureView = findViewById(R.id.main_texture_view);

        textureView.setSurfaceTextureListener(surfaceTextureListener);
    }


    private void useCamera() {
        cameraHelper = new CameraHelper(this, textureView, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        cameraHelper.useCamera();
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
}
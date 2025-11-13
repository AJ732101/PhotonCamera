package com.particlesdevs.photoncamera.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.particlesdevs.photoncamera.ui.camera.CameraActivity;
import com.particlesdevs.photoncamera.util.FileManager;
import com.particlesdevs.photoncamera.util.Log;

import java.io.File;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure splash screen starts in portrait mode
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        final File path = new File(FileManager.sPHOTON_DIR, "PhotonLog");
        Log.setLogFile(path);

        startActivity(new Intent(SplashActivity.this, CameraActivity.class));
        finish();
    }
}

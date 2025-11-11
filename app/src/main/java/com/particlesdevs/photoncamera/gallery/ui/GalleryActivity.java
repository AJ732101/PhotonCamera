package com.particlesdevs.photoncamera.gallery.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.app.base.BaseActivity;
import com.particlesdevs.photoncamera.databinding.ActivityGalleryBinding;
import com.particlesdevs.photoncamera.gallery.files.GalleryFileOperations;
import com.particlesdevs.photoncamera.gallery.ui.fragments.ImageLibraryFragment;
import com.particlesdevs.photoncamera.gallery.ui.fragments.ImageViewerFragment;
import com.particlesdevs.photoncamera.gallery.viewmodel.GalleryViewModel;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.ui.SplashActivity;

public class GalleryActivity extends BaseActivity {
    private ActivityGalleryBinding activityGalleryBinding;
    private GalleryViewModel viewModel;

    private boolean externalUsage = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // --- COLD START FIX ---
        if (PhotonCamera.getInstance(this) == null) {
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Stop further execution
        }
        // --- END OF FIX ---

        super.onCreate(savedInstanceState);
        // Hide system UI immediately to prevent flickering
        hideSystemUI();
        
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Object fragment = getIntent().getExtras().get("CameraFragment");
            externalUsage = fragment == null;
        } else {
            externalUsage = true;
        }

        getDelegate().setLocalNightMode(PreferenceKeys.getThemeValue());
        activityGalleryBinding = DataBindingUtil.setContentView(this, R.layout.activity_gallery);
        viewModel = new ViewModelProvider(this).get(GalleryViewModel.class);
        viewModel.fetchAllMedia();
        viewModel.setCurrentFolderImages(viewModel.getAllSelectedImageFolder().getValue());
//        DataBindingUtil.setContentView(this, R.layout.activity_gallery);
    }

    /*public void onBackArrowClicked(View view) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.gallery_navigation_host);
        NavController navController = navHostFragment.getNavController();
        navController.navigateUp();
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        // Apply hideSystemUI in onResume to prevent flickering when returning to the gallery
        hideSystemUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityGalleryBinding = null;
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            float displayAspectRatio = (float) Math.max(dm.heightPixels, dm.widthPixels) / Math.min(dm.heightPixels, dm.widthPixels);
            if (displayAspectRatio <= (16f / 9) || dm.densityDpi > 440) {
                hideSystemUI();
            }
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GalleryFileOperations.REQUEST_PERM_DELETE) {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.gallery_navigation_host);
            Fragment fragment = null;
            if (navHostFragment != null) {
                fragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
            }
            if (fragment instanceof ImageViewerFragment) {
                ((ImageViewerFragment) fragment).handleImagesDeletedCallback(resultCode == Activity.RESULT_OK);
            } else if (fragment instanceof ImageLibraryFragment) {
                ((ImageLibraryFragment) fragment).handleImagesDeletedCallback(resultCode == Activity.RESULT_OK);
            }
        }
    }
}

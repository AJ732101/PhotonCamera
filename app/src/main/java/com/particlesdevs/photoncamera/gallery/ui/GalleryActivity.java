package com.particlesdevs.photoncamera.gallery.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

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
    protected void onDestroy() {
        super.onDestroy();
        activityGalleryBinding = null;
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

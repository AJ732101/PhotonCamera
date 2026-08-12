package com.particlesdevs.photoncamera.ui.camera;

import android.graphics.ImageFormat;
import android.os.Bundle;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.databinding.LayoutBottombuttonsBinding;
import com.particlesdevs.photoncamera.databinding.LayoutMainTopbarBinding;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.ui.camera.views.modeswitcher.wefika.horizontalpicker.HorizontalPicker;
import com.particlesdevs.photoncamera.util.Utilities;

import java.util.List;

import static androidx.constraintlayout.widget.ConstraintSet.GONE;

public class CameraUIViewImpl implements CameraUIView {
    private static final String TAG = "CameraUIView";
    private final CameraFragment cameraFragment;
    private final ProgressBar mCaptureProgressBar;
    private final ImageButton mShutterButton;
    private final ProgressBar mProcessingProgressBar;
    private final HorizontalPicker mModePicker;
    private LayoutMainTopbarBinding topbar;
    private LayoutBottombuttonsBinding bottombuttons;
    private CameraUIEventsListener uiEventsListener;
    private CameraModeState currentState;
    private List<CameraMode> availableModes;
    private CameraMode currentMode;

    CameraUIViewImpl(CameraFragment cameraFragment) {
        this.cameraFragment = cameraFragment;
        this.topbar = cameraFragment.cameraFragmentBinding.layoutTopbar;
        this.bottombuttons = cameraFragment.cameraFragmentBinding.layoutBottombar.bottomButtons;
        this.mCaptureProgressBar = cameraFragment.cameraFragmentBinding.layoutViewfinder.captureProgressBar;
        this.mProcessingProgressBar = bottombuttons.processingProgressBar;
        this.mShutterButton = bottombuttons.shutterButton;
        this.mModePicker = cameraFragment.cameraFragmentBinding.layoutBottombar.modeSwitcher.modePickerView;
        this.initListeners();
        this.initModeSwitcher();
    }

    private void initListeners() {
        this.topbar.setTopBarClickListener(v -> this.uiEventsListener.onClick(v));
        this.bottombuttons.setBottomBarClickListener(v -> this.uiEventsListener.onClick(v));
    }

    private void initModeSwitcher() {
        this.availableModes = CameraMode.getAvailableModes();
        this.mModePicker.setValues(availableModes.stream().map(mode -> cameraFragment.getString(mode.getStringId())).toArray(String[]::new));
        this.mModePicker.setOverScrollMode(View.OVER_SCROLL_NEVER);
        this.mModePicker.setOnItemSelectedListener(index -> {
            if (index >= 0 && index < availableModes.size()) {
                switchToMode(availableModes.get(index));
            }
        });

        CameraMode persistedMode = CameraMode.valueOf(PreferenceKeys.getCameraModeOrdinal());
        CameraMode initialMode;

        if (availableModes.contains(persistedMode)) {
            initialMode = persistedMode;
        } else {
            initialMode = CameraMode.VIDEO; // Default to VIDEO if persisted mode is not available
        }

        if (!availableModes.contains(initialMode)) {
            initialMode = availableModes.isEmpty() ? null : availableModes.get(0);
        }

        if (initialMode != null) {
            // Update the source of truth (preferences) so the controller can read the correct state
            PreferenceKeys.setCameraModeOrdinal(initialMode.ordinal());
            this.mModePicker.setSelectedItem(availableModes.indexOf(initialMode));
            updateStateForMode(initialMode); // Set initial UI state
        } else {
            this.mModePicker.setVisibility(View.GONE); // Hide picker if no modes are available
        }
    }

    @Override
    public void setCameraUIEventsListener(CameraUIEventsListener cameraUIEventsListener) {
        this.uiEventsListener = cameraUIEventsListener;
        // DO NOT call onCameraModeChanged here. The controller will read the correct mode
        // from preferences when it initializes.
    }

    private void updateStateForMode(CameraMode cameraMode) {
        Log.d(TAG, "Updating state for Mode:" + cameraMode.name());
        this.currentMode = cameraMode;
        switch (cameraMode) {
            case VIDEO:
                currentState = new VideoModeState();
                break;
            case UNLIMITED:
            case RAWVIDEO:
                currentState = new UnlimitedModeState();
                break;
            case PHOTO:
            case MOTION:
                currentState = new PhotoMotionModeState();
                break;
            case NIGHT:
                currentState = new NightModeState();
                break;
        }
        currentState.reConfigureModeViews(cameraMode);
    }

    private void switchToMode(CameraMode cameraMode) {
        // This is called on user interaction. At this point, the listener is guaranteed to be ready.
        updateStateForMode(cameraMode);
        if (uiEventsListener != null) {
            uiEventsListener.onCameraModeChanged(cameraMode);
        }
    }

    @Override
    public void activateShutterButton(boolean status) {
        this.mShutterButton.post(() -> {
            this.mShutterButton.setActivated(status);
            this.mShutterButton.setClickable(status);
        });
    }

    private void toggleConstraints(CameraMode mode) {
        if (cameraFragment.displayAspectRatio <= 16f / 9f) {
            ConstraintLayout.LayoutParams camera_containerLP =
                    (ConstraintLayout.LayoutParams) cameraFragment.cameraFragmentBinding
                            .textureHolder
                            .findViewById(R.id.camera_container)
                            .getLayoutParams();
            switch (mode) {
                case VIDEO:
                    camera_containerLP.topToTop = R.id.textureHolder;
                    camera_containerLP.topToBottom = -1;
                    break;
                case UNLIMITED:
                case PHOTO:
                case MOTION:
                case NIGHT:
                    camera_containerLP.topToTop = -1;
                    camera_containerLP.topToBottom = R.id.layout_topbar;
            }
        }
    }

    @Override
    public void refresh(boolean processing) {
        cameraFragment.cameraFragmentBinding.invalidateAll();
        currentState.reConfigureModeViews(CameraMode.valueOf(PreferenceKeys.getCameraModeOrdinal()));
        this.resetCaptureProgressBar();
        if (!processing) {
            this.activateShutterButton(true);
            if (cameraFragment.getTimerFrameCountViewModel() != null) {
                cameraFragment.getTimerFrameCountViewModel().clearFrameTimeCnt();
            }
            this.setProcessingProgressBarIndeterminate(false);
            this.lockUIForBurst(false);
        }
    }

    @Override
    public void setProcessingProgressBarIndeterminate(boolean indeterminate) {
        this.mProcessingProgressBar.post(() -> this.mProcessingProgressBar.setIndeterminate(indeterminate));
    }

    @Override
    public void incrementCaptureProgressBar(int step) {
        this.mCaptureProgressBar.post(() -> this.mCaptureProgressBar.incrementProgressBy(step));
    }

    @Override
    public void resetCaptureProgressBar() {
        this.mCaptureProgressBar.post(() -> this.mCaptureProgressBar.setProgress(0));
        this.setCaptureProgressBarOpacity(0);
    }

    @Override
    public void setCaptureProgressBarOpacity(float alpha) {
        this.mCaptureProgressBar.post(() -> this.mCaptureProgressBar.setAlpha(alpha));
    }

    @Override
    public void setCaptureProgressMax(int max) {
        this.mCaptureProgressBar.post(() -> this.mCaptureProgressBar.setMax(max));
    }

    @Override
    public void showFlashButton(boolean flashAvailable) {
        this.topbar.setFlashVisible(flashAvailable);
        cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.flash_entry_layout, flashAvailable ? View.VISIBLE : GONE);
    }

    @Override
    public void lockUIForBurst(boolean locked) {
        if (this.bottombuttons != null) {
            this.bottombuttons.galleryImageButton.post(() -> this.bottombuttons.galleryImageButton.setEnabled(!locked));
        }
        if (this.mModePicker != null) {
            this.mModePicker.post(() -> this.mModePicker.setEnabled(!locked));
        }
        if (cameraFragment.cameraFragmentBinding != null) {
            cameraFragment.cameraFragmentBinding.auxButtonsContainer.post(() -> {
                cameraFragment.cameraFragmentBinding.auxButtonsContainer.setEnabled(!locked);
                cameraFragment.cameraFragmentBinding.auxButtonsContainer.setAlpha(locked ? 0.5f : 1.0f);
                cameraFragment.auxButtonsViewModel.setEnabled(!locked);
            });
        }
        if (cameraFragment.cameraFragmentBinding != null) {
            cameraFragment.cameraFragmentBinding.settingsBar.post(() -> {
                cameraFragment.cameraFragmentBinding.settingsBar.setEnabled(!locked);
                cameraFragment.cameraFragmentBinding.settingsBar.setAlpha(locked ? 0.5f : 1.0f);
            });
        }
        if (cameraFragment.cameraFragmentBinding != null) {
            cameraFragment.cameraFragmentBinding.manualMode.post(() -> {
                cameraFragment.cameraFragmentBinding.manualMode.setEnabled(!locked);
                cameraFragment.cameraFragmentBinding.manualMode.setAlpha(locked ? 0.5f : 1.0f);
            });
        }
        if (cameraFragment.textureView != null) {
            cameraFragment.textureView.post(() -> cameraFragment.textureView.setEnabled(!locked));
        }
    }

    @Override
    public void destroy() {
        topbar = null;
        bottombuttons = null;
    }

    public class VideoModeState implements CameraModeState {
        @Override
        public void reConfigureModeViews(CameraMode mode) {
            topbar.setZoomVisible(true);
            topbar.setNoiseVisible(true);
            topbar.setEdgeVisible(true);
            topbar.setEisVisible(true);
            topbar.setFpsVisible(true);
            topbar.setTimerVisible(false);
            topbar.setHdrxVisible(false);
            if (PhotonCamera.getSettings().frameCount == 1) {
                cameraFragment.cameraFragmentBinding.setZoomSliderVisible(PhotonCamera.getSettings().showZoomSlider);
            } else {
                cameraFragment.cameraFragmentBinding.setZoomSliderVisible(false);
            }
            cameraFragment.cameraFragmentBinding.setFramecountSliderVisible(false);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.fps_entry_layout, View.VISIBLE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.timer_entry_layout, View.GONE);
            mShutterButton.setBackgroundResource(R.drawable.unlimitedbutton);
            cameraFragment.cameraFragmentBinding.layoutBottombar.layoutBottombar.setBackgroundResource(R.color.panel_transparency);
            cameraFragment.cameraFragmentBinding.getRoot().setBackgroundResource(android.R.color.black);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.noise_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.edge_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.zoom_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.eis_entry_layout, View.VISIBLE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.hdrx_entry_layout, View.GONE);
            toggleConstraints(mode);
        }
    }

    public class UnlimitedModeState implements CameraModeState {
        @Override
        public void reConfigureModeViews(CameraMode mode) {
            topbar.setZoomVisible(false);
            topbar.setNoiseVisible(false);
            topbar.setEdgeVisible(false);
            topbar.setEisVisible(false);
            topbar.setFpsVisible(true);
            topbar.setTimerVisible(false);
            topbar.setHdrxVisible(false);
            cameraFragment.cameraFragmentBinding.setZoomSliderVisible(false);
            cameraFragment.cameraFragmentBinding.setFramecountSliderVisible(false);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.fps_entry_layout, View.VISIBLE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.timer_entry_layout, View.GONE);
            mShutterButton.setBackgroundResource(R.drawable.unlimitedbutton);
            cameraFragment.cameraFragmentBinding.layoutBottombar.layoutBottombar.setBackground(null);
            cameraFragment.cameraFragmentBinding.getRoot().setBackground(Utilities.resolveDrawable(cameraFragment.requireActivity(), R.attr.cameraFragmentBackground));
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.noise_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.edge_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.zoom_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.hdrx_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.eis_entry_layout, View.GONE);
            toggleConstraints(mode);
        }
    }

    public class PhotoMotionModeState implements CameraModeState {
        @Override
        public void reConfigureModeViews(CameraMode mode) {
            var frameCount = PhotonCamera.getSettings().frameCount;
            var previewFormat = PhotonCamera.getSettings().previewFormat;

            if (PhotonCamera.isSensorModeOn && (PhotonCamera.getSettings().sensorModeCfaPattern == -2)) {
                topbar.setZoomVisible(false);
                topbar.setNoiseVisible(false);
                topbar.setEdgeVisible(false);;
            } else if (PhotonCamera.isSingleShotJpegOrHeic()) {
                topbar.setZoomVisible(true);
                topbar.setNoiseVisible(true);
                topbar.setEdgeVisible(true);
                cameraFragment.cameraFragmentBinding.setZoomSliderVisible(PhotonCamera.getSettings().showZoomSlider);
                cameraFragment.cameraFragmentBinding.setFramecountSliderVisible(false);
            } else {
                topbar.setZoomVisible(false);
                topbar.setNoiseVisible(false);
                topbar.setEdgeVisible(false);
                cameraFragment.cameraFragmentBinding.setZoomSliderVisible(false);
                cameraFragment.cameraFragmentBinding.setFramecountSliderVisible(PhotonCamera.getSettings().showFramecountSlider);
                if (PhotonCamera.getSettings().showFramecountSlider) {
                    cameraFragment.getCameraFragmentViewModel().getCameraFragmentModel().refreshFrameCount();
                }
            }
            topbar.setEisVisible(false);
            topbar.setFpsVisible(true);
            topbar.setTimerVisible(true);
            topbar.setHdrxVisible(false);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.eis_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.fps_entry_layout, View.VISIBLE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.timer_entry_layout, View.VISIBLE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.hdrx_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.noise_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.edge_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.zoom_entry_layout, View.GONE);
            //mShutterButton.setBackgroundResource(R.drawable.roundbutton);
            mShutterButton.setBackgroundResource(R.drawable.shutter_button_main);
            cameraFragment.cameraFragmentBinding.layoutBottombar.layoutBottombar.setBackground(null);
            cameraFragment.cameraFragmentBinding.getRoot().setBackground(Utilities.resolveDrawable(cameraFragment.requireActivity(), R.attr.cameraFragmentBackground));
            toggleConstraints(mode);
        }
    }

    public class NightModeState implements CameraModeState {
        @Override
        public void reConfigureModeViews(CameraMode mode) {
            if (PhotonCamera.isSensorModeOn && (PhotonCamera.getSettings().sensorModeCfaPattern == -2)) {
                topbar.setZoomVisible(false);
                topbar.setNoiseVisible(false);
                topbar.setEdgeVisible(false);;
            } else if (PhotonCamera.isSingleShotJpegOrHeic()) {
                topbar.setZoomVisible(true);
                topbar.setNoiseVisible(true);
                topbar.setEdgeVisible(true);
                cameraFragment.cameraFragmentBinding.setZoomSliderVisible(PhotonCamera.getSettings().showZoomSlider);
                cameraFragment.cameraFragmentBinding.setFramecountSliderVisible(false);
            } else {
                topbar.setZoomVisible(false);
                topbar.setNoiseVisible(false);
                topbar.setEdgeVisible(false);
                cameraFragment.cameraFragmentBinding.setZoomSliderVisible(false);
                cameraFragment.cameraFragmentBinding.setFramecountSliderVisible(PhotonCamera.getSettings().showFramecountSlider);
                if (PhotonCamera.getSettings().showFramecountSlider) {
                    cameraFragment.getCameraFragmentViewModel().getCameraFragmentModel().refreshFrameCount();
                }
            }
            topbar.setEisVisible(false);
            topbar.setFpsVisible(true);
            topbar.setTimerVisible(true);
            topbar.setHdrxVisible(false);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.eis_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.fps_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.timer_entry_layout, View.VISIBLE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.hdrx_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.noise_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.edge_entry_layout, View.GONE);
            cameraFragment.cameraFragmentBinding.settingsBar.setChildVisibility(R.id.zoom_entry_layout, View.GONE);
            //mShutterButton.setBackgroundResource(R.drawable.roundbutton);
            mShutterButton.setBackgroundResource(R.drawable.shutter_button);
            cameraFragment.cameraFragmentBinding.layoutBottombar.layoutBottombar.setBackground(null);
            cameraFragment.cameraFragmentBinding.getRoot().setBackground(Utilities.resolveDrawable(cameraFragment.requireActivity(), R.attr.cameraFragmentBackground));
            toggleConstraints(mode);
        }
    }
}

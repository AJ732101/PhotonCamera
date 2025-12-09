package com.particlesdevs.photoncamera.ui.camera;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import com.particlesdevs.photoncamera.processing.parameters.IsoExpoSelector;
import com.particlesdevs.photoncamera.util.Log;
import android.webkit.MimeTypeMap;

import android.provider.MediaStore;
import android.view.View;

import androidx.lifecycle.Observer;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.control.CountdownTimer;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.settings.SettingType;
import com.particlesdevs.photoncamera.ui.GalleryChooserActivity;
import com.particlesdevs.photoncamera.ui.camera.model.TopBarSettingsData;
import com.particlesdevs.photoncamera.ui.camera.views.AuxButtonsLayout;
import com.particlesdevs.photoncamera.ui.camera.views.FlashButton;
import com.particlesdevs.photoncamera.ui.camera.views.TimerButton;
import android.content.ContentUris;
import android.widget.Toast;

/**
 * Implementation of {@link CameraUIEventsListener}
 * <p>
 * Responsible for converting user inputs into actions
 */
final class CameraUIController implements CameraUIEventsListener,
        Observer<TopBarSettingsData<?, ?>>, AuxButtonsLayout.AuxButtonListener {
    private static final String TAG = "CameraUIController";
    private final CameraFragment cameraFragment;
    private CountDownTimer countdownTimer;
    private View shutterButton;

    public CameraUIController(CameraFragment cameraFragment) {
        this.cameraFragment = cameraFragment;
    }

    private String getMimeType(Context context, Uri uri) {
        String extension;
        // Prüfen, ob die URI eine content:// URI ist
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.getContentResolver().getType(uri));
        } else {
            // Fallback für file:// URIs
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new java.io.File(uri.getPath())).toString());
        }

        if (extension == null) {
            // Manuelle Prüfung, wenn die Extension nicht ermittelt werden konnte
            String path = uri.getPath();
            if (path != null) {
                if (path.toLowerCase().endsWith(".dng")) return "image/x-adobe-dng";
                if (path.toLowerCase().endsWith(".heic")) return "image/heic";
                if (path.toLowerCase().endsWith(".heif")) return "image/heic";
                if (path.toLowerCase().endsWith(".avif")) return "image/avif";
            }
            return "*/*"; // Generischer Fallback
        }

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());

        // MimeTypeMap kennt HEIC/DNG oft nicht, also überschreiben wir es hier sicherheitshalber
        if ("dng".equalsIgnoreCase(extension)) {
            return "image/x-adobe-dng";
        }
        if ("avif".equalsIgnoreCase(extension)) {
            return "image/avif";
        }
        if ("heic".equalsIgnoreCase(extension) || "heif".equalsIgnoreCase(extension)) {
            return "image/heic";
        }

        return (mimeType != null) ? mimeType : "*/*";
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.shutter_button:
                shutterButton = view;
                switch (PhotonCamera.getSettings().selectedMode) {
                    case PHOTO:
                    case MOTION:
                    case NIGHT:
                        if (view.isHovered()) resetTimer();
                        else startTimer();
                        break;
                    case UNLIMITED:
                    case RAWVIDEO:
                        if (!cameraFragment.captureController.onUnlimited) {
                            cameraFragment.captureController.callUnlimitedStart();
                            view.setActivated(false);
                        } else {
                            cameraFragment.captureController.callUnlimitedEnd();
                            view.setActivated(true);
                        }
                        break;
                    case VIDEO:
                        if (!cameraFragment.captureController.mIsRecordingVideo) {
                            if (cameraFragment.captureController.VideoStart()) {
                                view.setActivated(false);
                            }
                        } else {
                            cameraFragment.captureController.VideoEnd();
                            view.setActivated(true);
                        }
                        break;
                }
                break;
            case R.id.settings_button:
                cameraFragment.launchSettings();
                break;

            case R.id.hdrx_toggle_button:
                PreferenceKeys.setHdrX(!PreferenceKeys.isHdrXOn());
                if (PreferenceKeys.isHdrXOn())
                    CaptureController.setTargetFormat(CaptureController.RAW_FORMAT);
                else
                    CaptureController.setTargetFormat(CaptureController.YUV_FORMAT);
                cameraFragment.showSnackBar(cameraFragment.getString(R.string.hdrx) + ':' + onOff(PreferenceKeys.isHdrXOn()));
                this.restartCamera();
                break;

            case R.id.gallery_image_button:
                if (PhotonCamera.getSettings().useExternalGallery) {
                    Uri lastImageUri = MediaStoreUtils.getLatestImageUri(cameraFragment.requireContext().getContentResolver());

                    if (lastImageUri != null) {
                        try {
                            // Den MIME-Typ dynamisch basierend auf der Dateiendung der URI ermitteln
                            String mimeType = getMimeType(cameraFragment.requireContext(), lastImageUri);
                            Log.d(TAG, "Opening URI: " + lastImageUri + " with MIME type: " + mimeType);

                            // Intent mit URI UND dem korrekten MIME-Typ erstellen
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(lastImageUri, mimeType);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(cameraFragment.requireContext());
                            String galleryPackage = prefs.getString(GalleryChooserActivity.KEY_DEFAULT_GALLERY_PACKAGE, null);

                            if (galleryPackage != null) {
                                intent.setPackage(galleryPackage);
                            }

                            cameraFragment.startActivity(intent);

                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(cameraFragment.getContext(), "Keine App zum Öffnen dieses Dateityps gefunden.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(cameraFragment.getContext(), "Kein Bild in der Galerie gefunden.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    cameraFragment.launchGallery();
                }
                break;


            /*case R.id.gallery_image_button:
                if (PhotonCamera.getSpecific().specificSetting.useExternalViewer) {
                    Uri lastImageUri = MediaStoreUtils.getLatestImageUri(cameraFragment.requireContext().getContentResolver());

                    if (lastImageUri != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, lastImageUri);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(cameraFragment.requireContext());
                        String galleryPackage = prefs.getString(GalleryChooserActivity.KEY_DEFAULT_GALLERY_PACKAGE, null);

                        if (galleryPackage != null) {
                            intent.setPackage(galleryPackage);
                        }

                        try {
                            cameraFragment.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(cameraFragment.getContext(), "No gallery app found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(cameraFragment.getContext(), "No image in gallery app found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    cameraFragment.launchGallery();
                }
                break;*/

            case R.id.eis_toggle_button:
                PreferenceKeys.setEisPhoto(!PreferenceKeys.isEisPhotoOn());
                cameraFragment.showSnackBar(cameraFragment.getString(R.string.eis_toggle_text) + ':' + onOff(PreferenceKeys.isEisPhotoOn()));
                cameraFragment.updateSettingsBar();
                this.restartCamera();
                break;

            case R.id.zoom_toggle_button:
                PreferenceKeys.setSetZoomOn(!PreferenceKeys.isZoomOn());
                cameraFragment.showSnackBar(cameraFragment.getString(R.string.zoom_toggle_text) + ':' + onOff(PreferenceKeys.isZoomOn()));
                cameraFragment.updateSettingsBar();
                this.restartCamera();
                break;

            case R.id.noise_toggle_button:
                if (PreferenceKeys.getNoiseProcessing() == 0) {
                    PreferenceKeys.setSetNoiseProcessing(1);
                }
                else {
                    PreferenceKeys.setSetNoiseProcessing(0);
                }
                cameraFragment.showSnackBar(cameraFragment.getString(R.string.noise_toggle_text) + ':' + onOff(PreferenceKeys.isNoiseProcessingOn()));
                cameraFragment.updateSettingsBar();
                this.restartCamera();
                break;

            case R.id.edge_toggle_button:
                if (PreferenceKeys.getEdgeProcessing() == 0) {
                    PreferenceKeys.setSetEdgeProcessing(1);
                }
                else {
                    PreferenceKeys.setSetEdgeProcessing(0);
                }
                cameraFragment.showSnackBar(cameraFragment.getString(R.string.edge_toggle_text) + ':' + onOff(PreferenceKeys.isEdgeProcessingOn()));
                cameraFragment.updateSettingsBar();
                this.restartCamera();
                break;

            case R.id.fps_toggle_button:
                PreferenceKeys.setFpsPreview(!PreferenceKeys.isFpsPreviewOn());
                cameraFragment.showSnackBar(cameraFragment.getString(R.string.fps_60_toggle_text) + ':' + onOff(PreferenceKeys.isFpsPreviewOn()));
                cameraFragment.updateSettingsBar();
                break;

            case R.id.quad_res_toggle_button:
                PreferenceKeys.setQuadBayer(!PreferenceKeys.isQuadBayerOn());
                cameraFragment.showSnackBar(cameraFragment.getString(R.string.quad_bayer_toggle_text) + ':' + onOff(PreferenceKeys.isQuadBayerOn()));
                this.restartCamera();
                cameraFragment.updateSettingsBar();
                break;

            case R.id.flip_camera_button:
                view.animate().rotationBy(180).setDuration(450).start();
                setID(cameraFragment.cycler(PreferenceKeys.getCameraID()));
                this.restartCamera();
                break;
            case R.id.grid_toggle_button:
                PreferenceKeys.setGridValue((PreferenceKeys.getGridValue() + 1) % view.getResources().getStringArray(R.array.vf_grid_entryvalues).length);
                view.setSelected(PreferenceKeys.getGridValue() != 0);
                cameraFragment.invalidateSurfaceView();
                cameraFragment.updateSettingsBar();
                break;

            case R.id.flash_button:
                PreferenceKeys.setAeMode((PreferenceKeys.getAeMode() + 1) % 4); //cycles in 0,1,2,3
                ((FlashButton) view).setFlashValueState(PreferenceKeys.getAeMode());
                cameraFragment.captureController.setPreviewAEModeRebuild(PreferenceKeys.getAeMode());
                cameraFragment.updateSettingsBar();
                break;

            case R.id.countdown_timer_button:
                PreferenceKeys.setCountdownTimerIndex((PreferenceKeys.getCountdownTimerIndex() + 1) % view.getResources().getIntArray(R.array.countdowntimer_entryvalues).length);
                ((TimerButton) view).setTimerIconState(PreferenceKeys.getCountdownTimerIndex());
                cameraFragment.updateSettingsBar();
                break;
        }
    }

    private int getTimerValue(Context context) {
        int[] timerValues = context.getResources().getIntArray(R.array.countdowntimer_entryvalues);
        return timerValues[PreferenceKeys.getCountdownTimerIndex()];
    }

    private void startTimer() {
        if (this.shutterButton != null) {
            this.shutterButton.setHovered(true);
            this.countdownTimer = new CountdownTimer(
                    cameraFragment.findViewById(R.id.frameTimer),
                    getTimerValue(this.shutterButton.getContext()) * 1000L, 1000,
                    this::onTimerFinished).start();
        }
    }

    private void resetTimer() {
        if (this.countdownTimer != null) this.countdownTimer.cancel();
        if (this.shutterButton != null) this.shutterButton.setHovered(false);
    }

    @Override
    public void onAuxButtonClicked(String id) {
        Log.d(TAG, "onAuxButtonClicked() called with: id = [" + id + "]");
        setID(id);
        this.restartCamera();

    }

    private void setID(String input) {
        PreferenceKeys.setCameraID(String.valueOf(input));
    }

    @Override
    public void onCameraModeChanged(CameraMode cameraMode) {
        PreferenceKeys.setCameraModeOrdinal(cameraMode.ordinal());
        Log.d(TAG, "onCameraModeChanged() called with: cameraMode = [" + cameraMode + "]");
        switch (cameraMode) {
            case PHOTO:
            case MOTION:
            case NIGHT:
            case UNLIMITED:
            case RAWVIDEO:
            default:
                break;
            case VIDEO:
                PreferenceKeys.setCameraModeOrdinal(CameraMode.VIDEO.ordinal());
                break;
        }
        this.restartCamera();
    }

    @Override
    public void onPause() {
        this.resetTimer();
    }

    private void restartCamera() {
        this.resetTimer();
        cameraFragment.captureController.restartCamera();
    }

    private String onOff(boolean value) {
        return value ? "On" : "Off";
    }

    private void onTimerFinished() {
        this.shutterButton.setHovered(false);
        this.shutterButton.setActivated(false);
        this.shutterButton.setClickable(false);
        cameraFragment.captureController.takePicture();
    }

    @Override
    public void onChanged(TopBarSettingsData<?, ?> topBarSettingsData) {
        if (topBarSettingsData != null && topBarSettingsData.getType() != null && topBarSettingsData.getValue() != null) {
            if (topBarSettingsData.getType() instanceof SettingType) {
                SettingType type = (SettingType) topBarSettingsData.getType();
                Object value = topBarSettingsData.getValue();
                switch (type) {
                    case FLASH:
                        PreferenceKeys.setAeMode((Integer) value); //cycles in 0,1,2,3
                        cameraFragment.captureController.setPreviewAEModeRebuild(PreferenceKeys.getAeMode());
                        cameraFragment.cameraFragmentBinding.layoutTopbar.flashButton.setFlashValueState((Integer) value);
                        break;
                    case HDRX:
                        PreferenceKeys.setHdrX(value.equals(1));
                        if (value.equals(1))
                            CaptureController.setTargetFormat(CaptureController.RAW_FORMAT);
                        else
                            CaptureController.setTargetFormat(CaptureController.YUV_FORMAT);
                        this.restartCamera();
                        break;
                    case QUAD:
                        PreferenceKeys.setQuadBayer(value.equals(1));
                        this.restartCamera();
                        break;
                    case GRID:
                        PreferenceKeys.setGridValue((Integer) value);
                        cameraFragment.invalidateSurfaceView();
                        break;
                    case FPS_60:
                        PreferenceKeys.setFpsPreview(value.equals(1));
                        this.restartCamera();
                        break;
                    case TIMER:
                        PreferenceKeys.setCountdownTimerIndex((Integer) value);
                        cameraFragment.cameraFragmentBinding.layoutTopbar.countdownTimerButton.setTimerIconState((Integer) value);
                        break;
                    case EIS:
                        PreferenceKeys.setEisPhoto(value.equals(1));
                        this.restartCamera();
                        break;
                    case ZOOM:
                        PreferenceKeys.setSetZoomOn(value.equals(1));
                        this.restartCamera();
                        break;
                    case NOISE:
                        if (PreferenceKeys.getNoiseProcessing() != 0) {
                            PreferenceKeys.setSetNoiseProcessing(0);
                        }
                        else {
                            PreferenceKeys.setSetNoiseProcessing(1);
                        }
                        this.restartCamera();
                        break;
                    case RAW:
                        PreferenceKeys.setSaveRaw((Integer) value);
                        break;
                    case BATTERY_SAVER:
                        PreferenceKeys.setBatterySaver(value.equals(1));
                        break;
                    case BRACKETING:
                        PreferenceKeys.setBracketingMode((Integer) value);
                        // Update HDR class to use the new bracketing mode
                        IsoExpoSelector.HDR = (Integer) value > 0;
                        break;

                }
                cameraFragment.cameraFragmentBinding.layoutTopbar.invalidateAll();
            }
        }
    }
}

class MediaStoreUtils {
    public static Uri getLatestImageUri(ContentResolver contentResolver) {
        Uri imageUri = null;
        String[] projection = new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN};
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        try (Cursor cursor = contentResolver.query(queryUri, projection, null, null, sortOrder)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                long id = cursor.getLong(idColumn);
                imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            }
        } catch (Exception e) {
            Log.e("MediaStoreUtils", "Fehler beim Abrufen des neuesten Bildes: " + e.getMessage());
        }

        return imageUri;
    }
}

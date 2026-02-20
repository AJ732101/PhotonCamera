package com.particlesdevs.photoncamera.ui.camera.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaFormat;
import android.util.Size;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.ListPreference;

import com.particlesdevs.photoncamera.BR;
import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.ui.camera.CameraUIController;
import com.particlesdevs.photoncamera.util.FileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class that holds the ui state, for now the orientation
 */
public class CameraFragmentModel extends BaseObservable {
    private int orientation;
    private int duration;
    private Bitmap bitmap;
    private boolean settingsBarVisibility;
    private boolean viewfinderMaginified = false;
    private boolean functionOneOn = false;
    private float screenAspectRatio = 9f / 16;
    private String dummyAspectRatio = "16:9";
    public final MutableLiveData<Float> zoomLevel = new MutableLiveData<>(1.0f);

    public void onMagnifyViewfinderClicked() {
        if (PhotonCamera.getCaptureController() != null) {
            PhotonCamera.getCaptureController().magnifyViewfinder();
            viewfinderMaginified = !viewfinderMaginified;
            notifyChange();
        }
    }

    public void onFunctionOneClicked() {
        if (PhotonCamera.getCaptureController() != null) {
            PhotonCamera.getCaptureController().functionOne();
            functionOneOn = !functionOneOn;
            notifyChange();
        }
    }

    public void onEisToggleLongClicked(View view, Object uiController) {
        Context context = view.getContext();
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            String[] entries = context.getResources().getStringArray(R.array.video_framerate_entries);
            String[] entryValues = context.getResources().getStringArray(R.array.video_framerate_entryValues);
            int currentVal = PreferenceKeys.getVideoFramerate();

            int checkedItem = -1;
            for (int i = 0; i < entryValues.length; i++) {
                if (Integer.valueOf(entryValues[i]) == currentVal) {
                    checkedItem = i;
                    break;
                }
            }

            new AlertDialog.Builder(context)
                    .setTitle(R.string.video_framerate)
                    .setSingleChoiceItems(entries, checkedItem, (dialog, which) -> {
                        int selectedValue = Integer.valueOf(entryValues[which]);
                        PhotonCamera.getSettings().videoFramrate = selectedValue;
                        PreferenceKeys.setVideoFramerate(selectedValue);

                        dialog.dismiss();
                        if (uiController instanceof CameraUIController) {
                            ((CameraUIController) uiController).refreshCameraUI(true);
                        }
                        notifyChange();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    public void onFlipCameraLongClicked(View view, Object uiController) {
        Context context = view.getContext();
        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            String[] entries = context.getResources().getStringArray(R.array.video_resolution_entries);
            String[] entryValues = context.getResources().getStringArray(R.array.video_resolution_entryValues);
            int currentVal = PreferenceKeys.getVideoHeight();

            int checkedItem = -1;
            for (int i = 0; i < entryValues.length; i++) {
                if (Integer.valueOf(entryValues[i]) == currentVal) {
                    checkedItem = i;
                    break;
                }
            }

            new AlertDialog.Builder(context)
                    .setTitle(R.string.resolution)
                    .setSingleChoiceItems(entries, checkedItem, (dialog, which) -> {
                        int selectedValue = Integer.valueOf(entryValues[which]);
                        PhotonCamera.getSettings().videoHeight = selectedValue;
                        PreferenceKeys.setVideoHeight(selectedValue);

                        dialog.dismiss();
                        if (uiController instanceof CameraUIController) {
                            ((CameraUIController) uiController).refreshCameraUI(true);
                        }
                        notifyChange();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    public void onSettingsLongClicked(View view, Object uiController) {
        Context context = view.getContext();

        String[] entries = context.getResources().getStringArray(R.array.contrast_curve_entries);
        String[] entryValues = context.getResources().getStringArray(R.array.contrast_curve_entryValues);

        String currentVal = PreferenceKeys.getContrastCurve();

        int checkedItem = -1;
        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i].equals(currentVal)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.contrast_curve)
                .setSingleChoiceItems(entries, checkedItem, (dialog, which) -> {
                    String selectedValue = entryValues[which];
                    PhotonCamera.getSettings().contrastCurve = selectedValue;
                    PreferenceKeys.setContrastCurve(selectedValue);

                    dialog.dismiss();
                    if (uiController instanceof CameraUIController) {
                        ((CameraUIController) uiController).refreshCameraUI(true);
                    }
                    notifyChange();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void onEdgeProcessingLongClicked(View view, Object uiController) {
        Context context = view.getContext();

        String[] entries = context.getResources().getStringArray(R.array.edge_processing_entries);
        String[] entryValues = context.getResources().getStringArray(R.array.edge_processing_entryValues);

        String currentVal = String.valueOf(PreferenceKeys.getEdgeProcessing());

        int checkedItem = -1;
        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i].equals(currentVal)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.edge_processing)
                .setSingleChoiceItems(entries, checkedItem, (dialog, which) -> {
                    String selectedValue = entryValues[which];
                    PhotonCamera.getSettings().edgeProcessing = Integer.valueOf(selectedValue);
                    PreferenceKeys.setEdgeProcessing(PhotonCamera.getSettings().edgeProcessing);

                    dialog.dismiss();
                    if (uiController instanceof CameraUIController) {
                        ((CameraUIController) uiController).refreshCameraUI(true);
                    }
                    notifyChange();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void onNoiseReductionLongClicked(View view, Object uiController) {
        Context context = view.getContext();
        String[] entries = context.getResources().getStringArray(R.array.noise_processing_entries);
        String[] entryValues = context.getResources().getStringArray(R.array.noise_processing_entryValues);
        String currentVal = String.valueOf(PreferenceKeys.getNoiseProcessing());

        int checkedItem = -1;
        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i].equals(currentVal)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.noise_processing)
                .setSingleChoiceItems(entries, checkedItem, (dialog, which) -> {
                    String selectedValue = entryValues[which];
                    PhotonCamera.getSettings().noiseProcessing = Integer.valueOf(selectedValue);
                    PreferenceKeys.setNoiseProcessing(PhotonCamera.getSettings().noiseProcessing);

                    dialog.dismiss();
                    if (uiController instanceof CameraUIController) {
                        ((CameraUIController) uiController).refreshCameraUI(true);
                    }
                    notifyChange();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void onCountdownTimerLongClicked(View view, Object uiController) {
        Context context = view.getContext();
        String currentValue = PreferenceKeys.getLutName();
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();

        entries.add("None");
        entryValues.add("lut.png");

        File tuningDir = FileManager.sPHOTON_TUNING_DIR;
        if (tuningDir.exists() && tuningDir.isDirectory()) {
            File[] files = tuningDir.listFiles((dir, name) -> name.toLowerCase().endsWith("_lut.png"));

            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (!entryValues.contains(fileName)) {
                        entries.add(fileName);
                        entryValues.add(fileName);
                    }
                }
            }
        }

        File lutDir = FileManager.sPHOTON_LUT_DIR;
        if (lutDir.exists() && lutDir.isDirectory()) {
            File[] files = lutDir.listFiles((dir, name) -> name.toLowerCase().endsWith("_lut.png"));

            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (!entryValues.contains(fileName)) {
                        entries.add(fileName);
                        entryValues.add(fileName);
                    }
                }
            }
        }

        int checkedItem = -1;
        for (int i = 0; i < entryValues.size(); i++) {
            if (entryValues.get(i).toString().equals(currentValue)) {
                checkedItem = i;
                break;
            }
        }

        CharSequence[] entriesArray = entries.toArray(new CharSequence[0]);
        CharSequence[] entryValuesArray = entryValues.toArray(new CharSequence[0]);

        new AlertDialog.Builder(context)
                .setTitle(R.string.color_lut)
                .setSingleChoiceItems(entriesArray, checkedItem, (dialog, which) -> {
                    String selectedValue = entryValuesArray[which].toString();
                    PhotonCamera.getSettings().lutName = selectedValue;
                    PreferenceKeys.setLutName(selectedValue);

                    dialog.dismiss();
                    if (uiController instanceof CameraUIController) {
                        ((CameraUIController) uiController).refreshCameraUI(true);
                    }
                    notifyChange();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void onGalleryLongClicked(View view, Object uiController) {
        Context context = view.getContext();
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();

        if (PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
            String currentVal = PreferenceKeys.getVideoCodec();
            CaptureController.EncoderInfoUtil encoderInfo = new CaptureController.EncoderInfoUtil();
            encoderInfo.getEncoderInfos();

            Size maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_AVC);
            boolean hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_AVC);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("AVC/H.264 (HW)");
                }
                else {
                    entries.add("AVC/H.264 (SW)");
                }
                entryValues.add("AVC");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_HEVC);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("HEVC/H.265 (HW)");
                }
                else {
                    entries.add("HEVC/H.265 (SW)");
                }
                entryValues.add("HEVC");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("Dolby Vision (HW)");
                }
                else {
                    entries.add("Dolby Vision (SW)");
                }
                entryValues.add("DOLBY_VISION");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_AV1);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_AV1);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("AV1 (HW)");
                }
                else {
                    entries.add("AV1 (SW)");
                }
                entryValues.add("AV1");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_APV);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_APV);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("APV (HW)");
                }
                else {
                    entries.add("APV (SW)");
                }
                entryValues.add("APV");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_VP8);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_VP8);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("VP8 (HW)");
                }
                else {
                    entries.add("VP8 (SW)");
                }
                entryValues.add("VP8");
            }
            maxRes = encoderInfo.getMaxResForMimeType(MediaFormat.MIMETYPE_VIDEO_VP9);
            hasHwSupport = encoderInfo.getHwSupportForMimeType(MediaFormat.MIMETYPE_VIDEO_VP9);
            if (maxRes != null) {
                if (hasHwSupport) {
                    entries.add("VP9 (HW)");
                }
                else {
                    entries.add("VP9 (SW)");
                }
                entryValues.add("VP9");
            }

            int checkedItem = -1;
            for (int i = 0; i < entryValues.size(); i++) {
                if (entryValues.get(i).toString().equals(currentVal)) {
                    checkedItem = i;
                    break;
                }
            }

            CharSequence[] entriesArray = entries.toArray(new CharSequence[0]);
            CharSequence[] entryValuesArray = entryValues.toArray(new CharSequence[0]);

            new AlertDialog.Builder(context)
                    .setTitle(R.string.video_codec)
                    .setSingleChoiceItems(entriesArray, checkedItem, (dialog, which) -> {
                        String selectedValue = entryValuesArray[which].toString();
                        PhotonCamera.getSettings().videoCodec = selectedValue;
                        PreferenceKeys.setVideoCodec(selectedValue);

                        dialog.dismiss();
                        if (uiController instanceof CameraUIController) {
                            ((CameraUIController) uiController).refreshCameraUI(true);
                        }
                        notifyChange();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
        else {
            String currentVal = String.valueOf(PreferenceKeys.getPreviewFormatValue());

            entries.add("JPEG");
            entryValues.add("256");
            if (PhotonCamera.mHeicIsSupported) {
                entries.add("HEIC");
                entryValues.add("1212500294");
            }
            if (PhotonCamera.mJpegRIsSupported) {
                entries.add("JPEG_R");
                entryValues.add("4101");
            }
            if (PhotonCamera.mHeicUltraHdrIsSupported) {
                entries.add("HEIC_ULTRA");
                entryValues.add("4102");
            }
            entries.add("AVIF (SW)");
            entryValues.add("999999999");
            entries.add("HEIC/HEIF (SW)");
            entryValues.add("999999991");
            entries.add("JPEG LUT (SW)");
            entryValues.add("999999992");
            if (PhotonCamera.mYuv10IsSupported) {
                entries.add("YUV RAW");
                entryValues.add("888888888");
            }
            entries.add("JPEG/RAW Stacking");
            entryValues.add("0");
            entries.add("Video Codec 8 Bit");
            entryValues.add("35");
            entries.add("Video Codec 10 Bit");
            entryValues.add("54");

            int checkedItem = -1;
            for (int i = 0; i < entryValues.size(); i++) {
                if (entryValues.get(i).toString().equals(currentVal)) {
                    checkedItem = i;
                    break;
                }
            }

            CharSequence[] entriesArray = entries.toArray(new CharSequence[0]);
            CharSequence[] entryValuesArray = entryValues.toArray(new CharSequence[0]);

            new AlertDialog.Builder(context)
                    .setTitle(R.string.preview_format)
                    .setSingleChoiceItems(entriesArray, checkedItem, (dialog, which) -> {
                        String selectedValue = entryValuesArray[which].toString();
                        PhotonCamera.getSettings().previewFormat = Integer.valueOf(selectedValue);
                        PreferenceKeys.setPreviewFormatValue(PhotonCamera.getSettings().previewFormat);

                        dialog.dismiss();
                        if (uiController instanceof CameraUIController) {
                            ((CameraUIController) uiController).refreshCameraUI(true);
                        }
                        notifyChange();
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    public void onDigitalZoomLongClicked(View view, Object uiController) {
        Context context = view.getContext();
        String[] entries = context.getResources().getStringArray(R.array.digital_zoom_factor_entries);
        String[] entryValues = context.getResources().getStringArray(R.array.digital_zoom_factor_entryValues);
        String currentVal = String.valueOf(PreferenceKeys.getDigitalZoomFactorValue()) + "f";

        int checkedItem = -1;
        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i].equals(currentVal)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.digital_zoom_factor)
                .setSingleChoiceItems(entries, checkedItem, (dialog, which) -> {
                    String selectedValue = entryValues[which];
                    if (selectedValue.equals("1.0f")) {
                        PreferenceKeys.setSetZoomOn(false);
                    }
                    else {
                        PreferenceKeys.setSetZoomOn(true);
                    }
                    PhotonCamera.getSettings().digitalZoomFactor = Float.valueOf(selectedValue);
                    PreferenceKeys.setDigitalZoomFactorValue(PhotonCamera.getSettings().digitalZoomFactor);

                    dialog.dismiss();
                    if (uiController instanceof CameraUIController) {
                        ((CameraUIController) uiController).refreshCameraUI(true);
                    }
                    notifyChange();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void onFunctionOneLongClicked(View view) {
        Context context = view.getContext();
        String[] entries = context.getResources().getStringArray(R.array.function_one_entries);
        String[] entryValues = context.getResources().getStringArray(R.array.function_one_entryValues);
        String currentVal = PreferenceKeys.getFunctionOneValue();

        List<CharSequence> entriesFunction = new ArrayList<>(Arrays.asList(entries));
        List<CharSequence> entryValuesFunction = new ArrayList<>(Arrays.asList(entryValues));

        if (PhotonCamera.hasXiaomiNight) {
            entriesFunction.add("Xiaomi Night Mode");
            entryValuesFunction.add("Xiaomi Night Mode");
        }
        if (PhotonCamera.hasXiaomiSuperNight) {
            entriesFunction.add("Xiaomi Super Night Mode");
            entryValuesFunction.add("Xiaomi Super Night Mode");
        }
        if (PhotonCamera.hasXiaomiAiAutoSceneDetection) {
            entriesFunction.add("Xiaomi AI Auto Scene Detection");
            entryValuesFunction.add("Xiaomi AI Auto Scene Detection");
        }
        if (PhotonCamera.hasXiaomiProVideoLog) {
            entriesFunction.add("Xiaomi Pro Video LOG");
            entryValuesFunction.add("Xiaomi Pro Video LOG");
        }
        if (PhotonCamera.hasXiaomiProVideoMovie) {
            entriesFunction.add("Xiaomi Pro Video Movie");
            entryValuesFunction.add("Xiaomi Pro Video Movie");
        }
        if (PhotonCamera.hasXiaomiReMosaic) {
            entriesFunction.add("Xiaomi Re-Mosaic");
            entryValuesFunction.add("Xiaomi Re-Mosaic");
        }
        if (PhotonCamera.hasXiaomiQuadCfa) {
            entriesFunction.add("Xiaomi Quad CFA");
            entryValuesFunction.add("Xiaomi Quad CFA");
        }
        if (PhotonCamera.hasXiaomiHdr) {
            entriesFunction.add("Xiaomi HDR");
            entryValuesFunction.add("Xiaomi HDR");
        }
        if (PhotonCamera.hasXiaomiSuperResolution) {
            entriesFunction.add("Xiaomi Super Resolution");
            entryValuesFunction.add("Xiaomi Super Resolution");
        }
        if (PhotonCamera.hasIdealRaw) {
            entriesFunction.add("Ideal RAW");
            entryValuesFunction.add("Ideal RAW");
        }
        if (PhotonCamera.hasEisLookAhead) {
            entriesFunction.add("EIS Look Ahead");
            entryValuesFunction.add("EIS Look Ahead");
        }
        if (PhotonCamera.hasEisV3) {
            entriesFunction.add("EIS V3");
            entryValuesFunction.add("EIS V3");
        }
        if (PhotonCamera.hasQucommAdrcOff) {
            entriesFunction.add("Qualcomm ADRC Off");
            entryValuesFunction.add("Qualcomm ADRC Off");
        }
        if (PhotonCamera.hasVivoZeissColor) {
            entriesFunction.add("Vivo Zeiss Color");
            entryValuesFunction.add("Vivo Zeiss Color");
        }
        if (PhotonCamera.hasVivoProMode) {
            entriesFunction.add("Vivo Pro Mode");
            entryValuesFunction.add("Vivo Pro Mode");
        }
        if (PhotonCamera.hasVivoDistortionCorrection) {
            entriesFunction.add("Vivo Distortion Correction");
            entryValuesFunction.add("Vivo Distortion Correction");
        }

        CharSequence[] finalEntries = entriesFunction.toArray(new CharSequence[0]);
        CharSequence[] finalValues = entryValuesFunction.toArray(new CharSequence[0]);

        int checkedItem = -1;
        for (int i = 0; i < finalValues.length; i++) {
            if (finalValues[i].toString().equals(currentVal)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(R.string.function_button_one)
                .setSingleChoiceItems(finalEntries, checkedItem, (dialog, which) -> {
                    String selectedValue = finalValues[which].toString();
                    PhotonCamera.getSettings().functionOne = selectedValue;
                    PreferenceKeys.setFunctionOneValue(selectedValue);

                    dialog.dismiss();
                    notifyChange();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public void onMagnifierLongClicked(View view) {
        if (PhotonCamera.getCaptureController() != null) {
            PhotonCamera.getCaptureController().setAutoExposureCenter();
        }
    }

    public final SeekBar.OnSeekBarChangeListener zoomChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                //onZoomProgressChanged(progress, fromUser);
                float linear_fraction = (float) (progress - 5) / 95.0f;
                float curved_fraction = (float) Math.pow(linear_fraction, 2.5);
                float newZoom = 0.5f + (9.5f * curved_fraction);
                PhotonCamera.getCaptureController().zoomSliderChanged(newZoom);
                zoomLevel.setValue(newZoom);
                notifyPropertyChanged(BR.zoomLevel);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            onZoomStartTracking();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            onZoomChanged(seekBar.getProgress());
        }
    };

    @Bindable
    public Float getZoomLevel() {
        return zoomLevel.getValue() != null ? zoomLevel.getValue() : 1.0f;
    }

    public void onZoomChanged(int progress) {
        /*if (PhotonCamera.getCaptureController() != null) {
            PhotonCamera.getCaptureController().zoomSliderChanged((float)(progress / 10.0f));
        }*/
    }

    public void onZoomStartTracking() {

    }

    public void onZoomProgressChanged(int progress, boolean fromUser) {
        if (fromUser) {
            if (PhotonCamera.getCaptureController() != null) {
                PhotonCamera.getCaptureController().zoomSliderChanged((float)(progress / 10.0f));
            }
        }
    }

    @Bindable
    public boolean isViewfinderMagnified() {
        return viewfinderMaginified;
    }

    public boolean isFunctionOneOn() {
        return functionOneOn;
    }

    @Bindable
    public float getScreenAspectRatio() {
        return screenAspectRatio;
    }

    public void setScreenAspectRatio(float screenAspectRatio) {
        this.screenAspectRatio = screenAspectRatio;
        notifyPropertyChanged(BR.screenAspectRatio);
    }

    @Bindable
    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        notifyChange();
    }

    @Bindable
    public int getOrientation() {
        return orientation;
    }

    /**
     * set the orientation and note the binded views about the change
     *
     * @param orientation
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
        notifyChange();
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    @Bindable
    public boolean isSettingsBarVisibility() {
        return settingsBarVisibility;
    }

    public void setSettingsBarVisibility(boolean settingsBarVisibility) {
        this.settingsBarVisibility = settingsBarVisibility;
        notifyChange();
    }
    
    @Bindable
    public String getDummyAspectRatio() {
        return dummyAspectRatio;
    }
    
    public void setDummyAspectRatio(String dummyAspectRatio) {
        this.dummyAspectRatio = dummyAspectRatio;
        notifyPropertyChanged(BR.dummyAspectRatio);
    }
}

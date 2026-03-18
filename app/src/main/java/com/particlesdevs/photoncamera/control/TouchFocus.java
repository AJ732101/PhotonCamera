package com.particlesdevs.photoncamera.control;

import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;

import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.Log;
import android.util.Size;
import android.view.View;
import android.view.View.OnTouchListener;

import androidx.annotation.Nullable;

import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.ui.camera.views.FocusCircleView;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.GLPreview;

public class TouchFocus {
    private static final String TAG = "TouchFocus";
    private static final int AUTO_HIDE_DELAY_MS = 3000;
    private final CaptureController captureController;
    private final GLPreview textureView;
    private final View focusCircleView;
    private final Runnable hideFocusCircleRunnable = this::hideFocusCircleView;
    public boolean isTouchFocus = false;
    private final OnTouchListener focusListener = (v, event) -> {
        v.performClick();
        resetFocusCircle();
        setInitialAFAE();
        return true;
    };


    public TouchFocus(CaptureController captureController, View focusCircle, GLPreview textureView) {
        this.captureController = captureController;
        this.focusCircleView = focusCircle;
        this.textureView = textureView;
        focusCircleView.setOnTouchListener(focusListener);
        resetFocusCircle();
    }

    public void processTouchToFocus(float fx, float fy) {
        focusCircleView.removeCallbacks(hideFocusCircleRunnable);
        focusCircleView.post(() -> showFocusCircle(fx, fy));
        setFocus((int) fy, (int) fx);
        focusCircleView.postDelayed(hideFocusCircleRunnable, AUTO_HIDE_DELAY_MS);
    }

    private void showFocusCircle(float fx, float fy) {
        focusCircleView.setX(fx - focusCircleView.getMeasuredWidth() / 2.0f);
        focusCircleView.setY(fy - focusCircleView.getMeasuredHeight() / 2.0f);
        focusCircleView.setVisibility(View.VISIBLE);
        focusCircleView.animate().scaleY(1.2f).scaleX(1.2f).setDuration(250)
                .withEndAction(() -> focusCircleView.animate().scaleY(1f).scaleX(1f).setDuration(250).start())
                .start();
    }

    /**
     * Sets state of focus circle view based on AF State
     */
    public void setState(@Nullable Integer afstate) {
        if (afstate != null) {
            ((FocusCircleView) focusCircleView).setAfState(afstate);
        }
    }

    private void setInitialAFAE() {
        captureController.reset3Aparams();
    }

    private void setFocus(int x, int y) {
        Point size = new Point(captureController.mImageReaderPreview.getWidth(), captureController.mImageReaderPreview.getHeight());
        Point CurUi = new Point(textureView.getWidth(),textureView.getHeight());
        Size sizee = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
        if (sizee == null) {
            sizee = new Size(size.x, size.y);
        }
        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;
        Log.v(TAG,"y:"+y);
        /*if (y > CurUi.y)
            y = CurUi.y;
        if (x > CurUi.x)
            x  =CurUi.x;*/
        //use 1/6 from the the sensor size for the focus rect
        int width_to_set = sizee.getWidth() / 8;
        float kProp = (float) CurUi.x / (float) (CurUi.y);
        int height_to_set = (int) (width_to_set * kProp);
        float x_scale = (float) sizee.getWidth() / (float) CurUi.y;
        float y_scale = (float) sizee.getHeight() / (float) CurUi.x;
        int x_to_set = (int) (x * x_scale) - width_to_set / 2;
        int y_to_set = (int) (y * y_scale) - height_to_set / 2;
        Log.v(TAG,"y_to_set:"+y_to_set);
        y_to_set = sizee.getHeight()-y_to_set-height_to_set;
        if (x_to_set < 0)
            x_to_set = 0;
        if (y_to_set < 0)
            y_to_set = 0;
        if (y_to_set - height_to_set > sizee.getHeight())
            y_to_set = sizee.getHeight() - height_to_set;
        if (x_to_set - width_to_set > sizee.getWidth())
            y_to_set = sizee.getWidth() - width_to_set;

        MeteringRectangle rect_to_set = new MeteringRectangle(x_to_set, y_to_set, width_to_set, height_to_set, MeteringRectangle.METERING_WEIGHT_MAX - 1);
        MeteringRectangle[] rectaf = new MeteringRectangle[1];
        Log.v(TAG, "\nInput x/y:" + x + "/" + y + "\n" +
                "sensor size width/height to set:" + width_to_set + "/" + height_to_set + "\n" +
                "preview/sensorsize: " + CurUi.toString() + " / " + sizee.toString() + "\n" +
                "scale x/y:" + x_scale + "/" + y_scale + "\n" +
                "final rect :" + rect_to_set.toString());
        rectaf[0] = rect_to_set;
        triggerAutoFocus(rectaf);
    }

    public boolean isSingleShotJpegOrHeic() {
        if ((PhotonCamera.getSettings().frameCount == 1) &&
           ((PhotonCamera.getSettings().previewFormat == ImageFormat.HEIC) ||
            (PhotonCamera.getSettings().previewFormat == ImageFormat.JPEG) ||
            (PhotonCamera.getSettings().previewFormat == ImageFormat.JPEG_R) ||
            (PhotonCamera.getSettings().previewFormat == ImageFormat.HEIC_ULTRAHDR) ||
            (PhotonCamera.getSettings().previewFormat == ImageFormat.YCBCR_P010) ||
            (PhotonCamera.getSettings().previewFormat == ImageFormat.YUV_420_888) ||
            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatAvifSw) ||
            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatHeifSw) ||
            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatJpegLutSw) ||
            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatPngSw) ||
            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLossySw) ||
            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatWebpLosslessSw) ||
            (PhotonCamera.getSettings().previewFormat == PhotonCamera.userFormatYuvRaw)) &&
            (PhotonCamera.getSettings().rawSaver != 2) &&
            !PhotonCamera.getSettings().selectedMode.equals(CameraMode.VIDEO)) {
           return true;
        }
        return false;
    }

    private void triggerAutoFocus(MeteringRectangle[] rectaf) {
        if(CaptureController.burst) return;
        CaptureRequest.Builder builder = captureController.mPreviewRequestBuilder;
        if (builder == null) {
            Log.w(TAG, "triggerAutoFocus(): mPreviewRequestBuilder is null");
            return;
        }
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        //builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        if (isSingleShotJpegOrHeic() && PhotonCamera.getSettings().useSceneAndEffectMode) {
            if (!captureController.getParamController().isManualMode()) {
                builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
                switch (PhotonCamera.getSettings().selectedMode) {
                    case NIGHT:
                        builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_NIGHT);
                        if (PhotonCamera.getSettings().effectMode != 0) {
                            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, PhotonCamera.getSettings().effectMode);
                        }
                        break;
                    case MOTION:
                        builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_SPORTS);
                        if (PhotonCamera.getSettings().effectMode != 0) {
                            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, PhotonCamera.getSettings().effectMode);
                        }
                        break;
                    case PHOTO:
                        builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_HDR);
                        if (PhotonCamera.getSettings().effectMode != 0) {
                            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, PhotonCamera.getSettings().effectMode);
                        }
                        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
                        break;
                }
            }
        }
        else {
            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        }
        captureController.rebuildPreviewBuilderOneShot();
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, rectaf);
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, rectaf);
        builder.set(CaptureRequest.CONTROL_AF_MODE, PreferenceKeys.getAfMode());
        builder.set(CaptureRequest.CONTROL_AE_MODE, Math.max(PreferenceKeys.getAeMode(), 1));
        //set focus area repeating,else cam forget after one frame where it should focus
        //trigger af start only once. cam starts focusing till its focused or failed
        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        captureController.rebuildPreviewBuilderOneShot();
        //set focus trigger back to idle to signal cam after focusing is done to do nothing
        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
        captureController.rebuildPreviewBuilderOneShot();
        captureController.rebuildPreviewBuilder();
        isTouchFocus = true;
    }
    private void resetAutoFocus() {
        if (CaptureController.burst) {
            return;
        }
        if (captureController == null) {
            return;
        }

        CaptureRequest.Builder builder = captureController.mPreviewRequestBuilder;
        if (builder == null) {
            Log.w(TAG, "triggerAutoFocus(): mPreviewRequestBuilder is null");
            return;
        }
        Log.d(TAG, "resetAutoFocus");
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, captureController.mPreviewMeteringAF);
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, captureController.mPreviewMeteringAE);
        builder.set(CaptureRequest.CONTROL_AF_MODE, captureController.mPreviewAFMode);
        builder.set(CaptureRequest.CONTROL_AE_MODE, captureController.mPreviewAEMode);
        captureController.rebuildPreviewBuilderOneShot();
        captureController.rebuildPreviewBuilder();
        isTouchFocus = false;
    }


    //Thread safe
    //call when focus circle needs to be hidden immediately
    public void resetFocusCircle() {
        focusCircleView.removeCallbacks(hideFocusCircleRunnable);
        focusCircleView.post(hideFocusCircleRunnable);
        resetAutoFocus();
    }

    //Must be run on UI Thread
    private void hideFocusCircleView() {
        if (focusCircleView.getVisibility() == View.VISIBLE) {
            focusCircleView.animate().alpha(0f).scaleY(1.8f).scaleX(1.8f).setDuration(100)
                    .withEndAction(() -> {
                        focusCircleView.setVisibility(View.GONE);
                        focusCircleView.setX((float) textureView.getWidth() / 2.f);
                        focusCircleView.setY((float) textureView.getHeight() / 2.f);
                        focusCircleView.setScaleY(1f);
                        focusCircleView.setScaleX(1f);
                        focusCircleView.setAlpha(1f);
                    })
                    .start();
        }
    }
}

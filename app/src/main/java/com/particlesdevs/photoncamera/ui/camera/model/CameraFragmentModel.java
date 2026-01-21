package com.particlesdevs.photoncamera.ui.camera.model;

import android.graphics.Bitmap;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.lifecycle.MutableLiveData;

import com.particlesdevs.photoncamera.BR;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.util.Log;

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

package com.particlesdevs.photoncamera.manual;
/*
 * Class responsible for setting manual mode parameters to the camera preview.
 *
 *         Copyright (C) 2021  Vibhor Srivastava
 *         This program is free software: you can redistribute it and/or modify
 *         it under the terms of the GNU General Public License as published by
 *         the Free Software Foundation, either version 3 of the License, or
 *         (at your option) any later version.
 *
 *         This program is distributed in the hope that it will be useful,
 *         but WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *         GNU General Public License for more details.
 *
 *         You should have received a copy of the GNU General Public License
 *         along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.hardware.camera2.CaptureRequest;

import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.util.Log;

import androidx.annotation.NonNull;

import com.particlesdevs.photoncamera.circularbarlib.control.ManualParamModel;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.processing.parameters.ExposureIndex;

import java.util.Observable;
import java.util.Observer;

/**
 * Observer class for {@link ManualParamModel}
 * This class is responsible for setting manual parameters to the camera preview
 * <p>
 * Created by Vibhor Srivastava on 07/Jan/2021
 */
public class ParamController implements Observer {
    public int ISO = -1;
    public int EV = 0;
    public long SHUTTER = -1;
    public float FOCUS = -1;
    private static final String TAG = "ParamController";
    private final CaptureController captureController;
    private ManualParamModel manualParamModel;

    public ParamController(@NonNull CaptureController captureController) {
        this.captureController = captureController;
    }

    public void setShutter(long shutterNs, int currentISO) {
        CaptureRequest.Builder builder = captureController.mPreviewRequestBuilder;
        if (builder == null) {
            Log.w(TAG, "setShutter(): mPreviewRequestBuilder is null");
            return;
        }
        if (shutterNs == ManualParamModel.EXPOSURE_AUTO) {
            if (currentISO == ManualParamModel.ISO_AUTO)//check if ISO is Auto
            {
                captureController.resetPreviewAEMode();
            }
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, Math.min(shutterNs, ExposureIndex.sec / 5));
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, captureController.mPreviewIso);
        }
        captureController.rebuildPreviewBuilder();
    }

    public void setISO(int isoVal, double currentExposure) {
        CaptureRequest.Builder builder = captureController.mPreviewRequestBuilder;
        if (builder == null) {
            Log.w(TAG, "setISO(): mPreviewRequestBuilder is null");
            return;
        }
        if (isoVal == ManualParamModel.ISO_AUTO) {
            if (currentExposure == ManualParamModel.EXPOSURE_AUTO) //check if Exposure is Auto
            {
                captureController.resetPreviewAEMode();
            }
        } else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, isoVal);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, captureController.mPreviewExposureTime);
        }
        captureController.rebuildPreviewBuilder();
    }

    public void setFocus(float focusDist) {
        CaptureRequest.Builder builder = captureController.mPreviewRequestBuilder;
        if (builder == null) {
            Log.w(TAG, "setFocus(): mPreviewRequestBuilder is null");
            return;
        }
        if (focusDist == ManualParamModel.FOCUS_AUTO) {
            builder.set(CaptureRequest.CONTROL_AF_MODE, PreferenceKeys.getAfMode());
        } else {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDist);
        }
        captureController.rebuildPreviewBuilder();
    }

    public void setEV(int ev) {
        CaptureRequest.Builder builder = captureController.mPreviewRequestBuilder;
        if (builder == null) {
            Log.w(TAG, "setEV(): mPreviewRequestBuilder is null");
            return;
        }
        builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ev);
        captureController.rebuildPreviewBuilder();
    }

    public boolean isManualMode() {
        if (manualParamModel != null)
            return manualParamModel.isManualMode();
        return false;
    }

    @Override
    public void update(Observable observable, Object object) {
        if (observable != null && object != null) {
            ManualParamModel model = (ManualParamModel) observable;
            manualParamModel = model;
            if (object.equals(ManualParamModel.ID_ISO)) {
                ISO = (int) model.getCurrentISOValue();
                setISO((int) model.getCurrentISOValue(), model.getCurrentExposureValue());
            }
            if (object.equals(ManualParamModel.ID_EV)) {
                EV = (int) model.getCurrentEvValue();
                setEV((int) model.getCurrentEvValue());
            }
            if (object.equals(ManualParamModel.ID_SHUTTER)) {
                SHUTTER = (long) model.getCurrentExposureValue();
                setShutter((long) model.getCurrentExposureValue(), (int) model.getCurrentISOValue());
            }
            if (object.equals(ManualParamModel.ID_FOCUS)) {
                FOCUS = (float) model.getCurrentFocusValue();
                setFocus((float) model.getCurrentFocusValue());
            }
            if(object.equals(ManualParamModel.PANEL_INVISIBILITY)) {
                Log.d(TAG, "update: " + model.isManualMode());
                ISO = -1;
                EV = 0;
                SHUTTER = -1;
                FOCUS = -1;
                captureController.unlockFocus();
            }
        }
    }

    public void setupPreview() {
        if (manualParamModel != null) {
            if(ISO != -1)
                setISO(ISO, manualParamModel.getCurrentExposureValue());
            if(EV != 0)
                setEV(EV);
            if(SHUTTER != -1)
                setShutter(SHUTTER, ISO);
            if(FOCUS != -1)
                setFocus(FOCUS);
        }
    }

    public double getCurrentExposureValue() {
        if (manualParamModel != null)
            return manualParamModel.getCurrentExposureValue();
        return ManualParamModel.EXPOSURE_AUTO;
    }

    public double getCurrentISOValue() {
        if (manualParamModel != null)
            return manualParamModel.getCurrentISOValue();
        return ManualParamModel.ISO_AUTO;
    }
}

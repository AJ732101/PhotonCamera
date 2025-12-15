package com.particlesdevs.photoncamera.control;

import android.hardware.Sensor;import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Provides device orientation data that is robust and specifically corrected for
 * a camera use-case by remapping the coordinate system. This avoids Gimbal Lock issues
 * when the device is held upright.
 */
public class HorizonAndGear implements SensorEventListener {

    private final SensorManager mSensorManager;
    private final Sensor mRotationVectorSensor;
    private final float[] mRotationVector = new float[5]; // Use 5 for compatibility

    private static final int MOVING_AVERAGE_SIZE = 15;
    private final Queue<Float> mYawHistory = new LinkedList<>();
    private final Queue<Float> mPitchHistory = new LinkedList<>();
    private final Queue<Float> mRollHistory = new LinkedList<>();
    private float mYawSum = 0f;
    private float mPitchSum = 0f;
    private float mRollSum = 0f;

    public HorizonAndGear(SensorManager sensorManager) {
        mSensorManager = sensorManager;
        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public void register() {
        if (mRotationVectorSensor != null) {
            mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
        mYawHistory.clear();
        mPitchHistory.clear();
        mRollHistory.clear();
        mYawSum = 0f;
        mPitchSum = 0f;
        mRollSum = 0f;
    }

    public void unregister() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            System.arraycopy(event.values, 0, mRotationVector, 0, event.values.length);
            updateHistory();
        }
    }

    private void updateHistory() {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, mRotationVector);

        // --- THIS IS THE FINAL, CORRECT, AND CRITICAL FIX ---
        // We remap the coordinate system to handle the device being held vertically
        // for photography. This is the standard way to solve the "Gimbal Lock" issue
        // for a camera app.
        float[] remappedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix,
                SensorManager.AXIS_X, SensorManager.AXIS_Z,
                remappedRotationMatrix);
        // --- END OF CRITICAL FIX ---

        float[] orientationAngles = new float[3];
        // Now, get the orientation from the REMAPPED matrix.
        SensorManager.getOrientation(remappedRotationMatrix, orientationAngles);

        // After remapping, the angles correspond correctly to the camera's view.
        float currentYaw = (float) Math.toDegrees(orientationAngles[0]);
        float currentPitch = (float) Math.toDegrees(orientationAngles[1]);
        float currentRoll = (float) Math.toDegrees(orientationAngles[2]);

        // Update Yaw history
        mYawHistory.add(currentYaw);
        mYawSum += currentYaw;
        if (mYawHistory.size() > MOVING_AVERAGE_SIZE) {
            mYawSum -= mYawHistory.poll();
        }

        // Update Pitch history
        mPitchHistory.add(currentPitch);
        mPitchSum += currentPitch;
        if (mPitchHistory.size() > MOVING_AVERAGE_SIZE) {
            mPitchSum -= mPitchHistory.poll();
        }

        // Update Roll history
        mRollHistory.add(currentRoll);
        mRollSum += currentRoll;
        if (mRollHistory.size() > MOVING_AVERAGE_SIZE) {
            mRollSum -= mRollHistory.poll();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    /**
     * @return The damped Yaw angle (azimuth/compass) in DEGREES.
     */
    public float getYaw() {
        if (mYawHistory.isEmpty()) return 0f;
        return mYawSum / mYawHistory.size();
    }

    /**
     * @return The damped Pitch angle (sky/ground tilt) in DEGREES.
     */
    public float getPitch() {
        if (mPitchHistory.isEmpty()) return 0f;
        return mPitchSum / mPitchHistory.size();
    }

    /**
     * @return The damped Roll angle (steering wheel motion) in DEGREES.
     */
    public float getRoll() {
        if (mRollHistory.isEmpty()) return 0f;
        return mRollSum / mRollHistory.size();
    }
}

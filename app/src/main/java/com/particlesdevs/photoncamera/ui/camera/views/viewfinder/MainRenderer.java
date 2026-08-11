package com.particlesdevs.photoncamera.ui.camera.views.viewfinder;

import static com.particlesdevs.photoncamera.util.FileManager.sPHOTON_TUNING_DIR;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.processing.opengl.GLImage;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;
import com.particlesdevs.photoncamera.util.Log;
import com.particlesdevs.photoncamera.util.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.util.function.Consumer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class MainRenderer implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private int[] hTex;
    private GLTexture hTexLut;
    private final FloatBuffer pVertex;
    private final FloatBuffer pTexCoord;
    private final float[] mSTMatrix = new float[16];
    private final float[] mTexRotateMatrix = new float[16];
    private int mNormalProgram;
    private int mMagnifyProgram;
    private int mLutProgram;
    private volatile boolean mIsMagnifyEnabled = false;
    private volatile boolean mIsLutEnabled = false;
    private boolean mGLInit = false;
    private boolean mUpdateST = false;
    private File currentLutFile = null;
    private boolean lutUpdateNeeded = false;
    private int lutSize = 0;
    private SurfaceTexture mSTexture;
    private final GLPreview mView;
    private int uTexRotateMatrix_Normal, uTexRotateMatrix_Magnify, uTexRotateMatrix_Lut;
    private int uSTMatrix_Normal, uSTMatrix_Magnify, uSTMatrix_Lut;
    private int vPosition_Normal, vPosition_Magnify, vPosition_Lut;
    private int vTexCoord_Normal, vTexCoord_Magnify, vTexCoord_Lut;
    private int enablePeak_Normal, enablePeak_Magnify;
    private int resolutionLocation_Normal, resolutionLocation_Magnify;
    private int uPostLutSize, uPostLutSizeTiles;
    private int uBinning_Normal;
    private int uCameraResolution_Normal;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mCameraWidth = 1920;
    private int mCameraHeight = 1080;
    private FloatBuffer mOffscreenVertexBuffer;
    private FloatBuffer mOffscreenTexCoordBuffer;
    private FloatBuffer mOffscreenTexCoordBufferRotated;
    private int mYuvToRgbProgram;
    private int[] mFboId = new int[1];
    private int[] mRboId = new int[1];
    private int[] mFboTextureId = new int[1];
    private int mYuvPositionHandle;
    private int mYuvTexCoordHandle;
    private int mYTextureHandle;
    private int mUTextureHandle;
    private int mVTextureHandle;
    private int mImageWidth;
    private int mImageHeight;
    private final Context mContext;
    private static final String TAG = MainRenderer.class.getSimpleName();
    private final float[] mOffscreenRotationMatrix = new float[16];
    private int uOffscreenTexRotateMatrixHandle_YUV;
    private int uOffscreenTexRotateMatrixHandle_LUT;

    private int mP010ToF16Program;
    private int mP010_vPosition;
    private int mP010_aTexCoord;
    private int mP010_Ytex, mP010_Utex, mP010_Vtex;
    private int uOffscreenTexRotateMatrixHandle_P010;

    private int mP010ToF16NoClampProgram;
    private int mP010NoClamp_vPosition;
    private int mP010NoClamp_aTexCoord;
    private int mP010NoClamp_Ytex, mP010NoClamp_Utex, mP010NoClamp_Vtex;
    private int uOffscreenTexRotateMatrixHandle_P010NoClamp;

    private int mLutNoClampProgram;
    private int vPosition_LutNoClamp;
    private int vTexCoord_LutNoClamp;
    private int uPostLutSizeNoClamp;
    private int uPostLutSizeTilesNoClamp;
    private int uOffscreenTexRotateMatrixHandle_LUTNoClamp;

    MainRenderer(GLPreview view) {
        mView = view;
        mContext = view.getContext();
        pVertex = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        float[] vtmp = {1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f};
        pVertex.put(vtmp);
        pVertex.position(0);
        pTexCoord = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        float[] ttmp = {1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};
        pTexCoord.put(ttmp);
        pTexCoord.position(0);
        setOrientation(180);
    }

    public void setVideoRecordingSurface(Surface surface, int width, int height) {

    }

    public void setFrameTimestamp(long timestamp) {

    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (!mGLInit) return;
        synchronized (this) {
            if (mUpdateST) {
                mSTexture.updateTexImage();
                mSTexture.getTransformMatrix(mSTMatrix);
                mUpdateST = false;
            }
            if (lutUpdateNeeded) {
                updateLutTexture();
            }
        }
        GLES20.glViewport(0, 0, mScreenWidth, mScreenHeight);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        int currentProgram;
        int uTexRotateMatrixHandle;
        int uSTMatrixHandle;
        int vPositionHandle;
        int vTexCoordHandle;
        if (mIsMagnifyEnabled) {
            currentProgram = mMagnifyProgram;
            uTexRotateMatrixHandle = uTexRotateMatrix_Magnify;
            uSTMatrixHandle = uSTMatrix_Magnify;
            vPositionHandle = vPosition_Magnify;
            vTexCoordHandle = vTexCoord_Magnify;
        } else if (mIsLutEnabled && hTexLut != null) {
            currentProgram = mLutProgram;
            uTexRotateMatrixHandle = uTexRotateMatrix_Lut;
            uSTMatrixHandle = uSTMatrix_Lut;
            vPositionHandle = vPosition_Lut;
            vTexCoordHandle = vTexCoord_Lut;
        } else {
            currentProgram = mNormalProgram;
            uTexRotateMatrixHandle = uTexRotateMatrix_Normal;
            uSTMatrixHandle = uSTMatrix_Normal;
            vPositionHandle = vPosition_Normal;
            vTexCoordHandle = vTexCoord_Normal;
        }
        if (currentProgram == 0) return;
        GLES20.glUseProgram(currentProgram);
        if (currentProgram == mNormalProgram) {
            int isQuad = ((PhotonCamera.getSettings().sensorModeCfaPattern == -2) && PhotonCamera.isSensorModeOn) ? 1 : 0;
            GLES20.glUniform1i(uBinning_Normal, isQuad);
            GLES20.glUniform2f(uCameraResolution_Normal, (float) mCameraWidth, (float) mCameraHeight);
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, hTex[0]);
        if (mIsLutEnabled && !mIsMagnifyEnabled && hTexLut != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hTexLut.mTextureID);
            double lutSizeDouble = (double) lutSize;
            float postLutSizeTilesVal = (float) Math.round(Math.pow(lutSizeDouble, 1.0 / 3.0));
            float postLutSizeVal = (float) (lutSizeDouble / postLutSizeTilesVal);
            GLES20.glUniform1f(uPostLutSize, postLutSizeVal);
            GLES20.glUniform1f(uPostLutSizeTiles, postLutSizeTilesVal);
        }
        GLES20.glUniformMatrix4fv(uTexRotateMatrixHandle, 1, false, mTexRotateMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, mSTMatrix, 0);
        GLES20.glVertexAttribPointer(vPositionHandle, 2, GLES20.GL_FLOAT, false, 8, pVertex);
        GLES20.glEnableVertexAttribArray(vPositionHandle);
        GLES20.glVertexAttribPointer(vTexCoordHandle, 2, GLES20.GL_FLOAT, false, 8, pTexCoord);
        GLES20.glEnableVertexAttribArray(vTexCoordHandle);
        if (mIsMagnifyEnabled) {
            GLES20.glUniform1i(enablePeak_Magnify, PhotonCamera.getSettings().focusPeak);
            GLES20.glUniform2f(resolutionLocation_Magnify, (float)mView.getWidth(), (float)mView.getHeight());
        } else if (currentProgram == mNormalProgram) {
            GLES20.glUniform1i(enablePeak_Normal, PhotonCamera.getSettings().focusPeak);
            GLES20.glUniform2f(resolutionLocation_Normal, (float)mView.getWidth(), (float)mView.getHeight());
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(vPositionHandle);
        GLES20.glDisableVertexAttribArray(vTexCoordHandle);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initTex();

        String p010_vs = "";
        File p010_vsFile = new File(sPHOTON_TUNING_DIR, "p010_to_f16_vs.glsl");
        if (p010_vsFile.exists()) {
            try {
                p010_vs = new String(Files.readAllBytes(p010_vsFile.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "p010_to_f16_vs " + e.getMessage());
            }
        }
        else {
            p010_vs = PhotonCamera.getAssetLoader().getString("shaders/lut/p010_to_f16_vs.glsl");
        }

        String p010_fs = "";
        File prevFs = new File(sPHOTON_TUNING_DIR, "p010_to_f16_fs.glsl");
        if (prevFs.exists()) {
            try {
                p010_fs = new String(Files.readAllBytes(prevFs.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "p010_to_f16_fs " + e.getMessage());
            }
        }
        else {
            p010_fs = PhotonCamera.getAssetLoader().getString("shaders/lut/p010_to_f16_fs.glsl");
        }

        mP010ToF16Program = loadShader(p010_vs, p010_fs);
        Log.d(TAG, "mP010ToF16Program ID: " + mP010ToF16Program);

        mP010_vPosition = GLES20.glGetAttribLocation(mP010ToF16Program, "aPosition");
        mP010_aTexCoord = GLES20.glGetAttribLocation(mP010ToF16Program, "aTexCoord");
        mP010_Ytex = GLES20.glGetUniformLocation(mP010ToF16Program, "y_texture");
        mP010_Utex = GLES20.glGetUniformLocation(mP010ToF16Program, "u_texture");
        uOffscreenTexRotateMatrixHandle_P010 = GLES20.glGetUniformLocation(mP010ToF16Program, "uTexRotateMatrix");

        mP010_Vtex = mP010_Utex;

        String p010_fs_noclamp = "";
        File p010_fs_noclampFile = new File(sPHOTON_TUNING_DIR, "p010_to_f16_noclamp_fs.glsl");
        if (p010_fs_noclampFile.exists()) {
            try {
                p010_fs_noclamp = new String(Files.readAllBytes(p010_fs_noclampFile.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "p010_to_f16_noclamp_fs " + e.getMessage());
            }
        }
        else {
            p010_fs_noclamp = PhotonCamera.getAssetLoader().getString("shaders/lut/p010_to_f16_noclamp_fs.glsl");
        }

        String simple_vs = "";
        File simple_vsFile = new File(sPHOTON_TUNING_DIR, "simple_vs.glsl");
        if (simple_vsFile.exists()) {
            try {
                simple_vs = new String(Files.readAllBytes(simple_vsFile.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "simple_vs " + e.getMessage());
            }
        }
        else {
            simple_vs = PhotonCamera.getAssetLoader().getString("shaders/lut/simple_vs.glsl");
        }


        mP010ToF16NoClampProgram = loadShader(simple_vs, p010_fs_noclamp);
        Log.d(TAG, "mP010ToF16Program ID: " + mP010ToF16Program);

        mP010NoClamp_vPosition = GLES20.glGetAttribLocation(mP010ToF16NoClampProgram, "aPosition");
        mP010NoClamp_aTexCoord = GLES20.glGetAttribLocation(mP010ToF16NoClampProgram, "aTexCoord");
        mP010NoClamp_Ytex = GLES20.glGetUniformLocation(mP010ToF16NoClampProgram, "y_texture");
        mP010NoClamp_Utex = GLES20.glGetUniformLocation(mP010ToF16NoClampProgram, "u_texture");
        uOffscreenTexRotateMatrixHandle_P010NoClamp = GLES20.glGetUniformLocation(mP010ToF16NoClampProgram, "uTexRotateMatrix");
        mP010NoClamp_Vtex = mP010NoClamp_Utex;

        mSTexture = new SurfaceTexture(hTex[0]);
        mSTexture.setOnFrameAvailableListener(this);

        String vss_default = "";
        File prevVs = new File(sPHOTON_TUNING_DIR, "main_vs.glsl");
        if (prevVs.exists()) {
            try {
                vss_default = new String(Files.readAllBytes(prevVs.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "main_vs " + e.getMessage());
            }
        }
        else {
            vss_default = PhotonCamera.getAssetLoader().getString("shaders/preview/main_vs.glsl");
        }

        String fss_default = PhotonCamera.getAssetLoader().getString("shaders/preview/main_fs.glsl");
        File defaultFs = new File(sPHOTON_TUNING_DIR, "main_fs.glsl");
        if (defaultFs.exists()) {
            try {
                fss_default = new String(Files.readAllBytes(defaultFs.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "fss_default " + e.getMessage());
            }
        }
        else {
            fss_default = PhotonCamera.getAssetLoader().getString("shaders/preview/main_fs.glsl");
        }

        String fss_magnify = "";
        File magFs = new File(sPHOTON_TUNING_DIR, "main_magnification_fs.glsl");
        if (magFs.exists()) {
            try {
                fss_magnify = new String(Files.readAllBytes(magFs.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "fss_magnify " + e.getMessage());
            }
        }
        else {
            fss_magnify = PhotonCamera.getAssetLoader().getString("shaders/preview/main_magnification_fs.glsl");
        }

        String fss_lut = "";
        File lutFs = new File(sPHOTON_TUNING_DIR, "main_lut_fs.glsl");
        if (lutFs.exists()) {
            try {
                fss_lut = new String(Files.readAllBytes(lutFs.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "fss_lut " + e.getMessage());
            }
        }
        else {
            fss_lut = PhotonCamera.getAssetLoader().getString("shaders/preview/main_lut_fs.glsl");
        }

        mNormalProgram = loadShader(vss_default, fss_default);
        mMagnifyProgram = loadShader(vss_default, fss_magnify);
        mLutProgram = loadShader(vss_default, fss_lut);
        Log.d(TAG, "mNormalProgram ID: " + mNormalProgram);
        Log.d(TAG, "mMagnifyProgram ID: " + mMagnifyProgram);
        Log.d(TAG, "mLutProgram ID: " + mLutProgram);

        uTexRotateMatrix_Normal = GLES20.glGetUniformLocation(mNormalProgram, "uTexRotateMatrix");
        uSTMatrix_Normal = GLES20.glGetUniformLocation(mNormalProgram, "uSTMatrix");
        vPosition_Normal = GLES20.glGetAttribLocation(mNormalProgram, "vPosition");
        vTexCoord_Normal = GLES20.glGetAttribLocation(mNormalProgram, "vTexCoord");
        enablePeak_Normal = GLES20.glGetUniformLocation(mNormalProgram, "enablePeak");
        resolutionLocation_Normal = GLES20.glGetUniformLocation(mNormalProgram, "resolution");
        uBinning_Normal = GLES20.glGetUniformLocation(mNormalProgram, "binning");
        uCameraResolution_Normal = GLES20.glGetUniformLocation(mNormalProgram, "uCameraResolution");

        uTexRotateMatrix_Magnify = GLES20.glGetUniformLocation(mMagnifyProgram, "uTexRotateMatrix");
        uSTMatrix_Magnify = GLES20.glGetUniformLocation(mMagnifyProgram, "uSTMatrix");
        vPosition_Magnify = GLES20.glGetAttribLocation(mMagnifyProgram, "vPosition");
        vTexCoord_Magnify = GLES20.glGetAttribLocation(mMagnifyProgram, "vTexCoord");
        enablePeak_Magnify = GLES20.glGetUniformLocation(mMagnifyProgram, "enablePeak");
        resolutionLocation_Magnify = GLES20.glGetUniformLocation(mMagnifyProgram, "resolution");

        uTexRotateMatrix_Lut = GLES20.glGetUniformLocation(mLutProgram, "uTexRotateMatrix");
        uSTMatrix_Lut = GLES20.glGetUniformLocation(mLutProgram, "uSTMatrix");
        vPosition_Lut = GLES20.glGetAttribLocation(mLutProgram, "vPosition");
        vTexCoord_Lut = GLES20.glGetAttribLocation(mLutProgram, "vTexCoord");
        uPostLutSize = GLES20.glGetUniformLocation(mLutProgram, "POSTLUTSIZE");
        uPostLutSizeTiles = GLES20.glGetUniformLocation(mLutProgram, "POSTLUTSIZETILES");
        uOffscreenTexRotateMatrixHandle_LUT = GLES20.glGetUniformLocation(mLutProgram, "uTexRotateMatrix");

        String fss_lut_noclamp ="";
        File fss_lut_noclampFile = new File(sPHOTON_TUNING_DIR, "main_lut_noclamp_2d_fs.glsl");
        if (fss_lut_noclampFile.exists()) {
            try {
                fss_lut_noclamp = new String(Files.readAllBytes(fss_lut_noclampFile.toPath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                Log.e(TAG, "fss_lut " + e.getMessage());
            }
        }
        else {
            fss_lut_noclamp = PhotonCamera.getAssetLoader().getString("shaders/lut/main_lut_noclamp_2d_fs.glsl");
        }

        mLutNoClampProgram = loadShader(simple_vs, fss_lut_noclamp);
        Log.d(TAG, "mLutNoClampProgram ID: " + mLutNoClampProgram);

        vPosition_LutNoClamp = GLES20.glGetAttribLocation(mLutNoClampProgram, "aPosition");
        vTexCoord_LutNoClamp = GLES20.glGetAttribLocation(mLutNoClampProgram, "aTexCoord");
        uPostLutSizeNoClamp = GLES20.glGetUniformLocation(mLutNoClampProgram, "POSTLUTSIZE");
        uPostLutSizeTilesNoClamp = GLES20.glGetUniformLocation(mLutNoClampProgram, "POSTLUTSIZETILES");
        uOffscreenTexRotateMatrixHandle_LUTNoClamp = GLES20.glGetUniformLocation(mLutNoClampProgram, "uTexRotateMatrix");

        setupOffscreenRendering();

        GLES20.glUseProgram(mNormalProgram);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mNormalProgram, "sTexture"), 0);

        int isQuad = ((PhotonCamera.getSettings().sensorModeCfaPattern == -2) && PhotonCamera.isSensorModeOn) ? 1 : 0;
        GLES20.glUniform1i(uBinning_Normal, isQuad);
        GLES20.glUseProgram(mMagnifyProgram);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mMagnifyProgram, "sTexture"), 0);

        GLES20.glUseProgram(mLutProgram);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mLutProgram, "sTexture"), 0);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mLutProgram, "PostLut"), 1);

        GLES20.glUseProgram(0);
        mGLInit = true;
        mView.fireOnSurfaceTextureAvailable(mSTexture, 0, 0);
    }

    private void updateLutTexture() {
        if (hTexLut != null) {
            hTexLut.close();
            hTexLut = null;
        }
        lutSize = 0;
        try {
            if (currentLutFile != null && currentLutFile.exists()) {
                GLImage lutbm = null;
                if (currentLutFile.getName().toLowerCase().endsWith(".cube")) {
                    // Try fast JNI acceleration first
                    Bitmap bmp = Utilities.parseCubeLut8Bit(currentLutFile);
                    if (bmp == null) {
                        // Fallback to original Java parsing if JNI fails
                        try (FileInputStream fis = new FileInputStream(currentLutFile)) {
                            bmp = Utilities.parseCubeLut16Bit(fis);
                        }
                    }
                    if (bmp != null) {
                        lutbm = new GLImage(bmp);
                    }
                } else {
                    lutbm = new GLImage(currentLutFile);
                }
                if (lutbm != null) {
                    hTexLut = new GLTexture(lutbm, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, 0);
                    lutSize = lutbm.size.x;
                    Log.d(TAG, "Successfully loaded LUT: " + currentLutFile.getName() + " with size: " + lutSize);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load LUT image.", e);
        }
        lutUpdateNeeded = false;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        mScreenWidth = width;
        mScreenHeight = height;
    }

    public void setMagnifyEnabled(boolean enabled) { mIsMagnifyEnabled = enabled; }
    public void setLutEnabled(boolean enabled) { mIsLutEnabled = enabled; mView.requestRender(); }
    public void setLut(File lutFile) { this.currentLutFile = lutFile; this.lutUpdateNeeded = true; mView.requestRender(); }
    public SurfaceTexture getmSTexture() { return mSTexture; }

    public void setCameraResolution(int width, int height) {
        this.mCameraWidth = width;
        this.mCameraHeight = height;
    }

    private void initTex() {
        hTex = new int[1];
        GLES20.glGenTextures(1, hTex, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, hTex[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    }

    public synchronized void onFrameAvailable(SurfaceTexture st) {
        mUpdateST = true;
        mView.requestRender();
    }

    private static String GetSupportedVersion() {
        return "#version 300 es";
    }

    private static int loadShader(String vss, String fss) {
        if (!vss.startsWith("#version")) {
            vss = "#version 300 es\n" + vss;
        }
        if (!fss.startsWith("#version")) {
            fss = "#version 300 es\n" + fss;
        }

        int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vshader, vss);
        GLES20.glCompileShader(vshader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Shader", "V-Shader Error: " + GLES20.glGetShaderInfoLog(vshader));
            return 0;
        }

        int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fshader, fss);
        GLES20.glCompileShader(fshader);
        int[] compiledF = new int[1];
        GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiledF, 0);
        if (compiledF[0] == 0) {
            Log.e("Shader", "F-Shader Error: " + GLES20.glGetShaderInfoLog(fshader));
            return 0;
        }

        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vshader);
        GLES20.glAttachShader(program, fshader);
        GLES20.glLinkProgram(program);

        return program;
    }

    private String readAssetFile(String assetPath) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = mContext.getAssets().open(assetPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            br.close();
        } catch (IOException e) {
            Log.e(TAG, "Error reading asset file: " + assetPath, e);
            return null;
        }
        return sb.toString();
    }

    private void setupOffscreenRendering() {
        final float[] vertexData = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
        mOffscreenVertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mOffscreenVertexBuffer.put(vertexData).position(0);
        final float[] texCoordData = {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f};
        mOffscreenTexCoordBuffer = ByteBuffer.allocateDirect(texCoordData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mOffscreenTexCoordBuffer.put(texCoordData).position(0);
        final float[] texCoordDataRotated = {1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f};
        mOffscreenTexCoordBufferRotated = ByteBuffer.allocateDirect(texCoordDataRotated.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mOffscreenTexCoordBufferRotated.put(texCoordDataRotated).position(0);
        String yuvVertexShader = readAssetFile("shaders/lut/yuv_to_rgb_vs.glsl");
        String yuvFragmentShader = readAssetFile("shaders/lut/yuv_to_rgb_fs.glsl");
        mYuvToRgbProgram = loadShader(yuvVertexShader, yuvFragmentShader);
        mYuvPositionHandle = GLES20.glGetAttribLocation(mYuvToRgbProgram, "aPosition");
        mYuvTexCoordHandle = GLES20.glGetAttribLocation(mYuvToRgbProgram, "aTexCoord");
        mYTextureHandle = GLES20.glGetUniformLocation(mYuvToRgbProgram, "y_texture");
        mUTextureHandle = GLES20.glGetUniformLocation(mYuvToRgbProgram, "u_texture");
        mVTextureHandle = GLES20.glGetUniformLocation(mYuvToRgbProgram, "v_texture");
        uOffscreenTexRotateMatrixHandle_YUV = GLES20.glGetUniformLocation(mYuvToRgbProgram, "uTexRotateMatrix");
        uOffscreenTexRotateMatrixHandle_LUT = GLES20.glGetUniformLocation(mLutProgram, "uTexRotateMatrix");

    }

    private void setupOffscreenFramebuffer(int width, int height) {
        GLES20.glDeleteFramebuffers(1, mFboId, 0);
        GLES20.glDeleteRenderbuffers(1, mRboId, 0);
        GLES20.glDeleteTextures(1, mFboTextureId, 0);

        GLES20.glGenFramebuffers(1, mFboId, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);

        GLES20.glGenTextures(1, mFboTextureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTextureId[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        setupTextureParameters();
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mFboTextureId[0], 0);

        GLES20.glGenRenderbuffers(1, mRboId, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRboId[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRboId[0]);

        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Framebuffer is not complete!");
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void setupTextureParameters() {
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void setupTextureParameters(int target, int filter) {
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, filter);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, filter);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    public void processYuvImage(final Image image, final int orientation, final Consumer<ByteBuffer> onComplete) {
        if (image == null) {
            onComplete.accept(null);
            return;
        }

        final int sensorWidth = image.getWidth();
        final int sensorHeight = image.getHeight();

        final ByteBuffer yBuffer;
        final ByteBuffer uBuffer;
        final ByteBuffer vBuffer;
        try {
            yBuffer = copyPlaneData(image.getPlanes()[0], sensorWidth, sensorHeight);
            uBuffer = copyPlaneData(image.getPlanes()[1], sensorWidth / 2, sensorHeight / 2);
            vBuffer = copyPlaneData(image.getPlanes()[2], sensorWidth / 2, sensorHeight / 2);
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy plane data before queuing", e);
            image.close();
            onComplete.accept(null);
            return;
        }

        image.close();

        mView.queueEvent(() -> {
            if (!PhotonCamera.getSettings().lutName.equals("lut.png") && (hTexLut == null)) {
                onComplete.accept(null);
                return;
            }

            final boolean isSideways = (orientation == 90 || orientation == 270 || orientation == -90);
            mImageWidth = isSideways ? sensorHeight : sensorWidth;
            mImageHeight = isSideways ? sensorWidth : sensorHeight;

            setupOffscreenFramebuffer(mImageWidth, mImageHeight);

            int[] yuvTextureIds = new int[3];
            GLES20.glGenTextures(3, yuvTextureIds, 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, sensorWidth, sensorHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, yBuffer);
            setupTextureParameters();

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[1]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, sensorWidth / 2, sensorHeight / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, uBuffer);
            setupTextureParameters();

            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureIds[2]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, sensorWidth / 2, sensorHeight / 2, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, vBuffer);
            setupTextureParameters();

            Matrix.setIdentityM(mOffscreenRotationMatrix, 0);
            Matrix.translateM(mOffscreenRotationMatrix, 0, 0.5f, 0.5f, 0.0f);
            Matrix.rotateM(mOffscreenRotationMatrix, 0, -orientation, 0.0f, 0.0f, 1.0f);
            Matrix.translateM(mOffscreenRotationMatrix, 0, -0.5f, -0.5f, 0.0f);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId[0]);
            GLES20.glViewport(0, 0, mImageWidth, mImageHeight);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mYuvToRgbProgram);
            GLES20.glUniformMatrix4fv(uOffscreenTexRotateMatrixHandle_YUV, 1, false, mOffscreenRotationMatrix, 0);
            GLES20.glUniform1i(mYTextureHandle, 0);
            GLES20.glUniform1i(mUTextureHandle, 1);
            GLES20.glUniform1i(mVTextureHandle, 2);

            mOffscreenVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(mYuvPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mOffscreenVertexBuffer);
            GLES20.glEnableVertexAttribArray(mYuvPositionHandle);
            mOffscreenTexCoordBuffer.position(0);
            GLES20.glVertexAttribPointer(mYuvTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mOffscreenTexCoordBuffer);
            GLES20.glEnableVertexAttribArray(mYuvTexCoordHandle);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

            GLES20.glUseProgram(mLutProgram);

            Matrix.setIdentityM(mOffscreenRotationMatrix, 0);

            GLES20.glUniformMatrix4fv(uOffscreenTexRotateMatrixHandle_LUT, 1, false, mOffscreenRotationMatrix, 0);
            GLES20.glUniformMatrix4fv(uSTMatrix_Lut, 1, false, mSTMatrix, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTextureId[0]);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mLutProgram, "sTexture"), 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hTexLut.mTextureID);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(mLutProgram, "PostLut"), 1);

            double lutSizeDouble = (double) lutSize;
            float postLutSizeTilesVal = (float) Math.round(Math.pow(lutSizeDouble, 1.0 / 3.0));
            float postLutSizeVal = (float) (lutSizeDouble / postLutSizeTilesVal);
            GLES20.glUniform1f(uPostLutSize, postLutSizeVal);
            GLES20.glUniform1f(uPostLutSizeTiles, postLutSizeTilesVal);

            mOffscreenVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(vPosition_Lut, 2, GLES20.GL_FLOAT, false, 0, mOffscreenVertexBuffer);
            GLES20.glEnableVertexAttribArray(vPosition_Lut);
            mOffscreenTexCoordBuffer.position(0);
            GLES20.glVertexAttribPointer(vTexCoord_Lut, 2, GLES20.GL_FLOAT, false, 0, mOffscreenTexCoordBuffer);
            GLES20.glEnableVertexAttribArray(vTexCoord_Lut);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(vPosition_Lut);
            GLES20.glDisableVertexAttribArray(vTexCoord_Lut);

            ByteBuffer processedData = ByteBuffer.allocateDirect(mImageWidth * mImageHeight * 4);
            processedData.order(ByteOrder.nativeOrder());
            GLES20.glReadPixels(0, 0, mImageWidth, mImageHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, processedData);
            processedData.rewind();

            GLES20.glDeleteTextures(3, yuvTextureIds, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            onComplete.accept(processedData);
        });
    }

    @NonNull
    private ByteBuffer copyPlaneData(@NonNull Image.Plane plane, int width, int height) {
        ByteBuffer buffer = plane.getBuffer();
        int rowStride = plane.getRowStride();
        int pixelStride = plane.getPixelStride();

        ByteBuffer src = buffer.duplicate().order(ByteOrder.nativeOrder());
        src.position(0);

        ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height);
        directBuffer.order(ByteOrder.nativeOrder());

        if (pixelStride == 1 && rowStride == width) {
            int size = Math.min(width * height, src.remaining());
            src.limit(size);
            directBuffer.put(src);
            directBuffer.rewind();
            return directBuffer;
        }

        byte[] rowData = new byte[rowStride];
        int bytesNeededInLastRow = (width - 1) * pixelStride + 1;

        for (int y = 0; y < height; y++) {
            int remaining = src.remaining();
            if (remaining <= 0) break;

            int bytesToRead = (y == height - 1) ? Math.min(bytesNeededInLastRow, remaining) : Math.min(rowStride, remaining);
            
            try {
                src.get(rowData, 0, bytesToRead);
            } catch (Exception e) {
                break;
            }

            if (pixelStride == 1) {
                directBuffer.put(rowData, 0, Math.min(width, bytesToRead));
            } else {
                for (int x = 0; x < width; x++) {
                    int offset = x * pixelStride;
                    if (offset < bytesToRead) {
                        directBuffer.put(rowData[offset]);
                    }
                }
            }

            if (y < height - 1 && bytesToRead < rowStride) {
                int skip = rowStride - bytesToRead;
                if (src.remaining() >= skip) {
                    src.position(src.position() + skip);
                }
            }
        }

        directBuffer.rewind();
        return directBuffer;
    }

    public void processYCbCrImage(final Image image, final int orientation, final Consumer<ByteBuffer> onComplete) {
        if (image == null || image.getFormat() != ImageFormat.YCBCR_P010) {
            Log.e(TAG, "processYCbCrImage: Invalid image or format (expected P010)");
            if (image != null) image.close();
            onComplete.accept(null);
            return;
        }

        final int sensorWidth = image.getWidth();
        final int sensorHeight = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();

        final ByteBuffer yBufferCopy = ByteBuffer.allocateDirect(planes[0].getBuffer().remaining());
        yBufferCopy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer ySrc = planes[0].getBuffer().duplicate();
        ySrc.position(0);
        yBufferCopy.put(ySrc);
        yBufferCopy.rewind();
        final int yRowStride = planes[0].getRowStride();

        final ByteBuffer uvBufferCopy = ByteBuffer.allocateDirect(planes[1].getBuffer().remaining());
        uvBufferCopy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer uvSrc = planes[1].getBuffer().duplicate();
        uvSrc.position(0);
        uvBufferCopy.put(uvSrc);
        uvBufferCopy.rewind();
        final int uvRowStride = planes[1].getRowStride();

        image.close();

        mView.queueEvent(() -> {
            if (!PhotonCamera.getSettings().lutName.equals("lut.png") && (hTexLut == null)) {
                onComplete.accept(null);
                return;
            }

            final boolean isSideways = (orientation == 90 || orientation == 270 || orientation == -90);
            mImageWidth = isSideways ? sensorHeight : sensorWidth;
            mImageHeight = isSideways ? sensorWidth : sensorHeight;

            int[] fbo1 = new int[1], rbo1 = new int[1], tex1 = new int[1];
            int[] fbo2 = new int[1], rbo2 = new int[1], tex2 = new int[1];
            int[] yuvTextures = new int[2];

            try {
                setupGenericFramebuffer(fbo1, rbo1, tex1, mImageWidth, mImageHeight, true);

                GLES20.glGenTextures(2, yuvTextures, 0);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 2);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, yRowStride / 2);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[0]);
                GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_R16UI, sensorWidth, sensorHeight, 0, GLES30.GL_RED_INTEGER, GLES30.GL_UNSIGNED_SHORT, yBufferCopy);
                setupTextureParameters(GLES20.GL_TEXTURE_2D, GLES20.GL_NEAREST);

                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, uvRowStride / 4);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[1]);
                GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_RG16UI, sensorWidth / 2, sensorHeight / 2, 0, GLES30.GL_RG_INTEGER, GLES30.GL_UNSIGNED_SHORT, uvBufferCopy);
                setupTextureParameters(GLES20.GL_TEXTURE_2D, GLES20.GL_NEAREST);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo1[0]);
                GLES20.glViewport(0, 0, mImageWidth, mImageHeight);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glUseProgram(mP010ToF16Program);

                Matrix.setIdentityM(mOffscreenRotationMatrix, 0);
                Matrix.translateM(mOffscreenRotationMatrix, 0, 0.5f, 0.5f, 0.0f);
                Matrix.rotateM(mOffscreenRotationMatrix, 0, -orientation, 0.0f, 0.0f, 1.0f);
                Matrix.translateM(mOffscreenRotationMatrix, 0, -0.5f, -0.5f, 0.0f);
                GLES20.glUniformMatrix4fv(uOffscreenTexRotateMatrixHandle_P010, 1, false, mOffscreenRotationMatrix, 0);

                GLES20.glUniform1i(mP010_Ytex, 0);
                GLES20.glUniform1i(mP010_Utex, 1);
                GLES20.glUniform1i(mP010_Vtex, 1);

                mOffscreenVertexBuffer.position(0);
                GLES20.glVertexAttribPointer(mP010_vPosition, 2, GLES20.GL_FLOAT, false, 0, mOffscreenVertexBuffer);
                GLES20.glEnableVertexAttribArray(mP010_vPosition);

                mOffscreenTexCoordBuffer.position(0);
                GLES20.glVertexAttribPointer(mP010_aTexCoord, 2, GLES20.GL_FLOAT, false, 0, mOffscreenTexCoordBuffer);
                GLES20.glEnableVertexAttribArray(mP010_aTexCoord);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                GLES20.glDisableVertexAttribArray(mP010_vPosition);
                GLES20.glDisableVertexAttribArray(mP010_aTexCoord);

                setupGenericFramebuffer(fbo2, rbo2, tex2, mImageWidth, mImageHeight, true);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo2[0]);
                GLES20.glViewport(0, 0, mImageWidth, mImageHeight);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glUseProgram(mLutProgram);

                Matrix.setIdentityM(mOffscreenRotationMatrix, 0);
                GLES20.glUniformMatrix4fv(uOffscreenTexRotateMatrixHandle_LUT, 1, false, mOffscreenRotationMatrix, 0);
                GLES20.glUniformMatrix4fv(uSTMatrix_Lut, 1, false, mSTMatrix, 0);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex1[0]);
                GLES20.glUniform1i(GLES20.glGetUniformLocation(mLutProgram, "sTexture"), 0);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hTexLut.mTextureID);
                GLES20.glUniform1i(GLES20.glGetUniformLocation(mLutProgram, "PostLut"), 1);

                double lutSizeDouble = (double) lutSize;
                float postLutSizeTilesVal = (float) Math.round(Math.pow(lutSizeDouble, 1.0 / 3.0));
                float postLutSizeVal = (float) (lutSizeDouble / postLutSizeTilesVal);
                GLES20.glUniform1f(uPostLutSize, postLutSizeVal);
                GLES20.glUniform1f(uPostLutSizeTiles, postLutSizeTilesVal);

                mOffscreenVertexBuffer.position(0);
                GLES20.glVertexAttribPointer(vPosition_Lut, 2, GLES20.GL_FLOAT, false, 0, mOffscreenVertexBuffer);
                GLES20.glEnableVertexAttribArray(vPosition_Lut);
                mOffscreenTexCoordBuffer.position(0);
                GLES20.glVertexAttribPointer(vTexCoord_Lut, 2, GLES20.GL_FLOAT, false, 0, mOffscreenTexCoordBuffer);
                GLES20.glEnableVertexAttribArray(vTexCoord_Lut);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                GLES20.glDisableVertexAttribArray(vPosition_Lut);
                GLES20.glDisableVertexAttribArray(vTexCoord_Lut);

                ByteBuffer processedData = ByteBuffer.allocateDirect(mImageWidth * mImageHeight * 4);
                processedData.order(ByteOrder.nativeOrder());
                GLES20.glReadPixels(0, 0, mImageWidth, mImageHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, processedData);
                processedData.rewind();

                onComplete.accept(processedData);

            } finally {
                GLES20.glDeleteTextures(2, yuvTextures, 0);
                GLES20.glDeleteTextures(1, tex1, 0);
                GLES20.glDeleteTextures(1, tex2, 0);
                GLES20.glDeleteFramebuffers(1, fbo1, 0);
                GLES20.glDeleteFramebuffers(1, fbo2, 0);
                GLES20.glDeleteRenderbuffers(1, rbo1, 0);
                GLES20.glDeleteRenderbuffers(1, rbo2, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        });
    }

    public void processYCbCrImageFP16(final Image image, final int orientation, final Consumer<Bitmap> onComplete) {
        if (image == null || image.getFormat() != ImageFormat.YCBCR_P010) {
            if (image != null) image.close();
            onComplete.accept(null);
            return;
        }

        final int sensorWidth = image.getWidth();
        final int sensorHeight = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();

        final ByteBuffer yBufferCopy = ByteBuffer.allocateDirect(planes[0].getBuffer().remaining());
        yBufferCopy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer ySrc = planes[0].getBuffer().duplicate();
        ySrc.position(0);
        yBufferCopy.put(ySrc);
        yBufferCopy.rewind();
        final int yRowStride = planes[0].getRowStride();

        final ByteBuffer uvBufferCopy = ByteBuffer.allocateDirect(planes[1].getBuffer().remaining());
        uvBufferCopy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer uvSrc = planes[1].getBuffer().duplicate();
        uvSrc.position(0);
        uvBufferCopy.put(uvSrc);
        uvBufferCopy.rewind();
        final int uvRowStride = planes[1].getRowStride();

        image.close();

        mView.queueEvent(() -> {
            if (!PhotonCamera.getSettings().lutName.equals("lut.png") && (hTexLut == null)) {
                onComplete.accept(null);
                return;
            }

            final boolean isSideways = (orientation == 90 || orientation == 270 || orientation == -90);
            mImageWidth = isSideways ? sensorHeight : sensorWidth;
            mImageHeight = isSideways ? sensorWidth : sensorHeight;

            int[] fbo1 = new int[1], rbo1 = new int[1], tex1 = new int[1];
            int[] fbo2 = new int[1], rbo2 = new int[1], tex2 = new int[1];
            int[] yuvTextures = new int[2];

            try {
                setupGenericFramebuffer(fbo1, rbo1, tex1, mImageWidth, mImageHeight, true);

                GLES20.glGenTextures(2, yuvTextures, 0);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 2);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, yRowStride / 2);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[0]);
                GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_R16UI, sensorWidth, sensorHeight, 0, GLES30.GL_RED_INTEGER, GLES30.GL_UNSIGNED_SHORT, yBufferCopy);
                setupTextureParameters(GLES20.GL_TEXTURE_2D, GLES20.GL_NEAREST);

                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, uvRowStride / 4);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[1]);
                GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_RG16UI, sensorWidth / 2, sensorHeight / 2, 0, GLES30.GL_RG_INTEGER, GLES30.GL_UNSIGNED_SHORT, uvBufferCopy);
                setupTextureParameters(GLES20.GL_TEXTURE_2D, GLES20.GL_NEAREST);
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo1[0]);
                GLES20.glViewport(0, 0, mImageWidth, mImageHeight);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                GLES20.glUseProgram(mP010ToF16NoClampProgram);

                Matrix.setIdentityM(mOffscreenRotationMatrix, 0);
                Matrix.translateM(mOffscreenRotationMatrix, 0, 0.5f, 0.5f, 0.0f);
                Matrix.rotateM(mOffscreenRotationMatrix, 0, -orientation, 0.0f, 0.0f, 1.0f);
                Matrix.translateM(mOffscreenRotationMatrix, 0, -0.5f, -0.5f, 0.0f);
                GLES20.glUniformMatrix4fv(uOffscreenTexRotateMatrixHandle_P010NoClamp, 1, false, mOffscreenRotationMatrix, 0);

                GLES20.glUniform1i(mP010NoClamp_Ytex, 0);
                GLES20.glUniform1i(mP010NoClamp_Utex, 1);
                GLES20.glUniform1i(mP010NoClamp_Vtex, 1);

                mOffscreenVertexBuffer.position(0);
                GLES20.glVertexAttribPointer(mP010NoClamp_vPosition, 2, GLES20.GL_FLOAT, false, 0, mOffscreenVertexBuffer);
                GLES20.glEnableVertexAttribArray(mP010NoClamp_vPosition);

                mOffscreenTexCoordBuffer.position(0);
                GLES20.glVertexAttribPointer(mP010NoClamp_aTexCoord, 2, GLES20.GL_FLOAT, false, 0, mOffscreenTexCoordBuffer);
                GLES20.glEnableVertexAttribArray(mP010NoClamp_aTexCoord);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                GLES20.glDisableVertexAttribArray(mP010NoClamp_vPosition);
                GLES20.glDisableVertexAttribArray(mP010NoClamp_aTexCoord);

                int finalFbo = fbo1[0];

                if (hTexLut != null && lutSize > 0) {
                    setupGenericFramebuffer(fbo2, rbo2, tex2, mImageWidth, mImageHeight, true);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo2[0]);
                    GLES20.glViewport(0, 0, mImageWidth, mImageHeight);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                    GLES20.glUseProgram(mLutNoClampProgram);

                    Matrix.setIdentityM(mOffscreenRotationMatrix, 0);
                    GLES20.glUniformMatrix4fv(uOffscreenTexRotateMatrixHandle_LUTNoClamp, 1, false, mOffscreenRotationMatrix, 0);

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex1[0]);
                    GLES20.glUniform1i(GLES20.glGetUniformLocation(mLutNoClampProgram, "sTexture"), 0);

                    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hTexLut.mTextureID);
                    GLES20.glUniform1i(GLES20.glGetUniformLocation(mLutNoClampProgram, "PostLut"), 1);

                    double lutSizeDouble = (double) lutSize;
                    float postLutSizeTilesVal = (float) Math.round(Math.pow(lutSizeDouble, 1.0 / 3.0));
                    float postLutSizeVal = (float) (lutSizeDouble / postLutSizeTilesVal);
                    GLES20.glUniform1f(uPostLutSizeNoClamp, postLutSizeVal);
                    GLES20.glUniform1f(uPostLutSizeTilesNoClamp, postLutSizeTilesVal);

                    mOffscreenVertexBuffer.position(0);
                    GLES20.glVertexAttribPointer(vPosition_LutNoClamp, 2, GLES20.GL_FLOAT, false, 0, mOffscreenVertexBuffer);
                    GLES20.glEnableVertexAttribArray(vPosition_LutNoClamp);
                    mOffscreenTexCoordBuffer.position(0);
                    GLES20.glVertexAttribPointer(vTexCoord_LutNoClamp, 2, GLES20.GL_FLOAT, false, 0, mOffscreenTexCoordBuffer);
                    GLES20.glEnableVertexAttribArray(vTexCoord_LutNoClamp);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
                    GLES20.glDisableVertexAttribArray(vPosition_LutNoClamp);
                    GLES20.glDisableVertexAttribArray(vTexCoord_LutNoClamp);
                    finalFbo = fbo2[0];
                }

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, finalFbo);
                ByteBuffer processedBuffer = ByteBuffer.allocateDirect(mImageWidth * mImageHeight * 8);
                processedBuffer.order(ByteOrder.nativeOrder());
                GLES20.glReadPixels(0, 0, mImageWidth, mImageHeight, GLES20.GL_RGBA, GLES30.GL_HALF_FLOAT, processedBuffer);

                Bitmap resultBitmap = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ColorSpace targetSpace = ColorSpace.get(ColorSpace.Named.DISPLAY_P3);
                    switch (PhotonCamera.getSettings().swColorSpace) {
                        case "scRGB LINEAR":
                            targetSpace = ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB);
                            break;
                        case "sRGB":
                            targetSpace = ColorSpace.get(ColorSpace.Named.SRGB);
                            break;
                        case "scRGB":
                            targetSpace = ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB);
                            break;
                        case "DISPLAY P3":
                            targetSpace = ColorSpace.get(ColorSpace.Named.DISPLAY_P3);
                            break;
                        case "BT.2020 HLG":
                            targetSpace = ColorSpace.get(ColorSpace.Named.BT2020_HLG);
                            break;
                        case "BT.2020 PQ":
                            targetSpace = ColorSpace.get(ColorSpace.Named.BT2020_PQ);
                            break;
                        case "ADOBE RGB":
                            targetSpace = ColorSpace.get(ColorSpace.Named.ADOBE_RGB);
                            break;
                        case "BT.2020":
                            targetSpace = ColorSpace.get(ColorSpace.Named.BT2020);
                            break;
                        case "BT.709":
                            targetSpace = ColorSpace.get(ColorSpace.Named.BT709);
                            break;
                        case "DCI P3":
                            targetSpace = ColorSpace.get(ColorSpace.Named.DCI_P3);
                            break;
                        case "sRGB LINEAR":
                            targetSpace = ColorSpace.get(ColorSpace.Named.LINEAR_SRGB);
                            break;
                    }
                    resultBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.RGBA_F16, true, targetSpace);
                } else {
                    resultBitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.RGBA_F16, true, ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB));
                }
                resultBitmap.copyPixelsFromBuffer(processedBuffer);

                onComplete.accept(resultBitmap);

            } finally {
                GLES20.glDeleteTextures(2, yuvTextures, 0);
                GLES20.glDeleteTextures(1, tex1, 0);
                GLES20.glDeleteTextures(1, tex2, 0);
                GLES20.glDeleteFramebuffers(1, fbo1, 0);
                GLES20.glDeleteFramebuffers(1, fbo2, 0);
                GLES20.glDeleteRenderbuffers(1, rbo1, 0);
                GLES20.glDeleteRenderbuffers(1, rbo2, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        });
    }

    public Future<Bitmap> processP010SdrImageGL(final Image image) {
        if (image == null || image.getFormat() != ImageFormat.YCBCR_P010) {
            if (image != null) image.close();
            throw new IllegalArgumentException("Image must be in YCBCR_P010 format");
        }

        final int width = image.getWidth();
        final int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();

        final ByteBuffer yBufferCopy = ByteBuffer.allocateDirect(planes[0].getBuffer().remaining());
        yBufferCopy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer ySrc = planes[0].getBuffer().duplicate();
        ySrc.position(0);
        yBufferCopy.put(ySrc);
        yBufferCopy.rewind();
        final int yRowStride = planes[0].getRowStride();

        final ByteBuffer uvBufferCopy = ByteBuffer.allocateDirect(planes[1].getBuffer().remaining());
        uvBufferCopy.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer uvSrc = planes[1].getBuffer().duplicate();
        uvSrc.position(0);
        uvBufferCopy.put(uvSrc);
        uvBufferCopy.rewind();
        final int uvRowStride = planes[1].getRowStride();

        image.close();

        Callable<Bitmap> task = () -> {
            int[] fboId = new int[1];
            int[] rboId = new int[1];
            int[] fboTextureId = new int[1];
            setupGenericFramebuffer(fboId, rboId, fboTextureId, width, height, true);

            int[] yuvTextures = new int[2];
            GLES20.glGenTextures(2, yuvTextures, 0);

            try {
                GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 2);

                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, yRowStride / 2);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[0]);
                GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_R16UI, width, height, 0, GLES30.GL_RED_INTEGER, GLES30.GL_UNSIGNED_SHORT, yBufferCopy);
                setupTextureParameters(GLES20.GL_TEXTURE_2D, GLES20.GL_NEAREST);

                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, uvRowStride / 4);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextures[1]);
                GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_RG16UI, width / 2, height / 2, 0, GLES30.GL_RG_INTEGER, GLES30.GL_UNSIGNED_SHORT, uvBufferCopy);
                setupTextureParameters(GLES20.GL_TEXTURE_2D, GLES20.GL_NEAREST);

                GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, 0);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);
                GLES20.glViewport(0, 0, width, height);
                GLES20.glUseProgram(mP010ToF16Program);

                GLES20.glUniform1i(mP010_Ytex, 0);
                GLES20.glUniform1i(mP010_Utex, 1);
                GLES20.glUniform1i(mP010_Vtex, 1);

                pVertex.position(0);
                GLES20.glVertexAttribPointer(mP010_vPosition, 2, GLES20.GL_FLOAT, false, 8, pVertex);
                GLES20.glEnableVertexAttribArray(mP010_vPosition);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

                ByteBuffer processedBuffer = ByteBuffer.allocateDirect(width * height * 8);
                processedBuffer.order(ByteOrder.nativeOrder());
                GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES30.GL_HALF_FLOAT, processedBuffer);

                Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGBA_F16);
                resultBitmap.copyPixelsFromBuffer(processedBuffer);

                return resultBitmap;

            } finally {
                GLES20.glDeleteTextures(2, yuvTextures, 0);
                GLES20.glDeleteFramebuffers(1, fboId, 0);
                GLES20.glDeleteRenderbuffers(1, rboId, 0);
                GLES20.glDeleteTextures(1, fboTextureId, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        };

        FutureTask<Bitmap> futureTask = new FutureTask<>(task);
        mView.queueEvent(futureTask);
        return futureTask;
    }

    private void setupGenericFramebuffer(int[] fboId, int[] rboId, int[] textureId, int width, int height, boolean useHdr) {
        if (fboId[0] > 0) GLES20.glDeleteFramebuffers(1, fboId, 0);
        if (rboId[0] > 0) GLES20.glDeleteRenderbuffers(1, rboId, 0);
        if (textureId[0] > 0) GLES20.glDeleteTextures(1, textureId, 0);

        GLES20.glGenFramebuffers(1, fboId, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);

        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

        if (useHdr) {
            GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES30.GL_RGBA16F, width, height, 0, GLES20.GL_RGBA, GLES30.GL_HALF_FLOAT, null);
        } else {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        }

        setupTextureParameters(GLES20.GL_TEXTURE_2D, GLES20.GL_LINEAR);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textureId[0], 0);

        GLES20.glGenRenderbuffers(1, rboId, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rboId[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, rboId[0]);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void setOrientation(int or) { Matrix.setRotateM(mTexRotateMatrix, 0, or, 0f, 0f, -1f); }
    public void setTransform(@NonNull android.graphics.Matrix matrix) {}
    public void scale(int in_width, int in_height, int out_width, int out_height, int rotation) {}
}

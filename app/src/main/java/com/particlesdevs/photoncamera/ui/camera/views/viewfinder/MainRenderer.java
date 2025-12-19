package com.particlesdevs.photoncamera.ui.camera.views.viewfinder;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import androidx.annotation.NonNull;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.processing.opengl.GLImage;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;
import com.particlesdevs.photoncamera.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.Consumer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

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

    // processing for single shot LUT
    private int mScreenWidth;
    private int mScreenHeight;
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
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, hTex[0]);
        if (mIsLutEnabled && !mIsMagnifyEnabled && hTexLut != null) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, hTexLut.mTextureID);
            double lutSizeDouble = (double) lutSize;
            float postLutSizeVal = (float) Math.cbrt(lutSizeDouble * lutSizeDouble);
            float postLutSizeTilesVal = (float) (lutSizeDouble / postLutSizeVal);
            GLES20.glUniform1f(uPostLutSize, postLutSizeVal); // z.B. 64.0f
            GLES20.glUniform1f(uPostLutSizeTiles, postLutSizeTilesVal); // z.B. 8.0f
        }
        GLES20.glUniformMatrix4fv(uTexRotateMatrixHandle, 1, false, mTexRotateMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, mSTMatrix, 0);
        GLES20.glVertexAttribPointer(vPositionHandle, 2, GLES20.GL_FLOAT, false, 8, pVertex);
        GLES20.glEnableVertexAttribArray(vPositionHandle);
        GLES20.glVertexAttribPointer(vTexCoordHandle, 2, GLES20.GL_FLOAT, false, 8, pTexCoord);
        GLES20.glEnableVertexAttribArray(vTexCoordHandle);
        if (mIsMagnifyEnabled) {
            GLES20.glUniform1i(enablePeak_Magnify, PhotonCamera.getSettings().focusPeak);
            GLES20.glUniform2f(resolutionLocation_Magnify, mView.getWidth(), mView.getHeight());
        } else {
            GLES20.glUniform1i(enablePeak_Normal, PhotonCamera.getSettings().focusPeak);
            GLES20.glUniform2f(resolutionLocation_Normal, mView.getWidth(), mView.getHeight());
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(vPositionHandle);
        GLES20.glDisableVertexAttribArray(vTexCoordHandle);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initTex();
        mSTexture = new SurfaceTexture(hTex[0]);
        mSTexture.setOnFrameAvailableListener(this);
        String vss_default = PhotonCamera.getAssetLoader().getString("shaders/preview/main_vs.glsl");
        String fss_default = PhotonCamera.getAssetLoader().getString("shaders/preview/main_fs.glsl");
        String fss_magnify = PhotonCamera.getAssetLoader().getString("shaders/preview/main_magnification_fs.glsl");
        String fss_lut = PhotonCamera.getAssetLoader().getString("shaders/preview/main_lut_fs.glsl");
        mNormalProgram = loadShader(vss_default, fss_default);
        mMagnifyProgram = loadShader(vss_default, fss_magnify);
        mLutProgram = loadShader(vss_default, fss_lut);
        Log.d("MainRenderer", "mNormalProgram ID: " + mNormalProgram);
        Log.d("MainRenderer", "mMagnifyProgram ID: " + mMagnifyProgram);
        Log.d("MainRenderer", "mLutProgram ID: " + mLutProgram);
        uTexRotateMatrix_Normal = GLES20.glGetUniformLocation(mNormalProgram, "uTexRotateMatrix");
        uSTMatrix_Normal = GLES20.glGetUniformLocation(mNormalProgram, "uSTMatrix");
        vPosition_Normal = GLES20.glGetAttribLocation(mNormalProgram, "vPosition");
        vTexCoord_Normal = GLES20.glGetAttribLocation(mNormalProgram, "vTexCoord");
        enablePeak_Normal = GLES20.glGetUniformLocation(mNormalProgram, "enablePeak");
        resolutionLocation_Normal = GLES20.glGetUniformLocation(mNormalProgram, "resolution");
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
        setupOffscreenRendering();
        GLES20.glUseProgram(mNormalProgram);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(mNormalProgram, "sTexture"), 0);
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
                GLImage lutbm = new GLImage(currentLutFile);
                hTexLut = new GLTexture(lutbm, GLES20.GL_LINEAR, GLES20.GL_CLAMP_TO_EDGE, 0);
                lutSize = lutbm.size.x;
                Log.d("MainRenderer", "Successfully loaded LUT: " + currentLutFile.getName() + " with size: " + lutSize);
            }
        } catch (Exception e) {
            Log.e("MainRenderer", "Failed to load LUT image.", e);
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
        String SupportedVersion = GetSupportedVersion();
        vss = SupportedVersion + "\n #line 1\n" + vss;
        fss = SupportedVersion + "\n #line 1\n" + fss;
        int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vshader, vss);
        GLES20.glCompileShader(vshader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile vshader");
            Log.v("Shader", "Could not compile vshader:" + GLES20.glGetShaderInfoLog(vshader));
            GLES20.glDeleteShader(vshader);
            vshader = 0;
        }

        int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fshader, fss);
        GLES20.glCompileShader(fshader);
        GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Shader", "Could not compile fshader");
            Log.v("Shader", "Could not compile fshader:" + GLES20.glGetShaderInfoLog(fshader));
            GLES20.glDeleteShader(fshader);
            fshader = 0;
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

    public void processYuvImage(final Image image, final int orientation, final Consumer<ByteBuffer> onComplete) {
        mView.queueEvent(() -> {
            if (hTexLut == null) {
                try { image.close(); } catch (Exception ignored) {}
                onComplete.accept(null);
                return;
            }

            final int sensorWidth = image.getWidth();
            final int sensorHeight = image.getHeight();
            final boolean isSideways = (orientation == 90 || orientation == 270);
            mImageWidth = isSideways ? sensorHeight : sensorWidth;
            mImageHeight = isSideways ? sensorWidth : sensorHeight;

            setupOffscreenFramebuffer(mImageWidth, mImageHeight);

            ByteBuffer yBuffer = copyPlaneData(image.getPlanes()[0], sensorWidth, sensorHeight);
            ByteBuffer uBuffer = copyPlaneData(image.getPlanes()[1], sensorWidth / 2, sensorHeight / 2);
            ByteBuffer vBuffer = copyPlaneData(image.getPlanes()[2], sensorWidth / 2, sensorHeight / 2);

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

            image.close();

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
            float postLutSizeVal = (float) Math.cbrt(lutSizeDouble * lutSizeDouble);
            float postLutSizeTilesVal = (float) (lutSizeDouble / postLutSizeVal);
            GLES20.glUniform1f(uPostLutSize, postLutSizeVal);
            GLES20.glUniform1f(uPostLutSizeTiles, postLutSizeTilesVal);

            mOffscreenVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(vPosition_Lut, 2, GLES20.GL_FLOAT, false, 0, mOffscreenVertexBuffer);
            GLES20.glEnableVertexAttribArray(vPosition_Lut);
            mOffscreenTexCoordBuffer.position(0);
            GLES20.glVertexAttribPointer(vTexCoord_Lut, 2, GLES20.GL_FLOAT, false, 0, mOffscreenTexCoordBuffer);
            GLES20.glEnableVertexAttribArray(vTexCoord_Lut);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

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

        // The destination buffer that will have the correct, packed data.
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height);
        directBuffer.order(ByteOrder.nativeOrder());

        // If the plane is already tightly packed, we can do a fast copy.
        if (pixelStride == 1 && rowStride == width) {
            buffer.limit(buffer.position() + width * height);
            directBuffer.put(buffer);
            directBuffer.rewind();
            return directBuffer;
        }

        // Otherwise, we have to copy row by row, de-interleaving as we go.
        byte[] rowData = new byte[rowStride];
        int readPosition = 0;

        for (int y = 0; y < height; y++) {
            // Set the position of the source buffer for each row to read from.
            buffer.position(readPosition);

            // Ensure we don't read past the buffer's limit.
            if (rowStride > buffer.remaining()) {
                // This should not happen with valid image data, but it's a safeguard.
                break;
            }

            // Read a full row (including padding and interleaved data).
            buffer.get(rowData, 0, rowStride);

            if (pixelStride == 1) {
                // Y-plane: copy the actual width data.
                directBuffer.put(rowData, 0, width);
            } else {
                // U/V-planes: de-interleave the data.
                for (int x = 0; x < width; x++) {
                    directBuffer.put(rowData[x * pixelStride]);
                }
            }

            // Advance the read position for the next row.
            readPosition += rowStride;
        }

        directBuffer.rewind();
        return directBuffer;
    }

    public void setOrientation(int or) { Matrix.setRotateM(mTexRotateMatrix, 0, or, 0f, 0f, -1f); }
    public void setTransform(@NonNull android.graphics.Matrix matrix) {}
    public void scale(int in_width, int in_height, int out_width, int out_height, int rotation) {}
}


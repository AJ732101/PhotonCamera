package com.particlesdevs.photoncamera.processing.processor;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;

import com.particlesdevs.photoncamera.api.ParseExif;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.processing.DngCreator;
import com.particlesdevs.photoncamera.processing.ImageSaver;
import com.particlesdevs.photoncamera.processing.ProcessingEventsListener;
import com.particlesdevs.photoncamera.processing.opengl.postpipeline.PostPipeline;
import com.particlesdevs.photoncamera.processing.parameters.FrameNumberSelector;
import com.particlesdevs.photoncamera.processing.parameters.IsoExpoSelector;
import com.particlesdevs.photoncamera.util.Allocator;
import com.particlesdevs.photoncamera.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class RawVideoProcessor extends ProcessorBase {
    private static final String TAG = "RawVideoProcessor";
    public static int videoCounter = 1;

    private final Object lock = new Object();
    private volatile boolean fillParams = false;
    private Path outputFolder;
    private int writeBufferSize = 2;
    private int writeBufferCounter = 0;
    private int threadCounter = 0;
    private volatile ByteBuffer[] dngBuffers = null;
    private DngCreator dngCreator = null;
    private boolean isRecording = false;

    public RawVideoProcessor(ProcessingEventsListener processingEventsListener) {
        super(processingEventsListener);
    }

    @SuppressLint("DefaultLocale")
    public void videoStart(Path outputFolder, ParseExif.ExifData exifData,
                           CameraCharacteristics characteristics,
                           CaptureResult captureResult,
                           CaptureRequest captureRequest,
                           int cameraRotation,
                           ProcessingCallback callback) {
        this.outputFolder = outputFolder;
        this.exifData = exifData;
        this.characteristics = characteristics;
        this.captureResult = captureResult;
        this.cameraRotation = cameraRotation;
        this.captureRequest = captureRequest;
        videoCounter = 0;
        fillParams = false;
        this.callback = callback;
        // Create output folder if not exists
        try {
            Files.createDirectories(outputFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        dngBuffers = new ByteBuffer[writeBufferSize];
        writeBufferCounter = 0;
    }

    public void videoCycle(Image image) {
        // Create new thread to save image
        int startCounter = videoCounter;
        if(!fillParams){
            Log.d(TAG, "videoCycle: " + this + " " + image + " " + startCounter);
            int width = image.getPlanes()[0].getRowStride() / image.getPlanes()[0].getPixelStride();
            //int height = image.getHeight();
            // Crop to 16:9
            int height = width * 9 / 16;
            PhotonCamera.getParameters().rawSize = new Point(width, height);
            PhotonCamera.getParameters().FillConstParameters(characteristics, PhotonCamera.getParameters().rawSize);
            PhotonCamera.getParameters().FillDynamicParameters(captureResult, captureRequest, 100);
            PhotonCamera.getParameters().cameraRotation = this.cameraRotation;
            exifData.IMAGE_DESCRIPTION = PhotonCamera.getParameters().toString();
            fillParams = true;
            dngCreator = new DngCreator();
            dngCreator.setParameters(PhotonCamera.getParameters());
            dngCreator.setCompression(false);
            dngCreator.setBitsPerSample(16);
            dngBuffers[0] = dngCreator.dngBuffer(image.getPlanes()[0].getBuffer(), PhotonCamera.getParameters().rawSize.x, PhotonCamera.getParameters().rawSize.y);
            for (int i = 1; i < writeBufferSize; i++) {
                dngBuffers[i] = Allocator.allocateAndCopy(dngBuffers[0].capacity(), dngBuffers[0], 0);
                dngBuffers[i].put(dngBuffers[0]);
                dngBuffers[i].position(0);
            }
            Log.d(TAG, "DNG buffer allocated, size: " + dngBuffers[0].capacity());
            image.close();
            isRecording = true;
        } else {
            if(dngBuffers[0] == null){
                image.close();
                return;
            }
            @SuppressLint("DefaultLocale")
            Thread thread = new Thread(() -> {
                if (fillParams) {
                    threadCounter++;
                    dngCreator.writeFile(dngBuffers[writeBufferCounter%writeBufferSize], image.getPlanes()[0].getBuffer(), outputFolder.resolve(String.format("RAW_%05d.dng", startCounter)).toString());
                    image.close();
                    threadCounter--;
                }
            });
            if(threadCounter < writeBufferSize) {
                thread.start();
            } else {
                image.close();
                Log.d(TAG, "Dropped frame");
            }
            writeBufferCounter++;
        }

        videoCounter++;
    }

    private void processVideo() {

    }

    public void videoEnd() {
        isRecording = false;
    }
}
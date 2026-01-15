package com.particlesdevs.photoncamera.processing;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import androidx.annotation.RequiresApi;
import androidx.exifinterface.media.ExifInterface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import com.particlesdevs.photoncamera.app.ContextProvider;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.ui.camera.views.viewfinder.MainRenderer;
import com.particlesdevs.photoncamera.util.Log;

import com.particlesdevs.photoncamera.BuildConfig;

public class YUVSaver extends DefaultSaver{
    private static final String TAG = "YUVSaver";
    public YUVSaver(ProcessingEventsListener processingEventsListener) {
        super(processingEventsListener);
    }

    @Override
    public void addImage(Image image, int orientation, int targetFormat, int quality, Bundle metadata, MainRenderer renderer) {
        // Check for 10-bit YUV format to encode as HEIC
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && ((image.getFormat() == ImageFormat.YCBCR_P010) || (image.getFormat() == ImageFormat.YUV_420_888))) {
            String usedCodec = PhotonCamera.getSettings().tenBitSurfaceTarget;
            int usedTargetFormat = PhotonCamera.getSettings().previewFormat;
            Log.d(TAG, "YCBCR_P010 format detected, attempting to save via MediaCodec or YCBCR_P010 RAW");

            Path storagePath = null;
            File heicFile = null;

            // YCBCR_P010 or YUV_420_888 RAW
            if (usedTargetFormat == 888888888) {
                storagePath = ImagePath.newYCBCR_P010FilePath();
                heicFile = new File(storagePath.toString());
                String rawPath = heicFile.getAbsolutePath();
                String metaPath = rawPath.substring(0, rawPath.lastIndexOf('.')) + ".txt";
                File metaFile = new File(metaPath);
                if (image.getFormat() == ImageFormat.YCBCR_P010) {
                    saveP010RawWithStride(image, heicFile);
                }
                else {
                    //saveYuv420Raw(image, heicFile);
                    saveP010RawWithStride(image, heicFile);
                }
                saveMetaInfo(image, metaFile, orientation, metadata);
                processingEventsListener.onProcessingFinished("YCBCR_P010 saved: " + storagePath.toAbsolutePath().toString());
                return;
            }

            // SW based PNG encoder solution
            if (usedTargetFormat == 999999993) {
                storagePath = ImagePath.newPNGFilePath();
                heicFile = new File(storagePath.toString());

                Bitmap originalBitmap = null;
                Bitmap rotatedBitmap = null;

                // 1. Convert the YUV Image to a high-precision Bitmap
                switch (image.getFormat()) {
                    case ImageFormat.YCBCR_P010:
                        try {
                            originalBitmap = ImageUtils.p010SdrToF16BitmapGL(image, renderer);
                            AvifEncoder.saveF16BitmapToPng(originalBitmap, heicFile);
                        }
                        catch (Exception e) {
                            Log.e(TAG, Log.getStackTraceString(e));
                        }
                        break;
                }

                image.close();
                processingEventsListener.onProcessingFinished("PNG saved: " + storagePath.toAbsolutePath().toString());
                return;
            }

            // SW based AVIF encoder solution
            if (usedTargetFormat == 999999999) {
                storagePath = ImagePath.newAVIFFilePath();
                heicFile = new File(storagePath.toString());
                AvifEncoder avifEncoder = new AvifEncoder();
                try {
                    avifEncoder.encodeYuvToAvif(image, heicFile, orientation, quality, metadata, renderer);
                }
                catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
                image.close();
                processingEventsListener.onProcessingFinished("AVIF saved: " + storagePath.toAbsolutePath().toString());
                return;
            }

            // SW based HEIC/HEIF encoder solution
            if (usedTargetFormat == 999999991) {
                storagePath = ImagePath.newHEIFFilePath();
                heicFile = new File(storagePath.toString());
                HeifEncoder heifEncoder = new HeifEncoder();
                try {
                    heifEncoder.encodeYuvToHeif(image, heicFile, orientation, quality, metadata);
                }
                catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
                image.close();
                processingEventsListener.onProcessingFinished("HEIF saved: " + storagePath.toAbsolutePath().toString());
                return;
            }

            if (usedCodec.equals("AVIF") || usedCodec.equals("AV1")) {
                storagePath = ImagePath.newAVIFFilePath();
            }
            else if (usedCodec.equals("APV")) {
                storagePath = ImagePath.newAPVFilePath();
            }
            else {
                storagePath = ImagePath.newHEIFFilePath();
            }
            heicFile = new File(storagePath.toString());

            MediaCodec encoder = null;
            MediaMuxer muxer = null;
            boolean muxerStarted = false;
            MediaFormat format = null;
            Size maxEncoderRes = null;
            String mimeVid = MediaFormat.MIMETYPE_VIDEO_HEVC;

            try {
                // 1. Configure and create Muxer and Encoder
                muxer = new MediaMuxer(heicFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_HEIF);

                switch (usedCodec) {
                    case "HEVC":
                    case "H265":
                        mimeVid = MediaFormat.MIMETYPE_VIDEO_HEVC;
                        break;
                    case "DOLBY_VISION":
                    case "DOLBY":
                        mimeVid = MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION;
                        break;
                    case "AV1":
                    case "AVIF":
                        mimeVid = MediaFormat.MIMETYPE_VIDEO_AV1;
                        break;
                    case "APV":
                        mimeVid = MediaFormat.MIMETYPE_VIDEO_APV;
                        break;
                    case "VP8":
                        mimeVid = MediaFormat.MIMETYPE_VIDEO_VP8;
                        break;
                    case "VP9":
                        mimeVid = MediaFormat.MIMETYPE_VIDEO_VP9;
                        break;
                    case "HEIC":
                        mimeVid = MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC;
                        break;
                    default:
                        mimeVid = MediaFormat.MIMETYPE_VIDEO_AVC;
                        break;
                }

                encoder = MediaCodec.createEncoderByType(mimeVid);
                try {
                    encoder = MediaCodec.createEncoderByType(mimeVid);
                    MediaCodecInfo codecInfo = encoder.getCodecInfo();
                    MediaCodecInfo.CodecCapabilities caps = codecInfo.getCapabilitiesForType(mimeVid);
                    MediaCodecInfo.VideoCapabilities videoCaps = caps.getVideoCapabilities();
                    maxEncoderRes = new Size(videoCaps.getSupportedWidths().getUpper(), videoCaps.getSupportedHeights().getUpper());
                    Log.d(TAG, "encodername: " + codecInfo.getName() + " - max encoder resolution: " + maxEncoderRes.toString() + " - HW supported: " + Boolean.toString(codecInfo.isHardwareAccelerated()));
                }
                catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }

                switch (usedCodec) {
                    case "AVIF":
                    case "AV1":
                        format = createAv1Format(Math.min(image.getWidth(), maxEncoderRes.getWidth()), Math.min(image.getHeight(), maxEncoderRes.getHeight()));
                        break;
                    case "APV":
                        format = createApvFormat(Math.min(image.getWidth(), maxEncoderRes.getWidth()), Math.min(image.getHeight(), maxEncoderRes.getHeight()));
                        break;
                    case "HEIC":
                        format = createDedicatedHeicFormat(Math.min(image.getWidth(), maxEncoderRes.getWidth()), Math.min(image.getHeight(), maxEncoderRes.getHeight()));
                        break;
                    case "HEVC":
                        format = createHeicFormat(Math.min(image.getWidth(), maxEncoderRes.getWidth()), Math.min(image.getHeight(), maxEncoderRes.getHeight()));
                        break;
                }

                /*switch (usedCodec) {
                    case "AVIF":
                    case "AV1":
                        format = createAv1Format(Math.min(image.getWidth(), maxEncoderRes.getWidth()), Math.min(image.getHeight(), maxEncoderRes.getHeight()));
                        encoder = MediaCodec.createByCodecName("c2.android.av1.encoder");
                        break;
                    case "APV":
                        format = createApvFormat(Math.min(image.getWidth(), maxEncoderRes.getWidth()), Math.min(image.getHeight(), maxEncoderRes.getHeight()));
                        encoder = MediaCodec.createByCodecName("c2.android.apv.encoder");
                        break;
                    case "HEIC":
                        format = createDedicatedHeicFormat(Math.min(image.getWidth(), maxEncoderRes.getWidth()), Math.min(image.getHeight(), maxEncoderRes.getHeight()));
                        encoder = MediaCodec.createByCodecName("c2.qti.heic.encoder");
                        break;
                    case "HEVC":
                        format = createHeicFormat(Math.min(image.getWidth(), maxEncoderRes.getWidth()), Math.min(image.getHeight(), maxEncoderRes.getHeight()));
                        encoder = MediaCodec.createByCodecName("c2.qti.hevc.encoder.hdr");
                        break;
                }*/

                Log.d(TAG, "Output format: " + format.getString(MediaFormat.KEY_MIME) + "codec max resolution: " + maxEncoderRes.toString());

                try {
                    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                }
                catch (Exception e) {
                    Log.e(TAG, "Encoder configuration failed", e);
                    throw e;
                }
                muxer.setOrientationHint(orientation);

                // 2. Start the encoder
                encoder.start();

                // 3. Feed the image data to the encoder's input buffer
                int inputBufferId = encoder.dequeueInputBuffer(-1); // Wait indefinitely
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufferId);
                    if (inputBuffer != null) {
                        // This is the correct way to copy planar YUV data, respecting strides.
                        Size resResolution = new Size(Math.min(image.getWidth(), maxEncoderRes.getWidth()), Math.min(image.getHeight(), maxEncoderRes.getHeight()));
                        if (resResolution.getWidth() == image.getWidth() && resResolution.getHeight() == image.getHeight()) {
                            copyPlanesToBuffer(image.getPlanes(), image.getWidth(), image.getHeight(), inputBuffer);
                        }
                        else {
                            copyPlanesToBufferCrop(image.getPlanes(), image.getWidth(), image.getHeight(), maxEncoderRes.getWidth(), maxEncoderRes.getHeight(), inputBuffer);
                        }
                        encoder.queueInputBuffer(inputBufferId, 0, inputBuffer.position(), image.getTimestamp(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

                // 4. Process the encoded output and write to muxer
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int trackIndex = -1;

                while (true) {
                    int outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, 1000000);
                    if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.d(TAG,"encoder status: INFO_TRY_AGAIN_LATER");
                    }
                    if (outputBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        Log.d(TAG,"encoder status: INFO_OUTPUT_BUFFERS_CHANGED");
                    }
                    if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        MediaFormat newFormat = encoder.getOutputFormat();
                        trackIndex = muxer.addTrack(newFormat);
                        muxer.start();
                        muxerStarted = true;
                    } else if (outputBufferId >= 0) {
                        if (!muxerStarted) {
                            throw new IllegalStateException("Muxer has not been started.");
                        }
                        ByteBuffer outputBuffer = encoder.getOutputBuffer(outputBufferId);
                        int encodedImageChunkSize = bufferInfo.size;
                        Log.d(TAG, "Encoded data size: " + encodedImageChunkSize + " bytes");
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                        encoder.releaseOutputBuffer(outputBufferId, false);

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break; // Encoding is done
                        }
                    }
                    // Loop until EOS is reached
                }

                Log.d(TAG, "Successfully saved HEIC/AVIF/APV still image to: " + heicFile.getAbsolutePath());
                processingEventsListener.onProcessingFinished("HEIC/AVIF/APV saved: " + heicFile.getName());
            } catch (Exception e) {
                Log.e(TAG, "Failed during HEIC/AVIF/APV encoding process", e);
                processingEventsListener.onProcessingError("HEIC/AVIF/APV encoding failed: " + e.getMessage());
            } finally {
                // 5. Clean up all resources
                if (encoder != null) {
                    try { encoder.stop(); } catch (Exception e) { Log.e(TAG, "Encoder stop error", e); }
                    encoder.release();
                }
                if (muxer != null) {
                    if (muxerStarted) {
                        try { muxer.stop(); } catch (Exception e) { Log.e(TAG, "Muxer stop error", e); }
                    }
                    muxer.release();
                }
                image.close();
            }
            return; // Skip default buffer processing
        }

        // Default behavior for other formats (e.g., YUV_420_888)
        Log.d(TAG, "start buffersize:" + IMAGE_BUFFER.size());
        IMAGE_BUFFER.add(getFrame(image));
        if (IMAGE_BUFFER.size() == PhotonCamera.getCaptureController().mMeasuredFrameCnt && PhotonCamera.getSettings().frameCount != 1) {
            IMAGE_BUFFER.clear();
        }
        if (PhotonCamera.getSettings().frameCount == 1) {
            IMAGE_BUFFER.clear();
            processingEventsListener.onProcessingFinished("YUV: Single Frame, Not Processed!");
        }
    }

    private MediaFormat createHeicFormat(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 200_000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 1);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
        format.setInteger(MediaFormat.KEY_IS_DEFAULT, 1);
        format.setLong(MediaFormat.KEY_DURATION, 0);
        format.setInteger(MediaFormat.KEY_ROTATION, 0);

        //final int BUFFER_SIZE_HINT = width * height * 3 / 2;
        //format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE_HINT * 3);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_HLG);
            //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_ST2084);
            //format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
            //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_LINEAR);
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel62);
        }
        return format;
    }

    private MediaFormat createAv1Format(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AV1, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 20_000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 0);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
        format.setInteger(MediaFormat.KEY_IS_DEFAULT, 1);
        format.setLong(MediaFormat.KEY_DURATION, 0);
        format.setInteger(MediaFormat.KEY_ROTATION, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
            //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_HLG);
            //format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_LINEAR);
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AV1Level41);
        }
        return format;
    }

    private MediaFormat createApvFormat(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_APV, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 200_000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 0);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
        format.setInteger(MediaFormat.KEY_IS_DEFAULT, 1);
        format.setLong(MediaFormat.KEY_DURATION, 0);
        format.setInteger(MediaFormat.KEY_ROTATION, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
            //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_HLG);
            //format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_LINEAR);
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.APVProfile422_10HDR10);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.APVLevel71Band3);
        }
        return format;
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private MediaFormat createDedicatedHeicFormat(int width, int height) {
        String mimeType = "image/heic";
        MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        format.setInteger(MediaFormat.KEY_QUALITY, 85);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 1);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
        format.setInteger(MediaFormat.KEY_IS_DEFAULT, 1);
        format.setLong(MediaFormat.KEY_DURATION, 0);
        format.setInteger(MediaFormat.KEY_ROTATION, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_HLG);
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
            //format.setInteger(MediaFormat.KEY_PROFILE, 0x6003);
            //format.setInteger(MediaFormat.KEY_LEVEL, 0x600C);
        }
        return format;
    }

    // Helper function to copy planar image data into a single buffer, respecting row strides
    private void copyPlanesToBuffer(Image.Plane[] planes, int width, int height, ByteBuffer dst) {
        Image.Plane yPlane = planes[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        int yRowStride = yPlane.getRowStride();
        int yPixelStride = yPlane.getPixelStride();
        int yRowDataWidthInBytes = width * yPixelStride;
        byte[] rowData = new byte[yRowStride];
        for (int j = 0; j < height; j++) {
            yBuffer.position(j * yRowStride);
            yBuffer.get(rowData, 0, yRowDataWidthInBytes);
            dst.put(rowData, 0, yRowDataWidthInBytes);
        }
        Image.Plane uvPlane = planes[1];
        ByteBuffer uvBuffer = uvPlane.getBuffer();
        int uvRowStride = uvPlane.getRowStride();
        int uvPixelStride = uvPlane.getPixelStride();
        int uvHeight = height / 2;
        int uvWidth = width / 2;
        int uvRowDataWidthInBytes = uvWidth * uvPixelStride;
        for (int j = 0; j < uvHeight; j++) {
            uvBuffer.position(j * uvRowStride);
            uvBuffer.get(rowData, 0, uvRowDataWidthInBytes);
            dst.put(rowData, 0, uvRowDataWidthInBytes);
        }
    }

    private boolean copyPlanesToBufferCrop(Image.Plane[] planes, int widthIn, int heightIn, int widthOut, int heightOut, ByteBuffer dst) {
        if (widthIn < widthOut || heightIn < heightOut) {
            Log.e(TAG, "Source image is smaller than target crop size! Cropping disabled.");
            copyPlanesToBuffer(planes, widthIn, heightIn, dst); // Fallback to non-cropping version
            return false;
        }

        // --- Y Plane (Luma) ---
        // Calculate the top-left corner of the crop rectangle.
        final int yCropX = (widthIn - widthOut) / 2;
        // Ensure the Y offset is even for correct Chroma alignment.
        final int yCropY = ((heightIn - heightOut) / 2) & ~1;

        Image.Plane yPlane = planes[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        int yRowStride = yPlane.getRowStride();
        int yPixelStride = yPlane.getPixelStride();
        byte[] rowDataY = new byte[widthOut * yPixelStride];

        for (int j = 0; j < heightOut; j++) {
            int sourceRow = yCropY + j;
            int sourceOffset = sourceRow * yRowStride + yCropX * yPixelStride;
            yBuffer.position(sourceOffset);
            yBuffer.get(rowDataY, 0, widthOut * yPixelStride);
            dst.put(rowDataY, 0, widthOut * yPixelStride);
        }

        // --- UV Plane (Chroma, Interleaved) ---
        // The chroma planes are subsampled by 2.
        final int uvWidthOut = widthOut / 2;
        final int uvHeightOut = heightOut / 2;
        final int uvCropX = yCropX / 2;
        final int uvCropY = yCropY / 2;

        Image.Plane uvPlane = planes[1];
        ByteBuffer uvBuffer = uvPlane.getBuffer();
        int uvRowStride = uvPlane.getRowStride();
        int uvPixelStride = uvPlane.getPixelStride();
        byte[] rowDataUV = new byte[uvWidthOut * uvPixelStride];

        for (int j = 0; j < uvHeightOut; j++) {
            int sourceRow = uvCropY + j;
            int sourceOffset = sourceRow * uvRowStride + uvCropX * uvPixelStride;
            uvBuffer.position(sourceOffset);
            uvBuffer.get(rowDataUV, 0, uvWidthOut * uvPixelStride);
            dst.put(rowDataUV, 0, uvWidthOut * uvPixelStride);
        }

        return true;
    }

    public void saveP010RawWithStride(Image image, File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            yBuffer.rewind();
            byte[] yBytes = new byte[yBuffer.remaining()];
            yBuffer.get(yBytes);
            fos.write(yBytes);

            ByteBuffer uvBuffer = image.getPlanes()[1].getBuffer();
            uvBuffer.rewind();
            byte[] uvBytes = new byte[uvBuffer.remaining()];
            uvBuffer.get(uvBytes);
            fos.write(uvBytes);

            long expectedSize = (long) (image.getWidth() * image.getHeight() * (image.getFormat() == ImageFormat.YCBCR_P010 ? 3.0 : 1.5));
            long currentSize = file.length();

            if (currentSize < expectedSize) {
                fos.write(0);
                Log.d(TAG, "Padding added to reach expected size");
            }

            Log.d(TAG, "RAW successfully saved. Size: " + file.length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void saveP010Raw(Image image, File file) {
        int size = image.getWidth() * image.getHeight() * 3;
        ByteBuffer cleanBuffer = ByteBuffer.allocateDirect(size);

        copyPlanesToBuffer(image.getPlanes(), image.getWidth(), image.getHeight(), cleanBuffer);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            cleanBuffer.flip();
            byte[] content = new byte[cleanBuffer.remaining()];
            cleanBuffer.get(content);
            fos.write(content);
            Log.d(TAG, "P010 still image stored in compact form (without strides).");
        } catch (IOException e) {
            Log.e(TAG, "P010 still image creation failed.", e);
        }
    }

    public void saveYuv420Raw(Image image, File file) {
        int size = (int) (image.getWidth() * image.getHeight() * 1.5f);
        ByteBuffer cleanBuffer = ByteBuffer.allocateDirect(size);

        copyPlanesToBuffer(image.getPlanes(), image.getWidth(), image.getHeight(), cleanBuffer);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            cleanBuffer.flip();
            byte[] content = new byte[cleanBuffer.remaining()];
            cleanBuffer.get(content);
            fos.write(content);
            Log.d(TAG, "YUV_420_888 stored in compact form (8-bit, no strides).");
        } catch (IOException e) {
            Log.e(TAG, "YUV_420_888 saving failed.", e);
        }
    }

    private void saveMetaInfo(Image image, File file, int orientation, Bundle metadata) {
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            boolean isP010 = (image.getFormat() == ImageFormat.YCBCR_P010);
            String formatName = isP010 ? "p010le" : "nv12";
            String widthHeight = image.getWidth() + "x" + image.getHeight();
            String rawFileName = file.getName().replace(".txt", ".raw");
            String transposeFilter = "";
            switch (orientation) {
                case 90:  transposeFilter = ",transpose=1"; break;
                case 180: transposeFilter = ",transpose=2,transpose=2"; break;
                case 270: transposeFilter = ",transpose=2"; break;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("FFmpeg Metadata & Hints:\n");
            sb.append("========================\n");
            sb.append("PhotonCamera Version: ").append(BuildConfig.VERSION_NAME).append(".").append(BuildConfig.VERSION_BUILD).append("\n");
            sb.append("Resolution: ").append(widthHeight).append("\n");
            sb.append("Orientation: ").append(orientation).append(" degrees\n");
            sb.append("Format: ").append(isP010 ? "10-bit P010" : "8-bit YUV420").append("\n");

            if (metadata != null) {
                if (metadata.containsKey("aperture")) {
                    sb.append("Aperture: F").append(metadata.getFloat("aperture")).append("\n");
                }
                if (metadata.containsKey("iso")) {
                    sb.append("ISO: ").append(metadata.get("iso")).append("\n");
                }
                if (metadata.containsKey("exposureTimeStr")) {
                    sb.append("Exposure Time: ").append(metadata.getString("exposureTimeStr")).append("\n");
                }
                if (metadata.containsKey("focalLength")) {
                    sb.append("Focal Length: ").append(metadata.get("focalLength")).append(" mm\n");
                }
                if (metadata.containsKey("focal35mm")) {
                    sb.append("Focal Length 35mm: ").append(metadata.get("focal35mm")).append(" mm\n");
                }
                if (metadata.containsKey("physCamID")) {
                    sb.append("Physical Camera ID: ").append(metadata.getString("physCamID")).append("\n");
                }
                if (metadata.containsKey("logiCamID")) {
                    sb.append("Logical Camera ID: ").append(metadata.getString("logiCamID")).append("\n");
                }
                sb.append("Noise Reduction: ").append((PhotonCamera.getSettings().noiseProcessing > 0) ? "Enabled" : "Disabled").append("\n");
                sb.append("Edge Processing: ").append((PhotonCamera.getSettings().edgeProcessing > 0) ? "Enabled" : "Disabled").append("\n");
                sb.append("Brand: ").append(Build.BRAND).append("\n");
                //sb.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
                sb.append("Device: ").append(Build.DEVICE).append("\n");
                sb.append("Model: ").append(Build.MODEL).append("\n");
                sb.append("SoC: ").append(Build.SOC_MODEL).append("\n");
                sb.append("Contrast Curve: ").append(PhotonCamera.getSettings().contrastCurve).append("\n");
            }

            sb.append("\n[1. VIEWING]\n");
            sb.append("ffplay -f rawvideo -pixel_format ").append(formatName)
                    .append(" -video_size ").append(widthHeight)
                    .append(" -vf \"setdar=4/3").append(transposeFilter).append(",scale=1440:-1\" \"")
                    .append(rawFileName).append("\"\n");

            sb.append("\n[2. ENCODING AVIF]\n");
            if (isP010) {
                // 10-bit HDR path
                sb.append("ffmpeg -f rawvideo -pixel_format p010le -video_size ").append(widthHeight)
                        .append(" -i \"").append(rawFileName).append("\" -c:v libaom-av1 -still-picture 1 ")
                        .append("-pix_fmt yuv420p10le -color_primaries bt2020 -color_trc smpte2084 -colorspace bt2020_ncl ")
                        .append("-vf \"setdar=4/3").append(transposeFilter).append("\" ")
                        .append("-crf 20 -cpu-used 6 \"").append(rawFileName.replace(".raw", "_hdr.avif")).append("\"\n");
            } else {
                // 8-bit SDR path
                sb.append("ffmpeg -f rawvideo -pixel_format nv12 -video_size ").append(widthHeight)
                        .append(" -i \"").append(rawFileName).append("\" -c:v libaom-av1 -still-picture 1 ")
                        .append("-pix_fmt yuv420p -vf \"setdar=4/3").append(transposeFilter).append("\" ")
                        .append("-crf 20 -cpu-used 8 \"").append(rawFileName.replace(".raw", "_8bit.avif")).append("\"\n");
            }

            writer.write(sb.toString());
            Log.d(TAG, "Meta-Info with orientation " + orientation + " saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save meta info", e);
        }
    }
}

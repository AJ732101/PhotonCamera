package com.particlesdevs.photoncamera.processing;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Environment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.util.Log;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class YUVSaver extends DefaultSaver{
    private static final String TAG = "YUVSaver";
    public YUVSaver(ProcessingEventsListener processingEventsListener) {
        super(processingEventsListener);
    }

    @Override
    public void addImage(Image image) {
        // Check for 10-bit YUV format to encode as HEIC
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                image.getFormat() == ImageFormat.YCBCR_P010) {

            Log.d(TAG, "YCBCR_P010 format detected, attempting to save via MediaCodec.");

            Path heicPath = ImagePath.newHEICFilePath();
            File heicFile = new File(heicPath.toString());

            MediaCodec encoder = null;
            MediaMuxer muxer = null;
            boolean muxerStarted = false;

            try {
                // 1. Configure and create Muxer and Encoder
                muxer = new MediaMuxer(heicFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_HEIF);
                MediaFormat format = createHeicFormat(image.getWidth(), image.getHeight());
                //MediaFormat format1 = createDedicatedHeicFormat(image.getWidth(), image.getHeight());
                //String codecName = "c2.qti.heic.encoder";
                //encoder = MediaCodec.createByCodecName(codecName);
                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
                //MediaFormat format = createAv1Format(image.getWidth(), image.getHeight());
                //encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AV1);
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                muxer.setOrientationHint(0);

                // 2. Start the encoder
                encoder.start();

                // 3. Feed the image data to the encoder's input buffer
                int inputBufferId = encoder.dequeueInputBuffer(-1); // Wait indefinitely
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = encoder.getInputBuffer(inputBufferId);
                    if (inputBuffer != null) {
                        // This is the correct way to copy planar YUV data, respecting strides.
                        copyPlanesToBuffer(image.getPlanes(), image.getWidth(), image.getHeight(), inputBuffer);
                        encoder.queueInputBuffer(inputBufferId, 0, inputBuffer.position(), image.getTimestamp(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }

                // 4. Process the encoded output and write to muxer
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int trackIndex = -1;

                while (true) {
                    int outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, 10000);
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
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                        encoder.releaseOutputBuffer(outputBufferId, false);

                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break; // Encoding is done
                        }
                    }
                    // Loop until EOS is reached
                }

                Log.d(TAG, "Successfully saved HEIC file to: " + heicFile.getAbsolutePath());
                processingEventsListener.onProcessingFinished("HEIC saved: " + heicFile.getName());

            } catch (Exception e) {
                Log.e(TAG, "Failed during HEIC encoding process", e);
                processingEventsListener.onProcessingError("HEIC encoding failed: " + e.getMessage());
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
        format.setInteger(MediaFormat.KEY_BIT_RATE, 200_000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 0);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
        format.setInteger(MediaFormat.KEY_IS_DEFAULT, 1);
        format.setLong(MediaFormat.KEY_DURATION, 0);
        format.setInteger(MediaFormat.KEY_ROTATION, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
            //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_ST2084);
            //format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_LINEAR);
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10);
            //format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel6);
        }
        return format;
    }

    private MediaFormat createAv1Format(int width, int height) {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AV1, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUVP010);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 10_000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 0);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0);
        format.setInteger(MediaFormat.KEY_IS_DEFAULT, 1);
        format.setLong(MediaFormat.KEY_DURATION, 0);
        format.setInteger(MediaFormat.KEY_ROTATION, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
            //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_HLG);
            //format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
            //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_LINEAR);
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
            format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10);
            format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AV1Level41);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_HLG);
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
            format.setInteger(MediaFormat.KEY_PROFILE, 0x6003);
            format.setInteger(MediaFormat.KEY_LEVEL, 0x600C);
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
        byte[] rowData = new byte[yRowStride]; // Wiederverwendbarer Puffer für eine Zeile
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
}

package com.particlesdevs.photoncamera.processing;

import android.graphics.ImageFormat;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
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
import android.util.Size;
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
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) && (image.getFormat() == ImageFormat.YCBCR_P010)) {
            String usedCodec = PhotonCamera.getSpecific().specificSetting.YCBCR_P010_TargetFormat;
            Log.d(TAG, "YCBCR_P010 format detected, attempting to save via MediaCodec.");

            Path heicPath = null;
            if (usedCodec.equals("AVIF")) {
                heicPath = ImagePath.newAVIFFilePath();
            }
            else if (usedCodec.equals("APV")) {
                heicPath = ImagePath.newAPVFilePath();
            }
            else {
                heicPath = ImagePath.newHEIFFilePath();
            }
            File heicFile = new File(heicPath.toString());

            MediaCodec encoder = null;
            MediaMuxer muxer = null;
            boolean muxerStarted = false;
            MediaFormat format = null;

            try {
                // 1. Configure and create Muxer and Encoder
                muxer = new MediaMuxer(heicFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_HEIF);

                switch (usedCodec) {
                    case "AVIF":
                        format = createAv1Format(Math.min(image.getWidth(), 1920), Math.min(image.getHeight(), 1920));
                        encoder = MediaCodec.createByCodecName("c2.android.av1.encoder");
                        //encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AV1);
                        break;
                    case "APV":
                        format = createApvFormat(Math.min(image.getWidth(), 1920), Math.min(image.getHeight(), 1920));
                        encoder = MediaCodec.createByCodecName("c2.android.apv.encoder");
                        //encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AV1);
                        break;
                    case "HEIC":
                        format = createDedicatedHeicFormat(image.getWidth(), image.getHeight());
                        encoder = MediaCodec.createByCodecName("c2.qti.heic.encoder");
                        //encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC);
                        break;
                    case "HEVC":
                        //encoder = MediaCodec.createByCodecName("c2.qti.hevc.encoder");
                        encoder = MediaCodec.createByCodecName("c2.qti.hevc.encoder");
                        //encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
                        format = createHeicFormat(image.getWidth(), image.getHeight());
                        break;
                }

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
                        if (usedCodec.equals("AVIF") || usedCodec.equals("APV")) {
                            copyPlanesToBufferCrop(image.getPlanes(), image.getWidth(), image.getHeight(), inputBuffer);
                        }
                        else {
                            copyPlanesToBuffer(image.getPlanes(), image.getWidth(), image.getHeight(), inputBuffer);
                        }
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

                /*boolean encodingSuccessful = false;
                while (true) {
                    int outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                    if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        Log.d(TAG, "Encoder output format changed. Adding track to muxer.");
                        MediaFormat newFormat = encoder.getOutputFormat();
                        trackIndex = muxer.addTrack(newFormat);
                        muxer.start();
                        muxerStarted = true;
                    } else if (outputBufferId >= 0) {
                        ByteBuffer outputBuffer = encoder.getOutputBuffer(outputBufferId);
                        if (outputBuffer == null) {
                            throw new RuntimeException("encoder.getOutputBuffer returned null");
                        }

                        if (bufferInfo.size > 0 && muxerStarted) {
                            // Adjust buffer to the actual data size
                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                            Log.d(TAG, "Writing sample data to muxer. Size: " + bufferInfo.size + " bytes");
                            muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                            encodingSuccessful = true;
                        }

                        encoder.releaseOutputBuffer(outputBufferId, false);

                        // Check for the end of the stream flag
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (!encodingSuccessful) {
                                throw new IOException("Encoder finished without producing any data. Check encoder configuration and input data.");
                            }
                            Log.d(TAG, "End of stream reached. Encoding successful.");
                            break; // Exit the loop
                        }
                    } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.d(TAG, "No output from encoder available yet. Retrying...");
                    }
                    // Continue looping
                }*/

                Log.d(TAG, "Successfully saved HEIC/AVIF file to: " + heicFile.getAbsolutePath());
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
        format.setInteger(MediaFormat.KEY_BIT_RATE, 200_000_000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        format.setInteger(MediaFormat.KEY_IS_DEFAULT, 1);
        format.setLong(MediaFormat.KEY_DURATION, 1);
        format.setInteger(MediaFormat.KEY_ROTATION, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
            //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_ST2084);
            //format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_LINEAR);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
            //format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_LINEAR);
            //format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
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

    private void copyPlanesToBufferCrop(Image.Plane[] planes, int width, int height, ByteBuffer dst) {
        final int TARGET_SIZE = 1920;

        if (width < TARGET_SIZE || height < TARGET_SIZE) {
            Log.e(TAG, "Source image is smaller than target crop size! Cropping disabled.");
            copyPlanesToBuffer(planes, width, height, dst); // Fallback to non-cropping version
            return;
        }

        // --- Y Plane (Luma) ---
        final int yCropX = (width - TARGET_SIZE) / 2;
        // Ensure even offset for chroma alignment
        final int yCropY = ((height - TARGET_SIZE) / 2) & ~1;

        Image.Plane yPlane = planes[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        int yRowStride = yPlane.getRowStride();
        int yPixelStride = yPlane.getPixelStride();

        byte[] rowDataY = new byte[TARGET_SIZE * yPixelStride];

        for (int j = 0; j < TARGET_SIZE; j++) {
            int sourceRow = yCropY + j;
            int sourceOffset = sourceRow * yRowStride + yCropX * yPixelStride;
            yBuffer.position(sourceOffset);
            yBuffer.get(rowDataY, 0, TARGET_SIZE * yPixelStride);
            dst.put(rowDataY, 0, TARGET_SIZE * yPixelStride);
        }

        // --- UV Plane (Chroma, Interleaved) ---
        final int uvTargetSize = TARGET_SIZE / 2;
        final int uvCropX = yCropX / 2;
        final int uvCropY = yCropY / 2;

        Image.Plane uvPlane = planes[1];
        ByteBuffer uvBuffer = uvPlane.getBuffer();
        int uvRowStride = uvPlane.getRowStride();
        int uvPixelStride = uvPlane.getPixelStride(); // This should be 4 for 16-bit interleaved CbCr

        byte[] rowDataUV = new byte[uvTargetSize * uvPixelStride];

        for (int j = 0; j < uvTargetSize; j++) {
            int sourceRow = uvCropY + j;
            int sourceOffset = sourceRow * uvRowStride + uvCropX * uvPixelStride;
            uvBuffer.position(sourceOffset);
            uvBuffer.get(rowDataUV, 0, uvTargetSize * uvPixelStride);
            dst.put(rowDataUV, 0, uvTargetSize * uvPixelStride);
        }
    }

    /**
     * Queries a specific encoder to find its maximum supported resolution for a given MIME type.
     * @param codecName The exact name of the encoder, e.g., "c2.android.av1.encoder".     * @param mimeType The MIME type to check, e.g., MediaFormat.MIMETYPE_VIDEO_AV1.
     * @return A Size object with the maximum width and height, or null if not found.
     */
    private Size getMaximumSupportedResolution(String codecName, String mimeType) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }
        try {
            MediaCodecList codecList = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
            for (MediaCodecInfo info : codecList.getCodecInfos()) {
                if (!info.isEncoder() || !info.getName().equalsIgnoreCase(codecName)) {
                    continue;
                }
                MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mimeType);
                if (caps == null) continue;

                MediaCodecInfo.VideoCapabilities videoCaps = caps.getVideoCapabilities();
                if (videoCaps == null) continue;

                int maxWidth = videoCaps.getSupportedWidths().getUpper();
                int maxHeight = videoCaps.getSupportedHeights().getUpper();
                Log.d(TAG, "Max resolution for " + codecName + ": " + maxWidth + "x" + maxHeight);
                return new Size(maxWidth, maxHeight);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not get max resolution for codec: " + codecName, e);
        }
        return null;
    }
}

package com.particlesdevs.photoncamera.capture;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import com.particlesdevs.photoncamera.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class RecordingUtils {
    static class VideoEncoderCallback extends MediaCodec.Callback {
        private MediaMuxer mMediaMuxer;
        public boolean mMuxerStarted = false;
        private final Object mMuxerLock = new Object();
        private MuxerThread mMuxerThread;
        private EncoderData mEncoderData = null;

        public VideoEncoderCallback(MediaMuxer muxer, EncoderData encoderData) {
            this.mMediaMuxer = muxer;
            this.mEncoderData = encoderData;
        }

        public void setMuxerThread(MuxerThread muxerThread) {
            this.mMuxerThread = muxerThread;
        }

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            //Log.d("VideoEncoderCallback", "onInputBufferAvailable");
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            //Log.d("VideoEncoderCallback", "onOutputBufferAvailable");
            ByteBuffer outputBuffer = codec.getOutputBuffer(index);
            try {
                if (mMuxerStarted && outputBuffer != null) {
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("VideoEncoderCallback", "received EOS-Flag - terminate recording");
                        mMuxerThread.signalVideoEOS();
                        // stop muxer here or in main logic
                    }
                    try {
                        mMuxerThread.queueVideoSample(outputBuffer, info);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                codec.releaseOutputBuffer(index, false);
            }
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            synchronized (mMuxerLock) {
                Log.d("VideoEncoderCallback", "onOutputFormatChanged");
                if (mMuxerStarted) {
                    Log.w("VideoEncoderCallback", "tried to start MediaMuxer a second time");
                    return;
                }
                mEncoderData.mVideoTrackIndex = mMediaMuxer.addTrack(format);
                if (mEncoderData.mVideoTrackIndex >= 0) {
                    try {
                        mMediaMuxer.start();
                        mMuxerThread.setVideoTrackIndex(mEncoderData.mVideoTrackIndex);
                        mMuxerThread.start();
                        mMuxerStarted = true;
                        Log.d("VideoEncoderCallback:onOutputFormatChanged", "MediaMuxer was started, Track-Index: " + mEncoderData.mVideoTrackIndex);
                    } catch (Exception e) {
                        Log.e("VideoEncoderCallback:VideoEncoderCallback", "MediaMuxer start failed - " + e.getMessage());
                    }
                } else {
                    Log.e("VideoEncoderCallback:VideoEncoderCallback", "Unable to add video track to MediaMuxer");
                }
            }
        }
        /*public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            synchronized (mMuxerLock) {
                Log.d("VideoEncoderCallback", "onOutputFormatChanged");
                if (mMuxerStarted) {
                    Log.w("VideoEncoderCallback", "tried to start MediaMuxer a second time");
                    return;
                }
                mEncoderData.mVideoTrackIndex = mMediaMuxer.addTrack(format);
                if (mEncoderData.mVideoTrackIndex >= 0) {
                    try {
                        mMuxerThread.setVideoTrackIndex(mEncoderData.mVideoTrackIndex);
                        Log.d("VideoEncoderCallback:onOutputFormatChanged", "MediaMuxer was started, Track-Index: " + mEncoderData.mVideoTrackIndex);
                    } catch (Exception e) {
                        Log.e("VideoEncoderCallback:VideoEncoderCallback", "MediaMuxer start failed - " + e.getMessage());
                    }
                } else {
                    Log.e("VideoEncoderCallback:VideoEncoderCallback", "Unable to add video track to MediaMuxer");
                }
                if ((mEncoderData.mVideoTrackIndex >= 0) && (mEncoderData.mAudioTrackIndex >= 0)) {
                    try {
                        mMediaMuxer.start();
                        mMuxerThread.start();
                        mMuxerStarted = true;
                        Log.d("VideoEncoderCallback:onOutputFormatChanged", "MediaMuxer was started, Track-Index: " + mEncoderData.mVideoTrackIndex);
                    } catch (Exception e) {
                        Log.e("VideoEncoderCallback:VideoEncoderCallback", "MediaMuxer start failed - " + e.getMessage());
                    }
                } else {
                    Log.e("VideoEncoderCallback:VideoEncoderCallback", "Unable to add video track to MediaMuxer");
                }
            }
        }*/

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Log.e("MediaCodec", e.getDiagnosticInfo());
        }
    }

    static class AudioEncoderCallback extends MediaCodec.Callback {
        private MediaMuxer mMediaMuxer;
        public boolean mMuxerStarted = false;
        private final Object mMuxerLock = new Object();
        private MuxerThread mMuxerThread;
        private EncoderData mEncoderData = null;

        public AudioEncoderCallback(MediaMuxer muxer, EncoderData encoderData) {
            this.mMediaMuxer = muxer;
            this.mEncoderData = encoderData;
        }

        public void setMuxerThread(MuxerThread muxerThread) {
            this.mMuxerThread = muxerThread;
        }

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            Log.d("AudioEncoderCallback", "onInputBufferAvailable");
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            //Log.d("AudioEncoderCallback", "onOutputBufferAvailable");
            ByteBuffer outputBuffer = codec.getOutputBuffer(index);
            try {
                if (mMuxerStarted && outputBuffer != null) {
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("VideoEncoderCallback", "received EOS-Flag - terminate recording");
                        mMuxerThread.signalAudioEOS();
                        // stop muxer here or in main logic
                    }
                    try {
                        mMuxerThread.queueAudioSample(outputBuffer, info);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                codec.releaseOutputBuffer(index, false);
            }
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            synchronized (mMuxerLock) {
                Log.d("AudioEncoderCallback", "onOutputFormatChanged");
                if (mMuxerStarted) {
                    Log.w("AudioEncoderCallback", "tried to start MediaMuxer a second time");
                    return;
                }
                mEncoderData.mAudioTrackIndex = mMediaMuxer.addTrack(format);
                if (mEncoderData.mAudioTrackIndex >= 0) {
                    try {
                        mMuxerThread.setAudioTrackIndex(mEncoderData.mAudioTrackIndex);
                        Log.d("AudioEncoderCallback:onOutputFormatChanged", "MediaMuxer was started, Track-Index: " + mEncoderData.mAudioTrackIndex);
                    } catch (Exception e) {
                        Log.e("AudioEncoderCallback:VideoEncoderCallback", "MediaMuxer start failed - " + e.getMessage());
                    }
                } else {
                    Log.e("AudioEncoderCallback:VideoEncoderCallback", "Unable to add video track to MediaMuxer");
                }
                if ((mEncoderData.mVideoTrackIndex >= 0) && (mEncoderData.mAudioTrackIndex >= 0)) {
                    try {
                        mMediaMuxer.start();
                        mMuxerThread.start();
                        mMuxerStarted = true;
                        Log.d("AudioEncoderCallback:onOutputFormatChanged", "MediaMuxer was started, Track-Index: " + mEncoderData.mAudioTrackIndex);
                    } catch (Exception e) {
                        Log.e("AudioEncoderCallback:VideoEncoderCallback", "MediaMuxer start failed - " + e.getMessage());
                    }
                } else {
                    Log.e("AudioEncoderCallback:VideoEncoderCallback", "Unable to add video track to MediaMuxer");
                }
            }
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Log.e("MediaCodec", e.getDiagnosticInfo());
        }
    }

    static class EncoderData {
        public int mVideoTrackIndex = -1;
        public int mAudioTrackIndex = -1;
    }

    static class MuxerData {
        public final ByteBuffer data;
        public final MediaCodec.BufferInfo info;

        public MuxerData(ByteBuffer data, MediaCodec.BufferInfo info) {
            this.data = data;
            this.info = new MediaCodec.BufferInfo();
            this.info.set(info.offset, info.size, info.presentationTimeUs, info.flags);
        }
    }

    static class MuxerThread extends Thread {

        private static final int MAX_QUEUE_SIZE = 100;
        // Puffer für die Übertragung von MuxerData zwischen Threads
        private final ArrayBlockingQueue<MuxerData> mMuxerVideoQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
        private final ArrayBlockingQueue<MuxerData> mMuxerAudioQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

        private final MediaMuxer mMediaMuxer;
        private volatile boolean mIsRunning = false;
        private int mAudioTrackIndex = -1; // video track index
        private int mVideoTrackIndex = -1; // video track index
        private volatile boolean mSawVideoEOS = false; // End-of-Stream flag
        private volatile boolean mSawAudioEOS = false; // End-of-Stream flag

        public MuxerThread(MediaMuxer muxer) {
            this.mMediaMuxer = muxer;
        }

        public void setVideoTrackIndex(int index) {
            mVideoTrackIndex = index;
        }
        public void setAudioTrackIndex(int index) {
            mAudioTrackIndex = index;
        }

        public void queueVideoSample(ByteBuffer data, MediaCodec.BufferInfo info) throws InterruptedException {
            ByteBuffer copy = ByteBuffer.allocateDirect(info.size);
            data.position(info.offset);
            data.limit(info.offset + info.size);
            copy.put(data);
            copy.flip();

            mMuxerVideoQueue.put(new MuxerData(copy, info));
        }

        public void queueAudioSample(ByteBuffer data, MediaCodec.BufferInfo info) throws InterruptedException {
            ByteBuffer copy = ByteBuffer.allocateDirect(info.size);
            data.position(info.offset);
            data.limit(info.offset + info.size);
            copy.put(data);
            copy.flip();

            mMuxerAudioQueue.put(new MuxerData(copy, info));
        }

        public void signalVideoEOS() {
            mSawVideoEOS = true;
            // Optional: Füge ein leeres (null) Sample zur Queue hinzu, um den Thread aufzuwecken
            // mMuxerQueue.put(null);
        }

        public void signalAudioEOS() {
            mSawAudioEOS = true;
            // Optional: Füge ein leeres (null) Sample zur Queue hinzu, um den Thread aufzuwecken
            // mMuxerQueue.put(null);
        }

        @Override
        public void run() {
            mIsRunning = true;
            Log.d("MuxerThread", "Muxer thread started");

            while (mIsRunning) {
                try {
                    MuxerData dataVideo = mMuxerVideoQueue.take();
                    //MuxerData dataAudio = mMuxerAudioQueue.take();
                    if (mVideoTrackIndex != -1) {
                        mMediaMuxer.writeSampleData(mVideoTrackIndex, dataVideo.data, dataVideo.info);
                    }
                    if ((dataVideo.info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("MuxerThread", "Received EOS-Flag in muxer thread");
                        mIsRunning = false;
                    }
                    /*if (mAudioTrackIndex != -1) {
                        mMediaMuxer.writeSampleData(mAudioTrackIndex, dataAudio.data, dataAudio.info);
                    }
                    if ((dataAudio.info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d("MuxerThread", "Received EOS-Flag in muxer thread");
                        mIsRunning = false;
                    }*/
                } catch (InterruptedException e) {
                    Log.d("MuxerThread", "Thread interrupted, terminate loop");
                    mIsRunning = false;
                } catch (Exception e) {
                    Log.e("MuxerThread", "Muxing error: " + e.getMessage());
                }
            }
            Log.d("MuxerThread", "Muxer thread terminated");
        }

        public void quit() {
            mIsRunning = false;
            this.interrupt();
        }
    }
}

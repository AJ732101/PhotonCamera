// FLAC audio encoder using technicallyflac (uncompressed FLAC, verbatim subframes).
// Audio capture is done in Java via AudioRecord; this file only handles encoding.

#define TECHNICALLYFLAC_IMPLEMENTATION
#include "deps/technicallyflac.h"

#include <jni.h>
#include <android/log.h>
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <cerrno>
#include <unistd.h>

#define FLAC_TAG "FlacRecorder"
#define FLOGD(...) __android_log_print(ANDROID_LOG_DEBUG, FLAC_TAG, __VA_ARGS__)
#define FLOGE(...) __android_log_print(ANDROID_LOG_ERROR, FLAC_TAG, __VA_ARGS__)

struct FlacEncContext {
    FILE *file;
    technicallyflac *flac;
    uint8_t *encBuf;
    uint32_t encBufSize;
    int32_t *ch[8];
    int channels;
    int blockSize;
};

extern "C" {

/**
 * Open the FLAC encoder using a file descriptor obtained from Java.
 * Using a Java-side fd bypasses FUSE file-type restrictions on Android 11+
 * that prevent native fopen() from creating audio files in DCIM directories.
 */
JNIEXPORT jlong JNICALL
Java_com_particlesdevs_photoncamera_util_FlacAudioRecorder_nativeOpenFd(
        JNIEnv */*env*/, jobject /*obj*/,
        jint fd, jint sampleRate, jint channels, jint bitDepth, jint blockSize) {

    // Duplicate the fd so native owns its own copy; Java closes the original
    int dupFd = dup((int)fd);
    if (dupFd < 0) {
        FLOGE("dup() failed: %s", strerror(errno));
        return 0;
    }

    FILE *f = fdopen(dupFd, "wb");
    if (!f) {
        FLOGE("fdopen() failed: %s", strerror(errno));
        close(dupFd);
        return 0;
    }

    auto *ctx = new FlacEncContext();
    memset(ctx, 0, sizeof(FlacEncContext));
    ctx->file = f;
    ctx->channels = (int)channels;
    ctx->blockSize = (int)blockSize;

    ctx->flac = (technicallyflac *)malloc(technicallyflac_size());
    if (!ctx->flac) {
        FLOGE("Cannot allocate flac context");
        fclose(f);
        delete ctx;
        return 0;
    }

    if (technicallyflac_init(ctx->flac, (uint32_t)blockSize, (uint32_t)sampleRate,
                             (uint8_t)channels, (uint8_t)bitDepth) != 0) {
        FLOGE("technicallyflac_init failed (blockSize=%d sr=%d ch=%d bps=%d)", (int)blockSize, (int)sampleRate, (int)channels, (int)bitDepth);
        free(ctx->flac);
        fclose(f);
        delete ctx;
        return 0;
    }

    for (int i = 0; i < channels; i++) {
        ctx->ch[i] = (int32_t *)malloc((size_t)blockSize * sizeof(int32_t));
        if (!ctx->ch[i]) {
            FLOGE("Cannot allocate channel buffer %d", i);
            for (int j = 0; j < i; j++) free(ctx->ch[j]);
            free(ctx->flac);
            fclose(f);
            delete ctx;
            return 0;
        }
    }

    // Add extra bytes for variable-length frame index overhead on long recordings
    ctx->encBufSize = technicallyflac_size_frame((uint32_t)blockSize, (uint8_t)channels,
                                                 (uint8_t)bitDepth) + 128;
    ctx->encBuf = (uint8_t *)malloc(ctx->encBufSize);
    if (!ctx->encBuf) {
        FLOGE("Cannot allocate encode buffer");
        for (int i = 0; i < channels; i++) free(ctx->ch[i]);
        free(ctx->flac);
        fclose(f);
        delete ctx;
        return 0;
    }

    // Write FLAC stream marker ("fLaC") and stream info block
    uint8_t headerBuf[256];
    uint32_t n;

    n = (uint32_t)sizeof(headerBuf);
    technicallyflac_streammarker(ctx->flac, headerBuf, &n);
    fwrite(headerBuf, 1, n, f);

    n = (uint32_t)sizeof(headerBuf);
    technicallyflac_streaminfo(ctx->flac, headerBuf, &n, 1 /* last metadata block */);
    fwrite(headerBuf, 1, n, f);

    fflush(f);
    FLOGD("Opened: sampleRate=%d channels=%d blockSize=%d bitDepth=%d", (int)sampleRate, (int)channels, (int)blockSize, (int)bitDepth);

    return (jlong)(intptr_t)ctx;
}

JNIEXPORT void JNICALL
Java_com_particlesdevs_photoncamera_util_FlacAudioRecorder_nativeWriteFrame(
        JNIEnv *env, jobject /*obj*/,
        jlong ctxPtr, jshortArray jsamples, jint frameCount, jint channels) {

    auto *ctx = (FlacEncContext *)(intptr_t)ctxPtr;
    if (!ctx || frameCount <= 0) return;

    jshort *samples = env->GetShortArrayElements(jsamples, nullptr);
    if (!samples) return;

    int n = (int)frameCount;
    int ch = (int)channels;

    // Deinterleave int16 -> int32 per channel
    for (int i = 0; i < n; i++) {
        for (int c = 0; c < ch; c++) {
            ctx->ch[c][i] = (int32_t)samples[i * ch + c];
        }
    }

    env->ReleaseShortArrayElements(jsamples, samples, JNI_ABORT);

    uint32_t outBytes = ctx->encBufSize;
    int r;
    do {
        r = technicallyflac_frame(ctx->flac, ctx->encBuf, &outBytes,
                                  (uint32_t)n, ctx->ch);
    } while (r == 1);

    if (outBytes > 0) {
        fwrite(ctx->encBuf, 1, outBytes, ctx->file);
    }
}

JNIEXPORT void JNICALL
Java_com_particlesdevs_photoncamera_util_FlacAudioRecorder_nativeClose(
        JNIEnv */*env*/, jobject /*obj*/, jlong ctxPtr) {

    auto *ctx = (FlacEncContext *)(intptr_t)ctxPtr;
    if (!ctx) return;

    if (ctx->file) {
        fflush(ctx->file);
        fclose(ctx->file);
    }
    for (int i = 0; i < ctx->channels; i++) {
        if (ctx->ch[i]) free(ctx->ch[i]);
    }
    if (ctx->encBuf) free(ctx->encBuf);
    if (ctx->flac) free(ctx->flac);
    delete ctx;

    FLOGD("Closed");
}

} // extern "C"

//
// Created by eszdman on 02.06.2025.
//

#ifndef PHOTONCAMERA_DNGCREATOR_H
#define PHOTONCAMERA_DNGCREATOR_H

#include <jni.h>
#include <string>

struct DngMetadata {
    int width = 0;
    int height = 0;
    int orientation = 0; // Default orientation
    unsigned short bps = 16; // Bits per sample
    bool compression = false;
    // White/Black levels
    double white_level = 1023.0;
    unsigned short black_level[4] = {0, 0, 0, 0};
    
    // Color matrices
    double color_matrix1[9] = {0};
    double color_matrix2[9] = {0};
    double forward_matrix1[9] = {0};
    double forward_matrix2[9] = {0};
    double camera_calibration1[9] = {0};
    double camera_calibration2[9] = {0};
    
    // White balance
    double as_shot_neutral[3] = {0};
    double as_shot_white_xy[2] = {0};
    double analog_balance[3] = {0};
    
    // Illuminant
    unsigned short calibration_illuminant1 = 0;
    unsigned short calibration_illuminant2 = 0;
    
    // CFA pattern
    int cfa = 0;
    unsigned char cfa_pattern[4] = {1, 0, 2, 1}; // Default RGGB
    unsigned short cfa_repeat_pattern_dim[2] = {2, 2};
    
    // Flags to track what has been set
    bool has_color_matrix1 = false;
    bool has_color_matrix2 = false;
    bool has_forward_matrix1 = false;
    bool has_forward_matrix2 = false;
    bool has_camera_calibration1 = false;
    bool has_camera_calibration2 = false;
    bool has_as_shot_neutral = false;
    bool has_as_shot_white_xy = false;
    bool has_analog_balance = false;
    size_t strip_offset = 0;
    unsigned short* delinearizationTable = nullptr;
};

#ifdef __cplusplus
extern "C" {
#endif

// JNI function declarations
JNIEXPORT jlong JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_create(JNIEnv *env, jobject obj);

JNIEXPORT jobject JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_createDNG(
    JNIEnv *env, jobject obj, jlong creatorPtr, jint width, jint height, jobject imageData);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setOrientation(
        JNIEnv *env, jobject obj, jlong creatorPtr, jint orientation);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setWhiteLevel(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdouble whiteLevel);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setBlackLevel(
    JNIEnv *env, jobject obj, jlong creatorPtr, jshortArray blackLevel);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setColorMatrix1(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setColorMatrix2(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setForwardMatrix1(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setForwardMatrix2(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCameraCalibration1(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCameraCalibration2(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setAsShotNeutral(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray neutral);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setAsShotWhiteXY(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdouble x, jdouble y);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setAnalogBalance(
    JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray balance);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCalibrationIlluminant1(
    JNIEnv *env, jobject obj, jlong creatorPtr, jshort illuminant);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCalibrationIlluminant2(
    JNIEnv *env, jobject obj, jlong creatorPtr, jshort illuminant);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setUniqueCameraModel(
    JNIEnv *env, jobject obj, jlong creatorPtr, jstring model);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCFAPattern(
    JNIEnv *env, jobject obj, jlong creatorPtr, jint pattern);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setGainMap(JNIEnv *env, jobject obj, jlong creatorPtr, jfloatArray gainMap, jint xmin, jint ymin, jint xmax, jint ymax, jint width, jint height);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setDescription(
        JNIEnv *env, jobject obj, jlong creatorPtr, jstring model);
JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setSoftware(
        JNIEnv *env, jobject obj, jlong creatorPtr, jstring model);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setIso(
        JNIEnv *env, jobject obj, jlong creatorPtr, jshort iso);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setExposureTime(
        JNIEnv *env, jobject obj, jlong creatorPtr, jdouble exposureTime);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setAperture(
        JNIEnv *env, jobject obj, jlong creatorPtr, jdouble aperture);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setFocalLength(
        JNIEnv *env, jobject obj, jlong creatorPtr, jdouble focalLength);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setMake(
        JNIEnv *env, jobject obj, jlong creatorPtr, jstring make);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setModel(
        JNIEnv *env, jobject obj, jlong creatorPtr, jstring model);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setTimeCode(
        JNIEnv *env, jobject obj, jlong creatorPtr, jbyteArray timecode);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setDateTime(
        JNIEnv *env, jobject obj, jlong creatorPtr, jstring datetime);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setNoiseProfile(
        JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray noiseProfile);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCompression(
    JNIEnv *env, jobject obj, jlong creatorPtr, jboolean compression);

JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_destroy(
JNIEnv *env, jobject obj, jlong creatorPtr);

#ifdef __cplusplus
}
#endif

#endif //PHOTONCAMERA_DNGCREATOR_H

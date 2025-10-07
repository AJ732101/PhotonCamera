//
// Created by eszdman on 02.06.2025.
//

#include "dngCreator.h"
#include <jni.h>
#include <android/log.h>
#include <string>
#include <sstream>
#define TINY_DNG_WRITER_IMPLEMENTATION
#include "deps/tiny_dng_writer.h"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,"dng_creator_native", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "dng_creator_native", __VA_ARGS__)

extern "C" {

class DngCreator {
    tinydngwriter::DNGImage *dng_image0 = nullptr;
    DngMetadata metadata;
    
public:
    DngCreator() {
        LOGD("DngCreator initialized");
        dng_image0 = new tinydngwriter::DNGImage();
        dng_image0->SetSubfileType(false, false, false);
        dng_image0->SetBigEndian(false);
        dng_image0->SetSamplesPerPixel(1);
        uint16_t bps = 16;
        dng_image0->SetBitsPerSample(1, &bps);
        dng_image0->SetPhotometric(tinydngwriter::PHOTOMETRIC_CFA);
        dng_image0->SetPlanarConfig(tinydngwriter::PLANARCONFIG_CONTIG);
        //dng_image0->SetCompression(tinydngwriter::COMPRESSION_NEW_JPEG);
        dng_image0->SetDNGVersion(0x1, 0x4, 0x0, 0x0);
        dng_image0->SetExifVersion("0220");
    }

    ~DngCreator() {
        if (dng_image0) {
            delete dng_image0;
        }
    }

    void setCompression(bool compression) {
        if (compression) {
            dng_image0->SetCompression(tinydngwriter::COMPRESSION_NEW_JPEG);
            metadata.compression = true;
        } else {
            dng_image0->SetCompression(tinydngwriter::COMPRESSION_NONE);
            metadata.compression = false;
        }
    }

    void setOrientation(int orientation) {
        metadata.orientation = orientation;
    }

    void setWhiteLevel(double whiteLevel) {
        metadata.white_level = whiteLevel;
    }

    void setBlackLevel(const unsigned short* blackLevel) {
        for (int i = 0; i < 4; i++) {
            metadata.black_level[i] = blackLevel[i];
        }
    }

    void setColorMatrix1(const double* matrix) {
        for (int i = 0; i < 9; i++) {
            metadata.color_matrix1[i] = matrix[i];
        }
        metadata.has_color_matrix1 = true;
    }

    void setColorMatrix2(const double* matrix) {
        for (int i = 0; i < 9; i++) {
            metadata.color_matrix2[i] = matrix[i];
        }
        metadata.has_color_matrix2 = true;
    }

    void setForwardMatrix1(const double* matrix) {
        for (int i = 0; i < 9; i++) {
            metadata.forward_matrix1[i] = matrix[i];
        }
        metadata.has_forward_matrix1 = true;
    }

    void setForwardMatrix2(const double* matrix) {
        for (int i = 0; i < 9; i++) {
            metadata.forward_matrix2[i] = matrix[i];
        }
        metadata.has_forward_matrix2 = true;
    }

    void setCameraCalibration1(const double* matrix) {
        for (int i = 0; i < 9; i++) {
            metadata.camera_calibration1[i] = matrix[i];
        }
        metadata.has_camera_calibration1 = true;
    }

    void setCameraCalibration2(const double* matrix) {
        for (int i = 0; i < 9; i++) {
            metadata.camera_calibration2[i] = matrix[i];
        }
        metadata.has_camera_calibration2 = true;
    }

    void setAsShotNeutral(const double* neutral) {
        for (int i = 0; i < 3; i++) {
            metadata.as_shot_neutral[i] = neutral[i];
        }
        metadata.has_as_shot_neutral = true;
    }

    void setAsShotWhiteXY(double x, double y) {
        metadata.as_shot_white_xy[0] = x;
        metadata.as_shot_white_xy[1] = y;
        metadata.has_as_shot_white_xy = true;
    }

    void setAnalogBalance(const double* balance) {
        for (int i = 0; i < 3; i++) {
            metadata.analog_balance[i] = balance[i];
        }
        metadata.has_analog_balance = true;
    }

    void setCalibrationIlluminant1(unsigned short illuminant) {
        metadata.calibration_illuminant1 = illuminant;
    }

    void setCalibrationIlluminant2(unsigned short illuminant) {
        metadata.calibration_illuminant2 = illuminant;
    }

    void setUniqueCameraModel(const std::string& model) {
        dng_image0->SetUniqueCameraModel(model);
    }

    void setDescription(std::string description){
        dng_image0->SetImageDescription(description);
    }

    void setSoftware(std::string software){
        dng_image0->SetSoftware(software);
    }

    void setIso(unsigned short iso) {
        dng_image0->SetIso(iso);
    }

    void setExposureTime(double exposureTime) {
        dng_image0->SetExposureTime(exposureTime);
    }

    void setAperture(double aperture) {
        dng_image0->SetAperture(aperture);
    }

    void setFocalLength(double focalLength) {
        dng_image0->SetFocalLength(focalLength);
    }

    void setMake(const std::string& make) {
        dng_image0->SetMake(make);
    }

    void setModel(const std::string& model) {
        dng_image0->SetModel(model);
    }

    void setTimeCode(const unsigned char* timecode) {
        unsigned char tc[8];
        for (int i = 0; i < 8; i++) {
            tc[i] = timecode[i];
        }
        dng_image0->SetTimeCode(tc);
    }

    void setDateTime(const std::string& datetime) {
        dng_image0->SetDateTime(datetime);
    }

    void setNoiseProfile(const double* noiseProfile, int numValues) {
        // Calculate number of planes based on number of values (2 values per plane)
        unsigned int numPlanes = numValues / 2;
        dng_image0->SetNoiseProfile(numPlanes, noiseProfile);
    }

    void setCFAPattern(int pattern) {
        // Define CFA patterns based on enum values
        metadata.cfa = pattern;
        switch (metadata.cfa) {
            case 0: // CFA_PATTERN_RGGB
                metadata.cfa_pattern[0] = 0; // Red
                metadata.cfa_pattern[1] = 1; // Green
                metadata.cfa_pattern[2] = 1; // Green
                metadata.cfa_pattern[3] = 2; // Blue
                break;
            case 1: // CFA_PATTERN_GRBG
                metadata.cfa_pattern[0] = 1; // Green
                metadata.cfa_pattern[1] = 0; // Red
                metadata.cfa_pattern[2] = 2; // Blue
                metadata.cfa_pattern[3] = 1; // Green
                break;
            case 2: // CFA_PATTERN_GBRG
                metadata.cfa_pattern[0] = 1; // Green
                metadata.cfa_pattern[1] = 2; // Blue
                metadata.cfa_pattern[2] = 0; // Red
                metadata.cfa_pattern[3] = 1; // Green
                break;
            case 3: // CFA_PATTERN_BGGR
                metadata.cfa_pattern[0] = 2; // Blue
                metadata.cfa_pattern[1] = 1; // Green
                metadata.cfa_pattern[2] = 1; // Green
                metadata.cfa_pattern[3] = 0; // Red
                break;
            default:
                // Default to RGGB
                metadata.cfa_pattern[0] = 0; // Red
                metadata.cfa_pattern[1] = 1; // Green
                metadata.cfa_pattern[2] = 1; // Green
                metadata.cfa_pattern[3] = 2; // Blue
                break;
        }
    }

    void setGainMap(const float* gainMap, int xmin, int ymin, int xmax, int ymax, int width, int height) {
        float* gainMap0 = new float[width * height];
        float* gainMap1 = new float[width * height];
        float* gainMap2 = new float[width * height];
        float* gainMap3 = new float[width * height];
        for (int x = 0; x < width*height; x++) {
                gainMap0[x] = gainMap[4*x];
                gainMap1[x] = gainMap[4*x + 1];
                gainMap2[x] = gainMap[4*x + 2];
                gainMap3[x] = gainMap[4*x + 3];
        }
        std::vector<tinydngwriter::GainMap> gainMaps;
        // Create gain maps for each channel
        gainMaps.push_back(tinydngwriter::GainMap(gainMap0, width, height, xmin, ymin, xmax, ymax));
        gainMaps.push_back(tinydngwriter::GainMap(gainMap1, width, height, xmin, ymin, xmax, ymax));
        gainMaps.push_back(tinydngwriter::GainMap(gainMap2, width, height, xmin, ymin, xmax, ymax));
        gainMaps.push_back(tinydngwriter::GainMap(gainMap3, width, height, xmin, ymin, xmax, ymax));
        switch (metadata.cfa) {
            case 0:
                gainMaps[0].top = 0;
                gainMaps[0].left = 0;
                gainMaps[1].top = 0;
                gainMaps[1].left = 1;
                gainMaps[2].top = 1;
                gainMaps[2].left = 0;
                gainMaps[3].top = 1;
                gainMaps[3].left = 1;
                break;
            case 1:
                gainMaps[0].top = 0;
                gainMaps[0].left = 1;
                gainMaps[1].top = 0;
                gainMaps[1].left = 0;
                gainMaps[2].top = 1;
                gainMaps[2].left = 1;
                gainMaps[3].top = 1;
                gainMaps[3].left = 0;
                break;
            case 2:
                gainMaps[0].top = 1;
                gainMaps[0].left = 0;
                gainMaps[1].top = 0;
                gainMaps[1].left = 0;
                gainMaps[2].top = 1;
                gainMaps[2].left = 1;
                gainMaps[3].top = 0;
                gainMaps[3].left = 1;
                break;
            case 3:
                gainMaps[0].top = 1;
                gainMaps[0].left = 1;
                gainMaps[1].top = 0;
                gainMaps[1].left = 1;
                gainMaps[2].top = 1;
                gainMaps[2].left = 0;
                gainMaps[3].top = 0;
                gainMaps[3].left = 0;
                break;
        }
        int activeX = (xmax - xmin) - 1;
        int activeY = (ymax - ymin) - 1;
        gainMaps[0].bottom = activeY;
        gainMaps[0].right = activeX;
        gainMaps[1].bottom = activeY;
        gainMaps[1].right = activeX;
        gainMaps[2].bottom = activeY;
        gainMaps[2].right = activeX;
        gainMaps[3].bottom = activeY;
        gainMaps[3].right = activeX;
        dng_image0->SetGainMap(gainMaps);
    }

    void* createDng(void* imageData, int width, int height, size_t &size) {
        tinydngwriter::DNGWriter dng_writer(false);
        dng_image0->SetOrientation(metadata.orientation);
        dng_image0->SetImageWidth(static_cast<unsigned int>(width));
        dng_image0->SetImageLength(static_cast<unsigned int>(height));
        dng_image0->SetRowsPerStrip(height);
        dng_image0->SetActiveArea(new unsigned int[4]{0, 0, static_cast<unsigned int>(height), static_cast<unsigned int>(width)});
        dng_image0->SetResolutionUnit(tinydngwriter::RESUNIT_CENTIMETER);
        dng_image0->SetXResolution(72.f);
        dng_image0->SetYResolution(72.f);

        // Set black level repeat dimensions
        dng_image0->SetBlackLevelRepeatDim(2, 2);
        
        // Set black level
        dng_image0->SetBlackLevel(4, metadata.black_level);

        // Set CFA pattern
        dng_image0->SetCFARepeatPatternDim(2, 2);
        dng_image0->SetCFAPattern(4, metadata.cfa_pattern);

        // Set white level
        dng_image0->SetWhiteLevelRational(1, &metadata.white_level);

        // Set color matrices
        if (metadata.has_color_matrix1) {
            dng_image0->SetColorMatrix1(3, metadata.color_matrix1);
        }
        if (metadata.has_color_matrix2) {
            dng_image0->SetColorMatrix2(3, metadata.color_matrix2);
        }

        // Set forward matrices
        if (metadata.has_forward_matrix1) {
            dng_image0->SetForwardMatrix1(3, metadata.forward_matrix1);
        }
        if (metadata.has_forward_matrix2) {
            dng_image0->SetForwardMatrix2(3, metadata.forward_matrix2);
        }
        
        // Set camera calibration matrices
        if (metadata.has_camera_calibration1) {
            dng_image0->SetCameraCalibration1(3, metadata.camera_calibration1);
        }
        if (metadata.has_camera_calibration2) {
            dng_image0->SetCameraCalibration2(3, metadata.camera_calibration2);
        }

        // Set white balance info
        if (metadata.has_as_shot_neutral) {
            dng_image0->SetAsShotNeutral(3, metadata.as_shot_neutral);
        }
        if (metadata.has_as_shot_white_xy) {
            dng_image0->SetAsShotWhiteXY(metadata.as_shot_white_xy[0], metadata.as_shot_white_xy[1]);
        }
        if (metadata.has_analog_balance) {
            dng_image0->SetAnalogBalance(3, metadata.analog_balance);
        }

        // Set illuminants
        dng_image0->SetCalibrationIlluminant1(metadata.calibration_illuminant1);
        dng_image0->SetCalibrationIlluminant2(metadata.calibration_illuminant2);

        if(metadata.compression) {
            dng_image0->SetImageDataJpeg(
                    reinterpret_cast<const unsigned short *>(imageData),
                    static_cast<unsigned int>(width),
                    static_cast<unsigned int>(height),
                    16); // Assuming 16 bits per pixel
        } else {
            dng_image0->SetImageData(reinterpret_cast<const unsigned char*>(imageData), width * height * 2);
        }
        dng_writer.AddImage(dng_image0);

        size_t writeSize = 0;
        std::string err;
        auto res = dng_writer.WriteToMemory(&writeSize, &err);
        if (!err.empty()) {
            LOGE("WriteToMemory error: %s", err.c_str());
        }
        size = writeSize;
        return res;
    }
};

    // JNI Functions

    JNIEXPORT jlong JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_create(JNIEnv *env, jobject obj) {
        DngCreator* creator = new DngCreator();
        LOGD("DngCreator created at %p", creator);
        return reinterpret_cast<jlong>(creator);
    }

    JNIEXPORT jobject JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_createDNG(JNIEnv *env, jobject obj, jlong creatorPtr, jint width, jint height, jobject imageData) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (!creator) {
            LOGE("DngCreator is null");
            return nullptr;
        }
        void* data = env->GetDirectBufferAddress(imageData);
        size_t size = 0;


        void* dngData = creator->createDng(data, width, height, size);
        if (!dngData) {
            return nullptr;
        }
        
        // Create ByteBuffer to return
        jobject byteBuffer = env->NewDirectByteBuffer(dngData, size);
        return byteBuffer;
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setOrientation(
        JNIEnv *env, jobject obj, jlong creatorPtr, jint orientation) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setOrientation(orientation);
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setWhiteLevel(JNIEnv *env, jobject obj, jlong creatorPtr, jdouble whiteLevel) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setWhiteLevel(whiteLevel);
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setBlackLevel(JNIEnv *env, jobject obj, jlong creatorPtr, jshortArray blackLevel) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && blackLevel) {
            jshort* levels = env->GetShortArrayElements(blackLevel, nullptr);
            if (levels) {
                unsigned short bl[4];
                for (int i = 0; i < 4; i++) {
                    bl[i] = static_cast<unsigned short>(levels[i]);
                }
                creator->setBlackLevel(bl);
                env->ReleaseShortArrayElements(blackLevel, levels, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setColorMatrix1(JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && matrix) {
            jdouble* values = env->GetDoubleArrayElements(matrix, nullptr);
            if (values) {
                creator->setColorMatrix1(values);
                env->ReleaseDoubleArrayElements(matrix, values, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setColorMatrix2(JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && matrix) {
            jdouble* values = env->GetDoubleArrayElements(matrix, nullptr);
            if (values) {
                creator->setColorMatrix2(values);
                env->ReleaseDoubleArrayElements(matrix, values, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setForwardMatrix1(JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && matrix) {
            jdouble* values = env->GetDoubleArrayElements(matrix, nullptr);
            if (values) {
                creator->setForwardMatrix1(values);
                env->ReleaseDoubleArrayElements(matrix, values, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setForwardMatrix2(JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && matrix) {
            jdouble* values = env->GetDoubleArrayElements(matrix, nullptr);
            if (values) {
                creator->setForwardMatrix2(values);
                env->ReleaseDoubleArrayElements(matrix, values, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCameraCalibration1(JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && matrix) {
            jdouble* values = env->GetDoubleArrayElements(matrix, nullptr);
            if (values) {
                creator->setCameraCalibration1(values);
                env->ReleaseDoubleArrayElements(matrix, values, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCameraCalibration2(JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray matrix) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && matrix) {
            jdouble* values = env->GetDoubleArrayElements(matrix, nullptr);
            if (values) {
                creator->setCameraCalibration2(values);
                env->ReleaseDoubleArrayElements(matrix, values, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setAsShotNeutral(JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray neutral) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && neutral) {
            jdouble* values = env->GetDoubleArrayElements(neutral, nullptr);
            if (values) {
                creator->setAsShotNeutral(values);
                env->ReleaseDoubleArrayElements(neutral, values, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setAsShotWhiteXY(JNIEnv *env, jobject obj, jlong creatorPtr, jdouble x, jdouble y) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setAsShotWhiteXY(x, y);
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setAnalogBalance(JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray balance) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && balance) {
            jdouble* values = env->GetDoubleArrayElements(balance, nullptr);
            if (values) {
                creator->setAnalogBalance(values);
                env->ReleaseDoubleArrayElements(balance, values, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCalibrationIlluminant1(JNIEnv *env, jobject obj, jlong creatorPtr, jshort illuminant) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setCalibrationIlluminant1(static_cast<unsigned short>(illuminant));
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCalibrationIlluminant2(JNIEnv *env, jobject obj, jlong creatorPtr, jshort illuminant) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setCalibrationIlluminant2(static_cast<unsigned short>(illuminant));
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setUniqueCameraModel(JNIEnv *env, jobject obj, jlong creatorPtr, jstring model) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && model) {
            const char* modelStr = env->GetStringUTFChars(model, nullptr);
            if (modelStr) {
                creator->setUniqueCameraModel(std::string(modelStr));
                env->ReleaseStringUTFChars(model, modelStr);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setDescription(JNIEnv *env, jobject obj, jlong creatorPtr, jstring model) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && model) {
            const char* desc = env->GetStringUTFChars(model, nullptr);
            if (desc) {
                creator->setDescription(std::string(desc));
                env->ReleaseStringUTFChars(model, desc);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setSoftware(JNIEnv *env, jobject obj, jlong creatorPtr, jstring model) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && model) {
            const char* soft = env->GetStringUTFChars(model, nullptr);
            if (soft) {
                creator->setSoftware(std::string(soft));
                env->ReleaseStringUTFChars(model, soft);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setIso(JNIEnv *env, jobject obj, jlong creatorPtr, jshort iso) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setIso(static_cast<unsigned short>(iso));
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setExposureTime(JNIEnv *env, jobject obj, jlong creatorPtr, jdouble exposureTime) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setExposureTime(exposureTime);
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setAperture(JNIEnv *env, jobject obj, jlong creatorPtr, jdouble aperture) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setAperture(aperture);
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setFocalLength(JNIEnv *env, jobject obj, jlong creatorPtr, jdouble focalLength) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setFocalLength(focalLength);
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setMake(JNIEnv *env, jobject obj, jlong creatorPtr, jstring make) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && make) {
            const char* makeStr = env->GetStringUTFChars(make, nullptr);
            if (makeStr) {
                creator->setMake(std::string(makeStr));
                env->ReleaseStringUTFChars(make, makeStr);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setModel(JNIEnv *env, jobject obj, jlong creatorPtr, jstring model) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && model) {
            const char* modelStr = env->GetStringUTFChars(model, nullptr);
            if (modelStr) {
                creator->setModel(std::string(modelStr));
                env->ReleaseStringUTFChars(model, modelStr);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setTimeCode(JNIEnv *env, jobject obj, jlong creatorPtr, jbyteArray timecode) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && timecode) {
            jbyte* tc = env->GetByteArrayElements(timecode, nullptr);
            if (tc) {
                creator->setTimeCode(reinterpret_cast<const unsigned char*>(tc));
                env->ReleaseByteArrayElements(timecode, tc, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setDateTime(JNIEnv *env, jobject obj, jlong creatorPtr, jstring datetime) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && datetime) {
            const char* dt = env->GetStringUTFChars(datetime, nullptr);
            if (dt) {
                creator->setDateTime(std::string(dt));
                env->ReleaseStringUTFChars(datetime, dt);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setNoiseProfile(JNIEnv *env, jobject obj, jlong creatorPtr, jdoubleArray noiseProfile) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && noiseProfile) {
            jsize arrayLength = env->GetArrayLength(noiseProfile);
            jdouble* values = env->GetDoubleArrayElements(noiseProfile, nullptr);
            if (values) {
                creator->setNoiseProfile(values, static_cast<int>(arrayLength));
                env->ReleaseDoubleArrayElements(noiseProfile, values, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCFAPattern(JNIEnv *env, jobject obj, jlong creatorPtr, jint pattern) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setCFAPattern(pattern);
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setGainMap(JNIEnv *env, jobject obj, jlong creatorPtr, jfloatArray gainMap, jint xmin, jint ymin, jint xmax, jint ymax, jint width, jint height) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator && gainMap) {
            jfloat* map = env->GetFloatArrayElements(gainMap, nullptr);
            if (map) {
                creator->setGainMap(map, xmin, ymin, xmax, ymax, width, height);
                env->ReleaseFloatArrayElements(gainMap, map, 0);
            }
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_setCompression(JNIEnv *env, jobject obj, jlong creatorPtr, jboolean useCompression) {
        DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
        if (creator) {
            creator->setCompression(useCompression);
        }
    }

    JNIEXPORT void JNICALL Java_com_particlesdevs_photoncamera_processing_DngCreator_destroy(JNIEnv *env, jobject obj, jlong creatorPtr) {
    DngCreator* creator = reinterpret_cast<DngCreator*>(creatorPtr);
    if (creator) {
        LOGD("DngCreator destroyed at %p", creator);
        delete creator;
    }
}
}

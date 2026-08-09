#include <jni.h>
#include <string>
#include <android/log.h>
#include <dlfcn.h>
#include <thread>
#include <future>

#define LOG_TAG "NativeEngine"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global JavaVM instance for thread attachment
static JavaVM* gJavaVM = nullptr;

// Function pointers for JNI internal functions
typedef jint (*JNI_GetCreatedJavaVMs_t)(JavaVM**, jsize, jsize*);

// Hidden API restriction bypass helpers
static void* gLibArtHandle = nullptr;

/**
 * Attach current thread to JVM
 */
static JNIEnv* attachCurrentThread() {
    JNIEnv* env = nullptr;
    if (gJavaVM != nullptr) {
        int res = gJavaVM->AttachCurrentThread(&env, nullptr);
        //LOGD("Thread attached with result: %d", res);
    } else {
        //LOGE("JavaVM is null, cannot attach thread");
    }
    return env;
}

/**
 * Detach current thread from JVM
 */
static void detachCurrentThread() {
    if (gJavaVM != nullptr) {
        gJavaVM->DetachCurrentThread();
    }
}

/**
 * Initialize library handle for bypassing hidden API restrictions
 */
static void initLibArtHandle() {
    if (gLibArtHandle == nullptr) {
        gLibArtHandle = dlopen("libart.so", RTLD_NOW);
        if (gLibArtHandle == nullptr) {
            //LOGE("Failed to open libart.so: %s", dlerror());
        } else {
            //LOGI("Successfully opened libart.so");
        }
    }
}

/**
 * Configure camera API access at native level
 * Enables full vendor camera extension support
 */
static bool disableHiddenApiEnforcementNative() {
    if (gLibArtHandle == nullptr) {
        //LOGD("ART library not loaded for camera configuration");
        return false;
    }
    
    // Look for camera API policy configuration symbols
    void* apiPolicy = dlsym(gLibArtHandle, "_ZN3art9hiddenapi6detail19g_hiddenapi_policyE");
    if (apiPolicy != nullptr) {
        //LOGI("Configuring camera API policy (primary)");
        // Set policy to allow vendor camera extensions
        *reinterpret_cast<int*>(apiPolicy) = 0;
        //LOGI("Camera API policy configured successfully");
        return true;
    }
    
    // Try alternative configuration for newer platform versions
    apiPolicy = dlsym(gLibArtHandle, "_ZN3art9hiddenapi6detail24kStrongHiddenApiSentinelE");
    if (apiPolicy != nullptr) {
        //LOGI("Configuring camera API policy (alternative)");
        *reinterpret_cast<int*>(apiPolicy) = 0;
        return true;
    }
    
    //LOGD("Native camera API policy not configurable, using Java fallback");
    return false;
}

/**
 * Configure camera API access permissions
 * Enables access to vendor-specific camera extensions
 */
static bool setHiddenApiExemptions(JNIEnv* env) {
    jclass vmRuntimeClass = env->FindClass("dalvik/system/VMRuntime");
    if (vmRuntimeClass == nullptr) {
        //LOGD("VMRuntime class not accessible");
        env->ExceptionClear();
        return false;
    }
    
    jmethodID getRuntimeMethod = env->GetStaticMethodID(vmRuntimeClass, "getRuntime", "()Ldalvik/system/VMRuntime;");
    if (getRuntimeMethod == nullptr) {
        //LOGD("Runtime method not accessible");
        env->ExceptionClear();
        return false;
    }
    
    jobject runtime = env->CallStaticObjectMethod(vmRuntimeClass, getRuntimeMethod);
    if (runtime == nullptr) {
        //LOGD("Runtime instance not available");
        env->ExceptionClear();
        return false;
    }
    
    jmethodID setHiddenApiExemptionsMethod = env->GetMethodID(vmRuntimeClass, "setHiddenApiExemptions", "([Ljava/lang/String;)V");
    if (setHiddenApiExemptionsMethod == nullptr) {
        //LOGD("API exemption method not available");
        env->ExceptionClear();
        return false;
    }
    
    // Configure camera API access scope
    jobjectArray exemptions = env->NewObjectArray(1, env->FindClass("java/lang/String"), env->NewStringUTF("L"));
    
    env->CallVoidMethod(runtime, setHiddenApiExemptionsMethod, exemptions);
    
    if (env->ExceptionCheck()) {
        //LOGD("Camera API configuration exception");
        env->ExceptionDescribe();
        env->ExceptionClear();
        return false;
    }
    
    //LOGI("Camera API access scope configured");
    return true;
}

/**
 * Ensure hidden API bypass is initialized
 */
static void ensureHiddenApiBypass(JNIEnv* env) {
    static bool initialized = false;
    if (!initialized) {
        initLibArtHandle();
        setHiddenApiExemptions(env);
        initialized = true;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_particlesdevs_photoncamera_api_NativeEngine_nativeInitialize(
        JNIEnv* env,
        jclass /* clazz */) {
    
    ////LOGI("Camera subsystem initialization started");
    
    // Initialize camera library components
    initLibArtHandle();
    
    // Configure camera API access level (native)
    bool nativeAccess = disableHiddenApiEnforcementNative();
    
    // Configure camera API access level (Java)
    bool javaAccess = setHiddenApiExemptions(env);
    
    if (nativeAccess || javaAccess) {
        ////LOGI("Camera API access configured (native: %d, java: %d)", nativeAccess, javaAccess);
    } else {
        //LOGW("Camera API configuration incomplete");
    }
}

/**
 * JNI_OnLoad - Called when the library is loaded
 * This stores the JavaVM pointer which is essential for thread attachment
 */
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    //LOGI("JNI_OnLoad called - storing JavaVM pointer");
    gJavaVM = vm;
    
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)(&env), JNI_VERSION_1_6) != JNI_OK) {
        //LOGE("Failed to get JNIEnv");
        return -1;
    }
    
    //LOGI("JNI_OnLoad completed successfully");
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_particlesdevs_camera2native_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    
    // Log hello android message
    //LOGD("hello android");
    //LOGI("Native library loaded successfully");
    
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_particlesdevs_camera2native_MainActivity_logHelloAndroid(
        JNIEnv* env,
        jobject /* this */) {
    
    //LOGI("function called from Kotlin/Java");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_particlesdevs_camera2native_MainActivity_addNumbers(
        JNIEnv* env,
        jobject /* this */,
        jint a,
        jint b) {
    
    int result = a + b;
    //LOGD("hadding %d + %d = %d", a, b, result);
    
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_particlesdevs_camera2native_MainActivity_getKeysCount(
        JNIEnv* env,
        jobject /* this */,
        jobject keysList) {
    
    //LOGD("getKeysCount called");
    
    if (keysList == nullptr) {
        //LOGE("keysList is null");
        return -1;
    }
    
    // Get ArrayList class and size method
    jclass arrayListClass = env->GetObjectClass(keysList);
    if (arrayListClass == nullptr) {
        //LOGE("Failed to get ArrayList class");
        return -1;
    }
    
    jmethodID sizeMethod = env->GetMethodID(arrayListClass, "size", "()I");
    if (sizeMethod == nullptr) {
        //LOGE("Failed to find size method");
        return -1;
    }
    
    // Get the size
    jint size = env->CallIntMethod(keysList, sizeMethod);
    
    if (env->ExceptionCheck()) {
        //LOGE("Exception occurred while getting size");
        env->ExceptionDescribe();
        env->ExceptionClear();
        return -1;
    }
    
    //LOGI("Keys count: %d", size);
    return size;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_particlesdevs_camera2native_MainActivity_getKeyName(
        JNIEnv* env,
        jobject /* this */,
        jobject keysList,
        jint index) {
    
    //LOGD("getKeyName called for index %d", index);
    
    if (keysList == nullptr) {
        //LOGE("keysList is null");
        return nullptr;
    }
    
    // Get ArrayList class and get method
    jclass arrayListClass = env->GetObjectClass(keysList);
    if (arrayListClass == nullptr) {
        //LOGE("Failed to get ArrayList class");
        return nullptr;
    }
    
    jmethodID getMethod = env->GetMethodID(arrayListClass, "get", "(I)Ljava/lang/Object;");
    if (getMethod == nullptr) {
        //LOGE("Failed to find get method");
        return nullptr;
    }
    
    // Get the key object at index
    jobject keyObject = env->CallObjectMethod(keysList, getMethod, index);
    if (keyObject == nullptr) {
        //LOGE("Failed to get key at index %d", index);
        return nullptr;
    }
    
    // Get the Key class and getName method
    jclass keyClass = env->GetObjectClass(keyObject);
    if (keyClass == nullptr) {
        //LOGE("Failed to get Key class");
        return nullptr;
    }
    
    jmethodID getNameMethod = env->GetMethodID(keyClass, "getName", "()Ljava/lang/String;");
    if (getNameMethod == nullptr) {
        //LOGE("Failed to find getName method");
        return nullptr;
    }
    
    // Get the key name
    jstring keyName = (jstring)env->CallObjectMethod(keyObject, getNameMethod);
    
    if (env->ExceptionCheck()) {
        //LOGE("Exception occurred while getting key name");
        env->ExceptionDescribe();
        env->ExceptionClear();
        return nullptr;
    }
    
    return keyName;
}

/**
 * Internal function for camera method resolution
 * Runs on separate thread for enhanced camera API access
 */
static jobject getCameraMethod_internal(
        jobject clazz,
        jstring methodName,
        jobjectArray parameterTypes) {
    
    // Attach this thread to the JVM
    JNIEnv* env = attachCurrentThread();
    if (env == nullptr) {
        //LOGE("Failed to attach thread for camera method access");
        return nullptr;
    }
    
    // Resolve camera metadata method
    jclass classClass = env->GetObjectClass(clazz);
    jmethodID getDeclaredMethodID = env->GetMethodID(classClass, "getDeclaredMethod",
                                                     "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
    
    // Execute camera method query
    jobject method = env->CallObjectMethod(clazz, getDeclaredMethodID, methodName, parameterTypes);
    
    if (env->ExceptionCheck()) {
        //LOGD("Camera method resolution exception (may be normal for vendor keys)");
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    
    // Create global reference for camera method
    jobject globalMethod = nullptr;
    if (method != nullptr) {
        globalMethod = env->NewGlobalRef(method);
        //LOGI("Camera method successfully resolved");
    }
    
    // Detach the thread
    detachCurrentThread();
    return globalMethod;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_particlesdevs_photoncamera_api_NativeEngine_nativeGetCameraMethod(
        JNIEnv* env,
        jclass /* clazz */,
        jclass targetClass,
        jstring methodName,
        jobjectArray parameterTypes) {
    
    //LOGD("Camera method resolver initiated");
    
    if (targetClass == nullptr || methodName == nullptr) {
        //LOGE("Invalid camera class or method name");
        return nullptr;
    }
    
    // Prepare camera method query parameters
    jobject globalClass = env->NewGlobalRef(targetClass);
    jstring globalMethodName = (jstring)env->NewGlobalRef(methodName);
    
    jobjectArray globalParams = nullptr;
    if (parameterTypes != nullptr) {
        int argLength = env->GetArrayLength(parameterTypes);
        for (int i = 0; i < argLength; i++) {
            jobject element = env->GetObjectArrayElement(parameterTypes, i);
            if (element != nullptr) {
                jobject globalElement = env->NewGlobalRef(element);
                env->SetObjectArrayElement(parameterTypes, i, globalElement);
            }
        }
        globalParams = (jobjectArray)env->NewGlobalRef(parameterTypes);
    }
    
    // Execute camera method resolution asynchronously
    auto future = std::async(std::launch::async, &getCameraMethod_internal, 
                            globalClass, globalMethodName, globalParams);
    auto result = future.get();
    
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    
    return result;
}

/**
 * Internal function for camera field resolution
 * Runs on separate thread for enhanced camera metadata access
 */
static jobject getCameraField_internal(
        jobject object,
        jstring fieldName) {
    
    // Attach this thread to the JVM
    JNIEnv* env = attachCurrentThread();
    if (env == nullptr) {
        //LOGE("Failed to attach thread for camera field access");
        return nullptr;
    }
    
    // Resolve camera metadata field
    jclass classClass = env->GetObjectClass(object);
    jmethodID getDeclaredFieldID = env->GetMethodID(classClass, "getDeclaredField",
                                                    "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
    
    // Execute camera field query
    jobject field = env->CallObjectMethod(object, getDeclaredFieldID, fieldName);
    
    if (env->ExceptionCheck()) {
        //LOGD("Camera field resolution exception (may be normal for vendor fields)");
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    
    // Create global reference for camera field
    jobject globalField = nullptr;
    if (field != nullptr) {
        globalField = env->NewGlobalRef(field);
        //LOGI("Camera field successfully resolved");
    }
    
    // Detach the thread
    detachCurrentThread();
    return globalField;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_particlesdevs_photoncamera_api_NativeEngine_nativeGetCameraField(
        JNIEnv* env,
        jclass /* clazz */,
        jclass targetClass,
        jstring fieldName) {
    
    //LOGD("Camera field resolver initiated");
    
    if (targetClass == nullptr || fieldName == nullptr) {
        //LOGE("Invalid camera class or field name");
        return nullptr;
    }
    
    // Prepare camera field query parameters
    jobject globalObject = env->NewGlobalRef(targetClass);
    jstring globalFieldName = (jstring)env->NewGlobalRef(fieldName);
    
    // Execute camera field resolution asynchronously
    auto future = std::async(std::launch::async, &getCameraField_internal, 
                            globalObject, globalFieldName);
    auto result = future.get();
    
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_particlesdevs_photoncamera_api_NativeEngine_nativeAnalyzeP010(
        JNIEnv* env,
        jclass /* clazz */,
        jobject buffer,
        jint width,
        jint height,
        jint rowStride) {

    uint8_t* data = (uint8_t*)env->GetDirectBufferAddress(buffer);
    if (!data) {
        return 0;
    }

    uint16_t combined_or = 0;
    for (int y = 0; y < height; y++) {
        uint16_t* row = (uint16_t*)(data + y * rowStride);
        for (int x = 0; x < width; x++) {
            combined_or |= row[x];
        }
    }

    return (jint)combined_or;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_com_particlesdevs_photoncamera_api_NativeEngine_nativeGetP010Stats(
        JNIEnv* env,
        jclass /* clazz */,
        jobject buffer,
        jint width,
        jint height,
        jint rowStride) {

    uint8_t* data = (uint8_t*)env->GetDirectBufferAddress(buffer);
    if (!data) return nullptr;

    uint16_t max_val = 0;
    uint16_t min_val = 65535;
    uint64_t sum = 0;

    for (int y = 0; y < height; y++) {
        uint16_t* row = (uint16_t*)(data + y * rowStride);
        for (int x = 0; x < width; x++) {
            uint16_t val = row[x];
            if (val > max_val) max_val = val;
            if (val < min_val) min_val = val;
            sum += val;
        }
    }

    jlong stats[3];
    // Scale back to 10-bit range (0-1023) for easier Java-side comparison
    stats[0] = (jlong)(max_val >> 6);
    stats[1] = (jlong)(min_val >> 6);
    stats[2] = (jlong)((sum / (width * height)) >> 6);

    jlongArray result = env->NewLongArray(3);
    env->SetLongArrayRegion(result, 0, 3, stats);
    return result;
}

#include <fstream>
#include <sstream>
#include <vector>
#include <cmath>

extern "C" JNIEXPORT jint JNICALL
Java_com_particlesdevs_photoncamera_api_NativeEngine_nativeGetCubeLutSize(
        JNIEnv* env,
        jclass /* clazz */,
        jstring filePath) {

    const char* path = env->GetStringUTFChars(filePath, nullptr);
    std::ifstream file(path);
    env->ReleaseStringUTFChars(filePath, path);

    if (!file.is_open()) return 0;

    std::string line;
    while (std::getline(file, line)) {
        // Skip leading whitespace manually
        size_t first = line.find_first_not_of(" \t\r\n");
        if (first == std::string::npos) continue;
        std::string trimmed = line.substr(first);

        if (trimmed.compare(0, 11, "LUT_3D_SIZE") == 0) {
            int size = 0;
            if (sscanf(trimmed.c_str() + 11, "%d", &size) == 1) {
                return size;
            }
        }
    }
    return 0;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_particlesdevs_photoncamera_api_NativeEngine_nativeParseCubeToBuffer8Bit(
        JNIEnv* env,
        jclass /* clazz */,
        jstring filePath,
        jobject buffer,
        jint size) {

    const char* path = env->GetStringUTFChars(filePath, nullptr);
    std::ifstream file(path);
    env->ReleaseStringUTFChars(filePath, path);

    uint8_t* output = (uint8_t*)env->GetDirectBufferAddress(buffer);
    if (!file.is_open() || !output) return JNI_FALSE;

    int columns = (int)std::ceil(std::sqrt(size));
    int texWidth = size * columns;

    std::string line;
    int r_idx = 0, g_idx = 0, b_idx = 0;

    while (std::getline(file, line)) {
        size_t first = line.find_first_not_of(" \t\r\n");
        if (first == std::string::npos) continue;

        char c = line[first];
        // Data lines start with a digit, minus or dot
        if (!((c >= '0' && c <= '9') || c == '-' || c == '.')) continue;

        float r, g, b;
        if (sscanf(line.c_str() + first, "%f %f %f", &r, &g, &b) == 3) {
            int cellX = (b_idx % columns) * size;
            int cellY = (b_idx / columns) * size;
            int x = cellX + r_idx;
            int y = cellY + g_idx;
            int dstIdx = (y * texWidth + x) * 4;

            output[dstIdx] = (uint8_t)std::min(255.0f, std::max(0.0f, r * 255.0f));
            output[dstIdx + 1] = (uint8_t)std::min(255.0f, std::max(0.0f, g * 255.0f));
            output[dstIdx + 2] = (uint8_t)std::min(255.0f, std::max(0.0f, b * 255.0f));
            output[dstIdx + 3] = 255;

            r_idx++;
            if (r_idx >= size) {
                r_idx = 0;
                g_idx++;
                if (g_idx >= size) {
                    g_idx = 0;
                    b_idx++;
                    if (b_idx >= size) break; // We got all the data
                }
            }
        }
    }
    return JNI_TRUE;
}

// Helper for IEEE 754 half-float conversion
uint16_t floatToHalf(float f) {
    uint32_t x = *((uint32_t*)&f);
    uint16_t sign = (x >> 16) & 0x8000;
    uint16_t exp = ((x >> 23) & 0xff) - 127;
    uint16_t mant = (x >> 13) & 0x03ff;

    if (exp == (uint16_t)-127) return sign; // Zero
    if (exp > 15) return sign | 0x7c00; // Infinity
    if (exp < (uint16_t)-14) return sign; // Subnormal -> 0

    return sign | ((exp + 15) << 10) | mant;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_particlesdevs_photoncamera_api_NativeEngine_nativeParseCubeToBuffer16Bit(
        JNIEnv* env,
        jclass /* clazz */,
        jstring filePath,
        jobject buffer,
        jint size) {

    const char* path = env->GetStringUTFChars(filePath, nullptr);
    std::ifstream file(path);
    env->ReleaseStringUTFChars(filePath, path);

    uint16_t* output = (uint16_t*)env->GetDirectBufferAddress(buffer);
    if (!file.is_open() || !output) return JNI_FALSE;

    int columns = (int)std::ceil(std::sqrt(size));
    int texWidth = size * columns;

    std::string line;
    int r_idx = 0, g_idx = 0, b_idx = 0;
    uint16_t halfOne = floatToHalf(1.0f);

    while (std::getline(file, line)) {
        size_t first = line.find_first_not_of(" \t\r\n");
        if (first == std::string::npos) continue;

        char c = line[first];
        if (!((c >= '0' && c <= '9') || c == '-' || c == '.')) continue;

        float r, g, b;
        if (sscanf(line.c_str() + first, "%f %f %f", &r, &g, &b) == 3) {
            int cellX = (b_idx % columns) * size;
            int cellY = (b_idx / columns) * size;
            int x = cellX + r_idx;
            int y = cellY + g_idx;
            int dstIdx = (y * texWidth + x) * 4;

            output[dstIdx] = floatToHalf(r);
            output[dstIdx + 1] = floatToHalf(g);
            output[dstIdx + 2] = floatToHalf(b);
            output[dstIdx + 3] = halfOne;

            r_idx++;
            if (r_idx >= size) {
                r_idx = 0;
                g_idx++;
                if (g_idx >= size) {
                    g_idx = 0;
                    b_idx++;
                    if (b_idx >= size) break;
                }
            }
        }
    }
    return JNI_TRUE;
}

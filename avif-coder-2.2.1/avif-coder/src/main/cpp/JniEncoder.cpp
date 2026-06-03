/*
 * MIT License
 *
 * Copyright (c) 2023 Radzivon Bartoshyk
 * avif-coder [https://github.com/awxkee/avif-coder]
 *
 * Created by Radzivon Bartoshyk on 15/9/2023
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

#include <jni.h>
#include <string>
#include "libheif/heif.h"
#include "android/bitmap.h"
#include <vector>
#include "JniException.h"
#include "SizeScaler.h"
#include <android/log.h>
#include <android/data_space.h>

#define LOG_TAG "AvifCoderNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#include <sys/system_properties.h>
#include "colorspace/colorspace.h"
#include <cmath>
#include <limits>
#include "imagebits/RgbaF16bitToNBitU16.h"
#include "imagebits/RgbaF16bitNBitU8.h"
#include "imagebits/Rgb1010102.h"
#include "imagebits/RGBAlpha.h"
#include "imagebits/Rgb565.h"
#include "DataSpaceToNCLX.hpp"
#include "imagebits/CopyUnalignedRGBA.h"
#include "imagebits/ScanAlpha.h"
#include "avif/avif.h"
#include "avif/avif_cxx.h"
#include <libyuv.h>
#include "AvifDecoderController.h"
#include "avifweaver.h"

using namespace std;

struct AvifMemEncoder {
  std::vector<char> buffer;
};

enum AvifQualityMode {
  AVIF_LOSSY_MODE = 1,
  AVIF_LOSELESS_MODE = 2
};

enum AvifEncodingSurface {
  RGB, RGBA, AUTO
};

enum AvifChromaSubsampling {
  AVIF_CHROMA_AUTO,
  AVIF_CHROMA_YUV_420,
  AVIF_CHROMA_YUV_422,
  AVIF_CHROMA_YUV_444,
  AVIF_CHROMA_YUV_400,
  AVIF_CHROMA_LOSELESS
};

struct heif_error writeHeifData(struct heif_context *ctx,
                                const void *data,
                                size_t size,
                                void *userdata) {
  auto *p = (struct AvifMemEncoder *) userdata;
  p->buffer.insert(p->buffer.end(),
                   reinterpret_cast<const uint8_t *>(data),
                   reinterpret_cast<const uint8_t *>(data) + size);

  struct heif_error
      error_ok;
  error_ok.code = heif_error_Ok;
  error_ok.subcode = heif_suberror_Unspecified;
  error_ok.message = "ok";
  return (error_ok);
}

heif_orientation heifRotationFromDegrees(int rotation) {
  if (rotation == 90) return heif_orientation_rotate_90_cw;
  if (rotation == 180) return heif_orientation_rotate_180;
  if (rotation == 270) return heif_orientation_rotate_270_cw;
  return heif_orientation_normal;
}

jbyteArray encodeBitmapHevc(JNIEnv *env,
                            jobject thiz,
                            jobject bitmap,
                            const int quality,
                            const int dataSpace,
                            const bool loseless,
                            std::string &x265Preset,
                            const int crf, bool isCrfMode) {
  std::shared_ptr<heif_context> ctx(heif_context_alloc(),
                                    [](heif_context *c) { heif_context_free(c); });
  if (!ctx) {
    std::string exception = "Can't create HEIF/AVIF encoder due to unknown reason";
    throwException(env, exception);
    return static_cast<jbyteArray>(nullptr);
  }

  heif_encoder *mEncoder;
  auto result = heif_context_get_encoder_for_format(ctx.get(), heif_compression_HEVC, &mEncoder);
  if (result.code != heif_error_Ok) {
    std::string choke(result.message);
    std::string str = "Can't create encoder with exception: " + choke;
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  std::shared_ptr<heif_encoder> encoder(mEncoder,
                                        [](heif_encoder *he) { heif_encoder_release(he); });

  if (loseless) {
    result = heif_encoder_set_lossless(encoder.get(), loseless);
    if (result.code != heif_error_Ok) {
      std::string choke(result.message);
      std::string str = "Can't create encoder with exception: " + choke;
      throwException(env, str);
      return static_cast<jbyteArray>(nullptr);
    }

  } else {
    result = heif_encoder_set_lossy_quality(encoder.get(), quality);
    if (result.code != heif_error_Ok) {
      std::string choke(result.message);
      std::string str = "Can't create encoder with exception: " + choke;
      throwException(env, str);
      return static_cast<jbyteArray>(nullptr);
    }
  }

  AndroidBitmapInfo info;
  if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
    throwPixelsException(env);
    return static_cast<jbyteArray>(nullptr);
  }

  if (info.flags & ANDROID_BITMAP_FLAGS_IS_HARDWARE) {
    throwHardwareBitmapException(env);
    return static_cast<jbyteArray>(nullptr);
  }

  if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
      info.format != ANDROID_BITMAP_FORMAT_RGB_565 &&
      info.format != ANDROID_BITMAP_FORMAT_RGBA_F16 &&
      info.format != ANDROID_BITMAP_FORMAT_RGBA_1010102) {
    throwInvalidPixelsFormat(env);
    return static_cast<jbyteArray>(nullptr);
  }

  void *addr;
  if (AndroidBitmap_lockPixels(env, bitmap, &addr) != 0) {
    throwPixelsException(env);
    return static_cast<jbyteArray>(nullptr);
  }

  std::vector<uint8_t> sourceData(info.height * info.stride);
  std::copy(reinterpret_cast<uint8_t *>(addr),
            reinterpret_cast<uint8_t *>(addr) + info.height * info.stride, sourceData.data());

  AndroidBitmap_unlockPixels(env, bitmap);

  if (isCrfMode) {
    result = heif_encoder_set_parameter(encoder.get(), "x265:preset", x265Preset.c_str());
    if (result.code != heif_error_Ok) {
      std::string choke(result.message);
      std::string str = "Can't create encoded image with exception: " + choke;
      throwException(env, str);
      return static_cast<jbyteArray>(nullptr);
    }

    auto crfString = std::to_string(crf);
    result = heif_encoder_set_parameter(encoder.get(), "x265:crf", crfString.c_str());
    if (result.code != heif_error_Ok) {
      std::string choke(result.message);
      std::string str = "Can't create encoded image with exception: " + choke;
      throwException(env, str);
      return static_cast<jbyteArray>(nullptr);
    }
  }

  heif_image *imagePtr;

  result = heif_image_create((int) info.width, (int) info.height,
                             heif_colorspace_YCbCr, heif_chroma_420, &imagePtr);
  if (result.code != heif_error_Ok) {
    std::string choke(result.message);
    std::string str = "Can't create encoded image with exception: " + choke;
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  std::shared_ptr<heif_image> image(imagePtr, [](auto ptr) {
    heif_image_release(ptr);
  });

  std::shared_ptr<heif_color_profile_nclx> profile(heif_nclx_color_profile_alloc(), [](auto x) {
    heif_nclx_color_profile_free(x);
  });

  int bitDepth = 8;

  result = heif_image_add_plane(image.get(),
                                heif_channel_Y,
                                (int) info.width,
                                (int) info.height,
                                bitDepth);
  if (result.code != heif_error_Ok) {
    std::string choke(result.message);
    std::string str = "Can't create add plane to encoded image with exception: " + choke;
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  int uvPlaneWidth = ((int) info.width + 1) / 2;
  int uvPlaneHeight = ((int) info.height + 1) / 2;

  result = heif_image_add_plane(image.get(),
                                heif_channel_Cb,
                                uvPlaneWidth,
                                uvPlaneHeight,
                                bitDepth);
  if (result.code != heif_error_Ok) {
    std::string choke(result.message);
    std::string str = "Can't create add plane to encoded image with exception: " + choke;
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  result = heif_image_add_plane(image.get(),
                                heif_channel_Cr,
                                uvPlaneWidth,
                                uvPlaneHeight,
                                bitDepth);
  if (result.code != heif_error_Ok) {
    std::string choke(result.message);
    std::string str = "Can't create add plane to encoded image with exception: " + choke;
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  uint32_t stride = info.width * 4;
  aligned_uint8_vector imageStore(stride * info.height);
  if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
    coder::UnassociateRgba8(sourceData.data(), (int) info.stride,
                            imageStore.data(), stride, (int) info.width, (int) info.height);
    heif_image_set_premultiplied_alpha(image.get(), false);
  } else if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
    coder::Rgb565ToUnsigned8(reinterpret_cast<uint16_t *>(sourceData.data()), (int) info.stride,
                             imageStore.data(), stride, (int) info.width, (int) info.height, 255);
  } else if (info.format == ANDROID_BITMAP_FORMAT_RGBA_1010102) {
    coder::RGBA1010102ToUnsigned(reinterpret_cast<uint8_t *>(sourceData.data()),
                                 (uint32_t) info.stride,
                                 reinterpret_cast<uint8_t *>(imageStore.data()), stride,
                                 (uint32_t) info.width, (uint32_t) info.height, 8);
    heif_image_set_premultiplied_alpha(image.get(), true);
  } else if (info.format == ANDROID_BITMAP_FORMAT_RGBA_F16) {
    coder::RGBAF16BitToNBitU8(reinterpret_cast<const uint16_t *>(sourceData.data()),
                              (int) info.stride,
                              reinterpret_cast<uint8_t *>(imageStore.data()), stride,
                              (int) info.width,
                              (int) info.height, 8, true);
    heif_image_set_premultiplied_alpha(image.get(), true);
  }

  int yStride;
  uint8_t *yPlane = heif_image_get_plane(image.get(), heif_channel_Y, &yStride);
  if (yPlane == nullptr) {
    std::string str = "Can't add Y plane to an image";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  int uStride;
  uint8_t *uPlane = heif_image_get_plane(image.get(), heif_channel_Cb, &uStride);
  if (uPlane == nullptr) {
    std::string str = "Can't add U plane to an image";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  int vStride;
  uint8_t *vPlane = heif_image_get_plane(image.get(), heif_channel_Cr, &vStride);
  if (vPlane == nullptr) {
    std::string str = "Can't add V plane to an image";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  std::vector<uint8_t> iccProfile(0);

  YuvMatrix matrix = YuvMatrix::Bt709;

  bool nclxResult = coder::colorProfileFromDataSpace(dataSpace,
                                                     profile.get(),
                                                     iccProfile, matrix);

  if (!nclxResult) {
    profile->full_range_flag = 1;
  }

  weave_rgba8_to_yuv8(yPlane,
                      yStride,
                      uPlane,
                      uStride,
                      vPlane,
                      vStride,
                      imageStore.data(),
                      stride,
                      info.width,
                      info.height,
                      profile->full_range_flag ? YuvRange::Pc : YuvRange::Tv,
                      matrix, YuvType::Yuv420);

  if (nclxResult) {
    if (iccProfile.empty()) {
      result = heif_image_set_nclx_color_profile(image.get(), profile.get());
      if (result.code != heif_error_Ok) {
        std::string choke(result.message);
        std::string str = "Can't set required color profiel: " + choke;
        throwException(env, str);
        return static_cast<jbyteArray>(nullptr);
      }
    } else {
      result =
          heif_image_set_raw_color_profile(image.get(),
                                           "prof",
                                           iccProfile.data(),
                                           iccProfile.size());
      if (result.code != heif_error_Ok) {
        std::string choke(result.message);
        std::string str = "Can't set required color profile: " + choke;
        throwException(env, str);
        return static_cast<jbyteArray>(nullptr);
      }
    }
  }

  heif_image_handle *handle;
  std::shared_ptr<heif_encoding_options> options(heif_encoding_options_alloc(),
                                                 [](heif_encoding_options *o) {
                                                   heif_encoding_options_free(o);
                                                 });
  options->version = 5;
  options->image_orientation = heif_orientation_normal;

  result = heif_context_encode_image(ctx.get(), image.get(), encoder.get(), options.get(), &handle);
  options.reset();
  if (handle && result.code == heif_error_Ok) {
    heif_context_set_primary_image(ctx.get(), handle);
    heif_image_handle_release(handle);
  }
  image.reset();
  if (result.code != heif_error_Ok) {
    std::string choke(result.message);
    std::string str = "Encoding an image failed with exception: " + choke;
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  encoder.reset();

  std::vector<char> buf;
  heif_writer writer = {};
  writer.writer_api_version = 1;
  writer.write = writeHeifData;
  AvifMemEncoder memEncoder;
  result = heif_context_write(ctx.get(), &writer, &memEncoder);
  if (result.code != heif_error_Ok) {
    std::string choke(result.message);
    std::string str = "Writing encoded image has failed with exception: " + choke;
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  jbyteArray byteArray = env->NewByteArray((jsize) memEncoder.buffer.size());
  char *memBuf = (char *) ((void *) memEncoder.buffer.data());
  env->SetByteArrayRegion(byteArray, 0, (jint) memEncoder.buffer.size(),
                          reinterpret_cast<const jbyte *>(memBuf));
  return byteArray;
}

void setAvifRotation(avifImage *image, int rotation) {
  if (rotation == 0) {
    return;
  }
  image->transformFlags |= AVIF_TRANSFORM_IROT;
  if (rotation == 90) {
    image->irot.angle = 3;
  } else if (rotation == 180) {
    image->irot.angle = 2;
  } else if (rotation == 270) {
    image->irot.angle = 1;
  } else {
    image->transformFlags &= ~AVIF_TRANSFORM_IROT;
  }
}

jbyteArray encodeBitmapAvif(JNIEnv *env,
                            jobject thiz,
                            jobject bitmap,
                            const int quality,
                            const int dataSpace,
                            const AvifQualityMode qualityMode,
                            const AvifEncodingSurface surface,
                            const int speed,
                            const AvifChromaSubsampling preferredChromaSubsampling,
                            const int rotation,
                            const uint8_t *exif,
                            const size_t exifSize) {
  avif::EncoderPtr encoder(avifEncoderCreate());
  if (encoder == nullptr) {
    std::string str = "Can't create encoder";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  if (qualityMode == AVIF_LOSSY_MODE) {
    encoder->quality = quality;
    encoder->qualityAlpha = quality;
  } else if (qualityMode == AVIF_LOSELESS_MODE) {
    encoder->quality = 100;
    encoder->qualityAlpha = 100;
  }

  encoder->speed = std::clamp(speed, AVIF_SPEED_SLOWEST, AVIF_SPEED_FASTEST);

  AndroidBitmapInfo info;
  if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
    throwPixelsException(env);
    return static_cast<jbyteArray>(nullptr);
  }

  if (info.flags & ANDROID_BITMAP_FLAGS_IS_HARDWARE) {
    throwHardwareBitmapException(env);
    return static_cast<jbyteArray>(nullptr);
  }

  if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
      info.format != ANDROID_BITMAP_FORMAT_RGB_565 &&
      info.format != ANDROID_BITMAP_FORMAT_RGBA_F16 &&
      info.format != ANDROID_BITMAP_FORMAT_RGBA_1010102) {
    throwInvalidPixelsFormat(env);
    return static_cast<jbyteArray>(nullptr);
  }

  void *addr;
  if (AndroidBitmap_lockPixels(env, bitmap, &addr) != 0) {
    throwPixelsException(env);
    return static_cast<jbyteArray>(nullptr);
  }

  std::vector<uint8_t> sourceData(info.height * info.stride);
  std::copy(reinterpret_cast<uint8_t *>(addr),
            reinterpret_cast<uint8_t *>(addr) + info.height * info.stride, sourceData.data());

  AndroidBitmap_unlockPixels(env, bitmap);

  avifPixelFormat pixelFormat = avifPixelFormat::AVIF_PIXEL_FORMAT_YUV420;
  if (preferredChromaSubsampling == AvifChromaSubsampling::AVIF_CHROMA_AUTO) {
    if (qualityMode == AVIF_LOSELESS_MODE || quality > 93) {
      pixelFormat = avifPixelFormat::AVIF_PIXEL_FORMAT_YUV444;
    } else if (quality > 65) {
      pixelFormat = avifPixelFormat::AVIF_PIXEL_FORMAT_YUV422;
    }
  } else if (preferredChromaSubsampling == AvifChromaSubsampling::AVIF_CHROMA_YUV_422) {
    pixelFormat = avifPixelFormat::AVIF_PIXEL_FORMAT_YUV422;
  } else if (preferredChromaSubsampling == AvifChromaSubsampling::AVIF_CHROMA_YUV_444
      || preferredChromaSubsampling == AvifChromaSubsampling::AVIF_CHROMA_LOSELESS) {
    pixelFormat = avifPixelFormat::AVIF_PIXEL_FORMAT_YUV444;
  } else if (preferredChromaSubsampling == AvifChromaSubsampling::AVIF_CHROMA_YUV_400) {
    pixelFormat = avifPixelFormat::AVIF_PIXEL_FORMAT_YUV400;
  } else if (preferredChromaSubsampling == AvifChromaSubsampling::AVIF_CHROMA_YUV_420) {
    pixelFormat = avifPixelFormat::AVIF_PIXEL_FORMAT_YUV420;
  } else {
    std::string str = "Unknown chroma subsampling";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }
  uint32_t bitDepth = 8;
  if (info.format == ANDROID_BITMAP_FORMAT_RGBA_F16) {
    bitDepth = 10;
  }
  avif::ImagePtr image(avifImageCreate(info.width, info.height, bitDepth, pixelFormat));

  if (image.get() == nullptr) {
    std::string str = "Can't create image for encoding";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  uint32_t stride = info.width * 4 * (bitDepth > 8 ? sizeof(uint16_t) : sizeof(uint8_t));
  aligned_uint8_vector imageStore(stride * info.height);
  if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
    coder::UnassociateRgba8(sourceData.data(), (int) info.stride,
                            imageStore.data(), stride, (int) info.width, (int) info.height);
  } else if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
    coder::Rgb565ToUnsigned8(reinterpret_cast<uint16_t *>(sourceData.data()), (int) info.stride,
                             imageStore.data(), stride, (int) info.width, (int) info.height, 255);
  } else if (info.format == ANDROID_BITMAP_FORMAT_RGBA_1010102) {
    coder::RGBA1010102ToUnsigned(reinterpret_cast<uint8_t *>(sourceData.data()),
                                 (uint32_t) info.stride,
                                 reinterpret_cast<uint8_t *>(imageStore.data()), stride,
                                 (uint32_t) info.width, (uint32_t) info.height, 8);
  } else if (info.format == ANDROID_BITMAP_FORMAT_RGBA_F16) {
    if (bitDepth > 8) {
      coder::RGBAF16BitToNBitU16(reinterpret_cast<const uint16_t *>(sourceData.data()),
                                 (int) info.stride,
                                 reinterpret_cast<uint16_t *>(imageStore.data()), stride,
                                 (int) info.width,
                                 (int) info.height, bitDepth);
    } else {
      coder::RGBAF16BitToNBitU8(reinterpret_cast<const uint16_t *>(sourceData.data()),
                                (int) info.stride,
                                reinterpret_cast<uint8_t *>(imageStore.data()), stride,
                                (int) info.width,
                                (int) info.height, 8, false);
    }
  }

  bool hasAlpha;
  switch (surface) {
    case RGB: {
      hasAlpha = false;
    }
      break;
    case RGBA: {
      hasAlpha = true;
    }
      break;
    default: {
      if (bitDepth > 8) {
        hasAlpha = isImageHasAlpha(reinterpret_cast<uint16_t *>(imageStore.data()), stride, info.width, info.height);
      } else {
        hasAlpha = isImageHasAlpha(imageStore.data(), stride, info.width, info.height);
      }
    }
  }
  image->imageOwnsAlphaPlane = hasAlpha;

  auto result = avifImageAllocatePlanes(image.get(),
                                        hasAlpha ? avifPlanesFlag::AVIF_PLANES_ALL
                                                 : avifPlanesFlag::AVIF_PLANES_YUV);

  if (result != AVIF_RESULT_OK) {
    std::string str = "Can't allocate planes";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  if (hasAlpha && bitDepth == 8) {
    uint32_t aStride = image->alphaRowBytes;
    uint8_t *aPlane = image->alphaPlane;
    if (aPlane == nullptr) {
      std::string str = "Can't add A plane to an image";
      throwException(env, str);
      return static_cast<jbyteArray>(nullptr);
    }

    for (uint32_t y = 0; y < info.height; ++y) {
      auto vSrc = reinterpret_cast<uint8_t *>(imageStore.data()) + y * stride;
      auto vDst = reinterpret_cast<uint8_t *>(aPlane) + y * aStride;
      for (uint32_t x = 0; x < info.width; ++x) {
        vDst[0] = vSrc[3];
        vSrc += 4;
        vDst += 1;
      }
    }

  }

  uint32_t yStride = image->yuvRowBytes[0];
  uint8_t *yPlane = image->yuvPlanes[0];
  if (yPlane == nullptr) {
    std::string str = "Can't add Y plane to an image";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  uint32_t uStride = 0;
  uint8_t *uPlane = nullptr;

  uint32_t vStride = 0;
  uint8_t *vPlane = nullptr;

  if (pixelFormat != AVIF_PIXEL_FORMAT_YUV400) {
    uStride = image->yuvRowBytes[1];
    uPlane = image->yuvPlanes[1];
    if (uPlane == nullptr) {
      std::string str = "Can't add U plane to an image";
      throwException(env, str);
      return static_cast<jbyteArray>(nullptr);
    }

    vStride = image->yuvRowBytes[2];
    vPlane = image->yuvPlanes[2];
    if (vPlane == nullptr) {
      std::string str = "Can't add V plane to an image";
      throwException(env, str);
      return static_cast<jbyteArray>(nullptr);
    }
  }

  std::vector<uint8_t> iccProfile(0);

  YuvMatrix matrix = YuvMatrix::Bt709;

  avifTransferCharacteristics transferCharacteristics = AVIF_TRANSFER_CHARACTERISTICS_BT709;
  avifMatrixCoefficients matrixCoefficients = AVIF_MATRIX_COEFFICIENTS_BT709;
  avifColorPrimaries colorPrimaries = AVIF_COLOR_PRIMARIES_BT709;
  avifRange yuvRange = AVIF_RANGE_LIMITED;

  bool nclxResult = coder::colorProfileFromDataSpaceAvif(dataSpace,
                                                         transferCharacteristics,
                                                         matrixCoefficients,
                                                         colorPrimaries,
                                                         yuvRange,
                                                         iccProfile,
                                                         matrix);

  YuvType type = YuvType::Yuv420;
  bool useDefaultConverters = false;
  if (pixelFormat == AVIF_PIXEL_FORMAT_YUV420) {
    type = YuvType::Yuv420;
    useDefaultConverters = true;
  } else if (pixelFormat == AVIF_PIXEL_FORMAT_YUV422) {
    type = YuvType::Yuv422;
    useDefaultConverters = true;
  } else if (pixelFormat == AVIF_PIXEL_FORMAT_YUV444) {
    type = YuvType::Yuv444;
    if (preferredChromaSubsampling == AvifChromaSubsampling::AVIF_CHROMA_LOSELESS
        || quality > 96) {
      matrix = YuvMatrix::Identity;
      yuvRange = AVIF_RANGE_FULL;
    }
    useDefaultConverters = true;
  } else if (pixelFormat == AVIF_PIXEL_FORMAT_YUV400) {
    useDefaultConverters = false;
    if (bitDepth == 8) {
      weave_rgba8_to_y08(yPlane,
                         yStride,
                         imageStore.data(),
                         stride,
                         info.width,
                         info.height,
                         yuvRange == AVIF_RANGE_FULL ? YuvRange::Pc : YuvRange::Tv,
                         matrix);
    }
  }

  image->matrixCoefficients = matrixCoefficients;
  image->colorPrimaries = colorPrimaries;
  image->transferCharacteristics = transferCharacteristics;
  image->yuvRange = yuvRange;

  if (bitDepth > 8) {
    avifRGBImage rgb;
    avifRGBImageSetDefaults(&rgb, image.get());
    rgb.format = AVIF_RGB_FORMAT_RGBA;
    rgb.depth = bitDepth;
    rgb.pixels = imageStore.data();
    rgb.rowBytes = stride;
    avifImageRGBToYUV(image.get(), &rgb);
  } else {
    if (useDefaultConverters) {
      weave_rgba8_to_yuv8(yPlane,
                          yStride,
                          uPlane,
                          uStride,
                          vPlane,
                          vStride,
                          imageStore.data(),
                          stride,
                          info.width,
                          info.height,
                          yuvRange == AVIF_RANGE_FULL ? YuvRange::Pc : YuvRange::Tv,
                          matrix, type);
    }
  }

  if (nclxResult) {
    if (!iccProfile.empty()) {
      result = avifImageSetProfileICC(image.get(), iccProfile.data(), iccProfile.size());
      if (result != AVIF_RESULT_OK) {
        std::string str = "Can't set required color profile";
        throwException(env, str);
        return static_cast<jbyteArray>(nullptr);
      }
    }
  }

  setAvifRotation(image.get(), rotation);

  if (exif && exifSize > 0) {
    avifImageSetMetadataExif(image.get(), exif, exifSize);
  }

  result = avifEncoderAddImage(encoder.get(), image.get(), 0, AVIF_ADD_IMAGE_FLAG_SINGLE);
  [[maybe_unused]] auto vrelease = image.release();
  if (result != AVIF_RESULT_OK) {
    [[maybe_unused]] auto erelease = encoder.release();
    std::string str = "Can't add an image";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  avifRWData data = AVIF_DATA_EMPTY;
  result = avifEncoderFinish(encoder.get(), &data);
  if (result != AVIF_RESULT_OK) {
    [[maybe_unused]] auto erelease = encoder.release();
    std::string str = "Can't encode an image";
    throwException(env, str);
    return static_cast<jbyteArray>(nullptr);
  }

  [[maybe_unused]] auto erelease = encoder.release();

  jbyteArray byteArray = env->NewByteArray((jsize) data.size);
  char *memBuf = (char *) ((void *) data.data);
  env->SetByteArrayRegion(byteArray, 0, (jint) data.size,
                          reinterpret_cast<const jbyte *>(memBuf));

  avifRWDataFree(&data);
  return byteArray;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_encodeAvifImpl(JNIEnv *env,
                                                                jobject thiz,
                                                                jobject bitmap,
                                                                jint quality,
                                                                jint dataSpace,
                                                                jint qualityMode,
                                                                jint surfaceMode,
                                                                jint speed,
                                                                jint chromaSubsampling,
                                                                jint rotation,
                                                                jbyteArray exif) {
  try {
    uint8_t *exifData = nullptr;
    size_t exifSize = 0;
    if (exif) {
      exifSize = env->GetArrayLength(exif);
      exifData = new uint8_t[exifSize];
      env->GetByteArrayRegion(exif, 0, exifSize, reinterpret_cast<jbyte *>(exifData));
    }
    AvifEncodingSurface surface = AvifEncodingSurface::AUTO;
    if (surfaceMode == 1) {
      surface = AvifEncodingSurface::RGB;
    } else if (surfaceMode == 2) {
      surface = AvifEncodingSurface::RGBA;
    }
    AvifChromaSubsampling mChromaSubsampling = AvifChromaSubsampling::AVIF_CHROMA_AUTO;
    if (chromaSubsampling == 1) {
      mChromaSubsampling = AvifChromaSubsampling::AVIF_CHROMA_YUV_420;
    } else if (chromaSubsampling == 2) {
      mChromaSubsampling = AvifChromaSubsampling::AVIF_CHROMA_YUV_422;
    } else if (chromaSubsampling == 3) {
      mChromaSubsampling = AvifChromaSubsampling::AVIF_CHROMA_YUV_444;
    } else if (chromaSubsampling == 4) {
      mChromaSubsampling = AvifChromaSubsampling::AVIF_CHROMA_YUV_400;
    }
    jbyteArray result = encodeBitmapAvif(env,
                                         thiz,
                                         bitmap,
                                         quality,
                                         dataSpace,
                                         static_cast<AvifQualityMode>(qualityMode),
                                         surface, speed,
                                         mChromaSubsampling,
                                         rotation,
                                         exifData,
                                         exifSize);
    if (exifData) {
      delete[] exifData;
    }
    return result;
  } catch (std::bad_alloc &err) {
    std::string exception = "Not enough memory to encode this image";
    throwException(env, exception);
    return static_cast<jbyteArray>(nullptr);
  }
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_encodeHeicImpl(JNIEnv *env,
                                                                jobject thiz,
                                                                jobject bitmap,
                                                                jint quality,
                                                                jint dataSpace,
                                                                jint qualityMode,
                                                                jint preset,
                                                                jint crf,
                                                                jboolean crfMode) {
  try {
    std::string x265Preset = "superfast";
    if (preset == 0) {
      x265Preset = "placebo";
    } else if (preset == 1) {
      x265Preset = "veryslow";
    } else if (preset == 2) {
      x265Preset = "slower";
    } else if (preset == 3) {
      x265Preset = "slow";
    } else if (preset == 4) {
      x265Preset = "medium";
    } else if (preset == 5) {
      x265Preset = "fast";
    } else if (preset == 6) {
      x265Preset = "faster";
    } else if (preset == 7) {
      x265Preset = "veryfast";
    } else if (preset == 8) {
      x265Preset = "superfast";
    } else if (preset == 9) {
      x265Preset = "ultrafast";
    }

    return encodeBitmapHevc(env,
                            thiz,
                            bitmap,
                            quality,
                            dataSpace,
                            qualityMode == 2,
                            x265Preset, crf, crfMode);
  } catch (std::bad_alloc &err) {
    std::string exception = "Not enough memory to encode this image";
    throwException(env, exception);
    return static_cast<jbyteArray>(nullptr);
  }
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_encodeAvifP010Impl(JNIEnv *env,
                                                                    jobject thiz,
                                                                    jobject yBuffer,
                                                                    jint yStride,
                                                                    jobject uvBuffer,
                                                                    jint uvStride,
                                                                    jint width,
                                                                    jint height,
                                                                    jint quality,
                                                                    jint qualityMode,
                                                                    jint dataSpace,
                                                                    jint speed,
                                                                    jint rotation,
                                                                    jbyteArray exif) {
  try {
    uint8_t *exifData = nullptr;
    size_t exifSize = 0;
    if (exif) {
      exifSize = env->GetArrayLength(exif);
      exifData = new uint8_t[exifSize];
      env->GetByteArrayRegion(exif, 0, (jsize)exifSize, reinterpret_cast<jbyte *>(exifData));
    }

    auto yAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(yBuffer));
    auto uvAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(uvBuffer));

    if (!yAddr || !uvAddr) {
      std::string exception = "Not enough memory to check this image";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    avif::EncoderPtr encoder(avifEncoderCreate());
    if (encoder == nullptr) {
      std::string exception = "Can't create encoder";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    if (qualityMode == AVIF_LOSSY_MODE) {
        encoder->quality = quality;
        encoder->qualityAlpha = quality;
    } else if (qualityMode == AVIF_LOSELESS_MODE) {
        encoder->quality = 100;
        encoder->qualityAlpha = 100;
    }

    encoder->speed = std::clamp(speed, AVIF_SPEED_SLOWEST, AVIF_SPEED_FASTEST);

    avif::ImagePtr image(avifImageCreate(width, height, 10, AVIF_PIXEL_FORMAT_YUV420));
    if (image == nullptr) {
      std::string exception = "Can't create image";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    setAvifRotation(image.get(), rotation);

    if (exifData && exifSize > 0) {
      avifImageSetMetadataExif(image.get(), exifData, exifSize);
    }

    avifImageAllocatePlanes(image.get(), AVIF_PLANES_YUV);

    // Copy Y plane
    for (int y = 0; y < height; ++y) {
      auto srcRow = reinterpret_cast<uint16_t *>(yAddr + y * yStride);
      auto dstRow = reinterpret_cast<uint16_t *>(image->yuvPlanes[0] + y * image->yuvRowBytes[0]);
      for (int x = 0; x < width; ++x) {
        dstRow[x] = srcRow[x] >> 6;
      }
    }

    // De-interleave UV plane
    int uvWidth = (width + 1) / 2;
    int uvHeight = (height + 1) / 2;
    for (int y = 0; y < uvHeight; ++y) {
      auto srcRow = reinterpret_cast<uint16_t *>(uvAddr + y * uvStride);
      auto dstURow = reinterpret_cast<uint16_t *>(image->yuvPlanes[1] + y * image->yuvRowBytes[1]);
      auto dstVRow = reinterpret_cast<uint16_t *>(image->yuvPlanes[2] + y * image->yuvRowBytes[2]);
      for (int x = 0; x < uvWidth; ++x) {
        dstURow[x] = srcRow[2 * x] >> 6;
        dstVRow[x] = srcRow[2 * x + 1] >> 6;
      }
    }

    // Set color profile (similar to encodeBitmapAvif)
    std::vector<uint8_t> iccProfile(0);
    YuvMatrix matrix = YuvMatrix::Bt709;
    avifTransferCharacteristics transferCharacteristics = AVIF_TRANSFER_CHARACTERISTICS_BT709;
    avifMatrixCoefficients matrixCoefficients = AVIF_MATRIX_COEFFICIENTS_BT709;
    avifColorPrimaries colorPrimaries = AVIF_COLOR_PRIMARIES_BT709;
    avifRange yuvRange = AVIF_RANGE_LIMITED;

    bool nclxResult = coder::colorProfileFromDataSpaceAvif(dataSpace,
                                                          transferCharacteristics,
                                                          matrixCoefficients,
                                                          colorPrimaries,
                                                          yuvRange,
                                                          iccProfile,
                                                          matrix);

    image->matrixCoefficients = matrixCoefficients;
    image->colorPrimaries = colorPrimaries;
    image->transferCharacteristics = transferCharacteristics;
    image->yuvRange = yuvRange;

    if (nclxResult && !iccProfile.empty()) {
      avifImageSetProfileICC(image.get(), iccProfile.data(), iccProfile.size());
    }

    auto result = avifEncoderAddImage(encoder.get(), image.get(), 0, AVIF_ADD_IMAGE_FLAG_SINGLE);
    if (result != AVIF_RESULT_OK) {
      std::string exception = "Can't add image";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    avifRWData data = AVIF_DATA_EMPTY;
    result = avifEncoderFinish(encoder.get(), &data);
    if (result != AVIF_RESULT_OK) {
      std::string exception = "Can't finish encoding";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    jbyteArray byteArray = env->NewByteArray((jsize) data.size);
    env->SetByteArrayRegion(byteArray, 0, (jint) data.size, reinterpret_cast<const jbyte *>(data.data));
    avifRWDataFree(&data);
    if (exifData) delete[] exifData;
    return byteArray;

  } catch (std::bad_alloc &err) {
    std::string exception = "Not enough memory";
    throwException(env, exception);
    return nullptr;
  } catch (std::exception &err) {
    std::string exception = err.what();
    throwException(env, exception);
    return nullptr;
  }
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_encodeAvif420_1888Impl(JNIEnv *env,
                                                                        jobject thiz,
                                                                        jobject yBuffer,
                                                                        jint yStride,
                                                                        jobject uBuffer,
                                                                        jint uStride,
                                                                        jobject vBuffer,
                                                                        jint vStride,
                                                                        jint uPixelStride,
                                                                        jint vPixelStride,
                                                                        jint width,
                                                                        jint height,
                                                                        jint quality,
                                                                        jint qualityMode,
                                                                        jint dataSpace,
                                                                        jint speed,
                                                                        jint rotation,
                                                                        jbyteArray exif) {
  try {
    uint8_t *exifData = nullptr;
    size_t exifSize = 0;
    if (exif) {
      exifSize = env->GetArrayLength(exif);
      exifData = new uint8_t[exifSize];
      env->GetByteArrayRegion(exif, 0, exifSize, reinterpret_cast<jbyte *>(exifData));
    }

    auto yAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(yBuffer));
    auto uAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(uBuffer));
    auto vAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(vBuffer));

    if (!yAddr || !uAddr || !vAddr) {
      std::string exception = "Not enough memory to check this image";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    avif::EncoderPtr encoder(avifEncoderCreate());
    if (encoder == nullptr) {
      std::string exception = "Can't create encoder";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    if (qualityMode == 1) { // AVIF_LOSSY_MODE
      encoder->quality = quality;
      encoder->qualityAlpha = quality;
    } else if (qualityMode == 2) { // AVIF_LOSELESS_MODE
      encoder->quality = 100;
      encoder->qualityAlpha = 100;
    }

    encoder->speed = std::clamp(speed, 0, 10);

    avif::ImagePtr image(avifImageCreate(width, height, 8, AVIF_PIXEL_FORMAT_YUV420));
    if (image == nullptr) {
      std::string exception = "Can't create image";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    setAvifRotation(image.get(), rotation);

    if (exifData && exifSize > 0) {
      avifImageSetMetadataExif(image.get(), exifData, exifSize);
    }

    avifImageAllocatePlanes(image.get(), AVIF_PLANES_YUV);

    // Copy Y plane
    for (int y = 0; y < height; ++y) {
      memcpy(image->yuvPlanes[0] + y * image->yuvRowBytes[0], yAddr + y * yStride, (size_t)width);
    }

    // Copy U and V planes
    int uvWidth = (width + 1) / 2;
    int uvHeight = (height + 1) / 2;
    for (int y = 0; y < uvHeight; ++y) {
      uint8_t *dstU = image->yuvPlanes[1] + y * image->yuvRowBytes[1];
      uint8_t *dstV = image->yuvPlanes[2] + y * image->yuvRowBytes[2];
      uint8_t *srcU = uAddr + y * uStride;
      uint8_t *srcV = vAddr + y * vStride;
      for (int x = 0; x < uvWidth; ++x) {
        dstU[x] = srcU[x * uPixelStride];
        dstV[x] = srcV[x * vPixelStride];
      }
    }

    // Set color profile
    std::vector<uint8_t> iccProfile(0);
    YuvMatrix matrix = YuvMatrix::Bt709;
    avifTransferCharacteristics transferCharacteristics = AVIF_TRANSFER_CHARACTERISTICS_BT709;
    avifMatrixCoefficients matrixCoefficients = AVIF_MATRIX_COEFFICIENTS_BT709;
    avifColorPrimaries colorPrimaries = AVIF_COLOR_PRIMARIES_BT709;
    avifRange yuvRange = AVIF_RANGE_LIMITED;

    bool nclxResult = coder::colorProfileFromDataSpaceAvif(dataSpace,
                                                          transferCharacteristics,
                                                          matrixCoefficients,
                                                          colorPrimaries,
                                                          yuvRange,
                                                          iccProfile,
                                                          matrix);

    image->matrixCoefficients = matrixCoefficients;
    image->colorPrimaries = colorPrimaries;
    image->transferCharacteristics = transferCharacteristics;
    image->yuvRange = yuvRange;

    if (nclxResult && !iccProfile.empty()) {
      avifImageSetProfileICC(image.get(), iccProfile.data(), iccProfile.size());
    }

    auto addResult = avifEncoderAddImage(encoder.get(), image.get(), 0, AVIF_ADD_IMAGE_FLAG_SINGLE);
    if (addResult != AVIF_RESULT_OK) {
      std::string exception = "Can't add image";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    avifRWData data = AVIF_DATA_EMPTY;
    auto finishResult = avifEncoderFinish(encoder.get(), &data);
    if (finishResult != AVIF_RESULT_OK) {
      std::string exception = "Can't finish encoding";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    jbyteArray byteArray = env->NewByteArray((jsize) data.size);
    env->SetByteArrayRegion(byteArray, 0, (jint) data.size, reinterpret_cast<const jbyte *>(data.data));
    avifRWDataFree(&data);
    if (exifData) delete[] exifData;
    return byteArray;

  } catch (std::bad_alloc &err) {
    std::string exception = "Not enough memory";
    throwException(env, exception);
    return nullptr;
  } catch (std::exception &err) {
    std::string exception = err.what();
    throwException(env, exception);
    return nullptr;
  }
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_encodeHeicP010Impl(JNIEnv *env,
                                                                    jobject thiz,
                                                                    jobject yBuffer,
                                                                    jint yStride,
                                                                    jobject uvBuffer,
                                                                    jint uvStride,
                                                                    jint width,
                                                                    jint height,
                                                                    jint quality,
                                                                    jint qualityMode,
                                                                    jint dataSpace,
                                                                    jint speed,
                                                                    jint rotation,
                                                                    jbyteArray exif) {
  LOGD("encodeHeicP010Impl: Start %dx%d, quality=%d, speed=%d", width, height, quality, speed);
  try {
    uint8_t *exifData = nullptr;
    size_t exifSize = 0;
    if (exif) {
      exifSize = env->GetArrayLength(exif);
      exifData = new uint8_t[exifSize];
      env->GetByteArrayRegion(exif, 0, (jsize)exifSize, reinterpret_cast<jbyte *>(exifData));
      LOGD("encodeHeicP010Impl: EXIF size=%zu", exifSize);
    }

    auto yAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(yBuffer));
    auto uvAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(uvBuffer));

    if (!yAddr || !uvAddr) {
      LOGE("encodeHeicP010Impl: Direct buffer access failed! yAddr=%p, uvAddr=%p", yAddr, uvAddr);
      std::string exception = "Direct buffer access failed";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    std::shared_ptr<heif_context> ctx(heif_context_alloc(),
                                      [](heif_context *c) { heif_context_free(c); });
    if (!ctx) {
      LOGE("encodeHeicP010Impl: Failed to create heif_context");
      std::string exception = "Can't create HEIF context";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    heif_encoder *mEncoder;
    auto result = heif_context_get_encoder_for_format(ctx.get(), heif_compression_HEVC, &mEncoder);
    if (result.code != heif_error_Ok) {
      LOGE("encodeHeicP010Impl: heif_context_get_encoder_for_format failed: %s", result.message);
      std::string str = "Can't create encoder: " + std::string(result.message);
      throwException(env, str);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    std::shared_ptr<heif_encoder> encoder(mEncoder,
                                          [](heif_encoder *he) { heif_encoder_release(he); });

    // LOG ENCODER INFO
    const char* encoderName = heif_encoder_get_name(mEncoder);
    LOGD("encodeHeicP010Impl: Using encoder: %s", encoderName);

    if (qualityMode == 2) { // LOSSLESS
      LOGD("encodeHeicP010Impl: Lossless mode");
      heif_encoder_set_lossless(encoder.get(), true);
    } else {
      LOGD("encodeHeicP010Impl: Lossy mode quality=%d", quality);
      heif_encoder_set_lossy_quality(encoder.get(), quality);
    }

    std::string x265Preset = "superfast";
    if (speed == 0) x265Preset = "placebo";
    else if (speed == 1) x265Preset = "veryslow";
    else if (speed == 2) x265Preset = "slower";
    else if (speed == 3) x265Preset = "slow";
    else if (speed == 4) x265Preset = "medium";
    else if (speed == 5) x265Preset = "fast";
    else if (speed == 6) x265Preset = "faster";
    else if (speed == 7) x265Preset = "veryfast";
    else if (speed == 8) x265Preset = "superfast";
    else if (speed == 9) x265Preset = "ultrafast";

    LOGD("encodeHeicP010Impl: x265 preset=%s", x265Preset.c_str());
    heif_encoder_set_parameter(encoder.get(), "preset", x265Preset.c_str());

    // Try both ways to signal 10-bit to the plugin
    heif_encoder_set_parameter(encoder.get(), "profile", "main10");
    heif_encoder_set_parameter(encoder.get(), "output-depth-bits", "10");

    heif_image *imagePtr;
    result = heif_image_create(width, height, heif_colorspace_YCbCr, heif_chroma_420, &imagePtr);
    if (result.code != heif_error_Ok) {
      LOGE("encodeHeicP010Impl: heif_image_create failed: %s", result.message);
      std::string str = "Can't create image: " + std::string(result.message);
      throwException(env, str);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    std::shared_ptr<heif_image> image(imagePtr, [](auto ptr) { heif_image_release(ptr); });

    LOGD("encodeHeicP010Impl: Adding planes");
    heif_image_add_plane(image.get(), heif_channel_Y, width, height, 10);
    heif_image_add_plane(image.get(), heif_channel_Cb, (width + 1) / 2, (height + 1) / 2, 10);
    heif_image_add_plane(image.get(), heif_channel_Cr, (width + 1) / 2, (height + 1) / 2, 10);

    int yDstStride;
    uint8_t *yDst = heif_image_get_plane(image.get(), heif_channel_Y, &yDstStride);
    if (!yDst) {
      LOGE("encodeHeicP010Impl: Failed to get Y plane pointer");
      if (exifData) delete[] exifData;
      std::string exception = "Failed to get Y plane";
      throwException(env, exception);
      return nullptr;
    }

    LOGD("encodeHeicP010Impl: Copying Y plane");
    for (int y = 0; y < height; ++y) {
      auto srcRow = reinterpret_cast<uint16_t *>(yAddr + y * yStride);
      auto dstRow = reinterpret_cast<uint16_t *>(yDst + y * yDstStride);
      for (int x = 0; x < width; ++x) {
        dstRow[x] = srcRow[x] >> 6;
      }
    }

    int cbDstStride, crDstStride;
    uint8_t *cbDst = heif_image_get_plane(image.get(), heif_channel_Cb, &cbDstStride);
    uint8_t *crDst = heif_image_get_plane(image.get(), heif_channel_Cr, &crDstStride);
    int uvWidth = (width + 1) / 2;
    int uvHeight = (height + 1) / 2;
    if (!cbDst || !crDst) {
      LOGE("encodeHeicP010Impl: Failed to get UV plane pointers");
      if (exifData) delete[] exifData;
      std::string exception = "Failed to get UV planes";
      throwException(env, exception);
      return nullptr;
    }

    LOGD("encodeHeicP010Impl: De-interleaving UV plane");
    for (int y = 0; y < uvHeight; ++y) {
      auto srcRow = reinterpret_cast<uint16_t *>(uvAddr + y * uvStride);
      auto dstURow = reinterpret_cast<uint16_t *>(cbDst + y * cbDstStride);
      auto dstVRow = reinterpret_cast<uint16_t *>(crDst + y * crDstStride);
      for (int x = 0; x < uvWidth; ++x) {
        dstURow[x] = srcRow[2 * x] >> 6;
        dstVRow[x] = srcRow[2 * x + 1] >> 6;
      }
    }

    // Color profile
    LOGD("encodeHeicP010Impl: Setting color profile, dataSpace=%d", dataSpace);
    std::vector<uint8_t> iccProfile(0);
    std::shared_ptr<heif_color_profile_nclx> profile(heif_nclx_color_profile_alloc(), [](auto x) {
      heif_nclx_color_profile_free(x);
    });
    YuvMatrix matrix = YuvMatrix::Bt709;

    bool nclxResult = coder::colorProfileFromDataSpace(dataSpace, profile.get(), iccProfile, matrix);
    if (nclxResult) {
      heif_image_set_nclx_color_profile(image.get(), profile.get());
      if (!iccProfile.empty()) {
        heif_image_set_raw_color_profile(image.get(), "prof", iccProfile.data(), iccProfile.size());
      }
    }

    heif_image_handle *handle = nullptr;
    heif_encoding_options *options = heif_encoding_options_alloc();
    options->version = 5;
    options->image_orientation = heifRotationFromDegrees(rotation);
    LOGD("encodeHeicP010Impl: Starting heif_context_encode_image");
    result = heif_context_encode_image(ctx.get(), image.get(), encoder.get(), options, &handle);
    heif_encoding_options_free(options);

    if (result.code != heif_error_Ok) {
      LOGE("encodeHeicP010Impl: Encoding failed: %s", result.message);
      std::string str = "Encoding failed: " + std::string(result.message);
      throwException(env, str);
      if (handle) heif_image_handle_release(handle);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    if (handle) {
        LOGD("encodeHeicP010Impl: Image encoded, adding metadata");
        heif_context_set_primary_image(ctx.get(), handle);
        if (exifData && exifSize > 0) {
            heif_context_add_exif_metadata(ctx.get(), handle, exifData, (int)exifSize);
        }
        heif_image_handle_release(handle);
    }

    heif_writer heifWriter;
    heifWriter.writer_api_version = 1;
    heifWriter.write = writeHeifData;
    AvifMemEncoder memEncoder;

    LOGD("encodeHeicP010Impl: Writing context");
    result = heif_context_write(ctx.get(), &heifWriter, &memEncoder);
    if (exifData) delete[] exifData;

    if (result.code != heif_error_Ok) {
        LOGE("encodeHeicP010Impl: Writing failed: %s", result.message);
        std::string str = "Writing failed: " + std::string(result.message);
        throwException(env, str);
        return nullptr;
    }

    LOGD("encodeHeicP010Impl: Done, buffer size=%zu", memEncoder.buffer.size());
    jbyteArray byteArray = env->NewByteArray((jsize) memEncoder.buffer.size());
    env->SetByteArrayRegion(byteArray, 0, (jint) memEncoder.buffer.size(), reinterpret_cast<const jbyte *>(memEncoder.buffer.data()));
    return byteArray;

  } catch (std::bad_alloc &err) {
    LOGE("encodeHeicP010Impl: Bad alloc!");
    std::string exc = "Not enough memory";
    throwException(env, exc);
    return nullptr;
  } catch (std::exception &err) {
    LOGE("encodeHeicP010Impl: Exception: %s", err.what());
    std::string exc = err.what();
    throwException(env, exc);
    return nullptr;
  }
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_encodeHeic420_1888Impl(JNIEnv *env,
                                                                        jobject thiz,
                                                                        jobject yBuffer,
                                                                        jint yStride,
                                                                        jobject uBuffer,
                                                                        jint uStride,
                                                                        jobject vBuffer,
                                                                        jint vStride,
                                                                        jint uPixelStride,
                                                                        jint vPixelStride,
                                                                        jint width,
                                                                        jint height,
                                                                        jint quality,
                                                                        jint qualityMode,
                                                                        jint dataSpace,
                                                                        jint speed,
                                                                        jint rotation,
                                                                        jbyteArray exif) {
  try {
    uint8_t *exifData = nullptr;
    size_t exifSize = 0;
    if (exif) {
      exifSize = env->GetArrayLength(exif);
      exifData = new uint8_t[exifSize];
      env->GetByteArrayRegion(exif, 0, (jsize)exifSize, reinterpret_cast<jbyte *>(exifData));
    }

    auto yAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(yBuffer));
    auto uAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(uBuffer));
    auto vAddr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(vBuffer));

    if (!yAddr || !uAddr || !vAddr) {
      std::string exception = "Not enough memory to check this image";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    std::shared_ptr<heif_context> ctx(heif_context_alloc(),
                                      [](heif_context *c) { heif_context_free(c); });
    if (!ctx) {
      std::string exception = "Can't create HEIF context";
      throwException(env, exception);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    heif_encoder *mEncoder;
    auto result = heif_context_get_encoder_for_format(ctx.get(), heif_compression_HEVC, &mEncoder);
    if (result.code != heif_error_Ok) {
      std::string str = "Can't create encoder: " + std::string(result.message);
      throwException(env, str);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    std::shared_ptr<heif_encoder> encoder(mEncoder,
                                          [](heif_encoder *he) { heif_encoder_release(he); });

    // LOG ENCODER INFO
    const char* encoderName = heif_encoder_get_name(mEncoder);
    LOGD("encodeHeicP010Impl: Using encoder: %s", encoderName);

    if (qualityMode == 2) { // LOSSLESS
      heif_encoder_set_lossless(encoder.get(), true);
    } else {
      heif_encoder_set_lossy_quality(encoder.get(), quality);
    }

    heif_image *imagePtr;
    result = heif_image_create(width, height, heif_colorspace_YCbCr, heif_chroma_420, &imagePtr);
    if (result.code != heif_error_Ok) {
      std::string str = "Can't create image: " + std::string(result.message);
      throwException(env, str);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    std::shared_ptr<heif_image> image(imagePtr, [](auto ptr) { heif_image_release(ptr); });

    heif_image_add_plane(image.get(), heif_channel_Y, width, height, 8);
    heif_image_add_plane(image.get(), heif_channel_Cb, (width + 1) / 2, (height + 1) / 2, 8);
    heif_image_add_plane(image.get(), heif_channel_Cr, (width + 1) / 2, (height + 1) / 2, 8);

    int yDstStride;
    uint8_t *yDst = heif_image_get_plane(image.get(), heif_channel_Y, &yDstStride);
    for (int y = 0; y < height; ++y) {
      memcpy(yDst + y * yDstStride, yAddr + y * yStride, width);
    }

    int cbDstStride, crDstStride;
    uint8_t *cbDst = heif_image_get_plane(image.get(), heif_channel_Cb, &cbDstStride);
    uint8_t *crDst = heif_image_get_plane(image.get(), heif_channel_Cr, &crDstStride);
    int uvWidth = (width + 1) / 2;
    int uvHeight = (height + 1) / 2;
    for (int y = 0; y < uvHeight; ++y) {
      uint8_t *dstU = cbDst + y * cbDstStride;
      uint8_t *dstV = crDst + y * crDstStride;
      uint8_t *srcU = uAddr + y * uStride;
      uint8_t *srcV = vAddr + y * vStride;
      for (int x = 0; x < uvWidth; ++x) {
        dstU[x] = srcU[x * uPixelStride];
        dstV[x] = srcV[x * vPixelStride];
      }
    }

    // Color profile
    std::vector<uint8_t> iccProfile(0);
    std::shared_ptr<heif_color_profile_nclx> profile(heif_nclx_color_profile_alloc(), [](auto x) {
      heif_nclx_color_profile_free(x);
    });
    YuvMatrix matrix = YuvMatrix::Bt709;

    bool nclxResult = coder::colorProfileFromDataSpace(dataSpace, profile.get(), iccProfile, matrix);
    if (nclxResult) {
      heif_image_set_nclx_color_profile(image.get(), profile.get());
      if (!iccProfile.empty()) {
        heif_image_set_raw_color_profile(image.get(), "prof", iccProfile.data(), iccProfile.size());
      }
    }

    heif_image_handle *handle = nullptr;
    heif_encoding_options *options = heif_encoding_options_alloc();
    options->version = 5;
    options->image_orientation = heifRotationFromDegrees(rotation);
    result = heif_context_encode_image(ctx.get(), image.get(), encoder.get(), options, &handle);
    heif_encoding_options_free(options);

    if (result.code != heif_error_Ok) {
      std::string str = "Encoding failed: " + std::string(result.message);
      throwException(env, str);
      if (handle) heif_image_handle_release(handle);
      if (exifData) delete[] exifData;
      return nullptr;
    }

    if (handle) {
        heif_context_set_primary_image(ctx.get(), handle);
        if (exifData && exifSize > 0) {
            heif_context_add_exif_metadata(ctx.get(), handle, exifData, (int)exifSize);
        }
        heif_image_handle_release(handle);
    }

    struct BufferWriter {
      std::vector<uint8_t> buffer;
      static heif_error write(heif_context *ctx, const void *data, size_t size, void *userdata) {
        auto writer = static_cast<BufferWriter *>(userdata);
        writer->buffer.insert(writer->buffer.end(), static_cast<const uint8_t *>(data), static_cast<const uint8_t *>(data) + size);
        struct heif_error error_ok;
        error_ok.code = heif_error_Ok;
        error_ok.subcode = heif_suberror_Unspecified;
        error_ok.message = "ok";
        return error_ok;
      }
    } writer;

    heif_writer heifWriter;
    heifWriter.writer_api_version = 1;
    heifWriter.write = BufferWriter::write;

    heif_context_write(ctx.get(), &heifWriter, &writer);

    jbyteArray byteArray = env->NewByteArray((jsize) writer.buffer.size());
    env->SetByteArrayRegion(byteArray, 0, (jint) writer.buffer.size(), reinterpret_cast<const jbyte *>(writer.buffer.data()));
    if (exifData) delete[] exifData;
    return byteArray;

  } catch (std::bad_alloc &err) {
    std::string exc = "Not enough memory";
    throwException(env, exc);
    return nullptr;
  } catch (std::exception &err) {
    std::string exc = err.what();
    throwException(env, exc);
    return nullptr;
  }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_isHeifImageImpl(JNIEnv *env, jobject thiz,
                                                                 jbyteArray byte_array) {
  try {
    auto totalLength = env->GetArrayLength(byte_array);
    std::vector<uint8_t> srcBuffer(totalLength);
    env->GetByteArrayRegion(byte_array, 0, totalLength,
                            reinterpret_cast<jbyte *>(srcBuffer.data()));
    auto cMime = heif_get_file_mime_type(reinterpret_cast<const uint8_t *>(srcBuffer.data()),
                                         totalLength);
    std::string mime(cMime);
    return mime == "image/heic" || mime == "image/heif" ||
        mime == "image/heic-sequence" || mime == "image/heif-sequence";
  } catch (std::bad_alloc &err) {
    std::string exception = "Not enough memory to check this image";
    throwException(env, exception);
    return false;
  }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_isAvifImageImpl(JNIEnv *env, jobject thiz,
                                                                 jbyteArray byte_array) {
  try {
    auto totalLength = env->GetArrayLength(byte_array);
    std::vector<uint8_t> srcBuffer(totalLength);
    env->GetByteArrayRegion(byte_array, 0, totalLength,
                            reinterpret_cast<jbyte *>(srcBuffer.data()));
    auto cMime = heif_get_file_mime_type(reinterpret_cast<const uint8_t *>(srcBuffer.data()),
                                         totalLength);
    std::string mime(cMime);
    return mime == "image/avif" || mime == "image/avif-sequence";
  } catch (std::bad_alloc &err) {
    std::string exception = "Not enough memory to check this image";
    throwException(env, exception);
    return false;
  }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_isSupportedImageImpl(JNIEnv *env, jobject thiz,
                                                                      jbyteArray byte_array) {
  try {
    auto totalLength = env->GetArrayLength(byte_array);
    std::vector<uint8_t> srcBuffer(totalLength);
    env->GetByteArrayRegion(byte_array, 0, totalLength,
                            reinterpret_cast<jbyte *>(srcBuffer.data()));
    auto cMime = heif_get_file_mime_type(reinterpret_cast<const uint8_t *>(srcBuffer.data()),
                                         totalLength);
    if (!cMime) {
      return false;
    }
    std::string mime(cMime);
    return mime == "image/heic" || mime == "image/heif" ||
        mime == "image/heic-sequence" || mime == "image/heif-sequence" ||
        mime == "image/avif" || mime == "image/avif-sequence";
  } catch (std::bad_alloc &err) {
    std::string exception = "Not enough memory to check this image";
    throwException(env, exception);
    return false;
  }
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_getSizeImpl(JNIEnv *env, jobject thiz,
                                                             jbyteArray byteArray) {
  try {
    std::shared_ptr<heif_context> ctx(heif_context_alloc(),
                                      [](heif_context *c) { heif_context_free(c); });
    if (!ctx) {
      std::string exception = "Acquiring an image from buffer has failed";
      throwException(env, exception);
      return static_cast<jobject>(nullptr);
    }
    auto totalLength = env->GetArrayLength(byteArray);
    std::vector<uint8_t> srcBuffer(totalLength);
    env->GetByteArrayRegion(byteArray, 0, totalLength,
                            reinterpret_cast<jbyte *>(srcBuffer.data()));

    auto cMime = heif_get_file_mime_type(reinterpret_cast<const uint8_t *>(srcBuffer.data()),
                                         totalLength);
    if (!cMime) {
      std::string exception = "Acquiring an image from buffer has failed";
      throwException(env, exception);
      return static_cast<jobject>(nullptr);
    }
    std::string mime(cMime);
    if (mime == "image/avif" || mime == "image/avif-sequence") {
      AvifImageSize size = AvifDecoderController::getImageSize(srcBuffer.data(), srcBuffer.size());
      jclass sizeClass = env->FindClass("android/util/Size");
      jmethodID methodID = env->GetMethodID(sizeClass, "<init>", "(II)V");
      auto sizeObject = env->NewObject(sizeClass,
                                       methodID,
                                       static_cast<jint >(size.width),
                                       static_cast<jint>(size.height));
      return sizeObject;
    }

    auto result = heif_context_read_from_memory_without_copy(ctx.get(), srcBuffer.data(),
                                                             totalLength,
                                                             nullptr);
    if (result.code != heif_error_Ok) {
      std::string exception = "Reading an file buffer has failed";
      throwException(env, exception);
      return static_cast<jobject>(nullptr);
    }

    heif_image_handle *handle;
    result = heif_context_get_primary_image_handle(ctx.get(), &handle);
    if (result.code != heif_error_Ok) {
      std::string exception = "Acquiring an image from buffer has failed";
      throwException(env, exception);
      return static_cast<jobject>(nullptr);
    }
    int bitDepth = heif_image_handle_get_chroma_bits_per_pixel(handle);
    if (bitDepth < 0) {
      heif_image_handle_release(handle);
      throwBitDepthException(env);
      return static_cast<jobject>(nullptr);
    }
    auto width = heif_image_handle_get_width(handle);
    auto height = heif_image_handle_get_height(handle);
    heif_image_handle_release(handle);

    jclass sizeClass = env->FindClass("android/util/Size");
    jmethodID methodID = env->GetMethodID(sizeClass, "<init>", "(II)V");
    auto sizeObject = env->NewObject(sizeClass, methodID, width, height);
    return sizeObject;
  } catch (std::bad_alloc &err) {
    std::string exception = "Not enough memory to load size of this image";
    throwException(env, exception);
    return static_cast<jobject>(nullptr);
  } catch (std::runtime_error &err) {
    std::string exception(err.what());
    throwException(env, exception);
    return static_cast<jobject>(nullptr);
  }
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_radzivon_bartoshyk_avif_coder_HeifCoder_isSupportedImageImplBB(JNIEnv *env, jobject thiz,
                                                                        jobject byteBuffer) {
  try {
    auto bufferAddress = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(byteBuffer));
    int length = (int) env->GetDirectBufferCapacity(byteBuffer);
    if (!bufferAddress || length <= 0) {
      std::string errorString = "Only direct byte buffers are supported";
      throwException(env, errorString);
      return (jboolean) false;
    }
    std::vector<uint8_t> srcBuffer(length);
    std::copy(bufferAddress, bufferAddress + length, srcBuffer.begin());
    auto cMime = heif_get_file_mime_type(reinterpret_cast<const uint8_t *>(srcBuffer.data()),
                                         (int) srcBuffer.size());
    std::string mime(cMime);
    return mime == "image/heic" || mime == "image/heif" ||
        mime == "image/heic-sequence" || mime == "image/heif-sequence" ||
        mime == "image/avif" || mime == "image/avif-sequence";
  } catch (std::bad_alloc &err) {
    std::string exception = "Not enough memory to check this image";
    throwException(env, exception);
    return false;
  }
}

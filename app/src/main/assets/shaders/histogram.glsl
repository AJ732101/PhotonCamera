precision highp sampler2D;
precision highp int;
precision highp float;
uniform sampler2D inTexture;
uniform vec4 exposure;
#define COL_R 1
#define COL_G 1
#define COL_B 1
#define COL_A 1
#define COL_CUSTOM 0
#define HISTSIZE 256
//#define HISTMPY 255.0
#define SCALE 1
#define HISTSTEPS uint(HISTSIZE/64)

#if COL_R == 1
layout(std430, binding = 1) buffer histogramRed {
    uint reds[];
};
shared uint localRed[HISTSIZE];
#endif
#if COL_G == 1
layout(std430, binding = 2) buffer histogramGreen {
    uint greens[];
};
shared uint localGreen[HISTSIZE];
#endif
#if COL_B == 1
layout(std430, binding = 3) buffer histogramBlue {
    uint blues[];
};
shared uint localBlue[HISTSIZE];
#endif
#if COL_A == 1
layout(std430, binding = 4) buffer histogramAlpha {
    uint alphas[];
};
shared uint localAlpha[HISTSIZE];
#endif

#define CUSTOM_PROGRAM //

#define LAYOUT //
LAYOUT

void main() {
    ivec2 storePos = ivec2(gl_GlobalInvocationID.xy)*SCALE;
    ivec2 imgsize = textureSize(inTexture,0).xy;
    uint index = uint(gl_LocalInvocationIndex) * HISTSTEPS; // 0 - 64 * HISTSTEPS
    for (uint i = 0u; i < HISTSTEPS; i++) {
        #if COL_R == 1
        localRed[index + i] = 0u;
        #endif
        #if COL_G == 1
        localGreen[index + i] = 0u;
        #endif
        #if COL_B == 1
        localBlue[index + i] = 0u;
        #endif
        #if COL_A == 1
        localAlpha[index + i] = 0u;
        #endif
    }
    barrier();

    if (storePos.x < imgsize.x && storePos.y < imgsize.y) {
        vec4 texColor = texture(inTexture,(vec2(storePos) + 0.5)/vec2(imgsize));
        uvec4 texColorUint = clamp(uvec4(exposure * texColor), uvec4(0), uvec4(HISTSIZE - 1));
        #if COL_CUSTOM == 1
            CUSTOM_PROGRAM;
        #endif
        #if COL_R == 1
        atomicAdd(localRed[texColorUint.r], 1u);
        #endif
        #if COL_G == 1
        atomicAdd(localGreen[texColorUint.g], 1u);
        #endif
        #if COL_B == 1
        atomicAdd(localBlue[texColorUint.b], 1u);
        #endif
        #if COL_A == 1
        atomicAdd(localAlpha[texColorUint.a], 1u);
        #endif
    }
    barrier();

    for (uint i = 0u; i < HISTSTEPS; i++) {
        #if COL_R == 1
        atomicAdd(reds[index + i], localRed[index + i]);
        #endif
        #if COL_G == 1
        atomicAdd(greens[index + i], localGreen[index + i]);
        #endif
        #if COL_B == 1
        atomicAdd(blues[index + i], localBlue[index + i]);
        #endif
        #if COL_A == 1
        atomicAdd(alphas[index + i], localAlpha[index + i]);
        #endif
    }
}
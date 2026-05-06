
precision highp float;
precision highp sampler2D;
// Averaged frame at rawHalf resolution; each texel is a normalized vec4 Bayer quad
uniform highp sampler2D inTexture;

struct HotPixel {
    uint x;        // Pixel coordinate (rawHalf)
    uint y;
    uint channels; // Bitmask: R=1, G1=2, B=4, G2=8
    uint strength; // Detection strength for filtering
};

layout(std430, binding = 1) buffer HotPixelList {
    uint count;
    HotPixel hotPixels[];
};
uniform float noiseS;
uniform float noiseO;
uniform float detectThr;
uniform int maxCount;
#import median

// Each texel in the averaged texture is already a normalized [0,1] vec4 Bayer quad
vec4 getBayerVec(ivec2 coords, highp sampler2D tex){
    return texelFetch(tex, coords, 0);
}

#define LAYOUT //
LAYOUT
void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    vec4 bayer[9];
    for(int i = -1; i <= 1; i++){
        for(int j = -1; j <= 1; j++){
            vec4 bvalue = getBayerVec(coord + ivec2(i,j), inTexture);
            int idx = (i+1)*3 + (j+1);
            bayer[idx] = bvalue;
        }
    }
    vec4 center = bayer[4];
    vec4 med = median9(bayer);
    vec4 noise = sqrt(noiseS * med + noiseO);
    vec4 diff = abs(center - med);
    
    // Dynamic threshold: increase if list is > half full
    uint currentCount = count;
    float dynThr = detectThr;
    if (currentCount > uint(maxCount) / 2u) {
        float fillRatio = float(currentCount - uint(maxCount) / 2u) / float(uint(maxCount) / 2u);
        dynThr = detectThr * (1.0 + fillRatio);
    }
    
    bvec4 hdet = greaterThan(diff, noise * dynThr);
    if (any(hdet)){
        uint idx = atomicAdd(count, 1u);
        if (idx < uint(maxCount)) {
            HotPixel hot;
            hot.x = uint(coord.x);
            hot.y = uint(coord.y);
            hot.channels = uint(hdet[0]) + uint(hdet[1])*2u + uint(hdet[2])*4u + uint(hdet[3])*8u;
            vec4 zscore = diff / (noise + 1e-6);
            float maxZ = max(max(zscore.r, zscore.g), max(zscore.b, zscore.a));
            hot.strength = uint(maxZ * 1000.0);
            hotPixels[idx] = hot;
        }
    }
}

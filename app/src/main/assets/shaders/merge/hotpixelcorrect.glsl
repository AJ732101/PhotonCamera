
precision highp float;
precision highp sampler2D;

struct HotPixel {
    uint x;
    uint y;
    uint channels; // Bitmask: R=1, G1=2, B=4, G2=8
    uint values;
};

layout(rgba16f, binding = 0) readonly uniform highp image2D inTexture;
layout(rgba16f, binding = 1) writeonly uniform highp image2D outTexture;
layout(std430, binding = 2) readonly buffer HotPixelList {
    uint count;
    HotPixel hotPixels[];
};

#define LAYOUT //
LAYOUT
void main() {
    uint idx = gl_GlobalInvocationID.x;
    if (idx >= count) return;

    HotPixel hot = hotPixels[idx];
    ivec2 coord = ivec2(hot.x, hot.y);
    uint channels = hot.channels;
    
    // Read current value
    vec4 current = imageLoad(inTexture, coord);
    
    // Sample 4 neighbors (plus pattern for same-channel interpolation)
    vec4 top    = imageLoad(inTexture, coord + ivec2(0, -1));
    vec4 bottom = imageLoad(inTexture, coord + ivec2(0,  1));
    vec4 left   = imageLoad(inTexture, coord + ivec2(-1, 0));
    vec4 right  = imageLoad(inTexture, coord + ivec2( 1, 0));
    
    // Compute replacement value as average of 4 neighbors
    vec4 replacement = (top + bottom + left + right) * 0.25;
    
    // Apply correction only to hot channels
    vec4 result = current;
    if ((channels & 1u) != 0u) result.r = replacement.r;  // R channel
    if ((channels & 2u) != 0u) result.g = replacement.g;  // G1 channel
    if ((channels & 4u) != 0u) result.b = replacement.b;  // B channel
    if ((channels & 8u) != 0u) result.a = replacement.a;  // G2 channel
    
    imageStore(outTexture, coord, result);
}


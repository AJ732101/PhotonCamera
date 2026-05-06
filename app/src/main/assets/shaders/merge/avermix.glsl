
precision highp float;

layout(rgba16f, binding = 0) readonly  uniform highp image2D currentTexture;
layout(rgba16f, binding = 1) readonly  uniform highp image2D newTexture;
layout(rgba16f, binding = 2) writeonly uniform highp image2D outTexture;

// weight = 1 / frameCount: mix(current, new, weight) gives a running average
uniform float weight;

#define LAYOUT //
LAYOUT
void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(outTexture);
    if (coord.x >= size.x || coord.y >= size.y) return;

    vec4 current = imageLoad(currentTexture, coord);
    vec4 newVal   = imageLoad(newTexture,     coord);
    imageStore(outTexture, coord, mix(current, newVal, weight));
}

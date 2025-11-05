#define LAYOUT //
LAYOUT
precision highp float;
precision highp sampler2D;
precision highp image2D;
uniform highp sampler2D baseTexture;
uniform highp sampler2D gainMap;
layout(rgba16f, binding = 0) uniform highp writeonly image2D outTexture;
uniform vec4 blackLevel;
uniform float whiteLevel;
uniform float sharpness;
#define TILE 2
#define CONCAT 1
#define M_PI 3.1415926535897932384626433832795
#define TILE_AL 16

vec4 getPixelLaplacian(ivec2 coords, highp sampler2D tex) {
    ivec2 size = textureSize(tex, 0);
    coords = clamp(coords, ivec2(1), size - ivec2(2));
    vec4 center = texelFetch(tex, coords, 0);
    vec4 left = texelFetch(tex, coords + ivec2(-1, 0), 0);
    vec4 right = texelFetch(tex, coords + ivec2(1, 0), 0);
    vec4 up = texelFetch(tex, coords + ivec2(0, -1), 0);
    vec4 down = texelFetch(tex, coords + ivec2(0, 1), 0);
    return (left + right + up + down) - center * 3.0;
}

void main() {
    ivec2 xy = ivec2(gl_GlobalInvocationID.xy);
    vec4 bayer = getPixelLaplacian(xy, baseTexture);
    bayer = mix(texelFetch(baseTexture, xy, 0), bayer, sharpness);
    float gains = dot(texture(gainMap, vec2(xy)/vec2(imageSize(outTexture).xy)),vec4(0.25));
    bayer = clamp((bayer - blackLevel) / (vec4(whiteLevel) - blackLevel), 0.0, 1.0);
    imageStore(outTexture, xy, bayer);
}
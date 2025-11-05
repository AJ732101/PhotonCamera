#define LAYOUT //
LAYOUT
precision highp float;
precision highp sampler2D;
precision highp image2D;
uniform highp usampler2D inTexture;
uniform highp sampler2D gainMap;
layout(rgba16f, binding = 0) uniform highp writeonly image2D outTexture;


uniform float whiteLevel;
uniform vec4 blackLevel;
uniform float exposure;
uniform float noiseS;
uniform float noiseO;
#define TILE 2
#define CONCAT 1
#define M_PI 3.1415926535897932384626433832795
#define TILE_AL 16

uint getBayer(ivec2 coords, highp usampler2D tex){
    return texelFetch(tex,coords,0).r;
}

vec4 getBayerVec(ivec2 coords, highp usampler2D tex){
    vec4 c0 = vec4(getBayer(coords,tex),getBayer(coords+ivec2(1,0),tex),getBayer(coords+ivec2(0,1),tex),getBayer(coords+ivec2(1,1),tex));
    return clamp((c0 - blackLevel)/(vec4(whiteLevel)-blackLevel), 0.0, 1.0);
}

void main() {
    ivec2 xy = ivec2(gl_GlobalInvocationID.xy);
    vec4 bayer;
    // multisample
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            ivec2 offset = ivec2(i, j);
            bayer += getBayerVec((xy+offset) * TILE, inTexture);
        }
    }
    bayer /= 16.0; // Average the samples
    float gains = dot(texture(gainMap, vec2(xy)/vec2(imageSize(outTexture).xy)),vec4(0.25));
    float br = dot(bayer, vec4(0.25));
    float noise = sqrt(noiseS * 1.0 +  noiseO);
    float thr = 0.0;
    bayer = clamp(bayer*gains, vec4(0.0), vec4(1.0));
    vec4 res = (clamp((bayer * vec4(exposure) - thr)/(1.0 - thr), 0.0, 1.0));
    //res *= res;
    //res = mix(res*res, (res), clamp((br-0.1)/0.9, 0.0, 1.0));
    imageStore(outTexture, xy, res);
}
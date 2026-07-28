#define LAYOUT //
LAYOUT
precision highp float;
precision highp sampler2D;
precision highp image2D;
uniform highp usampler2D inTexture;
layout(rgba16f, binding = 0) uniform highp writeonly image2D outTexture;


uniform float whiteLevel;
uniform vec4 blackLevel;
uniform float exposure;
uniform bool createDiff;
uniform float noiseS;
uniform float noiseO;
uniform ivec2 border;
uniform int cfaPattern;
uniform vec4 analogBalance;
uniform vec2 randF;
#define TILE 2
#define CONCAT 1
#define M_PI 3.1415926535897932384626433832795
#define TILE_AL 16

uint getBayer(ivec2 coords, highp usampler2D tex){
    return texelFetch(tex,coords,0).r;
}

float getBayerNorm(ivec2 coords, highp usampler2D tex){
    return clamp((float(getBayer(coords, tex)) - dot(blackLevel, vec4(0.25)))/(float(whiteLevel)-dot(blackLevel,vec4(0.25))), 0.0, 1.0);
}
vec4 getBayerVec(ivec2 coords, highp usampler2D tex){
    vec4 c0 = vec4(getBayer(coords,tex),getBayer(coords+ivec2(1,0),tex),getBayer(coords+ivec2(0,1),tex),getBayer(coords+ivec2(1,1),tex));
    return clamp((c0 - blackLevel)/(vec4(whiteLevel)-blackLevel), 0.0, 1.0);
}

float getTest(ivec2 coords, highp usampler2D tex){
    return (float(coords.x)/4000.0 + float(coords.y)/3000.0)/2.0;
}

vec4 getTestBayerVec(ivec2 coords, highp usampler2D tex){
    vec4 c0 = vec4(getTest(coords,tex),getTest(coords+ivec2(1,0),tex),getTest(coords+ivec2(0,1),tex),getTest(coords+ivec2(1,1),tex));
    return clamp(c0, 0.0, 1.0);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453123);
}

vec4 noise4(ivec2 p) {
    vec2 pf = vec2(p)+randF;
    return vec4(
    hash(pf),
    hash(pf + vec2(1.23, 4.56)), // Offset to decouple channels
    hash(pf + vec2(7.89, 0.12)),
    hash(pf + vec2(3.45, 6.78))
    );
}

void main() {
    ivec2 xy = ivec2(gl_GlobalInvocationID.xy);
    //vec4 bayer = getTestBayerVec(xy*TILE, inTexture)/analogBalance;
    vec4 bayer = getBayerVec(xy*TILE, inTexture);
    float br = dot(bayer, vec4(0.25));
    vec4 n = sqrt(noiseS*vec4(bayer) + noiseO);
    //bayer += (noise4(xy)-0.5) * (n) * 2.0;
    imageStore(outTexture, xy, bayer * vec4(exposure));
}
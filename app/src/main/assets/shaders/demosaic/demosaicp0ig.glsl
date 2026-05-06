#define LAYOUT //
LAYOUT
precision highp float;
precision highp sampler2D;
//uniform sampler2D RawBuffer;
//uniform sampler2D GreenBuffer;
layout(rgba16f, binding = 0) uniform highp readonly image2D inTexture;
layout(rgba16f, binding = 2) uniform highp writeonly image2D outTexture;
uniform int yOffset;
//out vec3 Output;
#define EPS 0.0001
#define EPS2 0.001
#define alpha 3.75
#define BETA 0.42
//#define BETA 0.0
#define THRESHOLD 1.9
#define L 3
//#define greenmin (0.04)
#define greenmin (0.01)
//#define greenmax (0.9)
#define greenmax (0.99)
// Helper function to determine Bayer pattern at a given position
// 0: R, 1: G (at red row), 2: G (at blue row), 3: B
int getBayerPattern(ivec2 pos) {
    int x = (pos.x) % 2;
    int y = (pos.y) % 2;
    return (y << 1) | x;
}

float getBayerSample(ivec2 pos) {
    return imageLoad(inTexture, pos).r;
    //return float(texelFetch(RawBuffer, pos, 0).x);
}

float bayer(ivec2 pos){
    return getBayerSample(pos);
    //return float(texelFetch(RawBuffer, pos, 0).x);
}
float cd(ivec2 pos, ivec2 stp) {
    return getBayerSample(pos) - (getBayerSample(pos - stp) + getBayerSample(pos + stp))/2.0;
}

float dxy(ivec2 pos, ivec2 stp) {
    //return abs((4.0 * getBayerSample(pos) - 3.0 * getBayerSample(pos + stp) - 3.0 * getBayerSample(pos - stp) + getBayerSample(pos - 2*stp) + getBayerSample(pos + 2*stp))/6.0);
    return (abs(cd(pos, stp) - cd(pos + stp, stp)) + abs(cd(pos, stp) - cd(pos - stp, stp)))/2.0;
}

float dt(ivec2 pos, ivec2 stp) {
    float c = dxy(pos, stp);
    float c2 = dxy(pos + 2*stp, stp);
    float c1 = dxy(pos + stp, stp);
    return (abs(c - c1) + abs(c1 - c2))/2.0;
    //return (dxy(pos-stp, stp) + dxy(pos+stp, stp) + dxy(pos,stp))/3.0;
}


float dxy2(ivec2 pos, ivec2 stp) {
    float c = getBayerSample(pos);
    return (abs(getBayerSample(pos) - getBayerSample(pos + 2*stp)) + abs(getBayerSample(pos - stp) - getBayerSample(pos + stp))) / 2.0 + alpha * dt(pos,stp);
}

float IG(ivec2 pos, ivec2 stp) {
    ivec2 invStep = ivec2(1,1)-stp;
    //return abs(getBayerSample(pos) - getBayerSample(pos + 2*stp)) + alpha * (2.0*dt(pos+stp,stp) + dt(pos+stp - invStep,stp) + dt(pos+stp + invStep,stp));
    return 2.0 * dxy2(pos,stp) + dxy2(pos - invStep,stp) + dxy2(pos + invStep,stp);
}

void main(){
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
    vec2 res = vec2(0.0);
    res = vec2(IG(pos, ivec2(1,0)), IG(pos, ivec2(0,1)));
    imageStore(outTexture, pos, clamp(abs(vec4(res.x, res.y, 0.0, 1.0)), vec4(0.0), vec4(1.0)));
}
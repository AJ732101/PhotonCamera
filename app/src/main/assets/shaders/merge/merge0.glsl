#define LAYOUT //
LAYOUT
precision highp float;
precision highp sampler2D;
precision highp image2D;
uniform highp usampler2D inTexture;
uniform highp sampler2D alignmentTexture;
//layout(r16ui, binding = 0) uniform highp readonly uimage2D inTexture;
layout(rgba16f, binding = 0) uniform highp readonly image2D avrTexture;
layout(rgba8, binding = 1) uniform highp readonly image2D hotPixTexture;
layout(rgba16f, binding = 2) uniform highp readonly image2D baseTexture;
layout(rgba16f, binding = 3) uniform highp writeonly image2D outTexture;
layout(rgba16f, binding = 4) uniform highp readonly image2D alterTexture;

uniform float minLevel;
uniform float whiteLevel;
uniform vec4 blackLevel;
uniform float exposure;
uniform float exposureLow;
uniform bool createDiff;
uniform float noiseS;
uniform float noiseO;
uniform ivec2 border;
uniform ivec2 shift;
uniform ivec2 alignmentSize;
uniform ivec2 rawHalf;
uniform vec4 analogBalance;
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

float window(float x){
    return 0.5f - 0.5f * cos(2.f * M_PI * ((0.5f * (x + 0.5f) / float(TILE_AL))));
}

float windowxy(ivec2 xy){
    return window(float(xy.x)) * window(float(xy.y));
}

vec4 windowxy4(ivec2 xy){
    return vec4(window(float(xy.x)) * window(float(xy.y)),
                window(float(xy.x+1)) * window(float(xy.y)),
                window(float(xy.x)) * window(float(xy.y+1)),
                window(float(xy.x+1)) * window(float(xy.y+1)));
}
/*
vec4 robustWeight(vec4 w){
    float mv = min(w.r, min(w.g, min(w.b, w.a)));
    //mv = smoothstep(0.1, 0.9, mv);
    return vec4(mv);
}*/
vec4 robustWeight(vec4 w){
    return vec4(w.r * 2.0 + w.g + w.a,
    w.g * 2.0 + w.b + w.r,
    w.b * 2.0 + w.a + w.g,
    w.a * 2.0 + w.r + w.b) / 4.0;
}

vec2 vec4ToAlignment(vec4 alignment) {
    vec2 converted = vec2(alignment.x * float(rawHalf.x), alignment.y * float(rawHalf.y));
    converted.xy += alignment.zw;
    return converted;
}
vec2 hash22(vec2 p)
{
    vec3 p3 = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx+33.33);
    return fract((p3.xx+p3.yz)*p3.zy);
}
void main() {
    ivec2 xy = ivec2(gl_GlobalInvocationID.xy);
    ivec2 outSize = imageSize(outTexture);
    vec2 uvScale = vec2(outSize-border);
    vec2 uv = vec2(xy)/uvScale + vec2(0.5)/uvScale;
    vec4 bayerBase = imageLoad(baseTexture,xy);
    vec4 bayer = getBayerVec(xy*TILE, inTexture);
    //vec4 hp = imageLoad(hotPixTexture, xy);
    //bayer = bayer * vec4(1.0-hp) + imageLoad(avrTexture, xy) * hp;
    vec4 noise = vec4(max(sqrt(max(bayer * noiseS + noiseO, 1e-6)), vec4(minLevel)));
    vec4 w[4];
    w[3] = windowxy4((TILE*xy)%TILE_AL);
    w[2] = windowxy4((TILE*xy)%TILE_AL + ivec2(TILE_AL,0));
    w[1] = windowxy4((TILE*xy)%TILE_AL + ivec2(0,TILE_AL));
    w[0] = windowxy4((TILE*xy)%TILE_AL + ivec2(TILE_AL));
    vec4 alignedSum = vec4(0.0);
    vec4 bayerNone = imageLoad(alterTexture, xy);
    //ivec2 alignPrev = ivec2(xy);
    for (int i = 0; i < 4; i++) {
        ivec2 xyT = clamp(ivec2((TILE*xy)/TILE_AL + ivec2(i % 2, i / 2)),ivec2(0),alignmentSize-1);
        vec4 alignLoad = texelFetch(alignmentTexture, xyT + shift, 0);
        //ivec2 xyT = (TILE*xy)/TILE_AL;
        /*vec2 seed = hash22(vec2(xyT)/vec2(alignmentSize));
        float mpy = 1.0;
        if(seed.x < 0.1) {
            mpy = 50.0;
        }*/
        //mpy = (seed.x + seed.y) * 5.0;
        ivec2 align = ivec2(vec4ToAlignment(alignLoad));
        //vec2 align = texture(alignmentTexture, uv + vec2(i % 2, i / 2) / uvScale).xy;
        ivec2 aligned = clamp(xy + align, ivec2(0), outSize - ivec2(1));
        //ivec2 alignedFull = aligned * TILE;
        //aligned = ivec2(clamp(aligned, ivec2(0), ivec2(outSize - 1)));
        /*bvec2 lt = lessThan(aligned, ivec2(0));
        aligned = aligned * ivec2(not(lt)) + ivec2(-aligned) * ivec2(lt);
        bvec2 gt = greaterThan(aligned, ivec2(outSize - 1));
        aligned = (2 * ivec2(outSize) - aligned - 1) * ivec2(gt) + aligned * ivec2(not(gt));*/
        vec4 bayerAlter = imageLoad(alterTexture, aligned);

        //vec4 bayerPrev = getBayerVec(alignPrev * TILE, alterTexture);
        vec4 w1 = (abs(bayerAlter*vec4(exposure) - bayerBase));
        vec4 w2 = (abs(bayerNone*vec4(exposure) - bayerBase));
        //vec4 w3 = (abs(bayerPrev*vec4(exposure) - bayerBase));
        /*if(dot(w3,vec4(0.25)) < dot(w2,vec4(0.25))) {
            bayerNone = mix(bayerPrev, bayerNone, clamp(w3/(w2+w3+0.0001),vec4(0.0),vec4(1.0)));
            w2 = w3;
        }*/
        bayerAlter = mix(bayerNone, bayerAlter, smoothstep(w2/(w1+w2),vec4(0.48),vec4(0.51)));

        //vec4 hp2 = imageLoad(hotPixTexture, aligned * TILE);
        //bayerAlter = bayerAlter * vec4(1.0-hp2) + imageLoad(avrTexture, aligned * TILE) * hp2;
        alignedSum += bayerAlter * w[i];
        //bayerPrev = bayerAlter;
    }


    alignedSum = clamp(alignedSum, vec4(0.0), vec4(1.0));
    alignedSum *= vec4(exposure);
    float target = 1.0;
    if(exposure <= 0.9){
        target = 0.8;
    }
    /*if(any(greaterThan(alignedSum, vec4(target*exposure))) || any(greaterThan(bayerBase, vec4(target*exposure)))){
        alignedSum = bayerBase;
    }*/
    float ma = max(max(max(alignedSum.r, alignedSum.g), alignedSum.b), alignedSum.a);
    float mb = max(max(max(bayerBase.r, bayerBase.g), bayerBase.b), bayerBase.a);
    float mixf = clamp(max((ma-target*exposure),(mb-target*exposure))/(max(0.01,exposure-target*exposure)),0.0,1.0);
    float mixf2 = clamp(max((target*exposureLow-ma),(target*exposureLow-mb))/(max(0.01,exposureLow-target*exposureLow)),0.0,1.0);
    vec4 bbDiff = bayerBase - bayer;
    vec4 bbDiffm = max(abs(bbDiff) - noise, vec4(0.0));
    bbDiff *= (((noise*noise*8.0)/(noise*noise*8.0 + bbDiffm*bbDiffm)));

    //vec4 denoised = mix(bayer+bbDiff, bayer, clamp(mb,0.0,1.0));
    vec4 denoised = bayer+bbDiff;
    //alignedSum = mix(alignedSum, bayer, clamp(mixf+mixf2, 0.0,1.0));
    alignedSum -= bayer;
    alignedSum *= analogBalance;
    /*if(any(greaterThan(abs(alignedSum), vec4(target*exposure)))) {
        alignedSum = vec4(0.0);
    }*/
    vec4 an = max(abs(alignedSum) - noise, vec4(0.0));
    alignedSum *= (((noise*noise*4.0)/(noise*noise*4.0 + an*an)));
    imageStore(outTexture, xy, clamp(alignedSum, vec4(-1.0), vec4(1.0)));
}

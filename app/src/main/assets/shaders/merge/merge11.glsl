#define LAYOUT //
LAYOUT
precision highp float;
precision highp sampler2D;
//uniform highp usampler2D alterTexture;
uniform highp usampler2D inTex;
layout(rgba16f, binding = 0) uniform highp readonly image2D inTexture;
layout(rgba16f, binding = 1) uniform highp readonly image2D diffTexture;
layout(rgba16f, binding = 2) uniform highp writeonly image2D outTexture;
layout(rgba16f, binding = 3) uniform highp readonly image2D diffOrTexture;
#define TILE 2
#define CONCAT 1
uniform float weight;
uniform float weight2;
uniform float exposure;
uniform float noiseS;
uniform float noiseO;
uniform float whiteLevel;
uniform vec4 blackLevel;
uniform vec4 analogBalance;
uniform int cfaPattern;
#import median
uint getBayer(ivec2 coords, highp usampler2D tex){
    return texelFetch(tex,coords,0).r;
}

vec4 getBayerVec(ivec2 coords, highp usampler2D tex){
    vec4 c0 = vec4(getBayer(coords,tex),getBayer(coords+ivec2(1,0),tex),getBayer(coords+ivec2(0,1),tex),getBayer(coords+ivec2(1,1),tex));
    return clamp((c0 - blackLevel)/(vec4(whiteLevel)-blackLevel), 0.0, 1.0);
}

vec4 robustWeight(vec4 w){
    return vec4(min(w.r, min(w.g, min(w.b, w.a))));
}

#define EPS 1e-6
void main() {
    ivec2 xy = ivec2(gl_GlobalInvocationID.xy);
    vec4 base = imageLoad(inTexture, xy);
    vec4 noise = sqrt(max(base * noiseS + noiseO,EPS));
    vec4 diff = imageLoad(diffTexture, xy);
    vec4 bayer = getBayerVec(xy*2, inTex);
    vec4 mean = vec4(0.0);
    vec4 medians[9];
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            vec4 diff = getBayerVec((xy+ivec2(i, j))*2, inTex);
            //mean += diff;
            medians[(i+1)*3+(j+1)] = diff;
        }
    }
    //mean /= 9.0;
    mean = median9(medians);
    vec4 variance = vec4(0.0);
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            vec4 diff = getBayerVec((xy+ivec2(i, j))*2, inTex);
            variance = max(((diff-mean)*(diff-mean)), variance);
        }
    }
    //variance /= 8.0;
    /*if(length(diff) > 0.1){
        diff = vec4(0.0);
    }*/
    //diff *= robustWeight(sqrt(vec4(1.0) - ((diff*diff)/(noise*noise*1.0 + diff*diff))));
    //float cexp = max(base.r, max(base.g, max(base.b, base.a)));
    /*if(cexp < exposure*0.7){
        diff*=weight;
    } else {
        diff*=weight2;
    }*/
    //vec4 alter = getBayerVec(xy*2, alterTexture);
    //vec4 alterFiltered = base + diff;
    //float d1 = length(alterFiltered - alter) + EPS;
    //float d2 = length(alterFiltered - base) + EPS;
    //alterFiltered = mix(alter, base, d1/(d1+d2));
    //alterFiltered = clamp(alterFiltered, min(alter, base), max(alter, base));
    //diff = alterFiltered - base;
    //vec4 diffOrigin = alter*vec4(exposure) - base;
    vec4 diffOrigin = imageLoad(diffOrTexture, xy);
    //if(dot(alter, vec4(0.25)) > exposure || dot(base, vec4(0.25)) > exposure) {
    //    diffOrigin = vec4(0.0);
    //}
    //diff = diffOrigin * (diff.x / (dot(diffOrigin, vec4(0.25)) + EPS));
    //diff = clamp(diff, min(diffOrigin, vec4(0.0)), max(diffOrigin, vec4(0.0)));
    //diff *= ((((noise*noise)/(noise*noise + diff*diff))));
    float lDiff = clamp(length(diff), EPS, sqrt(length(variance)*1.4826 + EPS));
    //float lDiff = length(diff);
    diff = diffOrigin / (length(diffOrigin) + EPS) * lDiff;
    //diff *= ((((noise*noise)/(noise*noise + diff*diff))));
    imageStore(outTexture, xy, mix(base, diff/analogBalance+bayer, weight));
    //imageStore(outTexture, xy, diff);
}

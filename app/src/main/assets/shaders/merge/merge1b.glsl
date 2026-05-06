#define LAYOUT //
LAYOUT
precision highp float;
precision highp sampler2D;
uniform mediump sampler2D brTexture;
uniform highp sampler2D baseTexture;
layout(rgba16f, binding = 0) uniform highp readonly image2D diffTexture;
layout(rgba16f, binding = 1) uniform highp writeonly image2D outTexture;
#define TILE 2
#define CONCAT 1
#define EPS 1e-6
uniform float minLevel;
uniform float noiseS;
uniform float noiseO;
uniform float integralNorm;
uniform bool first;
uniform int cfaPattern;
#define MAXWEIGHT 1.0
#define MINWEIGHT 0.0
#define SIGMA 1.5
/*vec4 robustWeight(vec4 w){
    return vec4(w.r * 2.0 + w.g + w.a,
                w.g * 2.0 + w.b + w.r,
                w.b * 2.0 + w.a + w.g,
                w.a * 2.0 + w.r + w.b) / 4.0;
}*/
vec4 robustWeight(vec4 w){
    float mv = max(w.r, max(w.g, max(w.b, w.a)));
    //mv = smoothstep(0.05, 0.95, mv);
    return vec4(mv);
}
/*vec4 robustWeight(vec4 w){
    return vec4(w.r + w.g + w.a + w.b,
                w.g + w.b + w.r + w.a,
                w.b + w.a + w.g + w.r,
                w.a + w.r + w.b + w.g) / 4.0;
}*/

float bayerCoord(ivec2 pos){
    int cnt = (pos.x %2) + 2 * (pos.y %2);
    return imageLoad(diffTexture, pos/2)[cnt];
}

#import interpolation
#import gaussian
void main() {
    ivec2 xy = ivec2(gl_GlobalInvocationID.xy);
    vec2 uv = vec2(xy) / vec2(imageSize(outTexture)) + vec2(0.5) / vec2(imageSize(outTexture));
    vec4 base = max(texture(baseTexture, uv), texture(baseTexture, uv+vec2(1,0)/vec2(imageSize(outTexture))));
    base = max(base, texture(baseTexture, uv+vec2(0,1)/vec2(imageSize(outTexture))));
    base = max(base, texture(baseTexture, uv+vec2(0,-1)/vec2(imageSize(outTexture))));
    base = max(base, texture(baseTexture, uv+vec2(-1,0)/vec2(imageSize(outTexture))));

    vec4 br = texture(brTexture, uv);
    vec4 noise = max(sqrt(max(br * noiseS + noiseO,EPS)), vec4(minLevel));
    // do wiener filtering
    vec4 weightSum = vec4(0.0001);
    float Z = 0.0001;
    vec4 diffCenter = imageLoad(diffTexture, xy);
    vec4 diffNormCenter = diffCenter*integralNorm;
    vec4 mean = vec4(0.0);
    vec4 variance = vec4(0.0);
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            vec4 diff = imageLoad(diffTexture, xy+ivec2(i, j));
            variance += ((diff-mean)*(diff-mean));
        }
    }
    variance /= 8.0;
    noise /= integralNorm*integralNorm;
    //variance = (max(variance - noise*noise, vec4(0.0)));
    vec4 w = (noise*noise) / (noise * noise + variance);
    w = ((clamp(w, MINWEIGHT, MAXWEIGHT)-MINWEIGHT)/(MAXWEIGHT-MINWEIGHT));
    if(first){
        base = vec4(1.0);
        //base *= robustWeight(w);
    }
    //base = vec4(1.0);
    imageStore(outTexture, xy, base*robustWeight(w));
    //imageStore(outTexture, xy, base + diffCenter);
}

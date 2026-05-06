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
uniform vec2 size;
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
    float mv = min(w.r, min(w.g, min(w.b, w.a)));
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
    vec2 uv = vec2(xy) * size;// + vec2(0.5) * size;
    vec4 base = texture(baseTexture, uv);

    vec4 br = texture(brTexture, uv);
    vec4 noise = max(sqrt(max(br * noiseS + noiseO,EPS)), vec4(minLevel));
    // use GAT
    //vec4 noise = sqrt(max(noiseS*br + noiseS*noiseS * 3.0/8.0 + noiseO, EPS));
    // do wiener filtering
    vec4 weightSum = vec4(0.0001);
    float Z = 0.0001;
    vec4 diffCenter = imageLoad(diffTexture, xy);
    vec4 diffNormCenter = diffCenter*integralNorm;
    vec4 mean = vec4(0.0);
    /*for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            vec4 diff = imageLoad(diffTexture, xy+ivec2(i, j));
            mean += diff;
        }
    }
    mean /= 9.0;*/
    vec4 variance = vec4(0.0);
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            vec4 diff = imageLoad(diffTexture, xy+ivec2(i, j));
            variance += ((diff-mean)*(diff-mean));
        }
    }
    variance /= 8.0;
    /*for (int i = -1; i <= 1; i++) {
        float f0 = pdf(float(i)/SIGMA);
        for (int j = -1; j <= 1; j++) {
            vec4 diff = imageLoad(diffTexture, xy+ivec2(i, j));
            vec4 diffNorm = diff*integralNorm;
            diffNorm *= diffNorm;
            diffNorm = max(diffNorm - noise*noise, vec4(0.0));
            //vec4 w = vec4(greaterThan(diffNorm, noise));
            vec4 w = diffNorm / (noise * noise + diffNorm);
            //w = vec4(1.0) - sqrt(w+EPS);
            //w = robustWeight(w);
            float w2 = 1.0/(length(diffNorm-diffNormCenter) + dot(noise,vec4(0.25)));
            float f = pdf(float(j)/SIGMA) * f0;
            //weightSum += f * w * w2;
            weightSum = max(weightSum, w);
            Z += f * w2;
        }
    }*/
    //vec4 w = weightSum;
    //vec4 w = weightSum;
    noise /= integralNorm*integralNorm;
    variance = (max(variance - noise*noise, vec4(0.0)));
    vec4 w = (noise*noise) / (noise * noise + variance);
    w = ((clamp(w, MINWEIGHT, MAXWEIGHT)-MINWEIGHT)/(MAXWEIGHT-MINWEIGHT));
    //w = (vec4(1.0) - w);
    if(first){
        //base = vec4(0.0);
        base *= (w);
    }
    //w = vec4(1.0);
    //w = robustWeight(w);

    //diffCenter.r *= w.r;
    //diffCenter.g *= w.g;
    //diffCenter.b *= w.b;
    //diffCenter.a *= w.a;
    /*vec4 storing = (base + diffCenter);
    storing.r *= w.r;
    storing.g *= w.g;
    storing.b *= w.b;
    storing.a *= w.a;*/

    //imageStore(outTexture, xy, clamp(base + diffCenter*robustWeight(w), -noise, noise));
    imageStore(outTexture, xy, base + diffCenter*(w));
    //imageStore(outTexture, xy, base + diffCenter);
}

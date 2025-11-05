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
uniform float noiseS;
uniform float noiseO;
uniform float integralNorm;
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
    //mv = smoothstep(0.1, 0.9, mv);
    return vec4(mv);
}
/*vec4 robustWeight(vec4 w){
    return vec4(w.r + w.g + w.a + w.b,
                w.g + w.b + w.r + w.a,
                w.b + w.a + w.g + w.r,
                w.a + w.r + w.b + w.g) / 4.0;
}*/

#import interpolation
#import gaussian
void main() {
    ivec2 xy = ivec2(gl_GlobalInvocationID.xy);
    vec2 uv = vec2(xy) / vec2(imageSize(outTexture)) + vec2(0.5) / vec2(imageSize(outTexture));
    //vec2 uv = vec2(xy) / vec2(imageSize(outTexture));
    vec4 base = textureBicubicHardware(baseTexture, uv);

    vec4 br = texture(brTexture, uv);
    vec4 noise = sqrt(max(br * noiseS + noiseO,EPS));
    // do wiener filtering
    vec4 weightSum = vec4(0.0001);
    float Z = 0.0001;
    vec4 diffCenter = imageLoad(diffTexture, xy);
    vec4 diffNormCenter = diffCenter*(integralNorm);
    for (int i = -1; i <= 1; i++) {
        float f0 = pdf(float(i)/SIGMA);
        for (int j = -1; j <= 1; j++) {
            vec4 diff = imageLoad(diffTexture, xy+ivec2(i, j));
            vec4 diffNorm = diff*sqrt(integralNorm);
            vec4 w = diffNorm * diffNorm / (noise * noise + diffNorm * diffNorm);
            //w = vec4(1.0) - sqrt(w+EPS);
            //w = robustWeight(w);
            float w2 = 1.0/(length(diffNorm-diffNormCenter) + dot(noise,vec4(0.25)));
            float f = pdf(float(j)/SIGMA) * f0;
            //weightSum += f * w * w2;
            weightSum = max(weightSum, w);
            Z += f * w2;
        }
    }
    //vec4 w = weightSum;// / vec4(Z);
    vec4 w = diffNormCenter * diffNormCenter / (noise * noise + diffNormCenter * diffNormCenter);
    w = ((clamp(w, MINWEIGHT, MAXWEIGHT)-MINWEIGHT)/(MAXWEIGHT-MINWEIGHT));
    //w = vec4(1.0) - w;
    //w = vec4(1.0);
    //w = robustWeight(w);

    //diffCenter.r *= w.r;
    //diffCenter.g *= w.g;
    //diffCenter.b *= w.b;
    //diffCenter.a *= w.a;
    vec4 storing = (diffCenter);

    imageStore(outTexture, xy, base + storing*robustWeight(w));
}

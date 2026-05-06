
#define SCALE 4
#define SCALEMPY (1.0/float(SCALE))
precision highp float;
precision highp sampler2D;
uniform sampler2D LowresInput;
uniform sampler2D Guide;
uniform sampler2D GuideHigh;
uniform float noiseS;
uniform float noiseO;
out vec3 Output;
#import interpolation
#import median
void computeAB(ivec2 center, out vec3 a, out vec3 b) {
    float momentX  = 0.0;
    vec3  momentY  = vec3(0.0);
    float momentX2 = 0.0;
    vec3  momentXY = vec3(0.0);
    float ws = 0.0;
    const float sigma     = 1.2;
    const float sigmaSq2  = 2.0 * sigma * sigma;
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            ivec2 pos     = center + ivec2(i, j);
            vec3  lowresVal  = texelFetch(LowresInput, pos, 0).rgb;
            float lightness  = dot(texelFetch(Guide, pos, 0).rgb, vec3(1.0/3.0));
            float w          = exp(-float(i*i + j*j) / sigmaSq2);
            momentX  += lightness * w;
            momentY  += lowresVal * w;
            momentX2 += lightness * lightness * w;
            momentXY += lightness * lowresVal * w;
            ws       += w;
        }
    }
    float invWs = 1.0 / ws;
    float meanX = momentX * invWs;
    vec3  meanY = momentY * invWs;
    vec3  covXY = momentXY * invWs - meanX * meanY;
    float varX  = momentX2 * invWs - meanX * meanX;
    // When variance is too low the guide provides no useful signal,
    // so blend linearly toward a=0 (output = meanY) to avoid instability.
    float varThreshold = 0.001;
    float varWeight    = varX / (varX + varThreshold);
    a = varWeight * covXY / (varX + varThreshold);
    b = meanY - a * meanX;
}

void main() {
    ivec2 xy   = ivec2(gl_FragCoord.xy);
    ivec2 size = textureSize(GuideHigh, 0);

    vec2  lowres_pos = vec2(xy) / float(SCALE);
    ivec2 c          = ivec2(floor(lowres_pos));
    vec2  f          = fract(lowres_pos);
    vec3 a00, b00, a10, b10, a01, b01, a11, b11;
    computeAB(c,                 a00, b00);
    computeAB(c + ivec2(1, 0),   a10, b10);
    computeAB(c + ivec2(0, 1),   a01, b01);
    computeAB(c + ivec2(1, 1),   a11, b11);

    vec3 a = mix(mix(a00, a10, f.x), mix(a01, a11, f.x), f.y);
    vec3 b = mix(mix(b00, b10, f.x), mix(b01, b11, f.x), f.y);

    vec3 guideCenter = texelFetch(GuideHigh, xy, 0).rgb;
    Output = a * dot(guideCenter, vec3(1.0/3.0)) + b;
    float diffs[9];
    for (int i = -1; i <= 1; i++) {
        for (int j = - 1; j <= 1; j++) {
            ivec2 pos = xy + ivec2(i, j);
            vec3  guideVal = texelFetch(GuideHigh, pos, 0).rgb;
            diffs[(i+1)*3 + (j+1)] = dot(guideVal - (a * dot(guideVal, vec3(1.0/3.0)) + b), vec3(1.0/3.0));
        }
    }
    float medianDiff = median9(diffs);
    float absDev[9];
    for(int i = 0; i < 9; i++) {
        absDev[i] = abs(diffs[i] - medianDiff);
    }
    float mad = median9(absDev);
    float centerDiff = diffs[4];
    float threshold = 4.5 * mad;
    float dev = abs(centerDiff - medianDiff);

    float br = dot(Output, vec3(1.0/3.0));
    float noise = sqrt(noiseS * br + noiseO);
    float guideBr = dot(guideCenter, vec3(1.0/3.0));
    if (dev > threshold) {
        //guideBr = guideBr - medianDiff;
    }
    //float pd = exp(((centerDiff - medianDiff)*(centerDiff - medianDiff))/(-2.0*noise*noise));
    //guideBr = mix(br, guideBr, pd);
    Output = (Output/br) * guideBr;
    //Output = abs(textureLinear(Guide, gl_FragCoord.xy / vec2(size)).rgb - guideCenter);
}

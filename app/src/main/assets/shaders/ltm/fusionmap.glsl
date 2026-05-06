precision highp sampler2D;
precision highp float;
uniform sampler2D InputBuffer;
uniform sampler2D BrBuffer;
uniform float factor;
out vec2 result;
uniform int yOffset;
#define DH (0.0)
#define FUSIONGAIN 1.0
#define NORM 64.0
#define luminocity(x) dot(x.rgb, vec3(0.299, 0.587, 0.114))
float gammaInverse(float x) {
    return x*x;
}

vec4 reinhard_extended(vec4 v, float max_white) {
    vec4 numerator = v * (vec4(1.0f) + (v / vec4(max_white * max_white)));
    return numerator / (vec4(1.0f) + v);
}

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    xy+=ivec2(0,yOffset);
    //float br = texelFetch(BrBuffer, xy, 0).r;
    //float br2 = texelFetch(InputBuffer, xy, 0).r;
    //br2*=br2;
    //br2=sqrt(br2);
    //br*=br;
    //br2 = gammaInverse(br2);
    //br2+=DH;
    //br = gammaInverse(br);
    //br+=DH+0.003;

    float momentX = 0.0, momentY = 0.0, momentX2 = 0.0, momentXY = 0.0;
    float ws = 0.0;
    const float sigma = 0.5;
    const float sigmaSq2 = 2.0 * sigma * sigma;
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            ivec2 pos     = xy + ivec2(i, j);
            float lowresVal  = (texelFetch(InputBuffer, pos, 0).r + 0.001) / (texelFetch(BrBuffer, pos, 0).r + 0.001);
            //float lowresVal  = texelFetch(BrBuffer, pos, 0).r * 4.0;
            float lightness  = gammaInverse(texelFetch(BrBuffer, pos, 0).r * 4.0);
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
    float meanY = momentY * invWs;
    float covXY = momentXY * invWs - meanX * meanY;
    float varX = momentX2 * invWs - meanX * meanX;
    // Handle zero variance case with epsilon for stability
    float a = covXY / (max(varX, 0.0) + 0.0001);
    float b = meanY - a * meanX;
    result = vec2(a,b);
    //result=(((br2+0.00001)/((br)+0.00001)));
    //result = max(result,mix(1.0,result,clamp(br*10.0,0.0,1.0)));
    result = clamp(result/(FUSIONGAIN),-1.0,1.0);
}


precision highp float;
precision highp usampler2D;
precision mediump sampler2D;
uniform usampler2D InputBuffer;
uniform sampler2D GainMap;
uniform sampler2D Kodak;
uniform ivec2 RawSize;
uniform vec2 RawInvSize;
uniform vec4 blackLevel;
uniform vec3 whitePoint;
uniform int CfaPattern;
uniform uint whitelevel;
uniform int MinimalInd;
#define BLR (0.0)
#define BLG (0.0)
#define BLB (0.0)
#define QUAD 0
#define RGBLAYOUT 0
#define TESTPATTERN 0
#define OFFSET 0,0
#define USEGAIN 1
#import interpolation
#if RGBLAYOUT == 1
out vec3 Output;
#else
out float Output;
#endif


void main() {
    ivec2 xy_orig = ivec2(gl_FragCoord.xy) - ivec2(OFFSET);
    ivec2 shift = ivec2(CfaPattern % 2, CfaPattern / 2);

    ivec2 fact;
    ivec2 xy;
    #if QUAD == 1
        fact = (xy_orig / 2) % 2;
        xy = xy_orig + shift * 2;
    #else
        fact = xy_orig % 2;
        xy = xy_orig + shift;
    #endif

    float balance;
    #if USEGAIN == 1
    vec4 gains = texture(GainMap, vec2(xy)*vec2(RawInvSize));
    gains.rgb = vec3(gains.r,(gains.g+gains.b)/2.0,gains.a);
    gains.rgb /= dot(gains.rgb,vec3(1.0/3.0));
    #else
    vec3 gains = vec3(1.0);
    #endif

    vec3 level = vec3(blackLevel.r,(blackLevel.g+blackLevel.b)/2.0,blackLevel.a);
    #if RGBLAYOUT == 1
    Output = vec3(texelFetch(InputBuffer, (xy), 0).rgb)/(float(whitelevel));
    Output = gains.rgb*(Output-level.rgb)/(vec3(1.0)-level.rgb);
    #else
    if(fact.x+fact.y == 1){
            balance = whitePoint.g;
            Output = float(texelFetch(InputBuffer, (xy), 0).x)/float(whitelevel);
            Output = gains.g*(Output-level.g-BLG)/(1.0-level.g);
        } else {
            if(fact.x == 0){
                balance = whitePoint.r;
                Output = float(texelFetch(InputBuffer, (xy), 0).x)/float(whitelevel);
                Output = gains.r*(Output-level.r-BLR)/(1.0-level.r);
            } else {
                balance = whitePoint.b;
                Output = float(texelFetch(InputBuffer, (xy), 0).x)/float(whitelevel);
                Output = gains.b*(Output-level.b-BLB)/(1.0-level.b);
            }
        }
    Output = clamp(Output/balance,0.0,1.0);
    #endif
    #if TESTPATTERN == 1
        ivec2 ksize = textureSize(Kodak,0);
        vec3 col2 = texelFetch(Kodak, xy%ksize, 0).rgb;
        // Simplified test pattern logic for brevity
        Output = length(col2*col2)*balance;
    #endif
}

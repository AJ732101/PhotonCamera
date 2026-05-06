
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
    ivec2 xy = ivec2(gl_FragCoord.xy) - ivec2(OFFSET);
    ivec2 fact = (xy)%2;
    xy+=ivec2(CfaPattern%2,CfaPattern/2);
    #if QUAD == 1
        fact = (xy/2)%2;
        xy+=ivec2(CfaPattern%2,CfaPattern/2)*2;
    #endif
    float balance;
    #if USEGAIN == 1
    vec4 gains = texture(GainMap, vec2(xy)*vec2(RawInvSize));
    gains.rgb = vec3(gains.r,(gains.g+gains.b)/2.0,gains.a);
    gains.rgb /= dot(gains.rgb,vec3(1.0/3.0));
    #else
    vec3 gains = vec3(1.0);
    #endif
    //gains.rgb = vec3(1.f);
    vec3 level = vec3(blackLevel.r,(blackLevel.g+blackLevel.b)/2.0,blackLevel.a);
    #if RGBLAYOUT == 1
    //Output = vec3(texelFetch(InputBuffer, (xy+ivec2(0,0)), 0).rgb)/float(whitelevel);
    Output = vec3(texelFetch(InputBuffer, (xy), 0).rgb)/(float(whitelevel));
    Output = gains.rgb*(Output-level.rgb)/(vec3(1.0)-level.rgb);
    #else
    vec3 col = vec3(0.0);
    if(fact.x+fact.y == 1){
            col.g = 1.0;
            balance = whitePoint.g;
            Output = float(texelFetch(InputBuffer, (xy+ivec2(0,0)), 0).x)/float(whitelevel);
            Output = gains.g*(Output-level.g-BLG)/(1.0-level.g);
        } else {
            if(fact.x == 0){
                col.r = 1.0;
                balance = whitePoint.r;
                Output = float(texelFetch(InputBuffer, (xy), 0).x)/float(whitelevel);
                Output = gains.r*(Output-level.r-BLR)/(1.0-level.r);
            } else {
                col.b = 1.0;
                balance = whitePoint.b;
                Output = float(texelFetch(InputBuffer, (xy), 0).x)/float(whitelevel);
                Output = gains.b*(Output-level.b-BLB)/(1.0-level.b);
            }
        }
    Output = clamp(Output/balance,0.0,1.0);
    #endif
    #if TESTPATTERN == 1
        ivec2 diag = ivec2(xy.x+xy.y,xy.x-xy.y);
        //Output = balance*float((xy.x+xy.y)%64)/64.0;
        //checkerboard pattern
        //Output = balance*float((diag.x/31+diag.y/31)%2);
        //colored checkerboard pattern
        /*vec3 col2;
        float main = 0.1;
        float sec = 1.0;
        if (diag.x/31%2 == 0){
            if (diag.y/31%2 == 0){
                col2 = vec3(main,sec,sec);
            } else {
                col2 = vec3(sec,main,sec);
            }
        } else {
            if (diag.y/32%2 == 0){
                col2 = vec3(sec,sec,main);
            } else {
                col2 = vec3(main,main,sec);
            }
        }*/
        //Output *= length(col*col2);
        // round dots pattern
        //ivec2 center = (xy/31) * 31 + 16;
        //float rad = min(length(vec2(center-xy)),16.0);
        //Output = (col.r+col.b)*balance*float(int(rad) < 16)*0.1;
        //Output += balance*float(int(rad) < 10)*0.8;
        ivec2 ksize = textureSize(Kodak,0);
        vec3 col2 = texelFetch(Kodak, xy%ksize, 0).rgb;
        Output = length(col*col2*col2)*balance;
    #endif
}

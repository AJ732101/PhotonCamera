precision highp float;
precision highp usampler2D;
precision highp sampler2D;
uniform sampler2D InputBuffer;
uniform usampler2D InputBuffer2;
uniform float unlimitedWeight;
uniform vec4 blackLevel;
uniform int CfaPattern;
uniform vec4 WhiteBalance;
uniform float whiteLevel;
uniform int first;
uniform int yOffset;
out vec4 Output;

vec4 bayerToVec4(ivec2 coords){
    return vec4(
        clamp(float(texelFetch(InputBuffer2, (coords + ivec2(0,0)), 0).x)/whiteLevel,0.0,1.0),
        clamp(float(texelFetch(InputBuffer2, (coords + ivec2(1,0)), 0).x)/whiteLevel,0.0,1.0),
        clamp(float(texelFetch(InputBuffer2, (coords + ivec2(0,1)), 0).x)/whiteLevel,0.0,1.0),
        clamp(float(texelFetch(InputBuffer2, (coords + ivec2(1,1)), 0).x)/whiteLevel,0.0,1.0)
    );
}

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    xy+=ivec2(0,yOffset);
    vec4 inp = clamp(WhiteBalance*(bayerToVec4(xy*2)-blackLevel)/(vec4(1.0)-blackLevel),
                     vec4(0.0), vec4(1.0));
    if(first == 1){
        Output = inp;
    } else {
        Output = mix(texelFetch(InputBuffer, (xy), 0), inp, unlimitedWeight);
    }
}
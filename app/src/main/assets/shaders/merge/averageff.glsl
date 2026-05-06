precision highp float;
precision highp sampler2D;
uniform sampler2D InputBuffer;
uniform sampler2D InputBuffer2;
uniform float unlimitedWeight;
uniform int yOffset;
out vec4 Output;

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    xy+=ivec2(0,yOffset);
    Output = mix(
        texelFetch(InputBuffer2, (xy), 0),
        texelFetch(InputBuffer, (xy), 0), unlimitedWeight);
}
precision highp float;
precision highp sampler2D;
uniform sampler2D InputBuffer;
uniform sampler2D Watermark;
uniform sampler2D Noise;
uniform int yOffset;
uniform int rotate;
uniform bool mirror;
uniform ivec2 cropSize;
uniform ivec2 rawSize;
out vec4 Output;
#define WATERMARK 1
#define watersizek (15.0)
#define OFFSET 0,0
#import interpolation

// Triangular remapping: maps [0,1] blue noise to [-0.5, 0.5] with triangular distribution.
// This eliminates the DC bias of simple centered remapping and produces
// a smoother error distribution across adjacent quantization levels.
float triRemap(float v) {
    float orig = v * 2.0 - 1.0;
    return sign(orig) * (1.0 - sqrt(1.0 - abs(orig))) * 0.5;
}
vec3 triRemap(vec3 v) {
    return vec3(triRemap(v.r), triRemap(v.g), triRemap(v.b));
}

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    ivec2 texSize = ivec2(textureSize(InputBuffer, 0));
    vec2 texS = vec2(textureSize(InputBuffer, 0));
    vec2 watersize = vec2(textureSize(Watermark, 0));
    vec2 noiseSize = vec2(textureSize(Noise, 0));
    vec4 water;
    vec2 cr;
    xy+=ivec2(0,yOffset)+ivec2(OFFSET);
    switch(rotate){
        case 0:
        xy += ivec2(0,(rawSize.y-cropSize.y));
        cr = (vec2(xy+ivec2(0,-texSize.y))/(texS));
        if(mirror)
            xy.y = texSize.y - xy.y;
        Output = texelFetch(InputBuffer, xy, 0);
        break;
        case 1:

        xy += ivec2((rawSize.y-cropSize.y),0);
        cr = (vec2(xy+ivec2(-(rawSize.y-cropSize.y),-cropSize.x))/(texS));
        if(mirror)
            xy.x = cropSize.y - xy.x;
        Output = texelFetch(InputBuffer, ivec2(texSize.x-xy.y,xy.x), 0);
        break;
        case 2:
        //xy += ivec2(0,-(texSize.y-rotatedSize.y)/4);
        cr = (vec2(xy+ivec2(0,-cropSize.y))/(texS));
        if(mirror)
            xy.y = texSize.y - xy.y;
        Output = texelFetch(InputBuffer, ivec2(texSize.x-xy.x,texSize.y-xy.y), 0);
        break;
        case 3:
        //xy += ivec2(-(texSize.x-rotatedSize.x)/4,0);
        cr = (vec2(xy+ivec2(0,-texSize.x))/(texS));
        if(mirror)
            xy.x = cropSize.y - xy.x;
        Output = texelFetch(InputBuffer, ivec2(xy.y,texSize.y-xy.x),0);
        break;
    }
    #if WATERMARK == 1
    cr+=vec2(0.0,1.0/watersizek);
    cr.x*=(texS.x)/(texS.y);
    cr.x/=watersize.x/watersize.y;
    cr.x*=1.025;
    cr*=watersizek;
    if(cr.x >= 0.0 && cr.y >= 0.0){
    water = texture(Watermark,cr);
    Output = mix(Output,water,water.a);
    }
    #endif
    //Output*=1.005;

    // Blue noise dithering: tile noise over the output at 1:1 pixel ratio.
    // Triangular remap gives a zero-mean [-0.5, 0.5] distribution, scaled
    // to one 8-bit quantization step so dither is invisible but breaks banding.
    vec2 noiseUV = vec2(gl_FragCoord.xy) / noiseSize;
    vec3 noiseVal = texture(Noise, noiseUV).rgb;
    //Output.rgb += triRemap(noiseVal) * (2.0 / 255.0);
    Output = clamp(Output, 0.0, 1.0);
    Output.a = 1.0;

}

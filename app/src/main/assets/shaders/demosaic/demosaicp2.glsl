precision highp float;
precision mediump sampler2D;
uniform sampler2D RawBuffer;
uniform sampler2D GreenBuffer;
uniform int yOffset;
uniform vec4 neutral;

//#define greenmin (0.04)
#define greenmin (0.00)
//#define greenmax (0.9)
#define greenmax (1.00)
#import interpolation


// Helper function to determine Bayer pattern at a given position
// 0: R, 1: G (at red row), 2: G (at blue row), 3: B
int getBayerPattern(ivec2 pos) {
    int x = (pos.x) % 2;
    int y = (pos.y) % 2;
    return (y << 1) | x;
}

float getBayerSample(ivec2 pos) {
    return texelFetch(RawBuffer, pos, 0).r/neutral[getBayerPattern(pos)];
    //return float(texelFetch(RawBuffer, pos, 0).x);
}

out vec3 Output;
float interpolateColor(in ivec2 coords){
    bool usegreen = true;
    float green[5];
    green[0] = float(texelFetch(GreenBuffer, (coords+ivec2(-1,-1)), 0).x);
    green[1] = float(texelFetch(GreenBuffer, (coords+ivec2(1,-1)),  0).x);
    green[2] = float(texelFetch(GreenBuffer, (coords+ivec2(-1,1)),  0).x);
    green[3] = float(texelFetch(GreenBuffer, (coords+ivec2(1,1)),   0).x);
    green[4] = float(texelFetch(GreenBuffer, (coords),              0).x);
    //for(int i = 0; i<5; i++)if(green[i] < greenmin || green[i] > greenmax) usegreen = false;
    //if(usegreen){
        float coeff[4];
        coeff[0] = float(getBayerSample((coords+ivec2(-1,-1))))-(green[0]/neutral.g);
        coeff[1] = float(getBayerSample((coords+ivec2(1,-1))))-(green[1]/neutral.g);
        coeff[2] = float(getBayerSample((coords+ivec2(-1,1))))-(green[2]/neutral.g);
        coeff[3] = float(getBayerSample((coords+ivec2(1,1))))-(green[3]/neutral.g);
        return ((green[4]/neutral.g)+(coeff[0]+coeff[1]+coeff[2]+coeff[3])/4.);
    //} else {
    //    return ((float(texelFetch(RawBuffer, (coords+ivec2(-1,-1)), 0).x)+float(texelFetch(RawBuffer, (coords+ivec2(1,-1)), 0).x)
    //    +float(texelFetch(RawBuffer, (coords+ivec2(-1,1)), 0).x)+float(texelFetch(RawBuffer, (coords+ivec2(1,1)), 0).x))/(4.));
    //    }
}
float interpolateColorx(in ivec2 coords){
    bool usegreen = true;
    float green[3];
    green[0] = float(texelFetch(GreenBuffer, (coords+ivec2(-1,0)), 0).x);
    green[1] = float(texelFetch(GreenBuffer, (coords+ivec2(0,0)),  0).x);
    green[2] = float(texelFetch(GreenBuffer, (coords+ivec2(1,0)),  0).x);
    //for(int i = 0; i<3; i++)if(green[i] < greenmin || green[i] > greenmax) usegreen = false;
    //if(usegreen){
        float coeff[2];
        coeff[0] = float(getBayerSample((coords+ivec2(-1,0))))-(green[0]/neutral.g);
        coeff[1] = float(getBayerSample((coords+ivec2(1,0))))-(green[2]/neutral.g);
        return ((green[1]/neutral.g)+(coeff[0]+coeff[1])/2.);
    //} else {
    //    return ((float(texelFetch(RawBuffer, (coords+ivec2(-1,0)), 0).x)+float(texelFetch(RawBuffer, (coords+ivec2(1,0)), 0).x))/(2.));
    //}
}
float interpolateColory(in ivec2 coords){
    bool usegreen = true;
    float green[3];
    green[0] = float(texelFetch(GreenBuffer, (coords+ivec2(0,-1)), 0).x);
    green[1] = float(texelFetch(GreenBuffer, (coords+ivec2(0,0)),  0).x);
    green[2] = float(texelFetch(GreenBuffer, (coords+ivec2(0,1)),  0).x);
    //for(int i = 0; i<3; i++)if(green[i] < greenmin || green[i] > greenmax) usegreen = false;
    //if(usegreen){
        float coeff[2];
        coeff[0] = float(getBayerSample((coords+ivec2(0,-1))))-(green[0]/neutral.g);
        coeff[1] = float(getBayerSample((coords+ivec2(0,1))))-(green[2]/neutral.g);
        return ((green[1]/neutral.g)+(coeff[0]+coeff[1])/2.);
    //} else {
    //    return ((float(texelFetch(RawBuffer, (coords+ivec2(0,-1)), 0).x)+float(texelFetch(RawBuffer, (coords+ivec2(0,1)), 0).x))/(2.));
    //}
}
void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    int fact1 = xy.x%2;
    int fact2 = xy.y%2;
    xy+=ivec2(0,yOffset);
    if(fact1 ==0 && fact2 == 0) {//rggb
        Output.g = texelFetch(GreenBuffer, (xy), 0).x;
        Output.r = float(getBayerSample((xy)));
        Output.b = interpolateColor(xy);
    } else
    if(fact1 ==1 && fact2 == 0) {//grbg
        Output.g = texelFetch(GreenBuffer, (xy), 0).x;
        Output.r = interpolateColorx(xy);
        Output.b = interpolateColory(xy);

    } else
    if(fact1 ==0 && fact2 == 1) {//gbrg
        Output.g = texelFetch(GreenBuffer, (xy), 0).x;
        Output.b = interpolateColorx(xy);
        Output.r = interpolateColory(xy);

    } else  {//bggr
        Output.g = texelFetch(GreenBuffer, (xy), 0).x;
        Output.b = float(getBayerSample((xy)));
        Output.r = interpolateColor(xy);
    }
    Output = clamp(Output*neutral.rga,0.0,1.0);
}
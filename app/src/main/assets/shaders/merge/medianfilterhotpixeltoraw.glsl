
precision highp float;
precision highp sampler2D;
// Input texture
uniform sampler2D InputBuffer;
uniform int CfaPattern;
uniform float whiteLevel;
uniform vec4 blackLevel;
uniform vec4 WhiteBalance;
uniform int yOffset;
out uint Output;
#define TILE 2
#import median

uvec4 getBayerVec(ivec2 coords){
    vec4 inp = clamp(texelFetch(InputBuffer, coords, 0),0.0,1.0);
    return uvec4((WhiteBalance * inp * (vec4(1.0)-blackLevel) + blackLevel) * vec4(whiteLevel));
    //return uvec4(inp * vec4(whiteLevel));
}
uvec4 getBayerVec2(ivec2 coords, vec4 inp){
    return uvec4((WhiteBalance * inp * (vec4(1.0)-blackLevel) + blackLevel) * vec4(whiteLevel));
}

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    xy+=ivec2(0,yOffset);
    /*ivec2 fact = (xy-ivec2(CfaPattern%2,CfaPattern/2))%2;
    float v[9];
    // Add the pixels which make up our window to the pixel array.
    if(fact.x+fact.y == 1){
        v[4] = float(texelFetch(InputBuffer, xy + ivec2(0,0), 0).r);
        v[1] = float(texelFetch(InputBuffer, xy + ivec2(1,1), 0).r);
        v[2] = float(texelFetch(InputBuffer, xy + ivec2(-1,-1), 0).r);
        v[3] = float(texelFetch(InputBuffer, xy + ivec2(1,-1), 0).r);
        v[0] = float(texelFetch(InputBuffer, xy + ivec2(-1,1), 0).r);
        v[5] = float(texelFetch(InputBuffer, xy + ivec2(0,2), 0).r);
        v[6] = float(texelFetch(InputBuffer, xy + ivec2(2,0), 0).r);
        v[7] = float(texelFetch(InputBuffer, xy + ivec2(0,-2), 0).r);
        v[8] = float(texelFetch(InputBuffer, xy + ivec2(-2,0), 0).r);
    } else {
        for (int dX = -1; dX <= 1; ++dX) {
            for (int dY = -1; dY <= 1; ++dY) {
                ivec2 offset = ivec2((dX), (dY));
                v[(dX + 1) * 3 + (dY + 1)] = float(texelFetch(InputBuffer, xy + offset*2, 0).r);
            }
        }
    }
    float avr = (v[0]+v[1]+v[2]+v[3]+v[5]+v[6]+v[7]+v[8])/8.0;
    float inp = v[4];
    if(inp*0.7 > avr){
        // Starting with a subset of size 6, remove the min and max each time
        inp = median9(v);
    }*/
    //Output = uint(inp*float(whitelevel));
    //Output = uint(texelFetch(InputBuffer, xy + ivec2(0,0), 0).r*float(whitelevel));
    int x = xy.x%TILE;
    int y = xy.y%TILE;
    uvec4 bayer = getBayerVec((xy/TILE));
    Output = bayer[x + y*TILE];
    //Output = uint(texelFetch(InputBuffer, xy, 0).r*float(whitelevel));
}
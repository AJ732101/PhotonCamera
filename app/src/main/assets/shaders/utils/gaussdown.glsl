precision highp float;
precision highp sampler2D;
uniform sampler2D InputBuffer;
uniform ivec2 inputSize;
uniform float downscaleFactor;
out vec4 result;

// 5x5 Gaussian kernel (separable, but we do it in one pass for simplicity)
// Kernel approximates Gaussian with sigma ~= 1.0
const float kernel[25] = float[](
    1.0/256.0,  4.0/256.0,  6.0/256.0,  4.0/256.0, 1.0/256.0,
    4.0/256.0, 16.0/256.0, 24.0/256.0, 16.0/256.0, 4.0/256.0,
    6.0/256.0, 24.0/256.0, 36.0/256.0, 24.0/256.0, 6.0/256.0,
    4.0/256.0, 16.0/256.0, 24.0/256.0, 16.0/256.0, 4.0/256.0,
    1.0/256.0,  4.0/256.0,  6.0/256.0,  4.0/256.0, 1.0/256.0
);

void main() {
    // Map output pixel to input space
    vec2 centerPos = (vec2(gl_FragCoord.xy) + 0.5) * downscaleFactor;
    ivec2 centerPosInt = ivec2(centerPos);
    
    // Apply 5x5 Gaussian blur centered at the downsample position
    vec4 sum = vec4(0.0);
    int idx = 0;
    for (int dy = -2; dy <= 2; dy++) {
        for (int dx = -2; dx <= 2; dx++) {
            ivec2 samplePos = centerPosInt + ivec2(dx, dy);
            // Clamp to avoid sampling outside bounds
            samplePos = clamp(samplePos, ivec2(0), inputSize - ivec2(1));
            vec4 sampleVal = texelFetch(InputBuffer, samplePos, 0);
            sum += sampleVal * kernel[idx];
            idx++;
        }
    }
    
    result = sum;
}

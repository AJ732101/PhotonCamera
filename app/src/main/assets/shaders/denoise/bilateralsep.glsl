precision highp float;
precision highp sampler2D;

uniform sampler2D InputBuffer;
uniform sampler2D OriginalBuffer;
uniform int yOffset;
out vec4 Output;

// Default defines (will be overridden by Java code)
#define KSIZE 7
#define SPATIAL_SIGMA 3.535  // ~5.0/sqrt(2)
#define NOISES 0.001
#define NOISEO 0.0001
#define INTENSE 1.0
#define INTENSITY_MPY 1.0
#define INSIZE 1,1
#define DIRECTION 0  // 0 = vertical (Y), 1 = horizontal (X)
#define LAST_PASS 0  // 0 = first pass, 1 = last pass

/**
 * Separable Bilateral Filter - Single Shader for both directions
 * DIRECTION = 0: Vertical pass (Y direction)
 * DIRECTION = 1: Horizontal pass (X direction)
 * 
 * Uses adjusted sigma = originalSigma/sqrt(2) for proper separable approximation
 */

// Gaussian function for spatial weighting
float normpdf(in float x, in float sigma) {
    return 0.39894 * exp(-0.5 * x * x / (sigma * sigma)) / sigma;
}

// Gaussian function for intensity weighting (3D color space)
float normpdf3(in vec3 v, in float sigma) {
    return 0.39894 * exp(-0.5 * dot(v, v) / (sigma * sigma)) / sigma;
}

void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    xy += ivec2(0, yOffset);
    
    // Center pixel color
    vec3 centerColor = vec3(texelFetch(InputBuffer, xy, 0).rgb);
    
    // Calculate noise-based intensity sigma
    float noiseFactor = dot(centerColor, vec3(0.25));
    float intensitySigma = sqrt(noiseFactor * NOISES + NOISEO) * INTENSITY_MPY;
    intensitySigma = max(intensitySigma, 0.001); // Prevent division by zero
    
    // Accumulation variables
    vec3 filteredColor = vec3(0.0);
    float totalWeight = 0.0;
    
    // Determine offset direction based on DIRECTION
    // DIRECTION = 0: vertical (offset in Y), DIRECTION = 1: horizontal (offset in X)
    ivec2 offsetDir = (DIRECTION == 0) ? ivec2(0, 1) : ivec2(1, 0);
    ivec2 size = textureSize(InputBuffer, 0);
    // 1D filtering along the specified direction
    for (int i = -KSIZE; i <= KSIZE; ++i) {
        // Sample neighbor pixel along the direction
        ivec2 samplePos = xy + i * offsetDir;
        if(any(lessThan(samplePos, ivec2(0))) || any(greaterThanEqual(samplePos, size))) {
            continue; // Skip out-of-bounds samples
        }
        vec3 sampleColor = vec3(texelFetch(InputBuffer, samplePos, 0).rgb);
        
        // Spatial weight (Gaussian based on distance along the axis)
        float spatialWeight = normpdf(float(i), SPATIAL_SIGMA);
        
        // Intensity weight (Gaussian based on color difference)
        // This is what makes bilateral filter edge-preserving
        float intensityWeight = normpdf3(sampleColor - centerColor, intensitySigma);
        
        // Combined weight
        float weight = spatialWeight * intensityWeight;
        
        // Accumulate
        filteredColor += weight * sampleColor;
        totalWeight += weight;
    }
    
    // Normalize and output
    if (totalWeight < 0.0001) {
        // Fallback to center pixel if weights are too small
        Output = vec4(centerColor, 1.0);
    } else {
        vec3 result = filteredColor / totalWeight;
        
        // Optional: Preserve brightness ratio only on last pass
        #if LAST_PASS == 1
        vec3 centerSource = vec3(texelFetch(OriginalBuffer, xy, 0).rgb);
        float centerBrightness = dot(centerSource, vec3(0.25, 0.5, 0.25));
        float resultBrightness = dot(result, vec3(0.25, 0.5, 0.25));
        if (resultBrightness > 0.0001) {
            result = centerBrightness * (result / resultBrightness);
        }
        #endif
        
        Output = vec4(clamp(result, 0.0, 1.0), 1.0);
    }
}


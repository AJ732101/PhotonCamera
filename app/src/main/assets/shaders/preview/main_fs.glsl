#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

uniform samplerExternalOES sTexture;
uniform vec2 resolution;
uniform vec2 uCameraResolution;
uniform bool enablePeak;
uniform bool mirror;
uniform bool binning;

out vec4 Output;
in vec2 texCoord;

void main() {
    vec2 uv = texCoord.xy;
    if (mirror) {
        uv.y = 1.0 - uv.y;
    }

    vec4 color;

    if (binning) {
        // Quad Bayer "Binning" for YUV/RGB Input
        vec2 texelSize = 1.0 / uCameraResolution;
        vec2 pixelCoord = uv * uCameraResolution;
        vec2 blockStart = floor(pixelCoord / 2.0) * 2.0;

        vec3 sum = vec3(0.0);
        sum += texture(sTexture, (blockStart + vec2(0.5, 0.5)) * texelSize).rgb;
        sum += texture(sTexture, (blockStart + vec2(1.5, 0.5)) * texelSize).rgb;
        sum += texture(sTexture, (blockStart + vec2(0.5, 1.5)) * texelSize).rgb;
        sum += texture(sTexture, (blockStart + vec2(1.5, 1.5)) * texelSize).rgb;

        vec3 rgb = sum * 0.25;

        // Apply gamma for preview visibility
        rgb = pow(rgb, vec3(1.0/2.2));

        // Grayscale for testing path activation
        float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
        color = vec4(vec3(gray), 1.0);
    } else {
        color = texture(sTexture, uv);
    }

    // Edge highlighting (Focus Peaking)
    if (enablePeak) {
        vec2 size = resolution;
        vec4 avg = vec4(0.0);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                avg += texture(sTexture, uv + vec2(float(i) * 2.0, float(j) * 2.0) / size);
            }
        }
        avg /= 9.0;

        float diff = dot(abs(color - avg), vec4(0.299, 0.587, 0.114, 0.0));
        float denoiseK = 0.05;
        float w = (diff * diff) / (denoiseK + (diff * diff));
        vec4 dc = vec4(1.0, 0.0, 1.0, 0.0); // Purple peaking color
        color = color + dc * 32.0 * diff * w;
    }

    Output = color;
}

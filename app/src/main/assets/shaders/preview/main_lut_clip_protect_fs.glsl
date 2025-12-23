
#extension GL_OES_EGL_image_external_essl3 : require
precision highp float;

in vec2 texCoord;
uniform samplerExternalOES sTexture;
uniform sampler2D          PostLut;
uniform float              POSTLUTSIZE;
uniform float              POSTLUTSIZETILES;

out vec4 Output;

vec3 tonemap(vec3 color) {
    return color / (color + vec3(1.0));
}

vec3 partialTonemap1(vec3 color) {
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float threshold = 0.9;

    if (luminance <= threshold) {
        return color;
    } else {
        float remapped = (luminance - threshold) / (1.0 - threshold);
        float transition = pow(remapped, 0.7);
        vec3 highColor = mix(color, vec3(1.0), transition * 0.5);
        return min(highColor, vec3(1.0));
    }
}

vec3 partialTonemap2(vec3 color) {
    float threshold = 0.9;
    vec3 upper = (color - threshold) / (1.0 - threshold);
    return mix(color, 1.0 - exp(-(color - threshold) * 5.0) * (1.0 - threshold), step(threshold, color));
}

vec3 applyLut(in vec3 textureColor) {
    textureColor = clamp(textureColor, 0.0, 1.0);

    highp float blueColor = textureColor.b * (POSTLUTSIZE - 1.0);
    highp float slice0_index = floor(blueColor);
    highp float slice1_index = ceil(blueColor);

    highp vec2 slice0_pos;
    slice0_pos.y = floor(slice0_index / POSTLUTSIZETILES);
    slice0_pos.x = slice0_index - (slice0_pos.y * POSTLUTSIZETILES);

    highp vec2 slice1_pos;
    slice1_pos.y = floor(slice1_index / POSTLUTSIZETILES);
    slice1_pos.x = slice1_index - (slice1_pos.y * POSTLUTSIZETILES);

    highp float tileWidth = 1.0 / POSTLUTSIZETILES;
    highp vec2 rg_offset = textureColor.rg * tileWidth;

    highp vec2 slice0_start_uv = slice0_pos * tileWidth;
    highp vec2 slice1_start_uv = slice1_pos * tileWidth;

    highp vec2 texPos1 = slice0_start_uv + rg_offset;
    highp vec2 texPos2 = slice1_start_uv + rg_offset;

    highp vec3 newColor1 = texture(PostLut, texPos1).rgb;
    highp vec3 newColor2 = texture(PostLut, texPos2).rgb;

    return mix(newColor1, newColor2, fract(blueColor));
}

void main() {
    vec3 hdrColor = texture(sTexture, texCoord).rgb;
    vec3 tonemappedColor = tonemap(hdrColor);
    vec3 finalColor = applyLut(tonemappedColor);

    Output = vec4(finalColor, 1.0);
}

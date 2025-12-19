#extension GL_OES_EGL_image_external_essl3 : require
precision highp float;

// Input & Uniforms
in vec2 texCoord;
uniform samplerExternalOES sTexture;
uniform sampler2D          PostLut;
uniform float              POSTLUTSIZE;      // Sollte jetzt korrekt sein (z.B. 64.0)
uniform float              POSTLUTSIZETILES; // Sollte jetzt korrekt sein (z.B. 8.0)

// Output
out vec4 Output;

vec3 applyLut(in vec3 textureColor) {
    textureColor = clamp(textureColor, 0.0, 1.0);

    // 1. Skaliere den Blau-Wert, um die Kachel-Indizes zu finden
    highp float blueColor = textureColor.b * (POSTLUTSIZE - 1.0);
    highp float slice0_index = floor(blueColor);
    highp float slice1_index = ceil(blueColor);

    // 2. Berechne die 2D-Position (Spalte/Zeile) der Kacheln
    highp vec2 slice0_pos;
    slice0_pos.y = floor(slice0_index / POSTLUTSIZETILES);
    slice0_pos.x = slice0_index - (slice0_pos.y * POSTLUTSIZETILES);

    highp vec2 slice1_pos;
    slice1_pos.y = floor(slice1_index / POSTLUTSIZETILES);
    slice1_pos.x = slice1_index - (slice1_pos.y * POSTLUTSIZETILES);

    // 3. Berechne die normalisierten UV-Koordinaten
    highp float tileWidth = 1.0 / POSTLUTSIZETILES;
    highp vec2 rg_offset = textureColor.rg * tileWidth;

    highp vec2 slice0_start_uv = slice0_pos * tileWidth;
    highp vec2 slice1_start_uv = slice1_pos * tileWidth;

    highp vec2 texPos1 = slice0_start_uv + rg_offset;
    highp vec2 texPos2 = slice1_start_uv + rg_offset;

    // 4. Lese die Farben aus der LUT und interpoliere
    highp vec3 newColor1 = texture(PostLut, texPos1).rgb;
    highp vec3 newColor2 = texture(PostLut, texPos2).rgb;

    return mix(newColor1, newColor2, fract(blueColor));
}

void main() {
    vec3 originalColor = texture(sTexture, texCoord).rgb;
    vec3 processedColor = applyLut(originalColor);
    Output = vec4(processedColor, 1.0);
}

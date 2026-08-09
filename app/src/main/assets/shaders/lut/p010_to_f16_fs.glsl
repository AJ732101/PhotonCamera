#version 300 es
precision highp float;
precision highp usampler2D;

in vec2 v_texCoord;

uniform usampler2D y_texture;
uniform usampler2D u_texture;

out vec4 FragColor;

void main() {
    // 1. Read 16-bit integer values and shift to 10-bit range
    float y_10bit = float(texture(y_texture, v_texCoord).r >> 6);
    uvec2 uv_raw = texture(u_texture, v_texCoord).rg;
    float u_10bit = float(uv_raw.r >> 6);
    float v_10bit = float(uv_raw.g >> 6);

    // 2. Y'CbCr to Float normalization (Limited Range BT.2020)
    // Y: [64, 940] -> [0.0, 1.0]
    float y_prime = (y_10bit - 64.0) / 876.0;
    // Cb/Cr: [64, 960] -> [-0.5, 0.5]
    float cb_prime = (u_10bit - 512.0) / 896.0;
    float cr_prime = (v_10bit - 512.0) / 896.0;

    // 3. BT.2020 to RGB Conversion (using standardized coefficients)
    float r = y_prime + 1.4746 * cr_prime;
    float g = y_prime - 0.16455 * cb_prime - 0.57135 * cr_prime;
    float b = y_prime + 1.8814 * cb_prime;

    // 4. Output as FP16. We keep the values in 0-1 range for standard LUT compatibility.
    FragColor = vec4(clamp(r, 0.0, 1.0), clamp(g, 0.0, 1.0), clamp(b, 0.0, 1.0), 1.0);
}

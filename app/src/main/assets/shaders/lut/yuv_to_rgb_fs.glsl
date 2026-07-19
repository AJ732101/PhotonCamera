#version 300 es
precision mediump float;

in vec2 vTexCoord;

uniform sampler2D y_texture;
uniform sampler2D u_texture;
uniform sampler2D v_texture;

out vec4 outColor;

void main() {
    float y = texture(y_texture, vTexCoord).r;
    float u = texture(u_texture, vTexCoord).r - 0.5;
    float v = texture(v_texture, vTexCoord).r - 0.5;

    float r = y + 1.402 * v;
    float g = y - 0.344 * u - 0.714 * v;
    float b = y + 1.772 * u;

    outColor = vec4(r, g, b, 1.0);
}

#version 300 es
precision highp float;

layout(location = 0) in vec4 aPosition;
out vec2 v_texCoord;

void main() {
    gl_Position = aPosition;

    // Standard-Konvertierung: [-1, 1] -> [0, 1]
    v_texCoord = aPosition.xy * 0.5 + 0.5;
}
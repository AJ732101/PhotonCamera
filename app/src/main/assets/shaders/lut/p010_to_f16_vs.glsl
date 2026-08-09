#version 300 es
precision highp float;

layout(location = 0) in vec4 aPosition;
layout(location = 1) in vec2 aTexCoord;
out vec2 v_texCoord;
uniform mat4 uTexRotateMatrix;

void main() {
    gl_Position = aPosition;
    v_texCoord = (uTexRotateMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
}

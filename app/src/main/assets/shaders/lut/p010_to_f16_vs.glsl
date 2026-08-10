#version 300 es
precision mediump float;

in vec4 aPosition;
in vec2 aTexCoord;
out vec2 v_texCoord;
uniform mat4 uTexRotateMatrix;

void main() {
    gl_Position = aPosition;
    v_texCoord = (uTexRotateMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
}

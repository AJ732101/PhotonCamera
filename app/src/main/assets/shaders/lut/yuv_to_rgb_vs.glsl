#version 300 es
in vec4 aPosition;
in vec2 aTexCoord;
out vec2 vTexCoord;
uniform mat4 uTexRotateMatrix;

void main() {
    gl_Position = aPosition;
    vTexCoord = (uTexRotateMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
}

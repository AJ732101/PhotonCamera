in vec2 vPosition;
in vec2 vTexCoord;
out vec2 v_texCoord;
uniform mat4 uTexRotateMatrix;

void main() {
    v_texCoord.yx = vTexCoord.xy;
    v_texCoord.x = 1.0 - v_texCoord.x;
    gl_Position = uTexRotateMatrix * vec4 ( vPosition.x, vPosition.y, 0.0, 1.0 );
}

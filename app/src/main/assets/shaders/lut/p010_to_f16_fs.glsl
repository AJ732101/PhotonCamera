#version 300 es
precision highp float;
precision highp usampler2D;

in vec2 v_texCoord;

// Y-Plane: Texture Unit 0
uniform usampler2D y_texture;
// UV-Plane: Texture Unit 1 (In Java mP010_Utex genannt)
uniform usampler2D u_texture;

out vec4 FragColor;

void main() {
    // 1. Lese die 16-Bit Integer-Werte
    // P010 speichert 10 Bit in den obersten 10 Bits von 16 Bit (Left-Shifted)
    // Shift um 6 bringt den Wert in den Bereich 0-1023
    float y_10bit = float(texture(y_texture, v_texCoord).r >> 6);

    // Die UV-Textur ist ein RG-Format. .r ist U (Cb), .g ist V (Cr)
    uvec2 uv_raw = texture(u_texture, v_texCoord).rg;
    float u_10bit = float(uv_raw.r >> 6);
    float v_10bit = float(uv_raw.g >> 6);

    // 2. Y'CbCr zu Float Normalisierung (Limited Range Rec. 709 / BT.2020)
    // Y: [64, 940] -> [0.0, 1.0]
    float y_prime = (y_10bit - 64.0) / 876.0;

    // Cb/Cr: [64, 960] mit Center 512 -> [-0.5, 0.5]
    float cb_prime = (u_10bit - 512.0) / 896.0;
    float cr_prime = (v_10bit - 512.0) / 896.0;

    // 3. Y'CbCr zu RGB Konvertierung (Rec. 709 Matrix)
    // Falls deine Quelle BT.2020 ist, müssten die Koeffizienten angepasst werden
    float r = y_prime + 1.402 * cr_prime;
    float g = y_prime - 0.344136 * cb_prime - 0.714136 * cr_prime;
    float b = y_prime + 1.772 * cb_prime;

    // 4. Output als F16 (RGBA_F16)
    // Clamping ist wichtig, falls die Matrix-Rechnung Werte außerhalb [0,1] erzeugt
    FragColor = vec4(clamp(r, 0.0, 1.0), clamp(g, 0.0, 1.0), clamp(b, 0.0, 1.0), 1.0);
}
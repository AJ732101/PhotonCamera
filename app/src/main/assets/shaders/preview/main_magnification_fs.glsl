#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;

// Uniforms (Inputs from your Java/Kotlin code)
uniform samplerExternalOES sTexture;
uniform vec2 resolution; // Screen resolution, e.g., 1920x1080
uniform bool enablePeak;

// Outputs/Inputs between shaders
out vec4 Output;
in vec2 texCoord; // UV coordinates (0.0 to 1.0) from the Vertex Shader

void main() {
    // --- 1. Define center and size of the zoom window ---
    vec2 zoom_center_uv = vec2(0.5, 0.5); // The center of the texture (and screen)
    vec2 zoom_window_px = vec2(200.0, 400.0); // Size of the window in pixels
    vec2 zoom_window_uv = zoom_window_px / resolution.xy; // Convert size to UV coordinates
    float zoom_factor = 4.0;

    vec2 uv = texCoord.xy; // Current UV coordinate for this pixel

    // --- 2. Check if the pixel is inside the zoom window ---
    vec2 zoom_window_min = zoom_center_uv - zoom_window_uv * 0.5;
    vec2 zoom_window_max = zoom_center_uv + zoom_window_uv * 0.5;

    float is_inside = step(zoom_window_min.x, uv.x) * step(uv.x, zoom_window_max.x) *
    step(zoom_window_min.y, uv.y) * step(uv.y, zoom_window_max.y);

    // --- 3. Calculate new UV coordinates for the zoom (only if inside) ---
    if (is_inside > 0.5) {
        // --- FINAL CORRECTED ZOOM LOGIC ---
        // 1. Get the coordinate of the current pixel relative to the window's center.
        // The result is in a range like [-window_size/2, +window_size/2]
        vec2 centered_uv = uv - zoom_center_uv;

        // 2. Scale this coordinate down by the zoom factor. This is the core of the zoom.
        // A pixel at the edge of the window now points to a location much closer to the center.
        centered_uv /= zoom_factor;

        // 3. Add the coordinate back to the texture center to get the final lookup coordinate.
        uv = zoom_center_uv + centered_uv;
    }

    // --- 4. Fetch color and apply Focus Peaking (unchanged code) ---
    vec4 color = texture(sTexture, uv);

    // Focus Peaking (optional, can be kept)
    vec2 size = resolution;
    vec4 avg = vec4(0.0);
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            avg += texture(sTexture, uv + vec2(i*2, j*2) / size);
        }
    }
    avg /= 9.0;
    float diff = dot(abs(color - avg), vec4(0.299, 0.587, 0.114, 0.0));
    float denoiseK = 0.05;
    float w = (diff * diff) / (denoiseK + (diff * diff));
    vec4 dc = vec4(1.0, 0.0, 1.0, 0.0); // Magenta color for peaking
    if (enablePeak)
    color = color + dc * 32.0 * diff * w;

    // --- 5. Output the final color ---
    Output = color;
}

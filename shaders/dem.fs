#version 330

in float fragHeight;
in vec2 fragXZ;
in vec4 fragColor;

uniform vec4 colDiffuse;    // the ground fill
uniform int mode;           // 0 the shade alone, 1 elevation bands, 2 bands and contour lines
uniform float yMin;         // scene y of the lowest drawn ground
uniform float bandM;        // meters per band, ten bands across the drawn ground
uniform float cellM;        // meters across one DEM cell, the hatch pitch
uniform vec3 bands[10];     // the band colors, low to high
uniform vec3 warn;          // the hatch over filled no-data ground

out vec4 finalColor;

// one pixel of line where s is a whole number, faded out once the lines would merge
float line(float s, float pitch) {
    float dm = abs(fract(s + 0.5) - 0.5) * pitch;
    float wm = fwidth(s) * pitch * 1.2;
    if (wm <= 1e-6) return 0.0;
    return (1.0 - smoothstep(0.0, wm, dm)) * (1.0 - smoothstep(0.25, 0.5, wm / pitch));
}

void main() {
    vec3 col = colDiffuse.rgb;
    if (fragColor.a < 0.5) {
        // diagonal caution lines, one per cell, across filled ground
        col = mix(col, warn, line((fragXZ.x - fragXZ.y) / cellM, cellM));
    } else if (mode > 0) {
        float b = (fragHeight - yMin) / bandM;
        col = bands[int(clamp(floor(b), 0.0, 9.0))];
        if (mode == 2) col = mix(col, col * 0.35, line(b, bandM));
    }
    finalColor = vec4(col * fragColor.r, 1.0);
}

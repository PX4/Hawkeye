#version 330

// Speed-ribbon heat gradient. fragTexCoord.x = heat, evaluated against the
// theme's 7-stop thermal ramp; fragColor.a = age fade.

in vec2 fragTexCoord;
in vec4 fragColor;

uniform vec3 thermal[7];
uniform sampler2D texture0;
uniform vec4 colDiffuse;

out vec4 finalColor;

void main()
{
    float heat = clamp(fragTexCoord.x, 0.0, 1.0);
    float scaled = heat * 6.0;
    int idx = int(min(scaled, 5.0));
    float frac = scaled - float(idx);
    vec3 c = mix(thermal[idx], thermal[idx + 1], frac);
    finalColor = vec4(c, fragColor.a) * colDiffuse;
}

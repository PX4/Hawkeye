#version 330

in vec3 vertexPosition;
in vec4 vertexColor;

uniform mat4 mvp;
uniform mat4 matModel;

out float fragHeight;   // scene y, meters above the origin
out vec2 fragXZ;        // scene x east and z south, meters
out vec4 fragColor;     // rgb the shade, alpha zero on filled no-data ground

void main() {
    vec4 world = matModel * vec4(vertexPosition, 1.0);
    fragHeight = world.y;
    fragXZ = world.xz;
    fragColor = vertexColor;
    gl_Position = mvp * vec4(vertexPosition, 1.0);
}

#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec3 ray;
out vec4 vertexColor;
out vec2 clock;
out vec4 dials;
out float extra;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    ray = Position;
    vertexColor = Color;
    clock = UV0 + vec2(UV1) * 0.0;
    dials = vec4(float(UV2.x), float(UV2.y), Normal.x * 127.0, Normal.y * 127.0);
    extra = Normal.z * 127.0;
}

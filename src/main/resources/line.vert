#version 430 core

layout (location=0) in vec2 v_pos;
layout (location=1) in vec2 v_start;
layout (location=2) in vec2 v_end;
layout (location=3) in float v_color;
layout (location=4) in float v_thickness;

uniform mat4 projection;
uniform mat4 view;

out vec2 f_pos;
out vec2 f_start;
out vec2 f_end;
out vec4 f_color;
out float f_thickness;

void main() {
    int color = floatBitsToInt(v_color);
    float a = float(color & 0xFF) / 255.0;
    float b = float((color >> 8) & 0xFF) / 255.0;
    float g = float((color >> 16) & 0xFF) / 255.0;
    float r = float((color >> 24) & 0xFF) / 255.0;
    f_pos = v_pos;
    f_start = v_start;
    f_end = v_end;
    f_color = vec4(r, g, b, a);
    f_thickness = v_thickness;
    gl_Position = projection * view * vec4(v_pos, 0.0, 1.0);
}
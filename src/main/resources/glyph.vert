#version 430 core

layout (location=0) in vec2 v_pos;
layout (location=1) in vec2 v_bl;
layout (location=2) in float v_color;
layout (location=3) in float v_scale;
layout (location=4) in float v_code;

uniform mat4 projection;
uniform mat4 view;

out vec2 f_pos;
out vec2 f_bl;
out vec4 f_color;
out float f_scale;
flat out int f_code;

void main() {
    int color = floatBitsToInt(v_color);
    float a = float(color & 0xFF) / 255.0;
    float b = float((color >> 8) & 0xFF) / 255.0;
    float g = float((color >> 16) & 0xFF) / 255.0;
    float r = float((color >> 24) & 0xFF) / 255.0;
    f_pos = v_pos;
    f_bl = v_bl;
    f_color = vec4(r, g, b, a);
    f_scale = v_scale;
    f_code = floatBitsToInt(v_code);
    gl_Position = projection * view * vec4(v_pos, 0.0, 1.0);
}
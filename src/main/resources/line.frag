#version 430 core

in vec2 f_pos;
in vec2 f_start;
in vec2 f_end;
in vec4 f_color;
in float f_thickness;

out vec4 color;

float dist(vec2 start, vec2 end, vec2 pos) {
    vec2 line = end - start;
    float len = length(line);
    vec2 v = pos - start;
    line = normalize(line);
    float d = dot(v, line);
    d = clamp(d, 0, len);
    vec2 line_pos = start + line * d;
    return length(f_pos.xy - line_pos);
}

void main() {
    if (dist(f_start, f_end, f_pos.xy) >= f_thickness) discard;
    else color = f_color;
//    color = vec4(0.0, 1.0, 0.0, 1.0);
}
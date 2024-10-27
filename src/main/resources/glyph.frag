#version 430 core
#extension GL_NV_gpu_shader5 : enable

layout(std430, binding = 0) buffer ssbo {
    int contours[1024 * 64];
    vec2 points[1024 * 4096];
};

in vec2 f_pos;
in vec2 f_bl;
in vec4 f_color;
in float f_scale;
flat in int f_code;

out vec4 color;

float isLeft(
    float x0, float y0,
    float x1, float y1,
    float x2, float y2
) {
    return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
}

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
    int wn = 0;
    int contourCount = contours[f_code * 64];
    int contourStartIndex = 0;
    float minDistance = 1000.0;
    for (int contourIndex = 0; contourIndex < contourCount; contourIndex++) {
        int contourEndIndex = contours[f_code * 64 + contourIndex + 1];
        for (int fromIndex = contourStartIndex; fromIndex <= contourEndIndex; fromIndex++) {
            int toIndex = fromIndex + 1;
            if (toIndex > contourEndIndex) toIndex = contourStartIndex;
            vec2 a = f_bl + points[f_code * 4096 + fromIndex] * f_scale;
            vec2 b = f_bl + points[f_code * 4096 + toIndex] * f_scale;
            float dist = dist(a, b, f_pos);
            if (dist < minDistance) minDistance = dist;
            if (a.y <= f_pos.y) {
                if (b.y > f_pos.y && isLeft(a.x, a.y, b.x, b.y, f_pos.x, f_pos.y) > 0) wn++;
            } else if (b.y <= f_pos.y && isLeft(a.x, a.y, b.x, b.y, f_pos.x, f_pos.y) < 0) wn--;
        }
        contourStartIndex = contourEndIndex + 1;
    }
    if (wn != 0) {
        color = f_color * (min(minDistance, 0.025) * 40.0);
    } else {
        discard;
    }
}
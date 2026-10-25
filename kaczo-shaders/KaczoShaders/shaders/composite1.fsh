#version 330 compatibility
#include "/lib/settings.glsl"
/* const int colortex1Format = RGBA16F; */
uniform sampler2D colortex0;
uniform float viewWidth, viewHeight;
in vec2 texcoord;
/* RENDERTARGETS: 1 */
layout(location = 0) out vec4 outColor;

vec3 bright(vec3 c) {
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    float k = smoothstep(0.72, 1.05, l);
    return c * k * 1.4;
}
void main() {
    vec2 px = vec2(1.0 / viewWidth, 0.0);
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int i = -8; i <= 8; i++) {
        float w = exp(-float(i * i) / 24.0);
        sum += bright(texture(colortex0, texcoord + px * float(i) * 2.0).rgb) * w;
        wsum += w;
    }
    outColor = vec4(sum / wsum, 1.0);
}

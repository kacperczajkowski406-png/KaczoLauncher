#version 330 compatibility
#include "/lib/settings.glsl"
const int colortex1Format = RGBA16F;
const int colortex3Format = RGBA16F;
uniform sampler2D colortex2;
uniform float viewWidth, viewHeight;
in vec2 texcoord;
/* RENDERTARGETS: 1,3 */
layout(location = 0) out vec4 outWide;
layout(location = 1) out vec4 outStreak;
void main() {
    vec2 px = vec2(1.0 / viewWidth, 0.0);
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int i = -8; i <= 8; i++) {
        float w = exp(-float(i * i) / 30.0);
        sum += texture(colortex2, texcoord + px * float(i) * 7.0).rgb * w;
        wsum += w;
    }
    outWide = vec4(sum / wsum, 1.0);
    vec3 st = vec3(0.0);
    float sw = 0.0;
#ifdef STREAK
    for (int i = -16; i <= 16; i++) {
        float w = exp(-abs(float(i)) / 7.0);
        st += texture(colortex2, texcoord + px * float(i) * 14.0).rgb * w;
        sw += w;
    }
    st /= sw;
#endif
    outStreak = vec4(st, 1.0);
}

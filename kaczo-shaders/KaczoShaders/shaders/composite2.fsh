#version 330 compatibility
#include "/lib/settings.glsl"
const int colortex2Format = RGBA16F;
uniform sampler2D colortex1;
uniform float viewWidth, viewHeight;
in vec2 texcoord;
/* RENDERTARGETS: 2 */
layout(location = 0) out vec4 outColor;
void main() {
    vec2 px = vec2(0.0, 1.0 / viewHeight);
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int i = -8; i <= 8; i++) {
        float w = exp(-float(i * i) / 24.0);
        sum += texture(colortex1, texcoord + px * float(i) * 2.0).rgb * w;
        wsum += w;
    }
    outColor = vec4(sum / wsum, 1.0);
}

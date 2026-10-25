#version 330 compatibility
#include "/lib/settings.glsl"
const int colortex2Format = RGBA16F;
uniform sampler2D colortex1;
uniform sampler2D colortex2;
uniform float viewWidth, viewHeight;
in vec2 texcoord;
/* RENDERTARGETS: 2 */
layout(location = 0) out vec4 outColor;
void main() {
    vec2 px = vec2(0.0, 1.0 / viewHeight);
    vec3 sum = vec3(0.0);
    float wsum = 0.0;
    for (int i = -8; i <= 8; i++) {
        float w = exp(-float(i * i) / 30.0);
        sum += texture(colortex1, texcoord + px * float(i) * 7.0).rgb * w;
        wsum += w;
    }
    vec3 wide = sum / wsum;
    vec3 near = texture(colortex2, texcoord).rgb;           // ciasny blur z composite2
    outColor = vec4(wide * 0.75 + near * 0.55, 1.0);
}

#version 330 compatibility
#include "/lib/settings.glsl"
#include "/lib/sky.glsl"
uniform sampler2D gtexture;
uniform mat4 gbufferModelViewInverse;
uniform vec3 sunPosition;
uniform float rainStrength, frameTimeCounter;
in vec2 texcoord;
in vec4 glcolor;
in vec3 normalW;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
    vec4 tex = texture(gtexture, texcoord);
    if (tex.a < 0.05) discard;
    vec3 sunDir = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    float day = smoothstep(-0.12, 0.30, sunDir.y);
    float twi = exp(-pow(sunDir.y / 0.20, 2.0));
    float top = normalW.y * 0.5 + 0.5;
    vec3 c = mix(vec3(0.10, 0.12, 0.20), vec3(1.0), day);
    c = mix(c, vec3(1.0, 0.66, 0.46), twi * 0.5);
    c *= mix(0.62, 1.0, top);
    c = mix(c, vec3(dot(c, vec3(0.333)) * 0.7), rainStrength * 0.7);
    outColor = vec4(c, tex.a * glcolor.a * 0.9);
}

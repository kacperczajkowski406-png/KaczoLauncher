#version 330 compatibility
#include "/lib/settings.glsl"
#include "/lib/sky.glsl"
uniform mat4 gbufferModelViewInverse, gbufferProjectionInverse;
uniform vec3 sunPosition;
uniform float viewWidth, viewHeight, rainStrength, frameTimeCounter;
in float isStar;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
    vec3 sunDir = normalize(mat3(gbufferModelViewInverse) * sunPosition);
    float night = 1.0 - smoothstep(-0.18, 0.04, sunDir.y);
    if (isStar > 0.5) {
        outColor = vec4(vec3(1.0, 0.97, 0.92) * night * 1.4 * (1.0 - rainStrength), 1.0);
        return;
    }
    vec4 p = gbufferProjectionInverse * vec4(gl_FragCoord.xy / vec2(viewWidth, viewHeight) * 2.0 - 1.0, 1.0, 1.0);
    vec3 dir = normalize(mat3(gbufferModelViewInverse) * normalize(p.xyz / p.w));
    outColor = vec4(skyColor(dir, sunDir, rainStrength, frameTimeCounter, true), 1.0);
}

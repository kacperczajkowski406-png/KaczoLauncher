#version 330 compatibility
uniform int renderStage;
out float isStar;
void main() {
    gl_Position = ftransform();
    isStar = renderStage == MC_RENDER_STAGE_STARS ? 1.0 : 0.0;
}

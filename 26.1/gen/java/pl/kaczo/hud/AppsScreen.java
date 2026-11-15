package pl.kaczo.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** „Aplikacje 3D” – pływający ekran startowy w stylu gogli VR: kafelki aplikacji z paralaksą i obrotem za kursorem. */
public class AppsScreen extends KScreen {
    private record App(String id, String name, String icon) { }
    private static final App[] APPS = {
            new App("mods", "Mods", "keystrokes"), new App("music", "Music", "music"), new App("wall", "Wallpapers", "theme"),
            new App("edit", "Edit HUD", "helditem"), new App("shaders", "Shaders", "shaders"), new App("cool", "Cooldowns", "cooldowns")};

    private final Screen parent;
    private final long opened = System.currentTimeMillis();
    private float S = 1f; private int vw, vh;
    private int hover = -1;

    public AppsScreen(Screen parent) { super(Component.literal("Apps")); this.parent = parent; }

    @Override public void onClose() { minecraft.setScreen(parent); }

    private void layout() { S = (float) Math.min(1.0, 1.5 / Ui.baseSf()); vw = Math.round(width / S); vh = Math.round(height / S); }

    private int tileW() { return Math.min(64, (vw - 40) / APPS.length - 8); }
    private int tileX(int i) { int tw = tileW(), total = APPS.length * (tw + 12) - 12; return (vw - total) / 2 + i * (tw + 12); }
    private int tileY() { return vh / 2 - tileW() / 2 - 6; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        Ui.beginFrame();
        layout();
        float a = Ui.easeOut((System.currentTimeMillis() - opened) / 320f);
        ctx.fill(0, 0, width, height, Ui.fade(0x77000000, a));
        Font tr = minecraft.font;
        mx = Math.round(mx / S); my = Math.round(my / S);
        Compat.push(ctx); Compat.scale(ctx, S, S); Ui.extra = S;
        try {
            Tilt3D.begin(ctx, mx, my, vw / 2.0, vh / 2.0, vw, vh);
            try {
                // pigułka u góry: godzina + utwór
                String top = Modules.pillText();
                int pw = tr.width(top) + 24, px = (vw - pw) / 2, py = Math.round(vh * 0.14f + Tilt3D.parallaxY(0.4f) - (1 - a) * 12);
                Ui.card(ctx, px + Math.round(Tilt3D.parallaxX(0.4f)), py, pw, 18, 9, Ui.fade(0x66101418, a), Ui.fade(0xAAFFFFFF, a));
                ctx.text(tr, top, px + 12 + Math.round(Tilt3D.parallaxX(0.4f)), py + 5, Ui.fade(0xFFFFFFFF, a), false);
                // szklana półka pod kafelkami
                int tw = tileW(), ty = tileY();
                int shelfX = tileX(0) - 16, shelfW = tileX(APPS.length - 1) + tw - tileX(0) + 32;
                Ui.glass(ctx, shelfX, ty - 14, shelfW, tw + 46, 18, Ui.fade(0x66000000, a));
                hover = -1;
                for (int i = 0; i < APPS.length; i++) {
                    float d = 0.6f + (i % 2) * 0.35f;                               // różna „głębia” – kafelki rozjeżdżają się przy ruchu
                    int x = tileX(i) + Math.round(Tilt3D.parallaxX(d)), y = ty + Math.round(Tilt3D.parallaxY(d) - (1 - a) * 18);
                    boolean h = Ui.in(mx, my, x, y, tw, tw + 16);
                    if (h) hover = i;
                    float hv = Ui.tween("app" + i, h ? 1f : 0f, 14f);
                    float sc = 1f + 0.12f * hv;
                    Compat.push(ctx);
                    Compat.translate(ctx, x + tw / 2f, y + tw / 2f);
                    Compat.scale(ctx, sc, sc);
                    Compat.translate(ctx, -(x + tw / 2f), -(y + tw / 2f));
                    Ui.card(ctx, x, y, tw, tw, tw / 3, Ui.fade(Ui.mix(0x30FFFFFF, 0x55FFFFFF, hv), a), Ui.fade(Ui.mix(0x55FFFFFF, 0xDDFFFFFF, hv), a));
                    if (hv < 0.02f || true) ItemIcons.draw(ctx, APPS[i].icon(), x + tw / 2.0, y + tw / 2.0, tw * 0.5f);
                    Compat.pop(ctx);
                    String n = tr.plainSubstrByWidth(L.t(APPS[i].name()), tw + 8);
                    ctx.text(tr, n, x + (tw - tr.width(n)) / 2, y + tw + 6, Ui.fade(h ? 0xFFFFFFFF : 0xFFC8CCD6, a), false);
                }
            } finally { Tilt3D.end(ctx); }
        } finally { Ui.extra = 1f; Compat.pop(ctx); }
    }

    @Override
    protected boolean onClick(double mxd, double myd, int button) {
        layout();
        int mx = (int) (mxd / S), my = (int) (myd / S);
        if (hover < 0) { onClose(); return true; }
        switch (APPS[hover].id()) {
            case "mods" -> minecraft.setScreen(new ModMenuScreen());
            case "music" -> minecraft.setScreen(new LyricsScreen(this));
            case "wall" -> minecraft.setScreen(new WallpaperScreen(this));
            case "edit" -> minecraft.setScreen(new EditScreen(this, null));
            case "shaders" -> { ModMenuScreen m = new ModMenuScreen(); m.sel = Modules.byId("shaders"); minecraft.setScreen(m); }
            default -> { ModMenuScreen m = new ModMenuScreen(); m.sel = Modules.byId("cooldowns"); minecraft.setScreen(m); }
        }
        return true;
    }
}

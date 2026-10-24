package pl.kaczo.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Karta „Teraz gra”: okładka, tytuł, artysta i JEDNA linijka tekstu (aktualna).
 * Gdy tytuł albo linijka są za długie, karta płynnie poszerza się w bok (animacja), a gdy się skrócą – zwęża.
 */
public class NowPlayingCard {
    private static final int PAD = 4, COVER = 40;
    private static float curW = 0;
    private static long lastNs = 0;

    private static boolean shown(Module m) { return !(NowPlaying.title.isEmpty() && m.bool("hide")); }

    private static int target(Minecraft mc, Module m) {
        int tx = PAD + (m.bool("cover") ? COVER + PAD : 0);
        int need = mc.font.width(NowPlaying.title.isEmpty() ? L.t("Nothing playing") : NowPlaying.title);
        need = Math.max(need, mc.font.width(NowPlaying.artist));
        if (m.bool("lyrics")) need = Math.max(need, mc.font.width(Lyrics.current()));
        int max = mc.getWindow().getGuiScaledWidth() - 8;
        return Math.min(max, Math.max(m.num("width"), tx + need + PAD + 2));
    }

    private static int height(Module m) { return Math.max(m.bool("cover") ? COVER : 0, 32) + 2 * PAD + 4; }

    public static int[] size(Minecraft mc) {
        Module m = Modules.MUSIC;
        if (!shown(m)) return new int[]{1, 1};
        int w = curW < 1 ? target(mc, m) : Math.round(curW);
        return new int[]{w, height(m)};
    }

    public static void draw(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y) {
        Module m = Modules.MUSIC;
        if (!shown(m)) { curW = 0; return; }

        // animacja szerokości (niezależna od FPS)
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        int tgt = target(mc, m);
        if (curW < 1) curW = tgt;
        curW += (tgt - curW) * Math.min(1f, dt * 16f);
        int w = Math.round(curW), h = height(m);

        Ui.box(ctx, x, y, w, h, m.bg());
        int col = m.color("color"), cy = y + PAD, tx = x + PAD;
        if (m.bool("cover")) {
            Identifier cover = NowPlaying.cover();
            if (cover != null) {
                Compat.push(ctx);
                Compat.translate(ctx, x + PAD, cy);
                float s = COVER / (float) NowPlaying.COVER;
                Compat.scale(ctx, s, s);
                Compat.tex(ctx, cover, NowPlaying.COVER, NowPlaying.COVER);
                Compat.pop(ctx);
            } else {
                ctx.fill(x + PAD, cy, x + PAD + COVER, cy + COVER, 0xFF222233);
                ctx.text(mc.font, "♪", x + PAD + COVER / 2 - 3, cy + COVER / 2 - 4, 0xFF59627A, false);
            }
            tx += COVER + PAD;
        }
        String title = NowPlaying.title.isEmpty() ? L.t("Nothing playing") : NowPlaying.title;
        ctx.enableScissor(x, y, x + w, y + h);          // podczas animacji nic nie wystaje poza kartę
        ctx.text(mc.font, title, tx, cy, col, true);
        if (!NowPlaying.artist.isEmpty()) ctx.text(mc.font, NowPlaying.artist, tx, cy + 11, 0xFFAAAAAA, true);
        if (m.bool("lyrics")) {
            String l = Lyrics.current();
            if (!l.isEmpty()) ctx.text(mc.font, l, tx, cy + 24, 0xFFFFFFFF, true);
        }
        ctx.disableScissor();

        if (NowPlaying.durationSec > 0) {
            int by = y + h - 4, bw = w - 2 * PAD;
            ctx.fill(x + PAD, by, x + PAD + bw, by + 2, 0x60FFFFFF);
            double f = Math.max(0, Math.min(1, NowPlaying.position() / NowPlaying.durationSec));
            ctx.fill(x + PAD, by, x + PAD + (int) (bw * f), by + 2, col);
        }
    }
}

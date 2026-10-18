package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Własna tablica wyników (sidebar) w stylu szkła: tło ma regulowaną przezroczystość, napisy zawsze są wyraźne,
 * a położenie ustawia się w edytorze HUD. Oryginalną tablicę wyłącza mixin – ale tylko wtedy, gdy ten mixin
 * rzeczywiście działa (inaczej rysowałyby się dwie).
 */
public final class Sb {
    private Sb() { }

    /** Ostatnia chwila, w której mixin zgłosił, że oryginalna tablica została wyłączona. */
    public static volatile long hookSeen = 0;
    public static volatile int curW = 110, curH = 80;
    private static int fails = 0;

    public static boolean hooked() { return System.currentTimeMillis() - hookSeen < 2500 && fails < 5; }

    private record Row(Text name, Text score) { }

    public static int[] rect(int screenW, int screenH) {
        float sc = Modules.SCOREBOARD.num("scale") / 100f;
        int w = Math.round(curW * sc), h = Math.round(curH * sc);
        return new int[]{screenW - w - 4 + Hud.sbDx(), screenH / 2 - h / 2 + Hud.sbDy(), w, h};
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        Module m = Modules.SCOREBOARD;
        if (m == null || !m.enabled() || !hooked() || mc.world == null || mc.player == null) return;
        try {
            draw(ctx, mc, m);
        } catch (Throwable t) {
            fails++;
        }
    }

    private static ScoreboardObjective objective(MinecraftClient mc) {
        Scoreboard sb = mc.world.getScoreboard();
        ScoreboardObjective obj = null;
        try {
            Team t = sb.getScoreHolderTeam(mc.player.getNameForScoreboard());
            if (t != null) {
                ScoreboardDisplaySlot slot = ScoreboardDisplaySlot.fromFormattingColor(t.getColor());
                if (slot != null) obj = sb.getObjectiveForSlot(slot);
            }
        } catch (Throwable ignored) { }
        return obj != null ? obj : sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    private static void draw(DrawContext ctx, MinecraftClient mc, Module m) {
        ScoreboardObjective obj = objective(mc);
        if (obj == null) return;
        Scoreboard sb = mc.world.getScoreboard();
        List<ScoreboardEntry> entries = new ArrayList<>();
        for (ScoreboardEntry e : sb.getScoreboardEntries(obj)) if (!e.hidden()) entries.add(e);
        entries.sort(Comparator.comparing(ScoreboardEntry::value).reversed()
                .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER.reversed()));
        if (entries.size() > 15) entries = entries.subList(0, 15);

        boolean showScores = m.bool("scores");
        List<Row> rows = new ArrayList<>();
        for (ScoreboardEntry e : entries) {
            Text name = Team.decorateName(sb.getScoreHolderTeam(e.owner()), e.name());
            Text score = showScores ? e.formatted(obj.getNumberFormatOr(StyledNumberFormat.RED)) : Text.empty();
            rows.add(new Row(name, score));
        }
        Text title = obj.getDisplayName();
        var font = mc.textRenderer;
        int tw = font.getWidth(title);
        int gap = font.getWidth(": ");
        int bodyW = 0;
        for (Row r : rows) bodyW = Math.max(bodyW, font.getWidth(r.name) + (showScores ? gap + font.getWidth(r.score) : 0));
        int w = Math.max(tw, bodyW) + 14;
        int titleH = 15;
        int h = titleH + rows.size() * 10 + 7;
        curW = w; curH = h;

        float sc = m.num("scale") / 100f;
        int[] rc = rect(ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        int bgA = Math.round(m.num("opacity") / 100f * 0xC0);          // przezroczystość dotyczy TYLKO tła
        int accent = m.color("accent");

        Compat.push(ctx);
        Compat.translate(ctx, rc[0], rc[1]);
        Compat.scale(ctx, sc, sc);
        if (bgA > 0) Ui.glass(ctx, 0, 0, w, h, Modules.GENERAL.num("radius") + 2, (bgA << 24) | 0x1A1410);
        ctx.drawTextWithShadow(font, title, (w - tw) / 2, 4, 0xFFFFFFFF);
        int ux = w / 2 - Math.max(10, w / 6);
        ctx.fill(ux, 13, w - ux, 14, accent);                           // cienka kreska pod tytułem
        int y = titleH + 3;
        for (Row r : rows) {
            ctx.drawTextWithShadow(font, r.name, 7, y, 0xFFFFFFFF);
            if (showScores) ctx.drawTextWithShadow(font, r.score, w - 7 - font.getWidth(r.score), y, 0xFFFFFFFF);
            y += 10;
        }
        Compat.pop(ctx);
    }
}

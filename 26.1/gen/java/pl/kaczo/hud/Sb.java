package pl.kaczo.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.network.chat.Component;

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

    private record Row(Component name, Component score) { }

    public static int[] rect(int screenW, int screenH) {
        float sc = Modules.SCOREBOARD.num("scale") / 100f;
        int w = Math.round(curW * sc), h = Math.round(curH * sc);
        return new int[]{screenW - w - 4 + Hud.sbDx(), screenH / 2 - h / 2 + Hud.sbDy(), w, h};
    }

    public static void render(GuiGraphicsExtractor ctx, Minecraft mc) {
        Module m = Modules.SCOREBOARD;
        if (m == null || !m.enabled() || !hooked() || mc.level == null || mc.player == null) return;
        try {
            draw(ctx, mc, m);
        } catch (Throwable t) {
            fails++;
        }
    }

    private static Objective objective(Minecraft mc) {
        Scoreboard sb = mc.level.getScoreboard();
        return sb.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    private static void draw(GuiGraphicsExtractor ctx, Minecraft mc, Module m) {
        Objective obj = objective(mc);
        if (obj == null) return;
        Scoreboard sb = mc.level.getScoreboard();
        List<PlayerScoreEntry> entries = new ArrayList<>();
        for (PlayerScoreEntry e : sb.listPlayerScores(obj)) if (!e.isHidden()) entries.add(e);
        entries.sort(Comparator.comparing(PlayerScoreEntry::value).reversed()
                .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER.reversed()));
        if (entries.size() > 15) entries = entries.subList(0, 15);

        boolean showScores = m.bool("scores");
        List<Row> rows = new ArrayList<>();
        for (PlayerScoreEntry e : entries) {
            Component ownerName = PlayerTeam.formatNameForTeam(sb.getPlayersTeam(e.owner()), e.ownerName());
            Component score = showScores ? e.formatValue(obj.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT)) : Component.empty();
            rows.add(new Row(name, score));
        }
        Component title = obj.getDisplayName();
        var font = mc.font;
        int tw = font.width(title);
        int gap = font.width(": ");
        int bodyW = 0;
        for (Row r : rows) bodyW = Math.max(bodyW, font.width(r.name) + (showScores ? gap + font.width(r.score) : 0));
        int w = Math.max(tw, bodyW) + 14;
        int titleH = 15;
        int h = titleH + rows.size() * 10 + 7;
        curW = w; curH = h;

        float sc = m.num("scale") / 100f;
        int[] rc = rect(ctx.guiWidth(), ctx.guiHeight());
        int bgA = Math.round(m.num("opacity") / 100f * 0xC0);          // przezroczystość dotyczy TYLKO tła
        int accent = m.color("accent");

        Compat.push(ctx);
        Compat.translate(ctx, rc[0], rc[1]);
        Compat.scale(ctx, sc, sc);
        if (bgA > 0) Ui.glass(ctx, 0, 0, w, h, Modules.GENERAL.num("radius") + 2, (bgA << 24) | 0x1A1410);
        ctx.text(font, title, (w - tw) / 2, 4, 0xFFFFFFFF, true);
        int ux = w / 2 - Math.max(10, w / 6);
        ctx.fill(ux, 13, w - ux, 14, accent);                           // cienka kreska pod tytułem
        int y = titleH + 3;
        for (Row r : rows) {
            ctx.text(font, r.name, 7, y, 0xFFFFFFFF, true);
            if (showScores) ctx.text(font, r.score, w - 7 - font.width(r.score), y, 0xFFFFFFFF, true);
            y += 10;
        }
        Compat.pop(ctx);
    }
}

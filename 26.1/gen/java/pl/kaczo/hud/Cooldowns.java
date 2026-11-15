package pl.kaczo.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Odliczanie cooldownów przedmiotów z ekwipunku (perły, tarcza, topór, maczuga itd.). */
public class Cooldowns {
    static final class Entry { ItemStack stack; String name; float p; double rate; double lastP; long lastNs; }
    private static final Map<Item, Entry> TRACK = new LinkedHashMap<>();

    static List<Entry> active(Minecraft mc) {
        List<Entry> out = new ArrayList<>();
        if (mc.player == null) return out;
        long now = System.nanoTime();
        Set<Item> seen = new HashSet<>();
        int n = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < n; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.isEmpty() || !seen.add(s.getItem())) continue;
            float p = Compat.cooldown(mc.player, s);
            Entry e = TRACK.get(s.getItem());
            if (p <= 0.001f) { TRACK.remove(s.getItem()); continue; }
            if (e == null) { e = new Entry(); e.stack = s; e.name = s.getHoverName().getString(); e.lastP = p; e.lastNs = now; e.rate = 0; TRACK.put(s.getItem(), e); }
            double dt = (now - e.lastNs) / 1e9;
            if (dt >= 0.06 && p < e.lastP) { double r = (e.lastP - p) / dt; e.rate = e.rate <= 0 ? r : e.rate * 0.7 + r * 0.3; e.lastP = p; e.lastNs = now; }
            e.p = p; e.stack = s;
            out.add(e);
        }
        return out;
    }

    static String time(Entry e) { return e.rate > 0 ? String.format(Locale.ROOT, "%.1fs", e.p / e.rate) : "..."; }

    static int widthOf(Minecraft mc) {
        int w = 96;
        for (Entry e : active(mc)) w = Math.max(w, 34 + mc.font.width(e.name + "  " + time(e)));
        return w;
    }

    static int heightOf(Minecraft mc) { return Math.max(1, active(mc).size()) * 20 - 4; }

    static void draw(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y) {
        List<Entry> l = active(mc);
        boolean edit = l.isEmpty() && mc.screen != null;
        int w = widthOf(mc), yy = y;
        if (edit) { drawRow(ctx, mc, x, yy, w, new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL), "Ender Pearl", "12.4s", 0.6f); return; }
        for (Entry e : l) { drawRow(ctx, mc, x, yy, w, e.stack, e.name, time(e), e.p); yy += 20; }
    }

    private static void drawRow(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y, int w, ItemStack st, String name, String t, float p) {
        Ui.card(ctx, x, y, w, 16, 8, 0x66101418, 0xAAFFFFFF);
        ctx.item(st, x + 1, y);
        int c = mc.font.width(t);
        ctx.text(mc.font, name, x + 20, y + 4, 0xFFFFFFFF, false);
        ctx.text(mc.font, t, x + w - c - 8, y + 4, 0xFFFFD27A, false);
        Ui.rrectD(ctx, x + 10, y + 14, (w - 20) * Math.max(0.04f, 1 - p), 1.5, 0.75, 0xCCFFFFFF);
    }
}

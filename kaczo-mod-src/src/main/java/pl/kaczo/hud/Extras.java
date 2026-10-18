package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayDeque;

/** Logika dodatkowych modułów w stylu klientów PvP: zasięg, combo, ruch myszy, widok wolny, znacznik, miejsce śmierci. */
public final class Extras {
    private Extras() { }

    // ───────────── zasięg i combo ─────────────
    private static double reach = -1;
    private static long reachT = 0;
    private static int combo = 0, bestCombo = 0, lastHurt = 0;
    private static long comboT = 0;
    private static Entity pending = null;

    // ───────────── ruch myszy ─────────────
    private static float lastYaw, lastPitch, msX, msY;
    private static boolean msInit = false;
    private static final ArrayDeque<float[]> TRAIL = new ArrayDeque<>();

    // ───────────── widok wolny (freelook) ─────────────
    private static boolean flActive = false;
    public static float flYaw, flPitch;
    private static Perspective flSaved = Perspective.FIRST_PERSON;
    /** Ustawiane przez mixin kamery, gdy faktycznie przejmuje obrót – dopiero wtedy blokujemy obrót gracza. */
    public static boolean camHookWorks = false;
    private static int camCalls = 0;

    // ───────────── śmierć, znacznik ─────────────
    private static boolean deathSeen = false;
    private static boolean hasDeath = false;
    private static int dx, dy, dz;
    private static String dDim = "";

    public static void tick(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (mc.player == null) { pending = null; flActive = false; msInit = false; return; }
        var p = mc.player;

        // zasięg + combo
        Entity tgt = mc.crosshairTarget instanceof EntityHitResult ehr ? ehr.getEntity() : null;
        if (pending != null) {
            if (pending instanceof LivingEntity le && le.hurtTime >= 8) { combo++; bestCombo = Math.max(bestCombo, combo); comboT = now; }
            pending = null;
        }
        if (Cps.leftClicked() && mc.currentScreen == null && tgt instanceof LivingEntity le && tgt != p) {
            reach = ((EntityHitResult) mc.crosshairTarget).getPos().distanceTo(p.getEyePos());
            reachT = now;
            pending = le;
        }
        if (p.hurtTime > lastHurt && p.hurtTime >= 8) combo = 0;           // dostałeś – combo przerwane
        lastHurt = p.hurtTime;
        if (combo > 0 && now - comboT > 3000) combo = 0;

        // ruch myszy (różnica obrotu między tickami)
        float yaw = p.getYaw(), pitch = p.getPitch();
        if (!msInit) { lastYaw = yaw; lastPitch = pitch; msInit = true; }
        float ddx = MathHelper.wrapDegrees(yaw - lastYaw), ddy = pitch - lastPitch;
        lastYaw = yaw; lastPitch = pitch;
        msX += (MathHelper.clamp(ddx / 14f, -1f, 1f) - msX) * 0.45f;
        msY += (MathHelper.clamp(ddy / 14f, -1f, 1f) - msY) * 0.45f;
        TRAIL.addFirst(new float[]{msX, msY});
        while (TRAIL.size() > 16) TRAIL.removeLast();

        // śmierć
        boolean dead = mc.currentScreen instanceof DeathScreen;
        if (dead && !deathSeen) {
            deathSeen = true; hasDeath = true;
            dx = p.getBlockX(); dy = p.getBlockY(); dz = p.getBlockZ();
            dDim = mc.world == null ? "" : mc.world.getRegistryKey().getValue().getPath();
        }
        if (!dead) deathSeen = false;

        // widok wolny
        boolean held = Modules.byId("freelook").enabled() && KaczoHudClient.freelookKey != null
                && KaczoHudClient.freelookKey.isPressed() && mc.currentScreen == null;
        if (held && !flActive) {
            flActive = true; flYaw = p.getYaw(); flPitch = p.getPitch();
            flSaved = mc.options.getPerspective();
            if (flSaved == Perspective.FIRST_PERSON) mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else if (!held && flActive) {
            flActive = false;
            mc.options.setPerspective(flSaved);
        }
    }

    // ───────────── widok wolny: dostęp dla mixinów ─────────────
    public static boolean freelookActive() { return flActive; }

    public static void look(double dxm, double dym) {
        flYaw += (float) dxm * 0.15f;
        flPitch = MathHelper.clamp(flPitch + (float) dym * 0.15f, -90f, 90f);
    }

    public static void camStart() { camCalls = 0; }

    /** Zwraca true tylko przy pierwszym ustawieniu obrotu kamery w danej klatce. */
    public static boolean camFirst() { return camCalls++ == 0; }

    // ───────────── teksty modułów ─────────────

    public static String reachText() {
        if (reach < 0) return "Reach: -";
        return String.format("Reach: %.2f", reach);
    }

    public static String comboText() {
        return L.t("Combo") + ": " + combo + (bestCombo > 0 ? "  (" + L.t("best") + " " + bestCombo + ")" : "");
    }

    public static String deathText(MinecraftClient mc) {
        if (!hasDeath) return L.t("No death yet");
        String d = String.format("%s %d %d %d", L.t("Death"), dx, dy, dz);
        if (mc.player != null) {
            double dist = Math.sqrt(Math.pow(mc.player.getX() - dx, 2) + Math.pow(mc.player.getZ() - dz, 2));
            d += String.format(" (%.0f m)", dist);
        }
        return d;
    }

    // ───────────── znacznik (waypoint) ─────────────

    private static Config.State wp() { return Config.st("waypoint"); }

    public static void setWaypoint(MinecraftClient mc) {
        if (mc.player == null) return;
        var v = wp().vals;
        v.put("x", String.valueOf(mc.player.getBlockX()));
        v.put("y", String.valueOf(mc.player.getBlockY()));
        v.put("z", String.valueOf(mc.player.getBlockZ()));
        v.put("set", "1");
        Config.save();
        mc.player.sendMessage(Text.literal("Kaczo: " + L.t("Waypoint set")), true);
    }

    public static void clearWaypoint(MinecraftClient mc) {
        wp().vals.put("set", "0");
        Config.save();
        if (mc.player != null) mc.player.sendMessage(Text.literal("Kaczo: " + L.t("Waypoint cleared")), true);
    }

    public static String waypointText(MinecraftClient mc) {
        var v = wp().vals;
        if (mc.player == null || !"1".equals(v.get("set"))) return L.t("No waypoint (press B)");
        try {
            double x = Integer.parseInt(v.get("x")) + 0.5, z = Integer.parseInt(v.get("z")) + 0.5;
            double ddx = x - mc.player.getX(), ddz = z - mc.player.getZ();
            double dist = Math.sqrt(ddx * ddx + ddz * ddz);
            double ang = Math.toDegrees(Math.atan2(-ddx, ddz));               // kierunek świata (jak yaw gracza)
            double rel = MathHelper.wrapDegrees(ang - mc.player.getYaw());
            String[] ar = {"^", "^>", ">", "v>", "v", "<v", "<", "<^"};
            int idx = (int) Math.round(((rel + 360) % 360) / 45.0) % 8;
            return String.format("%s %.0f m  [%s %s %s]", ar[idx], dist, v.get("x"), v.get("y"), v.get("z"));
        } catch (Exception e) { return "-"; }
    }

    // ───────────── rysowanie niestandardowych modułów ─────────────

    private static final ItemStack[] PVP = {
            new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.END_CRYSTAL), new ItemStack(Items.GOLDEN_APPLE),
            new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), new ItemStack(Items.EXPERIENCE_BOTTLE), new ItemStack(Items.ENDER_PEARL)};
    private static final String[] PVP_KEYS = {"totem", "crystal", "gapple", "egapple", "xpb", "pearl"};

    public static int[] pvpSize(MinecraftClient mc) {
        Module m = Modules.byId("pvpitems");
        int n = 0;
        for (String k : PVP_KEYS) if (m.bool(k)) n++;
        return new int[]{Math.max(20, n * 26), 22};
    }

    public static void drawPvpItems(DrawContext ctx, MinecraftClient mc, int x, int y) {
        if (mc.player == null) return;
        Module m = Modules.byId("pvpitems");
        int i = 0;
        for (int k = 0; k < PVP.length; k++) {
            if (!m.bool(PVP_KEYS[k])) continue;
            int cx = x + i * 26;
            Ui.box(ctx, cx - 1, y - 1, 24, 22, m.bg());
            ctx.drawItem(PVP[k], cx + 3, y + 1);
            int c = mc.player.getInventory().count(PVP[k].getItem());
            String s = String.valueOf(c);
            ctx.drawText(mc.textRenderer, s, cx + 21 - mc.textRenderer.getWidth(s), y + 11, c == 0 ? 0xFFFF5C63 : 0xFFFFFFFF, true);
            i++;
        }
    }

    public static void drawInventory(DrawContext ctx, MinecraftClient mc, int x, int y) {
        if (mc.player == null) return;
        Module m = Modules.byId("inventory");
        Ui.box(ctx, x - 3, y - 3, 9 * 18 + 6, 3 * 18 + 6, m.bg());
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                ItemStack st = mc.player.getInventory().getStack(9 + r * 9 + c);
                if (st.isEmpty()) continue;
                int ix = x + c * 18, iy = y + r * 18;
                ctx.drawItem(st, ix + 1, iy + 1);
                if (st.getCount() > 1) {
                    String s = String.valueOf(st.getCount());
                    ctx.drawText(mc.textRenderer, s, ix + 18 - mc.textRenderer.getWidth(s), iy + 10, 0xFFFFFFFF, true);
                }
            }
        }
    }

    public static void drawMouse(DrawContext ctx, MinecraftClient mc, int x, int y) {
        Module m = Modules.byId("mousestrokes");
        int s = 48;
        Ui.box(ctx, x, y, s, s, m.bg());
        int col = m.color("color");
        ctx.fill(x + s / 2, y + 3, x + s / 2 + 1, y + s - 3, 0x22FFFFFF);
        ctx.fill(x + 3, y + s / 2, x + s - 3, y + s / 2 + 1, 0x22FFFFFF);
        int i = 0, n = TRAIL.size();
        for (float[] t : TRAIL) {
            int px = x + s / 2 + Math.round(t[0] * (s / 2f - 6)), py = y + s / 2 + Math.round(t[1] * (s / 2f - 6));
            int a = (int) (200 * (1 - i / (float) n));
            int sz = i == 0 ? 3 : (i < 5 ? 2 : 1);
            ctx.fill(px - sz / 2, py - sz / 2, px - sz / 2 + sz, py - sz / 2 + sz, (a << 24) | (col & 0xFFFFFF));
            i++;
        }
    }
}

package pl.kaczo.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Rejestr wszystkich modułów. */
public class Modules {
    public static final List<Module> ALL = new ArrayList<>();
    public static final Module GENERAL = new Module("general", "Settings", "", "", "");
    public static Module CROSSHAIR, SCOREBOARD, MUSIC, ZOOM;

    private static boolean zooming = false, nvAdded = false;
    private static int savedFov = 70;
    private static int tick = 0;
    private static long joinStart = -1;
    private static boolean hadPlayer = false;
    private static boolean sneakHeld = false, sneakToggled = false;
    private static float lastHealth = -1, flash = 0;

    private static Module reg(Module m) { ALL.add(m); return m; }

    private static final long SESSION_START = System.currentTimeMillis();
    private static final net.minecraft.util.Identifier LOGO = net.minecraft.util.Identifier.of("kaczohud", "textures/gui/logo.png");

    static {
        GENERAL.add(Setting.bool("hint", "Show key hint", true));
        GENERAL.add(Setting.choice("style", "HUD style", 0, "Glass", "Flat"));
        GENERAL.add(Setting.num("radius", "Corner radius", 0, 8, 4));
        GENERAL.add(Setting.bool("glassborder", "Glass border", true));
        GENERAL.add(Setting.bool("glassbtn", "Glass buttons in game menus", true));
        GENERAL.add(Setting.bool("titlelogo", "Logo on title screen", true));
        GENERAL.add(Setting.bool("intro", "Intro animation", true));
        GENERAL.add(Setting.bool("hudanim", "Animate HUD on join", true));
        GENERAL.add(Setting.bool("titlebg", "Animated title background", true));
        GENERAL.add(Setting.bool("loadscreen", "Custom loading screen", true));
        GENERAL.add(Setting.num("logosize", "Title logo size %", 40, 140, 80));

        reg(new Module("fps", "FPS", "Frames per second.", "HUD", "FPS").on().hud(6, 6)
                .text(mc -> mc.getCurrentFps() + " FPS"));
        reg(new Module("ping", "Ping", "Latency to the server.", "HUD", "MS").on().hud(6, 20)
                .text(mc -> {
                    if (mc.player != null && mc.getNetworkHandler() != null) {
                        var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                        if (e != null) return e.getLatency() + " ms";
                    }
                    return "- ms";
                }));
        reg(new Module("coords", "Coordinates", "Your XYZ position.", "HUD", "XYZ").on().hud(6, 34)
                .text(mc -> mc.player == null ? "XYZ" : String.format("XYZ %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ())));
        reg(new Module("direction", "Direction", "Where you are facing.", "HUD", "N").hud(6, 48)
                .text(mc -> mc.player == null ? "N" : mc.player.getHorizontalFacing().asString().toUpperCase()));
        reg(new Module("clock", "Clock", "Real time.", "HUD", "12:00").hud(6, 62)
                .text(mc -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))));
        reg(new Module("day", "Day Counter", "World day number.", "HUD", "DAY").hud(6, 76)
                .text(mc -> L.t("Day") + " " + (mc.world == null ? 0 : mc.world.getTimeOfDay() / 24000L)));
        reg(new Module("worldtime", "World Time", "Minecraft time of day.", "HUD", "TIME").hud(6, 90)
                .text(mc -> {
                    if (mc.world == null) return "--:--";
                    long t = mc.world.getTimeOfDay() % 24000L;
                    return String.format("%02d:%02d", (t / 1000 + 6) % 24, (t % 1000) * 60 / 1000);
                }));
        reg(new Module("speed", "Speed Meter", "Blocks per second.", "HUD", "B/S").hud(6, 104)
                .text(mc -> {
                    if (mc.player == null) return "0.0 b/s";
                    var v = mc.player.getVelocity();
                    return String.format("%.1f b/s", Math.sqrt(v.x * v.x + v.z * v.z) * 20);
                }));
        reg(new Module("biome", "Biome", "Name of the biome you are in.", "HUD", "BIOME").fresh().hud(6, 118)
                .text(mc -> {
                    if (mc.world == null || mc.player == null) return "-";
                    return mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("?");
                }));
        reg(new Module("light", "Light Level", "Light level at your position.", "HUD", "LIGHT").fresh().hud(6, 132)
                .text(mc -> mc.world == null || mc.player == null ? "-" : "Light " + mc.world.getLightLevel(mc.player.getBlockPos())));
        reg(new Module("players", "Player Count", "Players online on the server.", "HUD", "NICK#").fresh().hud(6, 146)
                .text(mc -> mc.getNetworkHandler() == null ? "-" : mc.getNetworkHandler().getPlayerList().size() + " players"));
        reg(new Module("session", "Session Timer", "How long you have been playing.", "HUD", "TIMER").fresh().hud(6, 160)
                .text(mc -> {
                    long s = (System.currentTimeMillis() - SESSION_START) / 1000;
                    return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
                }));
        reg(new Module("memory", "System Resources", "Memory used by the game.", "HUD", "RAM").hud(6, 174)
                .text(mc -> {
                    Runtime r = Runtime.getRuntime();
                    return (r.totalMemory() - r.freeMemory()) / 1048576 + " / " + r.maxMemory() / 1048576 + " MB";
                }));
        reg(new Module("server", "Server Address", "Address of the current server.", "HUD", "IP").hud(6, 188)
                .text(mc -> mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : L.t("Singleplayer")));
        reg(new Module("itemcount", "Item Counter", "Count of the item in your hand.", "HUD", "ITEM#").fresh().hud(6, 202)
                .text(mc -> {
                    if (mc.player == null || mc.player.getMainHandStack().isEmpty()) return "-";
                    var st = mc.player.getMainHandStack();
                    return st.getName().getString() + " x" + mc.player.getInventory().count(st.getItem());
                }));
        reg(new Module("cps", "CPS", "Clicks per second.", "PvP", "CPS").hud(6, 216)
                .text(mc -> Cps.left() + " | " + Cps.right() + " CPS"));
        reg(new Module("keystrokes", "Keystrokes", "Shows WASD, space and mouse buttons.", "PvP", "WASD").fresh().hud(6, 232)
                .draw(54, 54, Modules::drawKeys));
        reg(new Module("armor", "Armor Status", "Your armor and durability.", "PvP", "ARM").hud(80, 232)
                .draw(70, 66, Modules::drawArmor));
        reg(new Module("potions", "Potion Effects", "Active effects and time left.", "PvP", "FX").fresh().hud(160, 232)
                .size(Modules::potionSize).draw(100, 20, Modules::drawPotions));
        reg(new Module("nick", "Nick Badge", "Your nickname with the Kaczo Client logo.", "HUD", "NICK").fresh().on().hud(6, 250)
                .size(mc -> new int[]{(logoOn() ? 12 : 0) + mc.textRenderer.getWidth(nick(mc)) + 2, 12})
                .draw(60, 12, Modules::drawNick)
                .add(Setting.bool("shownick", "Show nick", true))
                .add(Setting.bool("showlogo", "Show logo", true)));
        reg(new Module("health", "Health", "Your hearts as a number.", "HUD", "HP").fresh().hud(100, 6)
                .text(mc -> mc.player == null ? "-" : String.format("%.1f HP", mc.player.getHealth())));
        reg(new Module("hunger", "Hunger", "Food level.", "HUD", "FOOD").fresh().hud(100, 20)
                .text(mc -> mc.player == null ? "-" : mc.player.getHungerManager().getFoodLevel() + " " + L.t("food")));
        reg(new Module("xp", "Experience", "Your XP level.", "HUD", "XP").fresh().hud(100, 34)
                .text(mc -> mc.player == null ? "-" : L.t("Level") + " " + mc.player.experienceLevel));
        reg(new Module("rotation", "Rotation", "Yaw and pitch.", "HUD", "YAW").fresh().hud(100, 48)
                .text(mc -> mc.player == null ? "-" : String.format("%.0f / %.0f", net.minecraft.util.math.MathHelper.wrapDegrees(mc.player.getYaw()), mc.player.getPitch())));
        reg(new Module("chunk", "Chunk", "Chunk coordinates.", "HUD", "CHK").fresh().hud(100, 62)
                .text(mc -> mc.player == null ? "-" : "Chunk " + (mc.player.getBlockX() >> 4) + " " + (mc.player.getBlockZ() >> 4)));
        reg(new Module("helditem", "Held Item", "Name and durability of the item in hand.", "HUD", "HAND").fresh().hud(100, 76)
                .text(mc -> {
                    if (mc.player == null || mc.player.getMainHandStack().isEmpty()) return "-";
                    ItemStack st = mc.player.getMainHandStack();
                    String n = st.getName().getString();
                    return st.isDamageable() ? n + " " + (st.getMaxDamage() - st.getDamage()) + "/" + st.getMaxDamage() : n;
                }));
        reg(new Module("realdate", "Date", "Today's date.", "HUD", "DATE").fresh().hud(100, 90)
                .text(mc -> java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
        reg(new Module("fpsmax", "FPS Limit", "Shows the FPS limit from video settings.", "HUD", "LIM").fresh().hud(100, 104)
                .text(mc -> {
                    int l = mc.options.getMaxFps().getValue();
                    return l >= 260 ? L.t("Unlimited") : l + " FPS max";
                }));
        reg(new Module("brand", "Kaczo Badge", "Glass badge with the Kaczo Client logo.", "HUD", "LOGO").fresh().on().hud(100, 124)
                .size(mc -> new int[]{12 + mc.textRenderer.getWidth(brandText()) + 4, 12})
                .draw(70, 12, Modules::drawBrand)
                .add(Setting.bool("showlogo", "Show logo", true)));
        MUSIC = reg(new Module("music", "Now Playing", "Cover, title and lyrics of your music (Windows).", "HUD", "MUSIC").fresh().on().hud(6, 270)
                .size(NowPlayingCard::size).draw(150, 36, NowPlayingCard::draw)
                .add(Setting.bool("cover", "Show cover", true))
                .add(Setting.bool("lyrics", "Show lyrics", true))
                .add(Setting.num("width", "Min width", 100, 320, 130))
                .add(Setting.bool("hide", "Hide when nothing plays", true)));

        CROSSHAIR = reg(new Module("crosshair", "Custom Crosshair", "Your own crosshair.", "PvP", "+").on()
                .add(Setting.choice("style", "Style", 1, "Dot", "Cross", "Circle", "Square"))
                .add(Setting.color("color", "Color", Setting.YELLOW))
                .add(Setting.num("size", "Size", 2, 12, 5))
                .add(Setting.num("gap", "Gap", 0, 6, 2))
                .add(Setting.num("thick", "Thickness", 1, 4, 1))
                .add(Setting.bool("indicator", "Indicator", true))
                .add(Setting.color("tcolor", "Target color", "255,60,60,255,0"))
                .add(Setting.choice("react", "React on", 0, "Players", "All mobs")));
        ZOOM = reg(new Module("zoom", "Zoom", "Hold C to zoom in.", "Utility", "ZOOM").on()
                .add(Setting.num("fov", "Zoom FOV", 30, 70, 30)));
        reg(new Module("fullbright", "Fullbright", "See in the dark (night vision effect).", "Utility", "SUN"));
        reg(new Module("sprint", "Toggle Sprint", "Always sprint while moving forward.", "Utility", "RUN"));
        reg(new Module("togglesneak", "Toggle Sneak", "Press the sneak key once to stay sneaking.", "Utility", "SNK").fresh());
        reg(new Module("hitflash", "Damage Flash", "Red flash on the screen edges when you take damage.", "PvP", "HIT").fresh()
                .add(Setting.color("color", "Color", "255,40,40,255,0"))
                .add(Setting.num("strength", "Strength %", 10, 100, 50)));
        SCOREBOARD = reg(new Module("scoreboard", "Scoreboard", "Sidebar scoreboard: hide, scale, make transparent.", "Utility", "SB").on()
                .add(Setting.num("scale", "Size %", 40, 150, 100))
                .add(Setting.num("opacity", "Opacity %", 10, 100, 100)));
    }

    private static String brandText() { return "KACZO CLIENT"; }

    private static void drawBrand(DrawContext ctx, MinecraftClient mc, int x, int y) {
        Module m = byId("brand");
        int w = m.width(mc);
        Ui.box(ctx, x - 3, y - 3, w + 6, 15, m.bg());
        int tx = x;
        if (m.bool("showlogo")) {
            Compat.push(ctx);
            Compat.translate(ctx, x, y - 1);
            Compat.scale(ctx, 0.1875f, 0.1875f);
            Compat.tex(ctx, LOGO, 64, 64);
            Compat.pop(ctx);
            tx += 14;
        }
        ctx.drawTextWithShadow(mc.textRenderer, brandText(), tx, y + 1, m.color("color"));
    }

    public static net.minecraft.util.Identifier logo() { return LOGO; }

    private static boolean logoOn() { return byId("nick").bool("showlogo"); }
    private static String nick(MinecraftClient mc) { return byId("nick").bool("shownick") ? mc.getSession().getUsername() : ""; }

    private static void drawNick(DrawContext ctx, MinecraftClient mc, int x, int y) {
        Module m = byId("nick");
        int w = m.width(mc);
        Ui.box(ctx, x - 3, y - 3, w + 6, 15, m.bg());
        int tx = x;
        if (logoOn()) {
            Compat.push(ctx);
            Compat.translate(ctx, x, y - 1);
            Compat.scale(ctx, 0.1875f, 0.1875f);          // 64 px -> 12 px
            Compat.tex(ctx, LOGO, 64, 64);
            Compat.pop(ctx);
            tx += 14;
        }
        String n = nick(mc);
        if (!n.isEmpty()) ctx.drawTextWithShadow(mc.textRenderer, n, tx, y + 1, m.color("color"));
    }

    private static int[] potionSize(MinecraftClient mc) {
        int n = mc.player == null ? 0 : mc.player.getStatusEffects().size();
        return new int[]{110, Math.max(10, n * 10)};
    }

    private static void drawPotions(DrawContext ctx, MinecraftClient mc, int x, int y) {
        if (mc.player == null) return;
        Module m = byId("potions");
        int i = 0;
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            int secs = e.isInfinite() ? -1 : e.getDuration() / 20;
            String t = e.getEffectType().value().getName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "")
                    + (secs < 0 ? "" : String.format(" %d:%02d", secs / 60, secs % 60));
            ctx.drawTextWithShadow(mc.textRenderer, t, x, y + i * 10, m.color("color"));
            i++;
        }
    }

    // ───────────── rysowanie niestandardowych modułów ─────────────

    private static void key(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, String label, boolean down) {
        Module km = byId("keystrokes");
        Ui.box(ctx, x, y, w, h, down ? (0xD0000000 | (km.color("color") & 0xFFFFFF)) : km.bg());
        int tw = mc.textRenderer.getWidth(label);
        ctx.drawText(mc.textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, down ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    private static void drawKeys(DrawContext ctx, MinecraftClient mc, int x, int y) {
        var o = mc.options;
        key(ctx, mc, x + 18, y, 16, 16, "W", o.forwardKey.isPressed());
        key(ctx, mc, x, y + 18, 16, 16, "A", o.leftKey.isPressed());
        key(ctx, mc, x + 18, y + 18, 16, 16, "S", o.backKey.isPressed());
        key(ctx, mc, x + 36, y + 18, 16, 16, "D", o.rightKey.isPressed());
        key(ctx, mc, x, y + 36, 25, 16, "LMB", Cps.leftDown());
        key(ctx, mc, x + 27, y + 36, 25, 16, "RMB", Cps.rightDown());
    }

    private static void drawArmor(DrawContext ctx, MinecraftClient mc, int x, int y) {
        if (mc.player == null) return;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            ItemStack s = mc.player.getEquippedStack(slots[i]);
            int yy = y + i * 17;
            if (s.isEmpty()) continue;
            ctx.drawItem(s, x, yy);
            if (s.isDamageable()) {
                String d = String.valueOf(s.getMaxDamage() - s.getDamage());
                ctx.drawText(mc.textRenderer, d, x + 20, yy + 4, 0xFFFFFFFF, true);
            }
        }
    }

    // ───────────── logika co tick ─────────────

    public static void tick(MinecraftClient mc) {
        Cps.tick(mc);
        if (mc.player == null) { zooming = false; nvAdded = false; hadPlayer = false; return; }
        if (!hadPlayer) { hadPlayer = true; joinStart = System.currentTimeMillis(); }
        tick++;

        if (MUSIC.enabled()) NowPlaying.start();

        // Zoom (trzymaj C)
        boolean want = ZOOM.enabled() && mc.currentScreen == null
                && InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_C);
        if (want && !zooming) { savedFov = mc.options.getFov().getValue(); zooming = true; }
        if (want) mc.options.getFov().setValue(Math.max(30, Math.min(110, ZOOM.num("fov"))));
        if (!want && zooming) { mc.options.getFov().setValue(savedFov); zooming = false; }

        // Fullbright
        boolean fb = byId("fullbright").enabled();
        if (fb && tick % 40 == 0) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600, 0, false, false, false));
            nvAdded = true;
        }
        if (!fb && nvAdded) { mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION); nvAdded = false; }

        // Toggle Sneak
        if (byId("togglesneak").enabled() && mc.currentScreen == null) {
            boolean phys = InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                    net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(mc.options.sneakKey).getCode());
            if (phys && !sneakHeld) sneakToggled = !sneakToggled;
            sneakHeld = phys;
            mc.options.sneakKey.setPressed(sneakToggled || phys);
        } else if (sneakToggled) { sneakToggled = false; mc.options.sneakKey.setPressed(false); }

        // Damage Flash
        float hp = mc.player.getHealth();
        if (lastHealth >= 0 && hp < lastHealth) flash = 1f;
        lastHealth = hp;
        flash = Math.max(0, flash - 0.06f);

        // Toggle Sprint
        if (byId("sprint").enabled() && mc.options.forwardKey.isPressed() && !mc.player.isSneaking()
                && mc.player.getHungerManager().getFoodLevel() > 6) mc.player.setSprinting(true);
    }

    /** Ile ms minęło od wejścia do świata (do animacji HUD). */
    public static long sinceJoin() { return joinStart < 0 ? 100000 : System.currentTimeMillis() - joinStart; }

    public static float flash() { return flash; }

    public static Module byId(String id) {
        for (Module m : ALL) if (m.id.equals(id)) return m;
        return GENERAL;
    }
}

package pl.kaczo.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
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

    private static boolean zooming = false, nvAdded = false, fovSaved = false, hitboxSet = false;
    private static int fovBefore = 70;
    private static double fxBefore = 1.0;
    private static int savedFov = 70;
    private static int tick = 0;
    private static long joinStart = -1;
    private static boolean hadPlayer = false;
    private static boolean sneakHeld = false, sneakToggled = false;
    private static float lastHealth = -1, flash = 0;

    private static Module reg(Module m) { ALL.add(m); return m; }

    private static final long SESSION_START = System.currentTimeMillis();
    private static final net.minecraft.resources.Identifier LOGO = net.minecraft.resources.Identifier.fromNamespaceAndPath("kaczohud", "textures/gui/logo.png");

    static {
        GENERAL.add(Setting.header("Title screen"));
        GENERAL.add(Setting.bool("glassbtn", "Glass buttons on title screen", true));
        GENERAL.add(Setting.bool("titlelogo", "Logo on title screen", true));
        GENERAL.add(Setting.num("logosize", "Title logo size %", 40, 140, 80).unit("%"));
        GENERAL.add(Setting.bool("titlebg", "Animated title background", true));
        GENERAL.add(Setting.bool("loadscreen", "Custom loading screen", true));
        GENERAL.add(Setting.bool("intro", "Intro animation", true));
        GENERAL.add(Setting.header("HUD"));
        GENERAL.add(Setting.choice("style", "HUD style", 0, "Glass", "Flat"));
        GENERAL.add(Setting.num("radius", "Corner radius", 0, 8, 4));
        GENERAL.add(Setting.bool("glassborder", "Glass border", true));
        GENERAL.add(Setting.bool("hudanim", "Animate HUD on join", true));
        GENERAL.add(Setting.bool("hint", "Show key hint", true));
        GENERAL.add(Setting.header("Music"));
        GENERAL.add(Setting.choice("music", "Kaczo music", 1, "Off", "Menu", "Menu + game"));
        GENERAL.add(Setting.num("musicvol", "Music volume", 0, 100, 70).unit("%"));

        reg(new Module("fps", "FPS", "Frames per second.", "HUD", "FPS").on().hud(6, 6)
                .text(mc -> mc.getFps() + " FPS"));
        reg(new Module("ping", "Ping", "Latency to the server.", "HUD", "MS").on().hud(6, 20)
                .text(mc -> {
                    if (mc.player != null && mc.getConnection() != null) {
                        var e = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                        if (e != null) return e.getLatency() + " ms";
                    }
                    return "- ms";
                }));
        reg(new Module("coords", "Coordinates", "Your XYZ position.", "HUD", "XYZ").on().hud(6, 34)
                .text(mc -> mc.player == null ? "XYZ" : String.format("XYZ %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ())));
        reg(new Module("direction", "Direction", "Where you are facing.", "HUD", "N").hud(6, 48)
                .text(mc -> mc.player == null ? "N" : mc.player.getDirection().getSerializedName().toUpperCase()));
        reg(new Module("clock", "Clock", "Real time.", "HUD", "12:00").hud(6, 62)
                .text(mc -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))));
        reg(new Module("day", "Day Counter", "World day number.", "HUD", "DAY").hud(6, 76)
                .text(mc -> L.t("Day") + " " + (mc.level == null ? 0 : mc.Compat.dayTime(level) / 24000L)));
        reg(new Module("worldtime", "World Time", "Minecraft time of day.", "HUD", "TIME").hud(6, 90)
                .text(mc -> {
                    if (mc.level == null) return "--:--";
                    long t = mc.Compat.dayTime(world) % 24000L;
                    return String.format("%02d:%02d", (t / 1000 + 6) % 24, (t % 1000) * 60 / 1000);
                }));
        reg(new Module("speed", "Speed Meter", "Blocks per second.", "HUD", "B/S").hud(6, 104)
                .text(mc -> {
                    if (mc.player == null) return "0.0 b/s";
                    var v = mc.player.getDeltaMovement();
                    return String.format("%.1f b/s", Math.sqrt(v.x * v.x + v.z * v.z) * 20);
                }));
        reg(new Module("biome", "Biome", "Name of the biome you are in.", "HUD", "BIOME").fresh().hud(6, 118)
                .text(mc -> {
                    if (mc.level == null || mc.player == null) return "-";
                    return mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
                }));
        reg(new Module("light", "Light Level", "Light level at your position.", "HUD", "LIGHT").fresh().hud(6, 132)
                .text(mc -> mc.level == null || mc.player == null ? "-" : "Light " + mc.level.getMaxLocalRawBrightness(mc.player.blockPosition())));
        reg(new Module("players", "Player Count", "Players online on the server.", "HUD", "NICK#").fresh().hud(6, 146)
                .text(mc -> mc.getConnection() == null ? "-" : mc.getConnection().getOnlinePlayers().size() + " players"));
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
                .text(mc -> mc.getCurrentServer() != null ? mc.getCurrentServer().ip : L.t("Singleplayer")));
        reg(new Module("itemcount", "Item Counter", "Count of the item in your hand.", "HUD", "ITEM#").fresh().hud(6, 202)
                .text(mc -> {
                    if (mc.player == null || mc.player.getMainHandItem().isEmpty()) return "-";
                    var st = mc.player.getMainHandItem();
                    return st.getHoverName().getString() + " x" + mc.player.getInventory().countItem(st.getItem());
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
                .size(mc -> new int[]{(logoOn() ? 12 : 0) + mc.font.width(nick(mc)) + 2, 12})
                .draw(60, 12, Modules::drawNick)
                .add(Setting.bool("shownick", "Show nick", true))
                .add(Setting.bool("showlogo", "Show logo", true)));
        reg(new Module("health", "Health", "Your hearts as a number.", "HUD", "HP").fresh().hud(100, 6)
                .text(mc -> mc.player == null ? "-" : String.format("%.1f HP", mc.player.getHealth())));
        reg(new Module("hunger", "Hunger", "Food level.", "HUD", "FOOD").fresh().hud(100, 20)
                .text(mc -> mc.player == null ? "-" : mc.player.getFoodData().getFoodLevel() + " " + L.t("food")));
        reg(new Module("xp", "Experience", "Your XP level.", "HUD", "XP").fresh().hud(100, 34)
                .text(mc -> mc.player == null ? "-" : L.t("Level") + " " + mc.player.experienceLevel));
        reg(new Module("rotation", "Rotation", "Yaw and pitch.", "HUD", "YAW").fresh().hud(100, 48)
                .text(mc -> mc.player == null ? "-" : String.format("%.0f / %.0f", net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot()), mc.player.getXRot())));
        reg(new Module("chunk", "Chunk", "Chunk coordinates.", "HUD", "CHK").fresh().hud(100, 62)
                .text(mc -> mc.player == null ? "-" : "Chunk " + (mc.player.getBlockX() >> 4) + " " + (mc.player.getBlockZ() >> 4)));
        reg(new Module("helditem", "Held Item", "Name and durability of the item in hand.", "HUD", "HAND").fresh().hud(100, 76)
                .text(mc -> {
                    if (mc.player == null || mc.player.getMainHandItem().isEmpty()) return "-";
                    ItemStack st = mc.player.getMainHandItem();
                    String n = st.getHoverName().getString();
                    return st.isDamageableItem() ? n + " " + (st.getMaxDamage() - st.getDamageValue()) + "/" + st.getMaxDamage() : n;
                }));
        reg(new Module("realdate", "Date", "Today's date.", "HUD", "DATE").fresh().hud(100, 90)
                .text(mc -> java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
        reg(new Module("fpsmax", "FPS Limit", "Shows the FPS limit from video settings.", "HUD", "LIM").fresh().hud(100, 104)
                .text(mc -> {
                    int l = mc.options.framerateLimit().get();
                    return l >= 260 ? L.t("Unlimited") : l + " FPS max";
                }));
        reg(new Module("brand", "Kaczo Badge", "Glass badge with the Kaczo Client logo.", "HUD", "LOGO").fresh().on().hud(100, 124)
                .size(mc -> new int[]{12 + mc.font.width(brandText()) + 4, 12})
                .draw(70, 12, Modules::drawBrand)
                .add(Setting.bool("showlogo", "Show logo", true)));
        MUSIC = reg(new Module("music", "Now Playing", "Cover, title and lyrics of your music (Windows).", "HUD", "MUSIC").fresh().on().hud(6, 270)
                .size(NowPlayingCard::size).draw(150, 36, NowPlayingCard::draw)
                .add(Setting.bool("cover", "Show cover", true))
                .add(Setting.bool("lyrics", "Show lyrics", true))
                .add(Setting.bool("world", "Lyrics in the world (anchored)", true))
                .add(Setting.num("width", "Min width", 100, 320, 130))
                .add(Setting.bool("hide", "Hide when nothing plays", true)));

        reg(new Module("reach", "Reach Display", "Distance of your last hit.", "PvP", "REACH").fresh().hud(6, 290)
                .text(mc -> Extras.reachText()));
        reg(new Module("combo", "Combo Display", "Consecutive hits in a row.", "PvP", "COMBO").fresh().hud(6, 304)
                .text(mc -> Extras.comboText()));
        reg(new Module("mousestrokes", "Mouse Strokes", "Shows your mouse movement.", "PvP", "MOUSE").fresh().hud(80, 290)
                .draw(48, 48, Extras::drawMouse));
        reg(new Module("pvpitems", "PvP Items", "Totems, crystals, apples and pearls in your inventory.", "PvP", "ITEMS").fresh().hud(140, 290)
                .size(Extras::pvpSize).draw(80, 22, Extras::drawPvpItems)
                .add(Setting.bool("totem", "Totem", true)).add(Setting.bool("crystal", "End crystal", true))
                .add(Setting.bool("gapple", "Golden apple", true)).add(Setting.bool("egapple", "Enchanted apple", false))
                .add(Setting.bool("xpb", "XP bottle", false)).add(Setting.bool("pearl", "Ender pearl", true)));
        reg(new Module("inventory", "Inventory", "Your inventory on the screen.", "HUD", "INV").fresh().hud(6, 340)
                .draw(162, 54, Extras::drawInventory));
        reg(new Module("deathinfo", "Death Info", "Coordinates of your last death.", "HUD", "DEATH").fresh().hud(100, 140)
                .text(Extras::deathText));
        reg(new Module("waypoint", "Waypoint", "Distance and direction to a saved point (B = set, Shift+B = clear).", "HUD", "WAY").fresh().hud(100, 154)
                .text(Extras::waypointText));
        reg(new Module("freelook", "Perspective (Freelook)", "Hold Left Alt to look around without turning.", "Visual", "LOOK").fresh().on());
        reg(new Module("shaders", "Shaders", "One click turns on Kaczo Shaders: real lighting, sun shadows, sky, water (needs Iris – the launcher installs it).", "Visual", "SHD").fresh()
                .add(Setting.choice("style", "Look", 0, "Cinematic", "Noir Glass", "Dream")));
        reg(new Module("theme", "HUD Color", "One color for the whole HUD: all elements and their accents switch to it.", "Visual", "COLOR").fresh()
                .add(Setting.color("color", "Theme color", "255,60,60,255,0")));
        reg(new Module("nofire", "No Fire Overlay", "Hides the fire on your screen when you are burning.", "Visual", "FIRE").fresh());
        reg(new Module("nowater", "Clear Water", "Removes the blue overlay when you are underwater.", "Visual", "H2O").fresh());
        reg(new Module("hitbox", "Hitboxes", "Shows entity hitboxes (like F3 + B).", "Visual", "BOX").fresh());
        reg(new Module("nohurtcam", "No Hurt Camera", "No camera shake when you take damage.", "Visual", "HURT").fresh());
        reg(new Module("nobob", "No View Bobbing", "Turns off the swaying of the view while walking.", "Visual", "BOB").fresh());
        reg(new Module("hidebossbar", "Hide Boss Bar", "Hides boss health bars at the top of the screen.", "Visual", "BOSS").fresh());
        reg(new Module("fov", "FOV Changer", "Sets your field of view and the sprint/speed FOV effect.", "Visual", "FOV").fresh()
                .add(Setting.num("value", "Field of view", 30, 110, 90))
                .add(Setting.num("fx", "Speed FOV effect", 0, 100, 100).unit("%")));

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
                .add(Setting.num("opacity", "Background opacity %", 0, 100, 70))
                .add(Setting.bool("scores", "Show numbers", true))
                .add(Setting.color("accent", "Accent line", "255,225,74,200,0")));
    }

    private static String brandText() { return "KACZO CLIENT"; }

    private static void drawBrand(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y) {
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
        ctx.text(mc.font, brandText(), tx, y + 1, m.color("color"), true);
    }

    public static net.minecraft.resources.Identifier logo() { return LOGO; }

    private static boolean logoOn() { return byId("nick").bool("showlogo"); }
    private static String nick(Minecraft mc) { return byId("nick").bool("shownick") ? mc.getUser().getName() : ""; }

    private static void drawNick(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y) {
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
        if (!n.isEmpty()) ctx.text(mc.font, n, tx, y + 1, m.color("color"), true);
    }

    private static int[] potionSize(Minecraft mc) {
        int n = mc.player == null ? 0 : mc.player.getActiveEffects().size();
        return new int[]{110, Math.max(10, n * 10)};
    }

    private static void drawPotions(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y) {
        if (mc.player == null) return;
        Module m = byId("potions");
        int i = 0;
        for (MobEffectInstance e : mc.player.getActiveEffects()) {
            int secs = e.isInfiniteDuration() ? -1 : e.getDuration() / 20;
            String t = e.getEffect().value().getDisplayName().getString() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "")
                    + (secs < 0 ? "" : String.format(" %d:%02d", secs / 60, secs % 60));
            ctx.text(mc.font, t, x, y + i * 10, m.color("color"), true);
            i++;
        }
    }

    // ───────────── rysowanie niestandardowych modułów ─────────────

    private static void key(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y, int w, int h, String label, boolean down) {
        Module km = byId("keystrokes");
        Ui.box(ctx, x, y, w, h, down ? (0xD0000000 | (km.color("color") & 0xFFFFFF)) : km.bg());
        int tw = mc.font.width(label);
        ctx.text(mc.font, label, x + (w - tw) / 2, y + (h - 8) / 2, down ? 0xFF000000 : 0xFFFFFFFF, false);
    }

    private static void drawKeys(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y) {
        var o = mc.options;
        key(ctx, mc, x + 18, y, 16, 16, "W", o.keyUp.isDown());
        key(ctx, mc, x, y + 18, 16, 16, "A", o.keyLeft.isDown());
        key(ctx, mc, x + 18, y + 18, 16, 16, "S", o.keyDown.isDown());
        key(ctx, mc, x + 36, y + 18, 16, 16, "D", o.keyRight.isDown());
        key(ctx, mc, x, y + 36, 25, 16, "LMB", Cps.leftDown());
        key(ctx, mc, x + 27, y + 36, 25, 16, "RMB", Cps.rightDown());
    }

    private static void drawArmor(GuiGraphicsExtractor ctx, Minecraft mc, int x, int y) {
        if (mc.player == null) return;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            ItemStack s = mc.player.getItemBySlot(slots[i]);
            int yy = y + i * 17;
            if (s.isEmpty()) continue;
            ctx.item(s, x, yy);
            if (s.isDamageableItem()) {
                String d = String.valueOf(s.getMaxDamage() - s.getDamageValue());
                ctx.text(mc.font, d, x + 20, yy + 4, 0xFFFFFFFF, true);
            }
        }
    }

    // ───────────── logika co tick ─────────────

    public static void tick(Minecraft mc) {
        try { Music.tick(mc); } catch (Throwable ignored) { /* muzyka nie może wywrócić gry */ }
        Cps.tick(mc);
        try { Extras.tick(mc); } catch (Throwable ignored) { }
        if (mc.player == null) { zooming = false; nvAdded = false; hadPlayer = false; return; }
        if (!hadPlayer) { hadPlayer = true; joinStart = System.currentTimeMillis(); }
        tick++;

        if (MUSIC.enabled()) NowPlaying.start();

        // Zoom (trzymaj C)
        boolean want = ZOOM.enabled() && mc.screen == null
                && Compat.keyDown(mc, GLFW.GLFW_KEY_C);
        if (want && !zooming) { savedFov = mc.options.fov().get(); zooming = true; }
        if (want) mc.options.fov().set(Math.max(30, Math.min(110, ZOOM.num("fov"))));
        if (!want && zooming) { mc.options.fov().set(savedFov); zooming = false; }

        // FOV Changer (nie podczas zoomu)
        Module fv = byId("fov");
        if (fv.enabled() && !zooming) {
            if (!fovSaved) { fovBefore = mc.options.fov().get(); fxBefore = mc.options.fovEffectScale().get(); fovSaved = true; }
            mc.options.fov().set(Math.max(30, Math.min(110, fv.num("value"))));
            mc.options.fovEffectScale().set(Math.max(0, Math.min(100, fv.num("fx"))) / 100.0);
        } else if (!fv.enabled() && fovSaved) {
            mc.options.fov().set(fovBefore);
            mc.options.fovEffectScale().set(fxBefore);
            fovSaved = false;
        }

        // Hitboxy
        if (byId("hitbox").enabled()) { Compat.hitboxes(mc, true); hitboxSet = true; }
        else if (hitboxSet) { Compat.hitboxes(mc, false); hitboxSet = false; }

        // Fullbright
        boolean fb = byId("fullbright").enabled();
        if (fb && tick % 40 == 0) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false, false));
            nvAdded = true;
        }
        if (!fb && nvAdded) { mc.player.removeEffect(MobEffects.NIGHT_VISION); nvAdded = false; }

        // Toggle Sneak
        if (byId("togglesneak").enabled() && mc.screen == null) {
            boolean phys = Compat.keyDown(mc,
                    net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(mc.options.keyShift).getValue());
            if (phys && !sneakHeld) sneakToggled = !sneakToggled;
            sneakHeld = phys;
            mc.options.keyShift.setDown(sneakToggled || phys);
        } else if (sneakToggled) { sneakToggled = false; mc.options.keyShift.setDown(false); }

        // Damage Flash
        float hp = mc.player.getHealth();
        if (lastHealth >= 0 && hp < lastHealth) flash = 1f;
        lastHealth = hp;
        flash = Math.max(0, flash - 0.06f);

        // Toggle Sprint
        if (byId("sprint").enabled() && mc.options.keyUp.isDown() && !mc.player.isShiftKeyDown()
                && mc.player.getFoodData().getFoodLevel() > 6) mc.player.setSprinting(true);
    }

    /** Ile ms minęło od wejścia do świata (do animacji HUD). */
    public static long sinceJoin() { return joinStart < 0 ? 100000 : System.currentTimeMillis() - joinStart; }

    public static float flash() { return flash; }

    public static Module byId(String id) {
        for (Module m : ALL) if (m.id.equals(id)) return m;
        return GENERAL;
    }
}

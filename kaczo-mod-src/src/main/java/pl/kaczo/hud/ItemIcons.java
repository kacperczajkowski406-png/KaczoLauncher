package pl.kaczo.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

/** Zamiast literek moduły mają ikony – przedmioty z gry pasujące do ich działania. */
final class ItemIcons {
    private ItemIcons() { }

    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    private static Item item(String id) {
        return switch (id) {
            case "fps" -> Items.COMPARATOR;      case "fpsmax" -> Items.REPEATER;
            case "ping" -> Items.ECHO_SHARD;     case "coords" -> Items.MAP;
            case "direction" -> Items.COMPASS;   case "rotation" -> Items.RECOVERY_COMPASS;
            case "clock", "worldtime" -> Items.CLOCK;
            case "day" -> Items.DAYLIGHT_DETECTOR;
            case "realdate" -> Items.PAPER;      case "speed" -> Items.FEATHER;
            case "biome" -> Items.OAK_SAPLING;   case "light" -> Items.TORCH;
            case "players" -> Items.PLAYER_HEAD; case "session" -> Items.WRITABLE_BOOK;
            case "memory" -> Items.REDSTONE;     case "server" -> Items.BEACON;
            case "itemcount" -> Items.CHEST;     case "inventory" -> Items.ENDER_CHEST;
            case "cps" -> Items.STONE_BUTTON;    case "keystrokes" -> Items.LEVER;
            case "mousestrokes" -> Items.SLIME_BALL;
            case "armor" -> Items.IRON_CHESTPLATE; case "potions" -> Items.POTION;
            case "nick" -> Items.NAME_TAG;       case "health" -> Items.APPLE;
            case "hunger" -> Items.COOKED_BEEF;  case "xp" -> Items.EXPERIENCE_BOTTLE;
            case "chunk" -> Items.GRASS_BLOCK;   case "helditem" -> Items.IRON_PICKAXE;
            case "brand" -> Items.CRAFTING_TABLE; case "music" -> Items.JUKEBOX;
            case "reach" -> Items.ARROW;         case "combo" -> Items.BLAZE_POWDER;
            case "pvpitems" -> Items.DIAMOND_SWORD; case "deathinfo" -> Items.SKELETON_SKULL;
            case "waypoint" -> Items.LODESTONE;  case "freelook" -> Items.ENDER_EYE;
            case "sky", "shaders" -> Items.AMETHYST_SHARD;  case "theme" -> Items.PAINTING;
            case "nofire" -> Items.MAGMA_CREAM;  case "nowater" -> Items.WATER_BUCKET;
            case "hitbox" -> Items.IRON_BARS;    case "nohurtcam" -> Items.SHIELD;
            case "nobob" -> Items.RABBIT_FOOT;   case "hidebossbar" -> Items.DRAGON_HEAD;
            case "fov" -> Items.GLASS;           case "crosshair" -> Items.TARGET;
            case "zoom" -> Items.SPYGLASS;       case "fullbright" -> Items.GLOWSTONE;
            case "sprint" -> Items.GOLDEN_BOOTS; case "togglesneak" -> Items.LEATHER_BOOTS;
            case "hitflash" -> Items.REDSTONE_TORCH; case "scoreboard" -> Items.OAK_SIGN;
            default -> Items.PAPER;
        };
    }

    /** Rysuje ikonę przedmiotu (16×16) wyśrodkowaną w (cx, cy), skalowaną do {@code size} pikseli GUI. */
    static void draw(DrawContext ctx, String id, double cx, double cy, float size) {
        ItemStack st = CACHE.computeIfAbsent(id, k -> new ItemStack(item(k)));
        float sc = size / 16f;
        Compat.push(ctx);
        Compat.translate(ctx, (float) (cx - size / 2.0), (float) (cy - size / 2.0));
        Compat.scale(ctx, sc, sc);
        ctx.drawItem(st, 0, 0);
        Compat.pop(ctx);
    }
}

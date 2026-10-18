package pl.kaczo.hud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Bazowy ekran moda: ujednolica obsługę myszy i klawiatury (od Minecrafta 1.21.9 podpisy metod są inne,
 * dlatego ta klasa ma osobną wersję w każdym wariancie kompatybilności).
 */
public abstract class KScreen extends Screen {
    protected KScreen(Text title) { super(title); }

    protected boolean onClick(double x, double y, int button) { return false; }
    protected boolean onRelease(double x, double y, int button) { return false; }
    protected boolean onDrag(double x, double y, int button, double dx, double dy) { return false; }
    protected boolean onScroll(double x, double y, double amount) { return false; }
    protected boolean onKey(int key, int scan, int mods) { return false; }
    protected boolean onChar(int codePoint) { return false; }

    @Override public boolean mouseClicked(double x, double y, int button) { return onClick(x, y, button) || super.mouseClicked(x, y, button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return onRelease(x, y, button) || super.mouseReleased(x, y, button); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return onDrag(x, y, button, dx, dy) || super.mouseDragged(x, y, button, dx, dy); }
    @Override public boolean mouseScrolled(double x, double y, double h, double v) { return onScroll(x, y, v) || super.mouseScrolled(x, y, h, v); }
    @Override public boolean keyPressed(int key, int scan, int mods) { return onKey(key, scan, mods) || super.keyPressed(key, scan, mods); }
    @Override public boolean charTyped(char chr, int mods) { return onChar(chr) || super.charTyped(chr, mods); }
}

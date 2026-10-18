package pl.kaczo.hud;

import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Bazowy ekran moda – wariant dla 1.21.9+ (obiekty Click / KeyInput / CharInput). */
public abstract class KScreen extends Screen {
    protected KScreen(Text title) { super(title); }

    protected boolean onClick(double x, double y, int button) { return false; }
    protected boolean onRelease(double x, double y, int button) { return false; }
    protected boolean onDrag(double x, double y, int button, double dx, double dy) { return false; }
    protected boolean onScroll(double x, double y, double amount) { return false; }
    protected boolean onKey(int key, int scan, int mods) { return false; }
    protected boolean onChar(int codePoint) { return false; }

    @Override public boolean mouseClicked(Click c, boolean doubled) { return onClick(c.x(), c.y(), c.button()) || super.mouseClicked(c, doubled); }
    @Override public boolean mouseReleased(Click c) { return onRelease(c.x(), c.y(), c.button()) || super.mouseReleased(c); }
    @Override public boolean mouseDragged(Click c, double dx, double dy) { return onDrag(c.x(), c.y(), c.button(), dx, dy) || super.mouseDragged(c, dx, dy); }
    @Override public boolean mouseScrolled(double x, double y, double h, double v) { return onScroll(x, y, v) || super.mouseScrolled(x, y, h, v); }
    @Override public boolean keyPressed(KeyInput k) { return onKey(k.key(), k.scancode(), k.modifiers()) || super.keyPressed(k); }
    @Override public boolean charTyped(CharInput c) { return onChar(c.codepoint()) || super.charTyped(c); }
}

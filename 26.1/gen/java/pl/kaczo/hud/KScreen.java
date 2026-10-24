package pl.kaczo.hud;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Bazowy ekran moda – wariant 26.x (zdarzenia MouseButtonEvent / KeyEvent / CharacterEvent). */
public abstract class KScreen extends Screen {
    protected KScreen(Component title) { super(title); }

    protected boolean onClick(double x, double y, int button) { return false; }
    protected boolean onRelease(double x, double y, int button) { return false; }
    protected boolean onDrag(double x, double y, int button, double dx, double dy) { return false; }
    protected boolean onScroll(double x, double y, double amount) { return false; }
    protected boolean onKey(int key, int scan, int mods) { return false; }
    protected boolean onChar(int codePoint) { return false; }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubled) { return onClick(e.x(), e.y(), e.button()) || super.mouseClicked(e, doubled); }
    @Override public boolean mouseReleased(MouseButtonEvent e) { return onRelease(e.x(), e.y(), e.button()) || super.mouseReleased(e); }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { return onDrag(e.x(), e.y(), e.button(), dx, dy) || super.mouseDragged(e, dx, dy); }
    @Override public boolean mouseScrolled(double x, double y, double h, double v) { return onScroll(x, y, v) || super.mouseScrolled(x, y, h, v); }
    @Override public boolean keyPressed(KeyEvent k) { return onKey(k.key(), k.scancode(), k.modifiers()) || super.keyPressed(k); }
    @Override public boolean charTyped(CharacterEvent c) { return onChar(c.codepoint()) || super.charTyped(c); }
}

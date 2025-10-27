package com.stockportfolio.utils;

import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class KeyboardShortcutManager implements KeyListener {
    private Map<KeyStroke, Runnable> shortcuts;

    public KeyboardShortcutManager() {
        shortcuts = new HashMap<>();
    }

    public void registerShortcut(int keyCode, int modifiers, Runnable action) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        shortcuts.put(keyStroke, action);
    }

    public void registerShortcut(int keyCode, Runnable action) {
        registerShortcut(keyCode, 0, action);
    }

    @Override
    public void keyPressed(java.awt.event.KeyEvent e) {
        KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
        Runnable action = shortcuts.get(keyStroke);
        if (action != null) {
            action.run();
            e.consume();
        }
    }

    @Override
    public void keyReleased(java.awt.event.KeyEvent e) {
        // No action needed
    }

    @Override
    public void keyTyped(java.awt.event.KeyEvent e) {
        // No action needed
    }

    public void attachToComponent(JComponent component) {
        component.addKeyListener(this);
        component.setFocusable(true);
        component.requestFocusInWindow();
    }
}

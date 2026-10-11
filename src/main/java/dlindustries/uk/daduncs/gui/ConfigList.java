/*
 * This file is part of System Client (https://github.com/daduncs/system)
 * Website: https://download.daduncs.cloud/
 *
 * Copyright (c) 2024 - 2026 DLindustries and project contributors.
 *
 * System Client is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * System Client is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with System Client. If not, see <https://www.gnu.org/licenses/>.
 */

package dlindustries.uk.daduncs.gui;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.managers.ConfigManager;
import dlindustries.uk.daduncs.managers.ConfigManager.Config;
import dlindustries.uk.daduncs.render.Render2D;
import dlindustries.uk.daduncs.utils.Sounds;
import dlindustries.uk.daduncs.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static dlindustries.uk.daduncs.System.mc;

public final class ConfigList {

    private static final Identifier BIN = Identifier.of("system", "textures/gui/delete.png");
    private static final Identifier EXPORT = Identifier.of("system", "textures/gui/export.png");
    private static final Identifier UNKNOWN_SERVER = Identifier.ofVanilla("textures/misc/unknown_server.png");
    private static final Identifier SAVE = Identifier.of("system", "textures/gui/save.png");
    private static final Identifier IMPORT = Identifier.of("system", "textures/gui/download.png");
    private static final Identifier RESET = Identifier.of("system", "textures/gui/reset.png");

    private static final int PAD = 14;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int BUTTON_WIDTH = 19;

    private static final int ACTION_HEIGHT = 26;
    private static final int ACTION_GAP = 6;
    private static final float ACTION_RADIUS = 9f;
    private static final int ACTION_FILL = 0xDB1C1C1F;
    private static final int ACTION_FILL_HOVER = 0xDB27272B;
    private static final int DANGER = 0xFFFF8A8A;

    private static final long CONFIRM_MS = 3000L;
    private static final long FEEDBACK_MS = 1600L;

    private final List<Hit> hits = new ArrayList<>();

    private final Anim saveHover = new Anim(0f, 12f);
    private final Anim importHover = new Anim(0f, 12f);
    private final Anim resetHover = new Anim(0f, 12f);
    private final Sprite saveCheck = new Sprite("checkmark");
    private final Sprite importCheck = new Sprite("checkmark");
    private final Sprite resetCheck = new Sprite("checkmark");

    private int actionX;
    private int actionY;
    private int actionWidth;
    private int resetWidth;

    private long savedAt;
    private long importedAt;
    private long importFailedAt;
    private long resetArmedAt;
    private long resetDoneAt;

    private String selected;
    private String lastClicked;
    private long lastClickAt;

    private String renaming;
    private String text = "";
    private int cursor;
    private boolean highlighted;

    private String copied;
    private long copiedAt;

    public int height() {
        return ACTION_HEIGHT + ACTION_GAP + manager().getConfigs().size() * (ROW_HEIGHT + ROW_GAP);
    }

    public boolean renaming() {
        return renaming != null;
    }

    public void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float alpha) {
        hits.clear();

        actionX = x + PAD;
        actionY = y;
        int totalWidth = width - PAD * 2;
        actionWidth = (totalWidth - ACTION_GAP * 2) / 3;
        resetWidth = totalWidth - (actionWidth + ACTION_GAP) * 2;
        renderActions(context, mouseX, mouseY, alpha);

        y += ACTION_HEIGHT + ACTION_GAP;

        for (Config config : manager().getConfigs()) {
            renderRow(context, config, x + PAD, y, width - PAD * 2, mouseX, mouseY, alpha);
            y += ROW_HEIGHT + ROW_GAP;
        }
    }

    private int importX() {
        return actionX + actionWidth + ACTION_GAP;
    }

    private int resetX() {
        return actionX + (actionWidth + ACTION_GAP) * 2;
    }

    private void renderActions(DrawContext context, int mouseX, int mouseY, float alpha) {
        long now = java.lang.System.currentTimeMillis();
        int impX = importX();
        int resX = resetX();

        saveHover.to(overAction(actionX, actionWidth, mouseX, mouseY) ? 1f : 0f);
        saveHover.update(0f);
        importHover.to(overAction(impX, actionWidth, mouseX, mouseY) ? 1f : 0f);
        importHover.update(0f);
        resetHover.to(overAction(resX, resetWidth, mouseX, mouseY) ? 1f : 0f);
        resetHover.update(0f);

        boolean saved = now - savedAt < FEEDBACK_MS;
        String saveText = saved ? "Saved" : (actionWidth < 95 ? "Save" : "Save Config");
        renderAction(context, actionX, actionWidth, saved ? saveCheck.get() : SAVE, saveText,
                saved ? Theme.accent : Theme.text, saveHover.get(), alpha);

        boolean importing = manager().isImporting();
        boolean imported = now - importedAt < FEEDBACK_MS;
        boolean importFailed = now - importFailedAt < FEEDBACK_MS;
        String importText = importing ? "Browsing..." : imported ? "Imported" : importFailed ? "Failed" : (actionWidth < 95 ? "Import" : "Import Config");
        int importColour = imported ? Theme.accent : importFailed ? DANGER : importing ? Theme.accent : Theme.text;
        renderAction(context, impX, actionWidth, imported ? importCheck.get() : IMPORT, importText,
                importColour, importing ? 1f : importHover.get(), alpha);

        boolean armed = now - resetArmedAt < CONFIRM_MS;
        boolean done = now - resetDoneAt < FEEDBACK_MS;
        String resText = done ? "Reset" : armed ? (resetWidth < 95 ? "Confirm" : "Click to confirm") : (resetWidth < 95 ? "Reset" : "Reset Config");
        int resColour = done ? Theme.accent : armed ? DANGER : Theme.text;
        renderAction(context, resX, resetWidth, done ? resetCheck.get() : RESET, resText,
                resColour, armed ? 1f : resetHover.get(), alpha);
    }

    private void renderAction(DrawContext context, int x, int width, Identifier icon, String text, int colour, float hover, float alpha) {
        int fill = Theme.lerp(ACTION_FILL, ACTION_FILL_HOVER, hover);
        Render2D.drawBlurRoundRect(context, x, actionY, width, ACTION_HEIGHT, ACTION_RADIUS, 1f, Theme.BLUR * alpha, fill, alpha);

        int labelColour = Theme.fade(Theme.lerp(Theme.fade(colour, 0.85f), colour, hover), alpha);
        float centerX = x + width / 2f;
        float centerY = actionY + ACTION_HEIGHT / 2f;
        float contentWidth = 18f + TextRenderer.buttonWidth(text);
        float left = centerX - contentWidth / 2f;
        if (left < x + 4f) left = x + 4f;

        Render2D.drawRoundImage(context, left, centerY - 6f, 12f, 12f, 0f, icon, labelColour);
        TextRenderer.drawButton(text, context, left + 18f, centerY, labelColour);
    }

    private boolean overAction(int x, int width, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= actionY && mouseY <= actionY + ACTION_HEIGHT;
    }

    private void renderRow(DrawContext context, Config config, int x, int y, int width, int mouseX, int mouseY, float alpha) {
        Hit hit = new Hit(config, x, y, width);
        hits.add(hit);

        boolean overBin = hit.overBin(mouseX, mouseY);
        boolean overExport = hit.overExport(mouseX, mouseY);
        boolean overRow = !overBin && !overExport && hit.overRow(mouseX, mouseY);

        int background = Theme.field;
        if (config.name().equals(selected))
            background = Theme.lerp(Theme.field, Theme.accent, 0.26f);
        else if (overRow)
            background = Theme.lerp(Theme.field, Theme.cardHover, 0.4f);

        Render2D.drawRoundRect(context, x, y, width, ROW_HEIGHT, 6f, Theme.fade(background, alpha * 0.8f));

        Identifier icon = manager().getIcon(config);
        Render2D.drawRoundImage(context, x + 4, y + 3, 14, 14, 4f, icon != null ? icon : UNKNOWN_SERVER, Theme.fade(0xFFFFFFFF, alpha));

        int textX = x + 24;
        if (config.name().equals(renaming)) {
            renderField(context, textX, y, alpha);
        } else if (config.name().equals(copied) && java.lang.System.currentTimeMillis() - copiedAt < 1600) {
            TextRenderer.drawSmall("Config copied to clipboard", context, textX, y + 9, Theme.fade(Theme.accent, alpha));
        } else {
            int colour = manager().isActive(config) ? Theme.accent : Theme.text;
            TextRenderer.drawSmall(config.name(), context, textX, y + 9, Theme.fade(colour, alpha));
        }

        icon(context, EXPORT, hit.exportX(), y, overExport, Theme.fade(overExport ? Theme.text : Theme.muted, alpha));
        icon(context, BIN, hit.binX(), y, overBin, overBin ? Theme.fade(0xFFFF7A7A, alpha * 0.85f) : Theme.fade(Theme.muted, alpha));
    }

    private void renderField(DrawContext context, int x, int y, float alpha) {
        if (highlighted && !text.isEmpty())
            Render2D.drawRoundRect(context, x - 2, y + 4, TextRenderer.smallWidth(text) + 4, ROW_HEIGHT - 8, 3f, Theme.fade(Theme.accent, alpha * 0.35f));

        TextRenderer.drawSmall(text, context, x, y + 9, Theme.fade(Theme.text, alpha));

        if (!highlighted && java.lang.System.currentTimeMillis() / 500 % 2 == 0) {
            int caret = x + TextRenderer.smallWidth(text.substring(0, cursor));
            Render2D.drawRoundRect(context, caret, y + 5, 1, ROW_HEIGHT - 10, 0f, Theme.fade(Theme.text, alpha));
        }
    }

    private static void icon(DrawContext context, Identifier texture, int x, int y, boolean hovered, int colour) {
        float size = hovered ? 15f : 13f;
        Render2D.drawRoundImage(context, x + (BUTTON_WIDTH - size) / 2f, y + (ROW_HEIGHT - size) / 2f, size, size, 0f, texture, colour);
    }

    public boolean click(int mouseX, int mouseY) {
        if (overAction(actionX, actionWidth, mouseX, mouseY)) {
            finishRename();
            save();
            return true;
        }

        if (overAction(importX(), actionWidth, mouseX, mouseY)) {
            finishRename();
            importFile();
            return true;
        }

        if (overAction(resetX(), resetWidth, mouseX, mouseY)) {
            finishRename();
            reset();
            return true;
        }

        for (Hit hit : hits) {
            if (hit.overBin(mouseX, mouseY)) {
                finishRename();
                delete(hit.config);
                return true;
            }

            if (hit.overExport(mouseX, mouseY)) {
                finishRename();
                export(hit.config);
                return true;
            }

            if (hit.overRow(mouseX, mouseY))
                return clickRow(hit.config);
        }

        finishRename();
        selected = null;
        return false;
    }

    public void save() {
        Sounds.play(Sounds.CLICK);
        System.INSTANCE.getProfileManager().saveProfile();
        manager().save();

        savedAt = java.lang.System.currentTimeMillis();
        saveCheck.play();
        String active = manager().getActiveName();
        Notice.show("Saved config" + (active.isEmpty() ? "" : ": " + active));
    }

    private void importFile() {
        if (manager().isImporting()) return;
        Sounds.play(Sounds.CLICK);
        manager().openImportDialog((success, info) -> {
            if (success) {
                importedAt = java.lang.System.currentTimeMillis();
                importCheck.play();
                Sounds.play(Sounds.TOGGLE);
                Notice.show("Imported config: " + info);
            } else if (info != null && !info.isBlank()) {
                importFailedAt = java.lang.System.currentTimeMillis();
                Sounds.play(Sounds.CLICK);
                Notice.show(info);
            }
        });
    }

    private void reset() {
        long now = java.lang.System.currentTimeMillis();

        if (now - resetArmedAt >= CONFIRM_MS) {
            resetArmedAt = now;
            Sounds.play(Sounds.CLICK);
            return;
        }

        resetArmedAt = 0L;
        manager().reset();
        manager().save();

        resetDoneAt = now;
        resetCheck.play();
        Notice.show("Config reset to defaults");
    }

    private boolean clickRow(Config config) {
        String name = config.name();

        if (name.equals(renaming)) {
            highlighted = false;
            return true;
        }

        finishRename();

        long now = java.lang.System.currentTimeMillis();
        boolean doubled = name.equals(lastClicked) && now - lastClickAt < 350;
        lastClicked = name;
        lastClickAt = now;

        if (doubled) {
            renaming = name;
            text = name;
            cursor = name.length();
            highlighted = true;
            Sounds.play(Sounds.CLICK);
            return true;
        }

        selected = name;
        boolean loaded = manager().apply(name);
        if (loaded) {
            Sounds.play(Sounds.CLICK);
            Notice.show("Loaded config: " + name);
        } else {
            Sounds.play(Sounds.CLICK);
            Notice.show("Failed to load config: " + name);
        }
        return true;
    }

    public boolean pressed(int key, int modifiers) {
        if (renaming != null)
            return renameKey(key, (modifiers & GLFW.GLFW_MOD_CONTROL) != 0);

        if (key != GLFW.GLFW_KEY_DELETE || selected == null)
            return false;

        Config config = find(selected);
        if (config == null) {
            selected = null;
            return false;
        }

        delete(config);
        return true;
    }

    public boolean type(String typed) {
        if (renaming == null)
            return false;

        if (highlighted)
            clear();

        text = text.substring(0, cursor) + typed + text.substring(cursor);
        cursor += typed.length();
        Sounds.play(Sounds.KEYPRESS, 0.6f);
        return true;
    }

    private boolean renameKey(int key, boolean control) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                renaming = null;
                Sounds.play(Sounds.COLLAPSE);
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> finishRename();
            case GLFW.GLFW_KEY_A -> highlighted = highlighted || control;
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (highlighted) {
                    clear();
                } else if (control) {
                    int start = wordStart();
                    text = text.substring(0, start) + text.substring(cursor);
                    cursor = start;
                } else if (cursor > 0) {
                    text = text.substring(0, cursor - 1) + text.substring(cursor);
                    cursor--;
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (highlighted)
                    clear();
                else if (cursor < text.length())
                    text = text.substring(0, cursor) + text.substring(cursor + 1);
            }
            case GLFW.GLFW_KEY_LEFT -> moveCursor(highlighted ? 0 : cursor - 1);
            case GLFW.GLFW_KEY_RIGHT -> moveCursor(highlighted ? text.length() : cursor + 1);
            case GLFW.GLFW_KEY_HOME -> moveCursor(0);
            case GLFW.GLFW_KEY_END -> moveCursor(text.length());
            default -> {
            }
        }

        return true;
    }

    private void finishRename() {
        if (renaming == null)
            return;

        Config config = find(renaming);
        String renamed = config != null ? manager().rename(config, text) : null;
        if (renamed != null) {
            selected = renamed;
            Notice.show("Renamed config to: " + renamed);
        }

        renaming = null;
    }

    private void moveCursor(int position) {
        cursor = Math.clamp(position, 0, text.length());
        highlighted = false;
    }

    private void clear() {
        text = "";
        cursor = 0;
        highlighted = false;
    }

    private int wordStart() {
        int i = cursor;
        while (i > 0 && text.charAt(i - 1) == ' ')
            i--;
        while (i > 0 && text.charAt(i - 1) != ' ')
            i--;
        return i;
    }

    private void delete(Config config) {
        if (config.name().equals(selected))
            selected = null;

        manager().delete(config);
        Sounds.play(Sounds.CLICK);
        Notice.show("Deleted config: " + config.name());
    }

    private void export(Config config) {
        String json = manager().export(config);
        if (json == null)
            return;

        mc.keyboard.setClipboard(json);
        copied = config.name();
        copiedAt = java.lang.System.currentTimeMillis();
        Sounds.play(Sounds.CLICK);
        Notice.show("Copied " + config.name() + " to clipboard");
    }

    private Config find(String name) {
        for (Config config : manager().getConfigs())
            if (config.name().equals(name))
                return config;

        return null;
    }

    private static ConfigManager manager() {
        return System.INSTANCE.getConfigManager();
    }

    private record Hit(Config config, int x, int y, int width) {

        int binX() {
            return x + width - 4 - BUTTON_WIDTH;
        }

        int exportX() {
            return binX() - BUTTON_WIDTH;
        }

        boolean overBin(int mouseX, int mouseY) {
            return inside(binX(), mouseX, mouseY);
        }

        boolean overExport(int mouseX, int mouseY) {
            return inside(exportX(), mouseX, mouseY);
        }

        boolean overRow(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT;
        }

        private boolean inside(int buttonX, int mouseX, int mouseY) {
            return mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH && mouseY >= y + 1 && mouseY <= y + ROW_HEIGHT - 1;
        }
    }
}

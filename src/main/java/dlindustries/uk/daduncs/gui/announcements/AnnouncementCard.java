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

package dlindustries.uk.daduncs.gui.announcements;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.font.Fonts;
import dlindustries.uk.daduncs.gui.Anim;
import dlindustries.uk.daduncs.gui.Card;
import dlindustries.uk.daduncs.gui.Frame;
import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.managers.UpdateManager;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.ModuleManager;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.module.setting.*;
import dlindustries.uk.daduncs.render.Render2D;
import dlindustries.uk.daduncs.utils.Sounds;
import dlindustries.uk.daduncs.utils.TextRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

public final class AnnouncementCard {

    private static final Identifier LOGO = Identifier.of("system", "textures/gui/client_icon.png");
    private static final Identifier CHEVRON = Identifier.of("system", "textures/gui/chevron.png");
    private static final Identifier REFRESH = Identifier.of("system", "textures/gui/refresh.png");
    private static final Identifier DOWNLOAD = Identifier.of("system", "textures/gui/download.png");
    private static final Identifier SUPPORT = Identifier.of("system", "textures/gui/support.png");

    private static final int HEAD = Card.HEAD;
    private static final int PADDING = 12;
    private static final int BUTTON_HEIGHT = 26;
    private static final int BUTTON_GAP = 6;
    private static final float BUTTON_RADIUS = 9f;
    private static final float ICON_SIZE = 13f;
    private static final float LABEL_ICON_SIZE = 12f;

    private static final int BUTTON_FILL = 0xDB1C1C1F;
    private static final int BUTTON_FILL_HOVER = 0xDB27272B;

    private final Anim openAnim = new Anim(0f, 9f);
    private final Anim updateAnim = new Anim(0f, 9f);
    private final Anim headHover = new Anim(0f, 10f);
    private final Anim chevronHover = new Anim(0f, 14f);
    private final Anim refreshHover = new Anim(0f, 14f);
    private final Anim updateHover = new Anim(0f, 12f);
    private final Anim discordHover = new Anim(0f, 12f);

    private boolean open;
    private boolean wasHovered;
    private float refreshTurn;
    private float refreshTarget;

    public int height() {
        return Math.round(HEAD + ease(openAnim.get()) * getBodyHeight());
    }

    public void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float delta, float alpha) {
        UpdateManager manager = getManager();
        boolean hasUpdate = manager != null && manager.hasUpdate();

        openAnim.to(open ? 1f : 0f);
        openAnim.update(delta);
        updateAnim.to(hasUpdate ? 1f : 0f);
        updateAnim.update(delta);

        Layout layout = new Layout(x, y, width);
        int height = height();
        float shown = ease(openAnim.get());

        boolean headHovered = layout.head.isInside(mouseX, mouseY);
        if (headHovered && !wasHovered) {
            Sounds.hover();
        }

        wasHovered = headHovered;
        headHover.to(headHovered ? 1f : 0f);
        headHover.update(delta);

        int accent = legible(Theme.accent);
        int background = Theme.lerp(Theme.card, Theme.cardHover, headHover.get());
        Render2D.drawBlurRoundRect(context, x, y, width, height, Theme.CARD_ROUND, 1f, Theme.BLUR * alpha, background, alpha);

        renderHead(context, layout, x, y, mouseX, mouseY, delta, alpha, accent);

        if (shown <= 0.01f) {
            return;
        }

        context.enableScissor(x, y + HEAD - 2, x + width, y + height);
        Render2D.drawRoundRect(context, x + PADDING, y + HEAD - 2, width - PADDING * 2, 1, 0.5f, Theme.fade(Theme.divider, alpha * shown));
        renderBody(context, layout, mouseX, mouseY, delta, alpha * shown, accent);
        context.disableScissor();
    }

    private void renderHead(DrawContext context, Layout layout, int x, int y, int mouseX, int mouseY, float delta, float alpha, int accent) {
        UpdateManager manager = getManager();
        float shown = ease(openAnim.get());

        Render2D.drawRoundImage(context, x + 13, y + (HEAD - 18) / 2f, 18, 18, 0f, LOGO, Theme.fade(accent, alpha));

        String title = manager != null ? manager.getPageTitle() : "About System";
        TextRenderer.drawLabelMenu(title, context, x + 37, y + HEAD / 2 - 1, Theme.fade(Theme.text, alpha));

        if (layout.tag != null) {
            Box tag = layout.tag;
            Render2D.drawRoundRect(context, tag.x, tag.y, tag.width, tag.height, 6.5f, Theme.fade(accent, alpha * 0.22f));
            TextRenderer.drawTiny("UPDATE", context, (int) (tag.x + 6), y + HEAD / 2 - 1, Theme.fade(accent, alpha));
        }

        boolean chevronHovered = layout.chevron.isInside(mouseX, mouseY);
        chevronHover.to(chevronHovered ? 1f : 0f);
        chevronHover.update(delta);

        if (chevronHover.get() > 0.01f) {
            Render2D.drawCircle(context, layout.chevron.getCenterX(), layout.chevron.getCenterY(), 11f,
                    Theme.fade(Theme.field, alpha * 0.55f * chevronHover.get()));
        }

        int chevronColour = Theme.lerp(Theme.muted, Theme.text, Math.max(chevronHover.get(), headHover.get() * 0.5f));
        drawIcon(context, CHEVRON, layout.chevron.getCenterX(), layout.chevron.getCenterY(), (float) Math.PI * ease(openAnim.get()), Theme.fade(chevronColour, alpha));

        if (shown <= 0.01f) {
            return;
        }

        boolean loading = manager != null && manager.isLoading();
        boolean refreshHovered = shown > 0.6f && layout.refresh.isInside(mouseX, mouseY);
        refreshHover.to(refreshHovered ? 1f : 0f);
        refreshHover.update(delta);

        if (refreshHover.get() > 0.01f) {
            Render2D.drawCircle(context, layout.refresh.getCenterX(), layout.refresh.getCenterY(), 11f,
                    Theme.fade(Theme.field, alpha * shown * 0.55f * refreshHover.get()));
        }

        if (loading) {
            refreshTurn += Frame.delta() * 1.6f;
            refreshTarget = (float) Math.ceil(refreshTurn);
        } else {
            refreshTurn += (refreshTarget - refreshTurn) * (1f - (float) Math.exp(-9f * Frame.delta()));
        }

        int refreshColour = loading ? accent : Theme.lerp(Theme.muted, Theme.text, refreshHover.get());
        float refreshAngle = refreshTurn * (float) Math.PI * 2f;
        drawIcon(context, REFRESH, layout.refresh.getCenterX(), layout.refresh.getCenterY(), refreshAngle, Theme.fade(refreshColour, alpha * shown));
    }

    private void renderBody(DrawContext context, Layout layout, int mouseX, int mouseY, float delta, float alpha, int accent) {
        boolean interactive = openAnim.get() > 0.6f;

        renderUpdateButton(context, layout.update, mouseX, mouseY, delta, alpha, accent, interactive);
        renderDiscordButton(context, layout.discord, mouseX, mouseY, delta, alpha, interactive);
    }

    private void renderUpdateButton(DrawContext context, Box box, int mouseX, int mouseY, float delta, float alpha, int accent, boolean interactive) {
        float progress = updateAnim.get();
        if (progress <= 0.01f) {
            return;
        }

        UpdateManager manager = getManager();
        boolean hovered = interactive && manager != null && manager.hasUpdate() && box.isInside(mouseX, mouseY);
        updateHover.to(hovered ? 1f : 0f);
        updateHover.update(delta);

        String latest = manager != null ? manager.getLatestVersion().trim() : "";
        String text = latest.isEmpty() ? "Update" : "Update to " + latest;
        renderButton(context, box, DOWNLOAD, text, accent, updateHover.get(), alpha * progress);
    }

    private void renderDiscordButton(DrawContext context, Box box, int mouseX, int mouseY, float delta, float alpha, boolean interactive) {
        boolean hovered = interactive && box.isInside(mouseX, mouseY);
        discordHover.to(hovered ? 1f : 0f);
        discordHover.update(delta);

        renderButton(context, box, SUPPORT, "Support / Our Discord", Theme.text, discordHover.get(), alpha);
    }

    private void renderButton(DrawContext context, Box box, Identifier icon, String text, int colour, float hover, float alpha) {
        int fill = Theme.lerp(BUTTON_FILL, BUTTON_FILL_HOVER, hover);
        Render2D.drawBlurRoundRect(context, box.x, box.y, box.width, box.height, BUTTON_RADIUS, 1f, Theme.BLUR * alpha, fill, alpha);

        int labelColour = Theme.lerp(Theme.fade(colour, 0.85f), colour, hover);
        drawLabel(context, icon, text, box.getCenterX(), box.getCenterY(), Theme.fade(labelColour, alpha));
    }

    public boolean click(int x, int y, int width, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        Layout layout = new Layout(x, y, width);
        UpdateManager manager = getManager();
        boolean interactive = openAnim.get() > 0.6f;

        if (interactive && layout.refresh.isInside(mouseX, mouseY)) {
            Sounds.play(Sounds.CLICK);
            if (manager != null) {
                manager.fetchAsync();
            }
            refreshTarget = (float) Math.floor(refreshTurn) + 1f;
            return true;
        }

        if (layout.tag != null && layout.tag.isInside(mouseX, mouseY)) {
            Sounds.play(Sounds.CLICK);
            if (open && manager != null) {
                manager.openUrl(manager.getDownloadUrl());
            } else {
                open = true;
            }
            return true;
        }

        if (layout.head.isInside(mouseX, mouseY)) {
            Sounds.play(Sounds.COLLAPSE);
            open = !open;
            return true;
        }

        if (!interactive || mouseY > y + height()) {
            return false;
        }

        if (manager != null && manager.hasUpdate() && layout.update.isInside(mouseX, mouseY)) {
            Sounds.play(Sounds.CLICK);
            manager.openUrl(manager.getDownloadUrl());
            return true;
        }

        if (layout.discord.isInside(mouseX, mouseY)) {
            Sounds.play(Sounds.CLICK);
            if (manager != null) {
                manager.openUrl(UpdateManager.DISCORD_INVITE);
            }
            return true;
        }

        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height();
    }

    private UpdateManager getManager() {
        return System.INSTANCE != null ? System.INSTANCE.getUpdateManager() : null;
    }

    private float getBodyHeight() {
        return 4 + updateAnim.get() * (BUTTON_HEIGHT + BUTTON_GAP) + BUTTON_HEIGHT + PADDING;
    }

    private static int getTitleWidth(String title) {
        if (ClickGUI.modern.getValue()) {
            return Fonts.LABEL_MENU.getStringWidth(title);
        }

        return Math.round(MinecraftClient.getInstance().textRenderer.getWidth(title) * 1.35f);
    }

    private static void drawIcon(DrawContext context, Identifier texture, float centerX, float centerY, float angle, int colour) {
        if (angle == 0f) {
            Render2D.drawRoundImage(context, centerX - ICON_SIZE / 2f, centerY - ICON_SIZE / 2f, ICON_SIZE, ICON_SIZE, 0f, texture, colour);
            return;
        }

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX, centerY);
        matrices.rotate(angle);
        Render2D.drawRoundImage(context, -ICON_SIZE / 2f, -ICON_SIZE / 2f, ICON_SIZE, ICON_SIZE, 0f, texture, colour);
        matrices.popMatrix();
    }

    private static void drawLabel(DrawContext context, Identifier texture, String text, float centerX, float centerY, int colour) {
        float iconWidth = texture != null ? LABEL_ICON_SIZE + 6f : 0f;
        float left = centerX - (iconWidth + TextRenderer.buttonWidth(text)) / 2f;

        if (texture != null) {
            Render2D.drawRoundImage(context, left, centerY - LABEL_ICON_SIZE / 2f, LABEL_ICON_SIZE, LABEL_ICON_SIZE, 0f, texture, colour);
        }

        TextRenderer.drawButton(text, context, left + iconWidth, centerY, colour);
    }

    private static int legible(int colour) {
        int out = colour | 0xFF000000;

        for (int i = 0; i < 12 && luminance(out) < 0.28f; i++) {
            out = Theme.lerp(out, 0xFFFFFFFF, 0.14f);
        }

        return out;
    }

    private static float luminance(int colour) {
        return 0.2126f * linear((colour >> 16) & 0xFF) + 0.7152f * linear((colour >> 8) & 0xFF) + 0.0722f * linear(colour & 0xFF);
    }

    private static float linear(int channel) {
        float c = channel / 255f;
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }

    private static float ease(float t) {
        float c = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - c) * (1f - c) * (1f - c);
    }

    public static void resetConfig() {
        Sounds.play(Sounds.CLICK);
        if (System.INSTANCE == null || System.INSTANCE.getModuleManager() == null) {
            return;
        }

        ModuleManager moduleManager = System.INSTANCE.getModuleManager();
        for (Module module : moduleManager.getModules()) {
            if (module instanceof ClickGUI) {
                module.setEnabledStatus(true);
            } else {
                if (module.isEnabled()) {
                    module.setEnabled(false);
                }
                module.setEnabledStatus(false);
            }

            for (Setting<?> setting : module.getSettings()) {
                try {
                    resetSetting(setting);
                } catch (Exception ignored) {
                }
            }
        }

        ClickGUI.syncTheme();
    }

    private static void resetSetting(Setting<?> setting) {
        switch (setting) {
            case bool b -> b.setValue(b.getOriginalValue());
            case number n -> n.setValue(n.getOriginalValue());
            case mode<?> m -> m.setModeIndex(m.getOriginalValue());
            case bind k -> k.setKey(k.getOriginalKey());
            case range r -> {
                r.setMinValue(r.getOriginalMinValue());
                r.setMaxValue(r.getOriginalMaxValue());
            }
            case colour c -> c.setValue(c.getOriginalValue());
            case string s -> s.setValue(s.getOriginalValue());
            default -> {
            }
        }
    }

    private static class Box {
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        private Box(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private boolean isInside(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        private float getCenterX() {
            return x + width / 2f;
        }

        private float getCenterY() {
            return y + height / 2f;
        }
    }

    private class Layout {
        private final Box head;
        private final Box chevron;
        private final Box refresh;
        private final Box tag;
        private final Box update;
        private final Box discord;

        private Layout(int x, int y, int width) {
            UpdateManager manager = getManager();

            head = new Box(x, y, width, HEAD);
            chevron = new Box(x + width - 34, y + 7, 24, 24);
            refresh = new Box(x + width - 60, y + 7, 24, 24);

            if (manager != null && manager.hasUpdate()) {
                int tagWidth = TextRenderer.tinyWidth("UPDATE") + 12;
                int tagX = x + 43 + getTitleWidth(manager.getPageTitle());
                tag = new Box(tagX, y + HEAD / 2f - 7, tagWidth, 13);
            } else {
                tag = null;
            }

            float innerWidth = width - PADDING * 2;
            float rowY = y + HEAD + 4;

            update = new Box(x + PADDING, rowY, innerWidth, BUTTON_HEIGHT);
            rowY += updateAnim.get() * (BUTTON_HEIGHT + BUTTON_GAP);

            discord = new Box(x + PADDING, rowY, innerWidth, BUTTON_HEIGHT);
        }
    }
}

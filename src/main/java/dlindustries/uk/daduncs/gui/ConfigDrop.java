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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.managers.ConfigManager;
import dlindustries.uk.daduncs.render.Draw;
import dlindustries.uk.daduncs.render.Render2D;
import dlindustries.uk.daduncs.utils.Sounds;
import dlindustries.uk.daduncs.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.pack.PackScreen;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.nio.file.Path;
import java.util.List;

import static dlindustries.uk.daduncs.System.mc;

public final class ConfigDrop {

    private static final float INSET = 8f;
    private static final float RADIUS = 8f;
    private static final float START_SCALE = 0.85f;
    private static final long MESSAGE_MS = 1200L;

    private static final int RED = 0xFFFF8A8A;

    private static float progress;
    private static boolean hovering;
    private static long lastFrame = java.lang.System.nanoTime();

    private static String message;
    private static boolean failed;
    private static long messageAt;

    private static User32 user32;
    private static boolean windows = java.lang.System.getProperty("os.name").toLowerCase().contains("win");

    public static boolean onDrop(List<Path> paths) {
        hovering = false;

        Path file = null;
        for (Path path : paths) {
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".json") || name.endsWith(".txt")) {
                file = path;
                break;
            }
        }

        if (file == null || System.INSTANCE == null) return false;

        ConfigManager.ImportResult result = System.INSTANCE.getConfigManager().importConfig(file);
        failed = !result.success();
        message = result.success() ? "config loaded" : "not a config";
        messageAt = java.lang.System.currentTimeMillis();

        Sounds.play(result.success() ? Sounds.TOGGLE : Sounds.CLICK);
        if (result.success()) {
            Notice.show("Imported config: " + result.configName());
        } else {
            Notice.show(result.message());
        }
        return true;
    }

    public static void render(DrawContext context) {
        long now = java.lang.System.nanoTime();
        float delta = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;

        boolean wasHovering = hovering;
        hovering = !(mc.currentScreen instanceof PackScreen) && isDragging();

        if (hovering && !wasHovering) {
            message = null;
            Sounds.hover();
        }

        boolean showMessage = message != null && java.lang.System.currentTimeMillis() - messageAt < MESSAGE_MS;
        boolean opening = hovering || showMessage;

        progress = opening ? Math.min(1f, progress + delta * 4.5f) : Math.max(0f, progress - delta * 6f);
        if (progress <= 0f) return;

        Draw.warmup(context);

        float alpha = easeOut(progress);
        float scale = START_SCALE + (1f - START_SCALE) * (opening ? easeOutBack(progress) : alpha);

        float screenWidth = context.getScaledWindowWidth();
        float screenHeight = context.getScaledWindowHeight();
        float width = (screenWidth - INSET * 2f) * scale;
        float height = (screenHeight - INSET * 2f) * scale;

        Render2D.drawBlurRoundRect(context, (screenWidth - width) / 2f, (screenHeight - height) / 2f, width, height,
                RADIUS, 1f, Theme.BLUR * alpha, Theme.fade(Theme.card, 0.6f), alpha);

        String text = showMessage ? message : "drop to load config";
        int colour = showMessage && failed ? RED : Theme.text;
        float textX = screenWidth / 2f - TextRenderer.headlineWidth(text) / 2f;

        TextRenderer.drawHeadline(text, context, textX, screenHeight / 2f, Theme.fade(colour, alpha));
    }

    private static boolean isDragging() {
        if (!windows) return false;

        try {
            if (user32 == null) user32 = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

            long handle = mc.getWindow().getHandle();
            int button = user32.GetSystemMetrics(23) != 0 ? 0x02 : 0x01;

            if ((user32.GetAsyncKeyState(button) & 0x8000) == 0) return false;
            if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) return false;

            WinDef.HWND window = new WinDef.HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(handle)));
            if (user32.IsIconic(window) || window.equals(user32.GetForegroundWindow())) return false;

            WinDef.POINT cursor = new WinDef.POINT();
            if (!user32.GetCursorPos(cursor)) return false;

            WinDef.HWND hit = user32.WindowFromPoint(new WinDef.POINT.ByValue(cursor.x, cursor.y));
            WinDef.HWND root = hit == null ? null : user32.GetAncestor(hit, 2);
            if (root == null) return false;
            if (window.equals(root)) return true;

            char[] name = new char[32];
            int length = user32.GetClassName(root, name, name.length);
            if (!"SysDragImage".equals(new String(name, 0, Math.max(0, length)))) return false;

            WinDef.RECT rect = new WinDef.RECT();
            return user32.GetWindowRect(window, rect)
                    && cursor.x >= rect.left && cursor.x < rect.right
                    && cursor.y >= rect.top && cursor.y < rect.bottom;
        } catch (Throwable t) {
            windows = false;
            return false;
        }
    }

    private static float easeOut(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static float easeOutBack(float t) {
        float c = t - 1f;
        return 1f + 3.4f * c * c * c + 2.4f * c * c;
    }

    private interface User32 extends StdCallLibrary {
        short GetAsyncKeyState(int key);

        int GetSystemMetrics(int index);

        boolean GetCursorPos(WinDef.POINT point);

        WinDef.HWND WindowFromPoint(WinDef.POINT.ByValue point);

        WinDef.HWND GetAncestor(WinDef.HWND window, int flags);

        WinDef.HWND GetForegroundWindow();

        int GetClassName(WinDef.HWND window, char[] name, int max);

        boolean GetWindowRect(WinDef.HWND window, WinDef.RECT rect);

        boolean IsIconic(WinDef.HWND window);
    }
}

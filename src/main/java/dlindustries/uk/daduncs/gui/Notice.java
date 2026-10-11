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

import dlindustries.uk.daduncs.render.Draw;
import dlindustries.uk.daduncs.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class Notice {

    private static final long SHOW_MS = 4000L;
    private static final long FADE_IN_MS = 180L;
    private static final long FADE_OUT_MS = 700L;

    private static String text;
    private static long shownAt;

    public static void show(String message) {
        text = message;
        shownAt = java.lang.System.currentTimeMillis();
    }

    public static void render(DrawContext context) {
        if (text == null)
            return;

        long shown = java.lang.System.currentTimeMillis() - shownAt;
        if (shown > SHOW_MS) {
            text = null;
            return;
        }

        float alpha = Math.min(1f, shown / (float) FADE_IN_MS);
        if (shown > SHOW_MS - FADE_OUT_MS)
            alpha = (SHOW_MS - shown) / (float) FADE_OUT_MS;

        Draw.warmup(context);

        float base = ClickGui.panelTop(context.getScaledWindowHeight()) - 12;
        int x = context.getScaledWindowWidth() / 2 - TextRenderer.smallWidth(text) / 2;
        int y = (int) (base + 4 * (1f - alpha));

        TextRenderer.drawSmall(text, context, x, y, Theme.fade(Theme.text, alpha));
    }
}

/*
 * This file is part of System Client (https://github.com/DLindustries/System)
 * Website: https://system.daduncs.cloud
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

import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import net.minecraft.util.Identifier;

public final class Sprite {

    private static final int FRAMES = 24;
    private static final long FRAME_MS = 30L;

    private final Identifier still;
    private final Identifier[] frames = new Identifier[FRAMES];
    private long startedAt = -1L;

    public Sprite(String name) {
        this.still = Identifier.of("system", "textures/gui/" + name + ".png");

        for (int i = 0; i < FRAMES; i++)
            frames[i] = Identifier.of("system", "textures/gui/anim/" + name + "/" + i + ".png");
    }

    public void play() {
        if (!ClickGUI.animatedIcons.getValue())
            return;

        startedAt = java.lang.System.currentTimeMillis();
    }

    public Identifier get() {
        if (startedAt < 0 || !ClickGUI.animatedIcons.getValue())
            return still;

        int frame = (int) ((java.lang.System.currentTimeMillis() - startedAt) / FRAME_MS);
        if (frame >= FRAMES) {
            startedAt = -1L;
            return still;
        }

        return frames[frame];
    }
}

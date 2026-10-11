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

package dlindustries.uk.daduncs.gui.muon.parts;

import dlindustries.uk.daduncs.gui.Picker;
import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.gui.muon.Rect;
import dlindustries.uk.daduncs.gui.muon.Skin;
import dlindustries.uk.daduncs.module.setting.colour;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class Swatch extends Part<colour> {

	private static final float SIZE = 9;

	public Swatch(colour setting) {
		super(setting);
	}

	@Override
	public float wide() {
		return labelWidth(name()) + LABEL_GAP + Skin.tinyWidth("#000000") + 5 + SIZE;
	}

	@Override
	public void render(DrawContext context, float delta, float alpha, int accent) {
		label(context, name(), alpha);

		Rect swatch = swatch();
		String hex = String.format("#%06X", setting.getValue() & 0xFFFFFF);
		Skin.tiny(context, hex, swatch.x() - 5 - Skin.tinyWidth(hex), rect.centerY(), Skin.white(alpha * 0.7f));
		Skin.fill(context, swatch.x(), swatch.y(), SIZE, SIZE, 3, Theme.fade(setting.getValue() | 0xFF000000, alpha));
		Skin.ring(context, swatch.x(), swatch.y(), SIZE, SIZE, 3, 0.6f, Skin.white(alpha * 0.25f));
	}

	@Override
	public boolean grabs(double mouseX, double mouseY) {
		return swatch().contains(mouseX, mouseY);
	}

	@Override
	public boolean click(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

		Rect swatch = swatch();
		Picker.open(setting, Math.round(rect.right() + 52), Math.round(swatch.centerY()), 0.72f);
		Sounds.play(Sounds.CLICK);
		return true;
	}

	private Rect swatch() {
		return new Rect(rect.right() - SIZE, rect.centerY() - SIZE / 2, SIZE, SIZE);
	}
}

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

package dlindustries.uk.daduncs.gui.muon.parts;

import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.gui.muon.Skin;
import dlindustries.uk.daduncs.gui.muon.Spring;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class Toggle extends Part<bool> {

	private static final float DOT = 3.5f;

	private final Spring state = new Spring(320, 1);

	public Toggle(bool setting) {
		super(setting);
		state.snap(setting.getValue() ? 1 : 0);
	}

	@Override
	public float wide() {
		return labelWidth(name()) + LABEL_GAP + DOT * 2 + 1;
	}

	@Override
	public void render(DrawContext context, float delta, float alpha, int accent) {
		state.to(setting.getValue() ? 1 : 0);
		state.update(delta);
		float on = Math.clamp(state.get(), 0, 1);

		label(context, name(), alpha);

		float x = rect.right() - DOT - 0.5f;
		float y = rect.centerY();
		float size = DOT * (0.5f + 0.5f * on);

		Skin.ring(context, x - DOT, y - DOT, DOT * 2, DOT * 2, DOT, 0.7f, Skin.white(alpha * 0.35f * (1 - on)));
		Skin.circle(context, x, y, size, Theme.fade(accent, alpha * on));
	}

	@Override
	public boolean click(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

		setting.toggle();
		Sounds.play(Sounds.TOGGLE, 1f, setting.getValue() ? 1.1f : 0.9f);
		return true;
	}
}

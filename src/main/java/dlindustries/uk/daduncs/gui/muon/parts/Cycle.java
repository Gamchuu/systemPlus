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

import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.gui.muon.Skin;
import dlindustries.uk.daduncs.gui.muon.Spring;
import dlindustries.uk.daduncs.module.setting.mode;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class Cycle extends Part<mode<?>> {

	private static final float CHIP = 11;
	private static final float PAD = 4;
	private static final float DOT = 1.1f;
	private static final float SPACING = 3.3f;

	private final Spring chip = new Spring(420, 1);

	public Cycle(mode<?> setting) {
		super(setting);
		chip.snap(chipWidth(value()));
	}

	@Override
	public float wide() {
		float widest = 0;
		for (Enum<?> option : options())
			widest = Math.max(widest, chipWidth(pretty(option)));

		return labelWidth(name()) + LABEL_GAP + widest;
	}

	@Override
	public void render(DrawContext context, float delta, float alpha, int accent) {
		chip.to(chipWidth(value()));
		chip.update(delta);

		label(context, name(), alpha);

		float width = chip.get();
		float chipX = rect.right() - width;
		float chipY = rect.centerY() - CHIP / 2;
		Skin.fill(context, chipX, chipY, width, CHIP, 3.5f, Skin.black(alpha * 0.32f));

		int count = options().length;
		for (int i = 0; i < count; i++) {
			int color = i == setting.getModeIndex() ? Theme.fade(accent, alpha) : Skin.white(alpha * 0.3f);
			Skin.circle(context, chipX + PAD + DOT + i * SPACING, rect.centerY(), DOT, color);
		}

		String value = value();
		Skin.tiny(context, value, rect.right() - PAD - Skin.tinyWidth(value), rect.centerY(), Skin.white(alpha * 0.9f));
	}

	@Override
	public boolean click(double mouseX, double mouseY, int button) {
		int count = options().length;

		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) setting.cycle();
		else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) setting.setModeIndex((setting.getModeIndex() - 1 + count) % count);
		else return false;

		Sounds.play(Sounds.CLICK, 1f, 1.2f);
		return true;
	}

	private Enum<?>[] options() {
		return setting.getMode().getDeclaringClass().getEnumConstants();
	}

	private String value() {
		return pretty(setting.getMode());
	}

	private static String pretty(Enum<?> option) {
		return option.name().replace("_", " ").toLowerCase();
	}

	private float chipWidth(String value) {
		return PAD + options().length * SPACING + 3 + Skin.tinyWidth(value) + PAD;
	}
}

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
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class Slider extends Part<number> {

	static final float TRACK = 44;
	static final float KNOB = 3.1f;
	static final float THICK = 2.4f;
	private static final long SOUND_GAP = 60;

	private final Spring knob = new Spring(600, 1);
	private boolean dragging;
	private long lastSound;

	public Slider(number setting) {
		super(setting);
		knob.snap(fraction());
	}

	@Override
	public float wide() {
		float label = Math.max(labelWidth(name() + " " + format(setting.getMin(), setting.getIncrement())),
				labelWidth(name() + " " + format(setting.getMax(), setting.getIncrement())));
		return label + LABEL_GAP + TRACK + KNOB;
	}

	@Override
	public void render(DrawContext context, float delta, float alpha, int accent) {
		knob.to(fraction());
		knob.update(delta);

		label(context, name() + " " + format(setting.getValue(), setting.getIncrement()), alpha);

		float trackX = trackX();
		float trackY = rect.centerY() - THICK / 2;
		float knobX = trackX + TRACK * Math.clamp(knob.get(), 0, 1);

		Skin.fill(context, trackX, trackY, TRACK, THICK, THICK / 2, Skin.white(alpha * 0.2f));
		Skin.fill(context, trackX, trackY, knobX - trackX, THICK, THICK / 2, Theme.fade(accent, alpha));
		Skin.circle(context, knobX, rect.centerY(), KNOB, Theme.fade(0xFFF4F4F6, alpha));
	}

	@Override
	public boolean click(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !grabs(mouseX, mouseY)) return false;

		dragging = true;
		slide(mouseX);
		return true;
	}

	@Override
	public boolean grabs(double mouseX, double mouseY) {
		return mouseX >= trackX() - 4 && mouseX <= trackX() + TRACK + 4;
	}

	@Override
	public void drag(double mouseX, double mouseY) {
		if (dragging)
			slide(mouseX);
	}

	@Override
	public void release() {
		dragging = false;
	}

	private void slide(double mouseX) {
		double fraction = Math.clamp((mouseX - trackX()) / TRACK, 0, 1);
		double previous = setting.getValue();
		setting.setValue(setting.getMin() + fraction * (setting.getMax() - setting.getMin()));

		long now = java.lang.System.currentTimeMillis();
		if (setting.getValue() != previous && now - lastSound > SOUND_GAP) {
			lastSound = now;
			Sounds.play(Sounds.SCROLL, 1f, 0.9f + (float) fraction * 0.3f);
		}
	}

	private float trackX() {
		return rect.right() - TRACK - KNOB;
	}

	private float fraction() {
		return (float) ((setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin()));
	}

	static String format(double value, double increment) {
		if (increment >= 1)
			return String.valueOf(Math.round(value));

		int places = Math.max(1, (int) Math.ceil(-Math.log10(increment)));
		return String.format("%." + places + "f", value);
	}
}

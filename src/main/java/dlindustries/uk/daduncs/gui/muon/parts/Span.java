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
import dlindustries.uk.daduncs.module.setting.range;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import static dlindustries.uk.daduncs.gui.muon.parts.Slider.KNOB;
import static dlindustries.uk.daduncs.gui.muon.parts.Slider.THICK;
import static dlindustries.uk.daduncs.gui.muon.parts.Slider.TRACK;

public final class Span extends Part<range> {

	private final Spring low = new Spring(600, 1);
	private final Spring high = new Spring(600, 1);
	private int dragging;

	public Span(range setting) {
		super(setting);
		low.snap(fraction(setting.getMinValue()));
		high.snap(fraction(setting.getMaxValue()));
	}

	@Override
	public float wide() {
		String widest = Slider.format(setting.getMax(), setting.getIncrement());
		return labelWidth(name() + " " + widest + "-" + widest) + LABEL_GAP + TRACK + KNOB;
	}

	@Override
	public void render(DrawContext context, float delta, float alpha, int accent) {
		low.to(fraction(setting.getMinValue()));
		high.to(fraction(setting.getMaxValue()));
		low.update(delta);
		high.update(delta);

		label(context, name() + " " + Slider.format(setting.getMinValue(), setting.getIncrement()) + "-"
				+ Slider.format(setting.getMaxValue(), setting.getIncrement()), alpha);

		float trackX = trackX();
		float trackY = rect.centerY() - THICK / 2;
		float lowX = trackX + TRACK * Math.clamp(low.get(), 0, 1);
		float highX = trackX + TRACK * Math.clamp(high.get(), 0, 1);

		Skin.fill(context, trackX, trackY, TRACK, THICK, THICK / 2, Skin.white(alpha * 0.2f));
		Skin.fill(context, lowX, trackY, highX - lowX, THICK, THICK / 2, Theme.fade(accent, alpha));
		Skin.circle(context, lowX, rect.centerY(), KNOB, Theme.fade(0xFFF4F4F6, alpha));
		Skin.circle(context, highX, rect.centerY(), KNOB, Theme.fade(0xFFF4F4F6, alpha));
	}

	@Override
	public boolean click(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !grabs(mouseX, mouseY)) return false;

		float lowX = trackX() + TRACK * fraction(setting.getMinValue());
		float highX = trackX() + TRACK * fraction(setting.getMaxValue());
		dragging = Math.abs(mouseX - lowX) <= Math.abs(mouseX - highX) ? 1 : 2;
		slide(mouseX);
		return true;
	}

	@Override
	public boolean grabs(double mouseX, double mouseY) {
		return mouseX >= trackX() - 4 && mouseX <= trackX() + TRACK + 4;
	}

	@Override
	public void drag(double mouseX, double mouseY) {
		if (dragging != 0)
			slide(mouseX);
	}

	@Override
	public void release() {
		dragging = 0;
	}

	private void slide(double mouseX) {
		double fraction = Math.clamp((mouseX - trackX()) / TRACK, 0, 1);
		double value = setting.getMin() + fraction * (setting.getMax() - setting.getMin());

		if (dragging == 1) setting.setMinValue(Math.min(value, setting.getMaxValue()));
		else setting.setMaxValue(Math.max(value, setting.getMinValue()));

		Sounds.play(Sounds.SCROLL, 0.4f, 0.9f + (float) fraction * 0.3f);
	}

	private float trackX() {
		return rect.right() - TRACK - KNOB;
	}

	private float fraction(double value) {
		return (float) ((value - setting.getMin()) / (setting.getMax() - setting.getMin()));
	}
}

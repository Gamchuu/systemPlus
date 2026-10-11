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

import dlindustries.uk.daduncs.gui.muon.Catcher;
import dlindustries.uk.daduncs.gui.muon.MuonGui;
import dlindustries.uk.daduncs.gui.muon.Skin;
import dlindustries.uk.daduncs.module.setting.string;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class Words extends Part<string> implements Catcher {

	private static final float CHIP = 11;
	private static final float PAD = 4;
	private static final float MAX = 70;

	private final MuonGui gui;

	public Words(MuonGui gui, string setting) {
		super(setting);
		this.gui = gui;
	}

	@Override
	public float wide() {
		return labelWidth(name()) + LABEL_GAP + MAX;
	}

	@Override
	public void render(DrawContext context, float delta, float alpha, int accent) {
		label(context, name(), alpha);

		String text = setting.getValue();
		while (text.length() > 1 && Skin.tinyWidth(text) > MAX - PAD * 2)
			text = text.substring(1);

		float width = Math.min(MAX, PAD * 2 + Skin.tinyWidth(text) + (typing() ? 3 : 0));
		float chipX = rect.right() - width;
		Skin.fill(context, chipX, rect.centerY() - CHIP / 2, width, CHIP, 3.5f, Skin.black(alpha * (typing() ? 0.45f : 0.32f)));
		Skin.tiny(context, text, chipX + PAD, rect.centerY(), Skin.white(alpha * 0.9f));

		if (typing() && java.lang.System.currentTimeMillis() % 1000 < 500)
			Skin.fill(context, chipX + PAD + Skin.tinyWidth(text) + 1, rect.centerY() - 3, 0.8f, 6, 0.4f, Skin.white(alpha));
	}

	@Override
	public boolean click(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

		gui.listen(typing() ? null : this);
		Sounds.play(Sounds.TYPE);
		return true;
	}

	@Override
	public void capture(int key) {
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			String value = setting.getValue();
			if (!value.isEmpty()) setting.setValue(value.substring(0, value.length() - 1));
			return;
		}

		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
			gui.listen(null);
			Sounds.play(Sounds.KEYPRESS);
		}
	}

	@Override
	public boolean type(String typed) {
		setting.setValue(setting.getValue() + typed);
		Sounds.play(Sounds.KEYPRESS, 0.6f);
		return true;
	}

	@Override
	public boolean mouse() {
		return false;
	}

	private boolean typing() {
		return gui.listening() == this;
	}
}

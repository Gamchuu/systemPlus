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
import dlindustries.uk.daduncs.gui.muon.Spring;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class Keybind extends Part<bind> implements Catcher {

	private static final float CHIP = 11;
	private static final float PAD = 4;
	private static final String WAITING = "press a key";

	private final MuonGui gui;
	private final Spring chip = new Spring(420, 1);

	public Keybind(MuonGui gui, bind setting) {
		super(setting);
		this.gui = gui;
		chip.snap(chipWidth(text()));
	}

	@Override
	public float wide() {
		return labelWidth(name()) + LABEL_GAP + Math.max(chipWidth(WAITING), chipWidth(setting.getKeyName()));
	}

	@Override
	public void render(DrawContext context, float delta, float alpha, int accent) {
		String text = text();
		chip.to(chipWidth(text));
		chip.update(delta);

		label(context, name(), alpha);

		float width = chip.get();
		float chipX = rect.right() - width;
		Skin.fill(context, chipX, rect.centerY() - CHIP / 2, width, CHIP, 3.5f, Skin.black(alpha * 0.32f));

		float shown = listening() ? 0.6f + 0.3f * MathHelper.sin(java.lang.System.currentTimeMillis() / 170f) : 0.9f;
		Skin.tiny(context, text, chipX + (width - Skin.tinyWidth(text)) / 2, rect.centerY(), Skin.white(alpha * shown));
	}

	@Override
	public boolean click(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || listening()) return false;

		gui.listen(this);
		Sounds.play(Sounds.TYPE);
		return true;
	}

	@Override
	public void capture(int key) {
		if (key == GLFW.GLFW_KEY_UNKNOWN) return;

		gui.listen(null);
		if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
			setting.setKey(GLFW.GLFW_KEY_UNKNOWN);
			Sounds.play(Sounds.COLLAPSE);
			return;
		}

		setting.setKey(key);
		Sounds.play(Sounds.KEYPRESS);
	}

	private boolean listening() {
		return gui.listening() == this;
	}

	private String text() {
		return listening() ? WAITING : setting.getKeyName().toLowerCase();
	}

	private float chipWidth(String text) {
		return PAD * 2 + Skin.tinyWidth(text);
	}
}

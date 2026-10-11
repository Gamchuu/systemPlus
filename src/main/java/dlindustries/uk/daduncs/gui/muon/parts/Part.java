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
import dlindustries.uk.daduncs.gui.muon.MuonGui;
import dlindustries.uk.daduncs.gui.muon.Rect;
import dlindustries.uk.daduncs.gui.muon.Skin;
import dlindustries.uk.daduncs.module.setting.Setting;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.module.setting.colour;
import dlindustries.uk.daduncs.module.setting.mode;
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.module.setting.range;
import dlindustries.uk.daduncs.module.setting.string;
import net.minecraft.client.gui.DrawContext;

public abstract class Part<T extends Setting<?>> {

	public static final float HEIGHT = 14.5f;
	protected static final float LABEL_GAP = 12;

	protected final T setting;
	protected Rect rect = new Rect(0, 0, 0, HEIGHT);

	protected Part(T setting) {
		this.setting = setting;
	}

	public static Part<?> of(MuonGui gui, Setting<?> setting) {
		return switch (setting) {
			case bool value -> new Toggle(value);
			case number value -> new Slider(value);
			case range value -> new Span(value);
			case mode<?> value -> new Cycle(value);
			case bind value when !value.isModuleKey() -> new Keybind(gui, value);
			case colour value -> new Swatch(value);
			case string value -> new Words(gui, value);
			default -> null;
		};
	}

	public abstract float wide();

	public abstract void render(DrawContext context, float delta, float alpha, int accent);

	public boolean click(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean grabs(double mouseX, double mouseY) {
		return false;
	}

	public void drag(double mouseX, double mouseY) {
	}

	public void release() {
	}

	public void place(Rect rect) {
		this.rect = rect;
	}

	public boolean contains(double mouseX, double mouseY) {
		return rect.contains(mouseX, mouseY);
	}

	protected String name() {
		return setting.getName().toString();
	}

	protected void label(DrawContext context, String text, float alpha) {
		Skin.text(context, text, rect.x(), rect.centerY(), Theme.fade(Theme.text, alpha));
	}

	protected float labelWidth(String text) {
		return Skin.width(text);
	}
}

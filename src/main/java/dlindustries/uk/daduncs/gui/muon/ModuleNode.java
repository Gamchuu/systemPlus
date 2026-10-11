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

package dlindustries.uk.daduncs.gui.muon;

import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.setting.Setting;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public final class ModuleNode {

	public static final float HEIGHT = 19;
	private static final float PADDING = 8.5f;
	private static final float RADIUS = 6;
	private static final float DRAG = 2;

	private final MuonGui gui;
	private final CategoryNode parent;
	private final Module module;
	private final SettingsPanel settings;
	private final BindPanel bind;
	private final Wire wire;
	private final Spring progress = new Spring(260, 0.58f);
	private final Spring hover = new Spring(380, 1);
	private final Spring enabled = new Spring(300, 1);

	private float offsetX;
	private float offsetY;
	private boolean open;
	private boolean shown;
	private float delay;

	private boolean pressed;
	private boolean dragging;
	private double pressX;
	private double pressY;
	private float grabX;
	private float grabY;

	public ModuleNode(MuonGui gui, CategoryNode parent, Module module, long seed) {
		this.gui = gui;
		this.parent = parent;
		this.module = module;
		this.settings = new SettingsPanel(gui, module, seed * 3 + 1);
		this.bind = key(module) == null ? null : new BindPanel(gui, key(module), seed * 3 + 2);
		this.wire = new Wire(seed);
		enabled.snap(module.isEnabled() ? 1 : 0);
	}

	private static bind key(Module module) {
		for (Setting<?> setting : module.getSettings())
			if (setting instanceof bind value && value.isModuleKey())
				return value;

		return null;
	}

	public void open(boolean open, float delay) {
		this.open = open;
		this.delay = delay;
	}

	public void update(float delta) {
		if (delay > 0)
			delay -= delta;

		if (delay <= 0) {
			boolean target = open && gui.shown(MuonGui.MODULES);
			if (target != shown) {
				shown = target;
				Skin.open(progress, target);
			}
		}

		progress.update(delta);
		hover.to(gui.hovered() == this ? 1 : 0);
		hover.update(delta);
		enabled.to(module.isEnabled() ? 1 : 0);
		enabled.update(delta);
	}

	public void renderWire(DrawContext context, Rect category) {
		if (visible())
			wire.render(context, category, animated(category), alpha());
	}

	public void render(DrawContext context, Rect category, double mouseX, double mouseY, float delta) {
		if (!visible()) return;

		Rect rect = rect();
		float alpha = alpha();
		boolean pushed = Skin.push(context, rect, animated(category));

		boolean panels = shown && gui.shown(MuonGui.PANELS);
		settings.render(context, rect, delta, Theme.accent, panels, alpha);
		if (bind != null)
			bind.render(context, rect, mouseX, mouseY, delta, panels, alpha);

		Skin.surface(context, rect, RADIUS, alpha, hover.get());

		int color = Theme.lerp(Theme.text, Theme.accent, Math.clamp(enabled.get(), 0, 1));
		Skin.name(context, name(), rect.x() + PADDING, rect.centerY(), Theme.fade(color, alpha));

		if (pushed)
			context.getMatrices().popMatrix();
	}

	public Object hoveredAt(double mouseX, double mouseY) {
		if (!interactive()) return null;
		if (bind != null && bind.contains(mouseX, mouseY)) return bind;
		if (settings.contains(mouseX, mouseY)) return settings;
		return rect().contains(mouseX, mouseY) ? this : null;
	}

	public boolean click(double mouseX, double mouseY, int button) {
		if (!interactive()) return false;
		if (bind != null && bind.click(mouseX, mouseY, button)) return true;
		if (settings.click(mouseX, mouseY, button)) return true;
		if (!rect().contains(mouseX, mouseY)) return false;

		switch (button) {
			case GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
				pressed = true;
				dragging = false;
				pressX = mouseX;
				pressY = mouseY;
				grabX = (float) mouseX - rect().x();
				grabY = (float) mouseY - rect().y();
			}
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
				if (settings.empty()) return true;

				settings.open(!settings.isOpen(), side());
				Sounds.play(settings.isOpen() ? Sounds.CLICK : Sounds.COLLAPSE);
			}
			case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> {
				if (bind == null) return true;

				bind.open(!bind.isOpen(), side());
				Sounds.play(bind.isOpen() ? Sounds.CLICK : Sounds.COLLAPSE, 1f, 1.1f);
			}
			default -> {
			}
		}
		return true;
	}

	public void drag(double mouseX, double mouseY) {
		settings.drag(mouseX, mouseY);
		if (!pressed) return;

		if (!dragging && Math.hypot(mouseX - pressX, mouseY - pressY) > DRAG)
			dragging = true;

		if (dragging) {
			float x = Math.clamp((float) mouseX - grabX, 0, Math.max(0, gui.width - width()));
			float y = Math.clamp((float) mouseY - grabY, 0, Math.max(0, gui.height - HEIGHT));
			offsetX = x - parent.x();
			offsetY = y - parent.y();
		}
	}

	public void release(double mouseX, double mouseY, int button) {
		settings.release(mouseX, mouseY, button);
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !pressed) return;

		if (!dragging) {
			module.toggle();
			Sounds.play(Sounds.TOGGLE, 1f, module.isEnabled() ? 1.1f : 0.9f);
		}
		pressed = false;
		dragging = false;
	}

	public Rect rect() {
		return new Rect(parent.x() + offsetX, parent.y() + offsetY, width(), HEIGHT);
	}

	private Rect animated(Rect category) {
		return rect().grow(category.centerX(), category.centerY(), progress.get());
	}

	public void offset(float offsetX, float offsetY) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	public float width() {
		return PADDING * 2 + Skin.nameWidth(name());
	}

	public boolean visible() {
		return shown || progress.get() > 0.01f;
	}

	private boolean interactive() {
		return shown && progress.get() > 0.8f;
	}

	private float alpha() {
		return Skin.alpha(progress.get());
	}

	private String name() {
		return module.getName().toString();
	}

	private int side() {
		return rect().centerX() >= parent.rect().centerX() ? 1 : -1;
	}
}

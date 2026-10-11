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

package dlindustries.uk.daduncs.gui.muon;

import dlindustries.uk.daduncs.gui.muon.parts.Part;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.setting.Setting;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class SettingsPanel {

	private static final float PADDING = 7;
	private static final float VERTICAL = 5;
	private static final float RADIUS = 6;
	private static final float GAP = 11;
	private static final float INSET = 7;
	private static final float MIN_WIDTH = 96;
	private static final float DRAG = 2;

	private final MuonGui gui;
	private final List<Part<?>> parts = new ArrayList<>();
	private final Spring progress = new Spring(260, 0.58f);
	private final Wire wire;
	private boolean open;
	private boolean shown;
	private int side = 1;
	private Rect rect;

	private float offsetX;
	private float offsetY;

	private boolean pressed;
	private boolean dragging;
	private double pressX;
	private double pressY;
	private float grabX;
	private float grabY;
	private Part<?> pressedPart;

	public SettingsPanel(MuonGui gui, Module module, long seed) {
		this.gui = gui;
		for (Setting<?> setting : module.getSettings()) {
			Part<?> part = Part.of(gui, setting);
			if (part != null)
				parts.add(part);
		}
		this.wire = new Wire(seed);
	}

	public boolean empty() {
		return parts.isEmpty();
	}

	public void open(boolean open, int side) {
		this.open = open;
		if (open && progress.get() <= 0.01f)
			this.side = side;
	}

	public boolean isOpen() {
		return open;
	}

	public boolean visible() {
		return shown || progress.get() > 0.01f;
	}

	public void render(DrawContext context, Rect module, float delta, int accent, boolean allowed, float parentAlpha) {
		boolean target = open && allowed;
		if (target != shown) {
			shown = target;
			Skin.open(progress, target);
		}
		progress.update(delta);

		rect = layout(module);
		if (!visible()) return;

		float alpha = Skin.alpha(progress.get()) * parentAlpha;
		Rect animated = rect.grow(module.centerX(), module.bottom(), progress.get());

		wire.render(context, module, animated, alpha);

		boolean pushed = Skin.push(context, rect, animated);
		Skin.surface(context, rect, RADIUS, alpha, 0);

		for (int i = 0; i < parts.size(); i++) {
			Part<?> part = parts.get(i);
			part.place(new Rect(rect.x() + PADDING, rect.y() + VERTICAL + i * Part.HEIGHT, rect.width() - PADDING * 2, Part.HEIGHT));
			part.render(context, delta, alpha, accent);
		}

		if (pushed)
			context.getMatrices().popMatrix();
	}

	public boolean contains(double mouseX, double mouseY) {
		if (!shown || progress.get() <= 0.8f || rect == null) return false;
		return rect.contains(mouseX, mouseY) || partAt(mouseX, mouseY) != null;
	}

	public boolean click(double mouseX, double mouseY, int button) {
		if (!contains(mouseX, mouseY)) return false;

		Part<?> part = partAt(mouseX, mouseY);

		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && (part == null || !part.grabs(mouseX, mouseY))) {
			pressed = true;
			dragging = false;
			pressX = mouseX;
			pressY = mouseY;
			grabX = (float) mouseX - rect.x();
			grabY = (float) mouseY - rect.y();
			pressedPart = part;
			return true;
		}

		if (part != null)
			part.click(mouseX, mouseY, button);

		return true;
	}

	public void drag(double mouseX, double mouseY) {
		for (Part<?> part : parts)
			part.drag(mouseX, mouseY);

		if (!pressed) return;

		if (!dragging && Math.hypot(mouseX - pressX, mouseY - pressY) > DRAG)
			dragging = true;

		if (dragging) {
			float x = Math.clamp((float) mouseX - grabX, 0, Math.max(0, gui.width - rect.width()));
			float y = Math.clamp((float) mouseY - grabY, 0, Math.max(0, gui.height - rect.height()));
			offsetX += x - rect.x();
			offsetY += y - rect.y();
			rect = new Rect(x, y, rect.width(), rect.height());
		}
	}

	public void release(double mouseX, double mouseY, int button) {
		for (Part<?> part : parts)
			part.release();

		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !pressed) return;

		if (!dragging && pressedPart != null)
			pressedPart.click(mouseX, mouseY, button);

		pressed = false;
		dragging = false;
		pressedPart = null;
	}

	private Part<?> partAt(double mouseX, double mouseY) {
		for (Part<?> part : parts)
			if (part.contains(mouseX, mouseY))
				return part;

		return null;
	}

	private Rect layout(Rect module) {
		float width = MIN_WIDTH;
		for (Part<?> part : parts)
			width = Math.max(width, part.wide() + PADDING * 2);

		float height = parts.size() * Part.HEIGHT + VERTICAL * 2;
		float x = side > 0 ? module.x() + INSET : module.right() - INSET - width;
		float y = module.bottom() + GAP + offsetY;

		y = Math.max(0, Math.min(y, gui.height - height));
		return new Rect(x + offsetX, y, width, height);
	}
}

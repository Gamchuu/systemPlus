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
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.render.Render2D;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class CategoryNode {

	public static final float HEIGHT = 22;
	private static final float PADDING = 8.5f;
	private static final float ICON = 12;
	private static final float ICON_GAP = 5;
	private static final float RADIUS = 7;
	private static final float STAGGER = 0.045f;
	private static final float MODULE_GAP = 16;
	private static final float ROW_GAP = 34;
	private static final int PER_ROW = 5;
	private static final float MARGIN = 10;

	private final MuonGui gui;
	private final Category category;
	private final Identifier icon;
	private final List<ModuleNode> modules = new ArrayList<>();
	private final Spring hover = new Spring(380, 1);
	private final Spring appear = new Spring(260, 0.58f);

	private float x;
	private float y;
	private boolean open;
	private boolean shown;

	private boolean pressed;
	private float grabX;
	private float grabY;

	public CategoryNode(MuonGui gui, Category category, List<Module> modules) {
		this.gui = gui;
		this.category = category;
		this.icon = Identifier.of("system", "textures/gui/" + icon(category) + ".png");
		for (int i = 0; i < modules.size(); i++)
			this.modules.add(new ModuleNode(gui, this, modules.get(i), category.ordinal() * 1000L + i));
	}

	private static String icon(Category category) {
		return switch (category) {
			case CLIENT -> "client";
			case CRYSTAL -> "crystal";
			case MACE -> "mace";
			case MISC -> "optimizer";
			case CART -> "cart";
			case RENDER -> "render";
			case SPEAR -> "spear";
			case SWORD -> "sword";
		};
	}

	public void toggle() {
		open = !open;
		for (int i = 0; i < modules.size(); i++) {
			int order = open ? i : modules.size() - 1 - i;
			modules.get(i).open(open, order * STAGGER);
		}
		Sounds.play(open ? Sounds.CLICK : Sounds.COLLAPSE);
	}

	public void layout() {
		for (int start = 0; start < modules.size(); start += PER_ROW) {
			int end = Math.min(modules.size(), start + PER_ROW);
			int count = end - start;

			float total = MODULE_GAP * (count - 1);
			for (int i = start; i < end; i++)
				total += modules.get(i).width();

			int index = start / PER_ROW;
			int depth = index / 2;
			float row = index % 2 == 0 ? HEIGHT + 30 + depth * ROW_GAP : -30 - ModuleNode.HEIGHT - depth * ROW_GAP;
			float left = width() / 2 - total / 2;
			for (int i = start; i < end; i++) {
				ModuleNode module = modules.get(i);
				float t = count == 1 ? 0.5f : (i - start) / (float) (count - 1);
				module.offset(left, row + (i % 2) * 10 - Math.abs(t - 0.5f) * 12);
				left += module.width() + MODULE_GAP;
			}
		}
	}

	public void fit() {
		float left = x;
		float right = x + width();
		float top = y;
		float bottom = y + HEIGHT;
		for (ModuleNode module : modules) {
			left = Math.min(left, module.rect().x());
			right = Math.max(right, module.rect().right());
			top = Math.min(top, module.rect().y());
			bottom = Math.max(bottom, module.rect().bottom());
		}

		float shiftX = 0;
		if (left < MARGIN) shiftX = MARGIN - left;
		else if (right > gui.width - MARGIN) shiftX = gui.width - MARGIN - right;

		float shiftY = 0;
		if (top < MARGIN) shiftY = MARGIN - top;
		else if (bottom > gui.height - MARGIN) shiftY = gui.height - MARGIN - bottom;

		place(x + shiftX, y + shiftY);
	}

	public void render(DrawContext context, double mouseX, double mouseY, float delta) {
		boolean target = gui.shown(MuonGui.CATEGORIES);
		if (target != shown) {
			shown = target;
			Skin.open(appear, target);
		}
		appear.update(delta);

		Rect rect = rect();
		float alpha = Skin.alpha(appear.get());
		if (alpha <= 0.01f) return;

		boolean pushed = Skin.push(context, rect, rect.grow(rect.centerX(), rect.centerY(), appear.get()));

		for (ModuleNode module : modules)
			module.update(delta);

		for (ModuleNode module : modules)
			module.renderWire(context, rect);

		for (ModuleNode module : modules)
			module.render(context, rect, mouseX, mouseY, delta);

		hover.to(gui.hovered() == this ? 1 : 0);
		hover.update(delta);
		Skin.surface(context, rect, RADIUS, alpha, hover.get());

		int color = Theme.fade(Theme.accent, alpha);
		Render2D.drawRoundImage(context, x + PADDING, rect.centerY() - ICON / 2, ICON, ICON, 0f, icon, color);
		Skin.title(context, name(), x + PADDING + ICON + ICON_GAP, rect.centerY(), Theme.fade(Theme.text, alpha));

		if (pushed)
			context.getMatrices().popMatrix();
	}

	public boolean hidden() {
		return !shown && appear.get() <= 0.01f;
	}

	private boolean interactive() {
		return shown && appear.get() > 0.8f;
	}

	public Object hoveredAt(double mouseX, double mouseY) {
		if (!interactive()) return null;
		if (rect().contains(mouseX, mouseY)) return this;

		for (int i = modules.size() - 1; i >= 0; i--) {
			Object hovered = modules.get(i).hoveredAt(mouseX, mouseY);
			if (hovered != null) return hovered;
		}
		return null;
	}

	public boolean click(double mouseX, double mouseY, int button) {
		if (!interactive()) return false;

		if (rect().contains(mouseX, mouseY)) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				pressed = true;
				grabX = (float) mouseX - x;
				grabY = (float) mouseY - y;
			} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				toggle();
			}
			return true;
		}

		for (int i = modules.size() - 1; i >= 0; i--) {
			ModuleNode module = modules.get(i);
			if (module.click(mouseX, mouseY, button)) {
				modules.remove(i);
				modules.add(module);
				return true;
			}
		}
		return false;
	}

	public void drag(double mouseX, double mouseY) {
		if (pressed)
			place((float) mouseX - grabX, (float) mouseY - grabY);

		for (ModuleNode module : modules)
			module.drag(mouseX, mouseY);
	}

	public void release(double mouseX, double mouseY, int button) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
			pressed = false;

		for (ModuleNode module : modules)
			module.release(mouseX, mouseY, button);
	}

	public void place(float x, float y) {
		this.x = Math.clamp(x, 0, Math.max(0, gui.width - width()));
		this.y = Math.clamp(y, 0, Math.max(0, gui.height - HEIGHT));
	}

	public Rect rect() {
		return new Rect(x, y, width(), HEIGHT);
	}

	public float width() {
		return PADDING + ICON + ICON_GAP + Skin.titleWidth(name()) + PADDING;
	}

	private String name() {
		return category.name.toString();
	}

	public float x() {
		return x;
	}

	public float y() {
		return y;
	}
}

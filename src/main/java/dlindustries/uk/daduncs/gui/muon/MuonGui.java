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

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.gui.Picker;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static dlindustries.uk.daduncs.System.mc;

public final class MuonGui extends Screen {

	public static final int CATEGORIES = 0;
	public static final int MODULES = 1;
	public static final int PANELS = 2;
	private static final float LEVEL_DELAY = 0.09f;

	private static final float[] COLUMNS = {0.14f, 0.38f, 0.62f, 0.86f};
	private static final float[] ROWS = {0.26f, 0.7f};

	private final List<CategoryNode> categories = new ArrayList<>();
	private Catcher listening;
	private Object hovered;
	private boolean placed;
	private boolean closing;
	private float transition;
	private long lastFrame;

	public MuonGui() {
		super(Text.literal("Muon"));
		for (Category category : Category.values())
			categories.add(new CategoryNode(this, category, System.INSTANCE.getModuleManager().getModulesInCategory(category)));
	}

	@Override
	public void onDisplayed() {
		closing = false;
		transition = 0;
		lastFrame = java.lang.System.nanoTime();
	}

	@Override
	protected void init() {
		if (!placed) {
			placed = true;
			for (int i = 0; i < categories.size(); i++) {
				CategoryNode category = categories.get(i);
				float column = COLUMNS[i % COLUMNS.length];
				float row = ROWS[(i / COLUMNS.length) % ROWS.length];
				category.place(width * column - category.width() / 2, height * row);
				category.layout();
				category.fit();
			}
		}

		for (CategoryNode category : categories)
			category.place(category.x(), category.y());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float partialTick) {
		long now = java.lang.System.nanoTime();
		float delta = Math.min(0.1f, (now - lastFrame) / 1.0E9f);
		lastFrame = now;
		transition += delta;

		hovered = closing ? null : find(mouseX, mouseY);
		for (CategoryNode category : categories)
			category.render(context, mouseX, mouseY, delta);

		Picker.render(context, delta, 1f);

		if (closing && transition > PANELS * LEVEL_DELAY && categories.stream().allMatch(CategoryNode::hidden))
			finish();
	}

	public boolean shown(int level) {
		if (closing)
			return transition < (PANELS - level) * LEVEL_DELAY;

		return transition >= level * LEVEL_DELAY;
	}

	@Override
	public void close() {
		if (closing) return;

		System.INSTANCE.getModuleManager().getModule(ClickGUI.class).setEnabledStatus(false);
		Picker.close();
		closing = true;
		transition = 0;
		listening = null;
		ClickGUI.uiSound(Sounds.CLOSE);
	}

	private void finish() {
		closing = false;
		mc.setScreen(System.INSTANCE.previousScreen instanceof TitleScreen ? null : System.INSTANCE.previousScreen);
		System.INSTANCE.previousScreen = null;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void renderInGameBackground(DrawContext context) {
	}

	@Override
	protected void applyBlur(DrawContext context) {
	}

	@Override
	protected void renderDarkening(DrawContext context) {
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (closing) return false;

		if (Picker.click((int) click.x(), (int) click.y(), click.button()))
			return true;

		if (listening != null) {
			if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT && listening.mouse()) {
				listening.capture(click.button());
				return true;
			}
			listening = null;
		}

		for (int i = categories.size() - 1; i >= 0; i--) {
			CategoryNode category = categories.get(i);
			if (category.click(click.x(), click.y(), click.button())) {
				categories.remove(i);
				categories.add(category);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		Picker.drag((int) click.x(), (int) click.y());

		for (CategoryNode category : categories)
			category.drag(click.x(), click.y());

		return true;
	}

	@Override
	public boolean mouseReleased(Click click) {
		Picker.release();

		for (CategoryNode category : categories)
			category.release(click.x(), click.y(), click.button());

		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (Picker.pressed(input.key(), input.modifiers()))
			return true;

		if (listening != null) {
			listening.capture(input.key());
			return true;
		}

		return super.keyPressed(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (!input.isValidChar())
			return false;

		String typed = input.asString();

		if (Picker.type(typed))
			return true;

		if (listening != null && listening.type(typed))
			return true;

		return super.charTyped(input);
	}

	@Override
	public void removed() {
		listening = null;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private Object find(double mouseX, double mouseY) {
		for (int i = categories.size() - 1; i >= 0; i--) {
			Object found = categories.get(i).hoveredAt(mouseX, mouseY);
			if (found != null) return found;
		}
		return null;
	}

	public Object hovered() {
		return hovered;
	}

	public Catcher listening() {
		return listening;
	}

	public void listen(Catcher catcher) {
		this.listening = catcher;
	}
}

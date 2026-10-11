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

package dlindustries.uk.daduncs.argonsui;

import dlindustries.uk.daduncs.utils.Sounds;
import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.argonsui.components.ModuleButton;
import dlindustries.uk.daduncs.argonsui.components.settings.KeybindBox;
import dlindustries.uk.daduncs.argonsui.components.settings.RenderableSetting;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.utils.ColorUtils;
import dlindustries.uk.daduncs.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static dlindustries.uk.daduncs.System.mc;

public final class ClickGui extends Screen {
	public List<Window> windows = new ArrayList<>();
	public Color currentColor;
	private int lastWidth;
	private static final StackWalker sw = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	public ClickGui() {
		super(Text.empty());

		int offsetX = 50;
		for (Category category : Category.values()) {
			windows.add(new Window(offsetX, 50, 230, 30, category, this));
			offsetX += 250;
		}
	}

	@Override
	protected void init() {
		int screenWidth = mc.getWindow().getWidth();
		if (screenWidth == lastWidth)
			return;

		lastWidth = screenWidth;

		int offsetX = 50;
		int offsetY = 50;
		int tallest = 0;

		for (Window window : windows) {
			if (offsetX != 50 && offsetX + window.getWidth() > screenWidth) {
				offsetX = 50;
				offsetY += tallest + 20;
				tallest = 0;
			}

			window.setX(offsetX);
			window.setY(offsetY);

			tallest = Math.max(tallest, window.getHeight() * (window.moduleButtons.size() + 1));
			offsetX += window.getWidth() + 20;
		}
	}

	public boolean isDraggingAlready() {
		for(Window window : windows)
			if(window.dragging)
				return true;

		return false;
	}

	@Override
	protected void setInitialFocus() {
		if (client == null) {
			return;
		}
		super.setInitialFocus();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if (mc.currentScreen == this) {
			if (System.INSTANCE.previousScreen != null)
				System.INSTANCE.previousScreen.render(context, 0, 0, delta);

			if (currentColor == null)
				currentColor = new Color(0, 0, 0, 0);
			else currentColor = new Color(0, 0, 0, currentColor.getAlpha());

			if (currentColor.getAlpha() != 0)
				currentColor = ColorUtils.smoothAlphaTransition(0.05F, 0, currentColor);

			if (mc.currentScreen instanceof ClickGui)
				context.fill(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(), currentColor.getRGB());

			RenderUtils.unscaledProjection(context);
			mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
			mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
			super.render(context, mouseX, mouseY, delta);

			for (Window window : windows) {
				window.render(context, mouseX, mouseY, delta);
				window.updatePosition(mouseX, mouseY, delta);
			}

			RenderUtils.scaledProjection(context);
		}
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		int keyCode = keyInput.key();
		int scanCode = keyInput.scancode();
		int modifiers = keyInput.modifiers();

		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			for (Window window : windows)
				for (ModuleButton moduleButton : window.moduleButtons)
					for (RenderableSetting setting : moduleButton.settings)
						if (setting instanceof KeybindBox keybindBox && keybindBox.keybind.isListening()) {
							keybindBox.keybind.setListening(false);
							return true;
						}
		}

		for (Window window : windows)
			window.keyPressed(keyCode, scanCode, modifiers);

		return super.keyPressed(keyInput);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		double mouseX = click.x();
		double mouseY = click.y();
		int button = click.button();

		mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();

		for (Window window : windows)
			window.mouseClicked(mouseX, mouseY, button);

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		double mouseX = click.x();
		double mouseY = click.y();
		int button = click.button();

		mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();

		for (Window window : windows)
			window.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);

		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		MinecraftClient mc = MinecraftClient.getInstance();
		mouseY *= mc.getWindow().getScaleFactor();

		for (Window window : windows)
			window.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		System.INSTANCE.getModuleManager().getModule(ClickGUI.class).setEnabledStatus(false);
		onGuiClose();
	}

	public void onGuiClose() {
		ClickGUI.uiSound(Sounds.CLOSE_QUICK);
		if (System.INSTANCE.previousScreen instanceof TitleScreen) {
			mc.setScreenAndRender(null);
		} else {
			mc.setScreenAndRender(System.INSTANCE.previousScreen);
		}
		System.INSTANCE.previousScreen = null;
		currentColor = null;

		for (Window window : windows)
			window.onGuiClose();
	}

	@Override
	public boolean mouseReleased(Click click) {
		double mouseX = click.x();
		double mouseY = click.y();
		int button = click.button();

		mouseX *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();
		mouseY *= (int) MinecraftClient.getInstance().getWindow().getScaleFactor();

		for (Window window : windows)
			window.mouseReleased(mouseX, mouseY, button);

		return super.mouseReleased(click);
	}
}

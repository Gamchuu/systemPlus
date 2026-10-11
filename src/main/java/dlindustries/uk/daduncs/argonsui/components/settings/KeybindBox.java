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

package dlindustries.uk.daduncs.argonsui.components.settings;

import dlindustries.uk.daduncs.argonsui.components.ModuleButton;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.module.setting.Setting;
import dlindustries.uk.daduncs.utils.ColorUtils;
import dlindustries.uk.daduncs.utils.KeyUtils;
import dlindustries.uk.daduncs.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;


public final class KeybindBox extends RenderableSetting {
	public bind keybind;
	private Color currentAlpha;

	public KeybindBox(ModuleButton parent, Setting<?> setting, int offset) {
		super(parent, setting, offset);
		this.keybind = (bind) setting;
	}

	@Override
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (isHovered(mouseX, mouseY)) {
			if(!keybind.isListening()) {
				if(button == 0) {
					keybind.toggleListening();
					keybind.setListening(true);
				}
			} else {
				if(keybind.isModuleKey()) parent.module.setKey(button);

				keybind.setKey(button);
				keybind.setListening(false);
			}
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {
		if(keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			if(mouseOver) {
				if (keybind.isModuleKey())
					parent.module.setKey(keybind.getOriginalKey());

				keybind.setKey(keybind.getOriginalKey());
				keybind.setListening(false);
			}
		} else {
			if (keybind.isListening() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
				if (keybind.isModuleKey()) parent.module.setKey(keyCode);

				keybind.setKey(keyCode);
				keybind.setListening(false);
			}

			if (keybind.getKey() == GLFW.GLFW_KEY_ESCAPE) {
				if (keybind.isModuleKey()) parent.module.setKey(parent.module.getKey());

				keybind.setKey(keybind.getKey());
				keybind.setListening(false);
			}
		}
		super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		int off = parentX() + 6;
		if (!keybind.isListening()) {
			String label = setting.getName() + (setting.getTag() != null ? " " + setting.getTag() : "") + ": " + KeyUtils.getKey(keybind.getKey());
			TextRenderer.drawString(label, context, off, (parentY() + parentOffset() + offset) + 9, new Color(245, 245, 245, 255).getRGB());
		} else TextRenderer.drawString("Listening...", context, off, (parentY() + parentOffset() + offset) + 6, new Color(245, 245, 245, 255).getRGB());

		if (!parent.parent.dragging) {
			int toHoverAlpha = isHovered(mouseX, mouseY) ? 15 : 0;

			if (currentAlpha == null)
				currentAlpha = new Color(255, 255, 255, toHoverAlpha);
			else currentAlpha = new Color(255, 255, 255, currentAlpha.getAlpha());

			if (currentAlpha.getAlpha() != toHoverAlpha)
				currentAlpha = ColorUtils.smoothAlphaTransition(0.05F, toHoverAlpha, currentAlpha);

			context.fill(parentX(), parentY() + parentOffset() + offset, parentX() + parentWidth(), parentY() + parentOffset() + offset + parentHeight(), currentAlpha.getRGB());
		}
	}
}

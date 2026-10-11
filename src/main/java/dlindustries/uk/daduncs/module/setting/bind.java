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

package dlindustries.uk.daduncs.module.setting;

import dlindustries.uk.daduncs.utils.KeyUtils;
import org.lwjgl.glfw.GLFW;

public final class bind extends Setting<bind> {
	private int keyCode;
	private boolean listening;
	private boolean hold;
	private final boolean moduleKey;
	private final int originalKey;

	public bind(CharSequence name, int key, boolean moduleKey) {
		super(name);
		this.keyCode = key;
		this.originalKey = key;
		this.moduleKey = moduleKey;
	}

	public boolean isModuleKey() {
		return moduleKey;
	}

	public boolean isListening() {
		return listening;
	}

	public boolean isHold() {
		return hold;
	}

	public void setHold(boolean hold) {
		this.hold = hold;
	}

	public int getOriginalKey() {
		return originalKey;
	}

	public void setListening(boolean listening) {
		this.listening = listening;
	}

	public int getKey() {
		return keyCode;
	}

	public void setKey(int key) {
		this.keyCode = key;
	}

	public void toggleListening() {
		this.listening = !listening;
	}

	public boolean isPressed() {
		if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
			return false;
		}
		return KeyUtils.isKeyPressed(keyCode);
	}

	public String getKeyName() {
		if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
			return "NONE";
		}
		return KeyUtils.getKey(keyCode).toString().toUpperCase();
	}

	public boolean shouldIgnoreKey(int key) {
		return key == GLFW.GLFW_KEY_UNKNOWN;
	}
}
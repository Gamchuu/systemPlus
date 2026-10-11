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

package dlindustries.uk.daduncs.module.setting;

import java.util.Arrays;
import java.util.List;

public final class mode<T extends Enum<T>> extends Setting<mode<T>> {
	public int index;
	private final List<T> possibleValues;
	private final int originalValue;

	public mode(CharSequence name, T defaultValue, Class<T> type) {
		super(name);
		T[] values = type.getEnumConstants();
		this.possibleValues = Arrays.asList(values);
		this.index = this.possibleValues.indexOf(defaultValue);
		this.originalValue = this.index;
	}

	public T getMode() {
		return possibleValues.get(index);
	}

	public void setMode(T mode) {
		index = possibleValues.indexOf(mode);
	}

	public void setModeIndex(int mode) {
		if (mode >= 0 && mode < possibleValues.size())
			index = mode;
	}

	public int getModeIndex() {
		return index;
	}

	public int getOriginalValue() {
		return originalValue;
	}

	public void cycle() {
		if (index < possibleValues.size() - 1)
			index++;
		else index = 0;
	}

	public boolean isMode(T mode) {
		return index == possibleValues.indexOf(mode);
	}
}

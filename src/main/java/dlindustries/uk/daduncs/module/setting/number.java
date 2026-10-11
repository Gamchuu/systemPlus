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

public final class number extends Setting<number> {
	private final double min;
	private final double max;
	private final double increment;
	private final double originalValue;
	private double value;

	public number(CharSequence name, double min, double max, double value, double increment) {
		super(name);
		this.min = min;
		this.max = max;
		this.value = value;
		this.increment = increment;
		this.originalValue = value;
	}

	public double getValue() {
		return value;
	}

	public double getOriginalValue() {
		return originalValue;
	}

	public double getIncrement() {
		return increment;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public int getValueInt() {
		return (int) value;
	}

	public float getValueFloat() {
		return (float) value;
	}

	public long getValueLong() {
		return (long) value;
	}

	public void setValue(double value) {
		double precision = 1.0D / this.increment;
		this.value = Math.round(Math.max(this.min, Math.min(this.max, value)) * precision) / precision;
	}
}

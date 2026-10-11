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

import java.util.Random;

public final class range extends Setting<range> {
	private final double min, max;
	private final double increment;
	private final double originalMinValue, originalMaxValue;
	private double minValue;
	private double maxValue;

	public range(CharSequence name, double min, double max, double increment, double defaultMin, double defaultMax) {
		super(name);
		this.min = min;
		this.max = max;
		this.increment = increment;
		this.minValue = defaultMin;
		this.maxValue = defaultMax;
		this.originalMinValue = defaultMin;
		this.originalMaxValue = defaultMax;
	}

	public int getMinInt() {
		return (int) minValue;
	}

	public float getMinFloat() {
		return (float) minValue;
	}

	public long getMinLong() {
		return (long) minValue;
	}

	public int getMaxInt() {
		return (int) maxValue;
	}

	public float getMaxFloat() {
		return (float) maxValue;
	}

	public long getMaxLong() {
		return (long) maxValue;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getMinValue() {
		return minValue;
	}

	public double getMaxValue() {
		return maxValue;
	}

	public double getOriginalMinValue() {
		return originalMinValue;
	}

	public double getOriginalMaxValue() {
		return originalMaxValue;
	}

	public double getIncrement() {
		return increment;
	}

	public double getRandomValue() {
		if (getMaxValue() > getMinValue())
			return new Random().nextDouble(getMinValue(), getMaxValue());
		else return getMinValue();
	}

	public int getRandomValueInt() {
		if (getMaxValue() > getMinValue())
			return new Random().nextInt(getMinInt(), getMaxInt());
		else return getMinInt();
	}

	public float getRandomValueFloat() {
		if (getMaxValue() > getMinValue())
			return new Random().nextFloat(getMinFloat(), getMaxFloat());
		else return getMinFloat();
	}

	public long getRandomValueLong() {
		if (getMaxValue() > getMinValue())
			return new Random().nextLong(getMinLong(), getMaxLong());
		else return getMinLong();
	}

	public void setMinValue(double value) {
		double precision = 1.0D / this.increment;
		this.minValue = Math.round(Math.max(this.min, Math.min(this.max, value)) * precision) / precision;
	}

	public void setMaxValue(double value) {
		double precision = 1.0D / this.increment;
		this.maxValue = Math.round(Math.max(this.min, Math.min(this.max, value)) * precision) / precision;
	}
}

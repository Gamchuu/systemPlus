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

import dlindustries.uk.daduncs.utils.MathUtils;

public final class AnimationUtils {
	private double value;
	private final double originalValue;
	private double endValue;

	public AnimationUtils(double value) {
		this.value = value;
		this.originalValue = value;
	}

	public double animate(double delta, double end) {
		this.endValue = end;
		value = MathUtils.goodLerp((float) delta, value, end);

		return value;
	}

	public double getValue() {
		return value;
	}

	public double getOriginalValue() {
		return originalValue;
	}

	public double getEndValue() {
		return endValue;
	}

	public double getAnimationProgress() {
		return value / endValue;
	}

	public void reset(double delta) {
		value = MathUtils.smoothStepLerp(delta, value, originalValue);
	}
}

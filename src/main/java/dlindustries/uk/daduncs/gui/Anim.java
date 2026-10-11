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

package dlindustries.uk.daduncs.gui;

public final class Anim {

	private float value;
	private float target;
	private final float system;
	private final float fall;

	public Anim(float value) {
		this(value, 12f);
	}

	public Anim(float value, float speed) {
		this(value, speed, speed);
	}

	public Anim(float value, float system, float fall) {
		this.value = value;
		this.target = value;
		this.system = system;
		this.fall = fall;
	}

	public void to(float target) {
		this.target = target;
	}

	public void snap(float value) {
		this.value = value;
		this.target = value;
	}

	public float get() {
		return value;
	}

	public float target() {
		return target;
	}

	public boolean done() {
		return Math.abs(target - value) < 0.0005f;
	}

	public void update(float unused) {
		if (done()) {
			value = target;
			return;
		}

		float speed = target > value ? system : fall;
		value += (target - value) * (1f - (float) Math.exp(-speed * Frame.delta()));
	}

	public float overshoot() {
		float t = Math.max(0f, Math.min(1f, value));
		if (t <= 0f || t >= 1f)
			return t;

		float p = t - 1f;
		return 1f + 2.2f * p * p * p + 1.2f * p * p;
	}
}

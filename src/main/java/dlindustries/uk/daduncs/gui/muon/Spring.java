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

public final class Spring {

	private static final float STEP = 1f / 240f;
	private static final float MAX_DELTA = 0.1f;

	private float stiffness;
	private float damping;
	private float value;
	private float velocity;
	private float target;
	private float pile;

	public Spring(float stiffness, float ratio) {
		configure(stiffness, ratio);
	}

	public Spring configure(float stiffness, float ratio) {
		this.stiffness = stiffness;
		this.damping = 2f * ratio * (float) Math.sqrt(stiffness);
		return this;
	}

	public void update(float delta) {
		if (settled()) return;

		pile += Math.min(delta, MAX_DELTA);
		while (pile >= STEP) {
			float force = stiffness * (target - value) - damping * velocity;
			velocity += force * STEP;
			value += velocity * STEP;
			pile -= STEP;
		}

		if (Math.abs(target - value) < 1.0E-4f && Math.abs(velocity) < 1.0E-3f) {
			value = target;
			velocity = 0;
			pile = 0;
		}
	}

	public void to(float target) {
		this.target = target;
	}

	public void snap(float value) {
		this.value = value;
		this.target = value;
		this.velocity = 0;
		this.pile = 0;
	}

	public float get() {
		return value;
	}

	public boolean settled() {
		return value == target && velocity == 0;
	}
}

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

package dlindustries.uk.daduncs.gui.muon;

public record Rect(float x, float y, float width, float height) {

	private static final float MIN_SCALE = 0.2f;

	public float right() {
		return x + width;
	}

	public float bottom() {
		return y + height;
	}

	public float centerX() {
		return x + width / 2;
	}

	public float centerY() {
		return y + height / 2;
	}

	public boolean contains(double pointX, double pointY) {
		return pointX >= x && pointX <= right() && pointY >= y && pointY <= bottom();
	}

	public Rect grow(float originX, float originY, float progress) {
		float scale = MIN_SCALE + (1 - MIN_SCALE) * Math.max(progress, 0);
		float cx = originX + (centerX() - originX) * progress;
		float cy = originY + (centerY() - originY) * progress;
		return new Rect(cx - width * scale / 2, cy - height * scale / 2, width * scale, height * scale);
	}

	public boolean same(Rect other) {
		return Math.abs(x - other.x) < 1.0E-3f && Math.abs(y - other.y) < 1.0E-3f && Math.abs(width - other.width) < 1.0E-3f;
	}
}

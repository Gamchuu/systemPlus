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

package dlindustries.uk.daduncs.utils;

public final class TimerUtils {
	private long lastMS;

	public TimerUtils() {
		this.reset();
	}

	public long getCurrentMS() {
		return System.nanoTime() / 1000000L;
	}

	public boolean hasReached(final double milliseconds) {
		return this.getCurrentMS() - this.lastMS >= milliseconds;
	}

	public void reset() {
		this.lastMS = this.getCurrentMS();
	}

	public long getElapsedTime() {
		return this.getCurrentMS() - this.lastMS;
	}

	public boolean delay(final float milliSec) {
		return this.getTime() - this.lastMS >= milliSec;
	}

	public long getTime() {
		return System.nanoTime() / 1000000L;
	}
}

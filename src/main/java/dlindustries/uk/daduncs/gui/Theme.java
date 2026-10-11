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

package dlindustries.uk.daduncs.gui;

public final class Theme {

	public static int accent = 0xFFC8A57D;
	public static int secondary = 0xFFFFFFFF;

	public static final int surface = 0xCC1A1A1A;
	public static final int card = 0xDB121212;
	public static final int cardHover = 0xDB151515;
	public static final int field = 0xB3404046;
	public static final int text = 0xFFF0F0F0;
	public static final int muted = 0xFF8A8A8E;
	public static final int off = 0x59FFFFFF;
	public static final int divider = 0x1FFFFFFF;

	public static final float BLUR = 57f;
	public static final float SHEEN = 0.55f;
	public static final float CARD_ROUND = 19f;
	public static final float PANEL_ROUND = 12f;
	public static final float SEARCH_ROUND = 18f;
	public static final int slot = 0xDB121212;

	public static int knob() {
		return lerp(accent, secondary, 0.45f);
	}

	public static int fade(int color, float alpha) {
		int a = (int) (((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
		return (a << 24) | (color & 0xFFFFFF);
	}

	public static int lerp(int from, int to, float progress) {
		float p = Math.max(0f, Math.min(1f, progress));
		int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * p);
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * p);
		int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * p);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * p);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private Theme() {
	}
}

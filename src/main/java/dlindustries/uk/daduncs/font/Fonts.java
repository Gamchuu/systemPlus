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

package dlindustries.uk.daduncs.font;

import dlindustries.uk.daduncs.render.skia.SkiaFont;

/**
 * The client's font styles. Each {@link SkiaFont} keeps the same bundled
 * TrueType file and size as before, so the API the UI calls
 * ({@code drawString} / {@code drawGlow} / {@code getStringWidth} /
 * {@code getFontHeight}) is unchanged; when {@code SkiaRenderer.ENABLED} is
 * true the text is shaped and drawn by Skia on the UI surface instead of the
 * legacy AWT glyph atlas.
 */
public final class Fonts {
	public static SkiaFont QUICKSAND = new SkiaFont("/assets/system/font/font.ttf", 40);
	public static SkiaFont BOLD = new SkiaFont("/assets/system/font/unbounded.ttf", 40, true);
	public static SkiaFont LIGHT = new SkiaFont("/assets/system/font/unbounded.ttf", 26);
	public static SkiaFont TIP = new SkiaFont("/assets/system/font/unbounded.ttf", 31);
	public static SkiaFont SMALL = new SkiaFont("/assets/system/font/unbounded.ttf", 18);
	public static SkiaFont SMALLBOLD = new SkiaFont("/assets/system/font/unbounded.ttf", 20, true);
	public static SkiaFont TITLE = new SkiaFont("/assets/system/font/unbounded.ttf", 30, true);
	public static SkiaFont MENU = new SkiaFont("/assets/system/font/unbounded.ttf", 24, true);
	public static SkiaFont TINY = new SkiaFont("/assets/system/font/unbounded.ttf", 15);
	public static SkiaFont LABEL = new SkiaFont("/assets/system/font/misans.ttf", 30);
	public static SkiaFont LABEL_MENU = new SkiaFont("/assets/system/font/misans.ttf", 26);
	public static SkiaFont LABEL_SMALL = new SkiaFont("/assets/system/font/misans.ttf", 18);
	public static SkiaFont PRODUCT_SANS_MEDIUM_36 = new SkiaFont("/assets/system/font/product_sans_medium.ttf", 36);
	public static SkiaFont PRODUCT_SANS_MEDIUM_18 = new SkiaFont("/assets/system/font/product_sans_medium.ttf", 18);
	public static SkiaFont PRODUCT_SANS_REGULAR_32 = new SkiaFont("/assets/system/font/product_sans_regular.ttf", 32);
	public static SkiaFont PRODUCT_SANS_REGULAR_16 = new SkiaFont("/assets/system/font/product_sans_regular.ttf", 16);

	public static void preload() {
		QUICKSAND.preload();
		BOLD.preload();
		LIGHT.preload();
		TIP.preload();
		SMALL.preload();
		SMALLBOLD.preload();
		TITLE.preload();
		MENU.preload();
		TINY.preload();
		LABEL.preload();
		LABEL_MENU.preload();
		LABEL_SMALL.preload();
		PRODUCT_SANS_MEDIUM_36.preload();
		PRODUCT_SANS_MEDIUM_18.preload();
		PRODUCT_SANS_REGULAR_32.preload();
		PRODUCT_SANS_REGULAR_16.preload();
	}
}
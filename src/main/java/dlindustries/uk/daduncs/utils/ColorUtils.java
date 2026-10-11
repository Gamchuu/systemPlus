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

package dlindustries.uk.daduncs.utils;

import java.awt.*;

public final class ColorUtils {
	public static Color getBreathingRGBColor(int increment, int alpha) {
		Color color = Color.getHSBColor((((((System.currentTimeMillis()) * 3) + increment * 175) % (360 * 20)) / (360f * 20)), 0.6f, 1f);
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	public static Color getMainColor(Color color, int n, int n2) {
		float[] fArray = new float[3];
		Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), fArray);

		float f = Math.abs(((float) (System.currentTimeMillis() % 2000) / 1000 + (float) n / (float) n2 * 2) % 2 - 1);
		fArray[2] = 0.25f + 0.75f * f % 2;

		int rgb = Color.HSBtoRGB(fArray[0], fArray[1], fArray[2]);
		return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color.getAlpha());
	}

	public static Color smoothColorTransition(float speed, Color toColor, Color fromColor) {
		return new Color((int) MathUtils.goodLerp(speed, fromColor.getRed(), toColor.getRed()),
                (int) MathUtils.goodLerp(speed, fromColor.getGreen(), toColor.getGreen()),
                (int) MathUtils.goodLerp(speed, fromColor.getBlue(), toColor.getBlue()));
	}

	public static Color smoothAlphaTransition(float speed, int toAlpha, Color fromColor) {
		return new Color(fromColor.getRed(),
				fromColor.getGreen(),
				fromColor.getBlue(),
                (int) MathUtils.goodLerp(speed, fromColor.getAlpha(), toAlpha));
	}
}
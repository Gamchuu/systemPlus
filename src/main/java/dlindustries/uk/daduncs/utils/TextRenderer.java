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

import dlindustries.uk.daduncs.font.Fonts;
import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

import static dlindustries.uk.daduncs.System.mc;


public final class TextRenderer {

	private static boolean modern() {
		return ClickGUI.modern.getValue();
	}

	private static void vanilla(CharSequence string, DrawContext context, int x, int y, int color, float scale, int drop) {
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();

		matrices.scale(scale, scale);
		context.drawText(mc.textRenderer, string.toString(), Math.round((x) / scale), Math.round((y - drop) / scale), color, false);

		matrices.popMatrix();
	}

	private static int vanillaWidth(CharSequence string, float scale) {
		return Math.round(mc.textRenderer.getWidth(string.toString()) * scale);
	}

	public static void drawString(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.9f, 8);
			return;
		}

		Fonts.QUICKSAND.drawString(context, string, x, y - 8, color);
	}

	public static void drawBoldGlow(CharSequence string, DrawContext context, int x, int y, int color, float spread) {
		if (!modern())
			return;

		Fonts.BOLD.drawGlow(context, string, x, y - 8, color, spread);
	}

	public static void drawBold(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.9f, 8);
			return;
		}

		Fonts.BOLD.drawString(context, string, x, y - 8, color);
	}

	public static void drawTitle(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.6f, 6);
			return;
		}

		Matrix3x2fStack matrices = context.getMatrices();

		for (int pass = 0; pass < 3; pass++) {
			matrices.pushMatrix();
			matrices.translate(pass * 0.4f, 0f);

			Fonts.TITLE.drawString(context, string, x, y - 6, color);

			matrices.popMatrix();
		}
	}

	public static void drawLabel(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.6f, 6);
			return;
		}

		Fonts.LABEL.drawString(context, string, x, y - 8, color);
	}

	public static void drawLabelMenu(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.35f, 4);
			return;
		}

		Fonts.LABEL_MENU.drawString(context, string, x, y - 8, color);
	}

	public static void drawLabelSmall(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.1f, 2);
			return;
		}

		Fonts.LABEL_SMALL.drawString(context, string, x, y - 5, color);
	}

	public static void drawMenu(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.35f, 4);
			return;
		}

		Fonts.MENU.drawString(context, string, x, y - 5, color);
	}

	public static void drawTip(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.45f, 5);
			return;
		}

		Fonts.TIP.drawString(context, string, x, y - 4, color);
	}

	public static void drawLight(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.2f, 3);
			return;
		}

		Fonts.LIGHT.drawString(context, string, x, y - 5, color);
	}

	public static void drawSmall(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.1f, 2);
			return;
		}

		Fonts.SMALL.drawString(context, string, x, y - 4, color);
	}

	public static void drawButton(CharSequence string, DrawContext context, float x, float y, int color) {
		if (!modern()) {
			vanilla(string, context, Math.round(x), Math.round(y), color, 1.1f, 4);
			return;
		}

		Fonts.PRODUCT_SANS_MEDIUM_18.drawString(context, string, x, y - 5.78f, color);
	}

	public static void drawHeadline(CharSequence string, DrawContext context, float x, float y, int color) {
		if (!modern()) {
			vanilla(string, context, Math.round(x), Math.round(y), color, 2f, 7);
			return;
		}

		Fonts.PRODUCT_SANS_MEDIUM_36.drawString(context, string, x, y - 9f, color);
	}

	public static void drawSmallBold(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 1.2f, 3);
			return;
		}

		Fonts.SMALLBOLD.drawString(context, string, x, y - 4, color);
	}

	public static void drawTiny(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 0.9f, 1);
			return;
		}

		Fonts.TINY.drawString(context, string, x, y - 4, color);
	}

	public static void drawBoldCentered(CharSequence string, DrawContext context, int x, int y, int color) {
		drawBold(string, context, x - boldWidth(string) / 2, y, color);
	}

	public static void drawCenteredString(CharSequence string, DrawContext context, int x, int y, int color) {
		drawString(string, context, x - getWidth(string) / 2, y, color);
	}

	public static int boldWidth(CharSequence string) {
		return modern() ? Fonts.BOLD.getStringWidth(string) : vanillaWidth(string, 1.9f);
	}

	public static int boldHeight() {
		return modern() ? Fonts.BOLD.getFontHeight() : 17;
	}

	public static int titleWidth(CharSequence string) {
		return modern() ? Fonts.TITLE.getStringWidth(string) : vanillaWidth(string, 1.6f);
	}

	public static int labelWidth(CharSequence string) {
		return modern() ? Fonts.LABEL.getStringWidth(string) : vanillaWidth(string, 1.6f);
	}

	public static int labelSmallWidth(CharSequence string) {
		return modern() ? Fonts.LABEL_SMALL.getStringWidth(string) : vanillaWidth(string, 1.1f);
	}

	public static int menuWidth(CharSequence string) {
		return modern() ? Fonts.MENU.getStringWidth(string) : vanillaWidth(string, 1.35f);
	}

	public static int tipWidth(CharSequence string) {
		return modern() ? Fonts.TIP.getStringWidth(string) : vanillaWidth(string, 1.45f);
	}

	public static int lightWidth(CharSequence string) {
		return modern() ? Fonts.LIGHT.getStringWidth(string) : vanillaWidth(string, 1.2f);
	}

	public static int smallWidth(CharSequence string) {
		return modern() ? Fonts.SMALL.getStringWidth(string) : vanillaWidth(string, 1.1f);
	}

	public static int buttonWidth(CharSequence string) {
		return modern() ? Fonts.PRODUCT_SANS_MEDIUM_18.getStringWidth(string) : vanillaWidth(string, 1.1f);
	}

	public static int headlineWidth(CharSequence string) {
		return modern() ? Fonts.PRODUCT_SANS_MEDIUM_36.getStringWidth(string) : vanillaWidth(string, 2f);
	}

	public static int smallBoldWidth(CharSequence string) {
		return modern() ? Fonts.SMALLBOLD.getStringWidth(string) : vanillaWidth(string, 1.2f);
	}

	public static int tinyWidth(CharSequence string) {
		return modern() ? Fonts.TINY.getStringWidth(string) : vanillaWidth(string, 0.9f);
	}

	public static int tinyHeight() {
		return modern() ? Fonts.TINY.getFontHeight() : 8;
	}

	public static int getWidth(CharSequence string) {
		return modern() ? Fonts.QUICKSAND.getStringWidth(string) : vanillaWidth(string, 1.9f);
	}

	public static void drawLargeString(CharSequence string, DrawContext context, int x, int y, int color) {
		if (!modern()) {
			vanilla(string, context, x, y, color, 2.6f, 12);
			return;
		}

		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();

		matrices.scale(1.4f, 1.4f);
		Fonts.QUICKSAND.drawString(context, string, x, y - 8, color);

		matrices.popMatrix();
	}

	public static void drawMinecraftText(CharSequence string, DrawContext context, int x, int y, int color) {
		vanilla(string, context, x, y, color, 2f, 8);
	}

	public static void DrawClickGuiWatermark(DrawContext context, int x, int y, float alpha) {
		if (!modern()) {
			vanilla(EncryptedString.of("System"), context, x, y + 5, Theme.fade(Theme.accent, alpha), 1.6f, 6);
			return;
		}

		String name = "System";
		int whiteColor = Theme.fade(0xFFFFFFFF, alpha);
		Fonts.PRODUCT_SANS_REGULAR_32.drawString(context, name, x, y, whiteColor);

		int nameWidth = Fonts.PRODUCT_SANS_REGULAR_32.getStringWidth(name);
		String version = dlindustries.uk.daduncs.System.INSTANCE.version.trim();
		int accentColor = Theme.fade(Theme.accent, Math.min(200f / 255f, alpha));
		Fonts.PRODUCT_SANS_REGULAR_16.drawString(context, version, x + nameWidth + 3, y - 2, accentColor);
	}
}

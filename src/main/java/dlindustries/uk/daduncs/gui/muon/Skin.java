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

import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.render.Draw;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

public final class Skin {

	public static final int LINE = 0xFFE4E4E8;

	public static void surface(DrawContext context, Rect rect, float radius, float alpha, float hover) {
		if (alpha <= 0.01f) return;

		int shade = Math.round((0.66f - 0.07f * hover) * 255);

		Draw.frost(context, rect.x(), rect.y(), rect.width(), rect.height(), radius, Theme.BLUR, (shade << 24) | 0x101010, alpha);
		ring(context, rect.x(), rect.y(), rect.width(), rect.height(), radius, 0.5f, white(alpha * 0.06f));
	}

	public static void fill(DrawContext context, float x, float y, float width, float height, float radius, int color) {
		Draw.shape(context, x, y, width, height, radius, 0f, color);
	}

	public static void ring(DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color) {
		Draw.shape(context, x, y, width, height, radius, thickness, color);
	}

	public static void circle(DrawContext context, float centerX, float centerY, float radius, int color) {
		Draw.shape(context, centerX - radius, centerY - radius, radius * 2, radius * 2, radius, 0f, color);
	}

	public static boolean push(DrawContext context, Rect rest, Rect animated) {
		if (rest.same(animated)) return false;

		float scale = animated.width() / rest.width();
		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(animated.centerX(), animated.centerY());
		matrices.scale(scale, scale);
		matrices.translate(-rest.centerX(), -rest.centerY());
		return true;
	}

	public static void open(Spring spring, boolean open) {
		if (open) spring.configure(260, 0.58f);
		else spring.configure(340, 1f);

		spring.to(open ? 1 : 0);
	}

	public static float alpha(float progress) {
		return Math.clamp(progress * 2f, 0f, 1f);
	}

	public static int white(float alpha) {
		return Theme.fade(0xFFFFFFFF, alpha);
	}

	public static int black(float alpha) {
		return Theme.fade(0xFF000000, alpha);
	}

	public static void title(DrawContext context, String text, float x, float centerY, int color) {
		draw(context, MuonFont.MEDIUM, text, x, centerY, 12.5f, color);
	}

	public static float titleWidth(String text) {
		return MuonFont.MEDIUM.width(text, 12.5f);
	}

	public static void name(DrawContext context, String text, float x, float centerY, int color) {
		draw(context, MuonFont.MEDIUM, text, x, centerY, 11.5f, color);
	}

	public static float nameWidth(String text) {
		return MuonFont.MEDIUM.width(text, 11.5f);
	}

	public static void text(DrawContext context, String text, float x, float centerY, int color) {
		draw(context, MuonFont.REGULAR, text, x, centerY, 9.5f, color);
	}

	public static float width(String text) {
		return MuonFont.REGULAR.width(text, 9.5f);
	}

	public static void tiny(DrawContext context, String text, float x, float centerY, int color) {
		draw(context, MuonFont.REGULAR, text, x, centerY, 9f, color);
	}

	public static float tinyWidth(String text) {
		return MuonFont.REGULAR.width(text, 9f);
	}

	private static void draw(DrawContext context, MuonFont font, String text, float x, float centerY, float size, int color) {
		font.draw(context, text, x, centerY - font.cap(size) / 2, size, color);
	}

	private Skin() {
	}
}

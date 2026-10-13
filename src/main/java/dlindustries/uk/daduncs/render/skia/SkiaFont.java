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

package dlindustries.uk.daduncs.render.skia;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.font.GlyphPageFontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ClipMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.types.Rect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2f;

/**
 * Skija-backed drop-in for {@link GlyphPageFontRenderer} as held by
 * {@link dlindustries.uk.daduncs.font.Fonts}. Exposes the same methods the
 * client's call sites use - {@code drawString}, {@code drawGlow},
 * {@code getStringWidth}, {@code getFontHeight} - so no file that renders text
 * needs to change.
 *
 * <p>Coordinates are transformed from GUI space into window pixels by multiplying
 * the matrix translation by the window scale factor, and the legacy fonts rasterized
 * at 2x, so the Skia port draws and measures at half the configured size to keep
 * every layout identical. When {@link SkiaRenderer#ENABLED} is false the old AWT
 * glyph-atlas renderer is used as a fallback, keeping the default pipeline untouched.
 */
public final class SkiaFont {

	private final String resource;
	private final float size;
	private final boolean bold;

	private Font skiaFont;
	private GlyphPageFontRenderer legacy;

	public SkiaFont(String resource, float size) {
		this(resource, size, false);
	}

	public SkiaFont(String resource, float size, boolean bold) {
		this.resource = resource;
		this.size = size;
		this.bold = bold;
	}

	private static float guiScale() {
		MinecraftClient mc = System.mc;
		if (mc == null || mc.getWindow() == null) {
			return 1f;
		}
		return (float) mc.getWindow().getScaleFactor();
	}

	/**
	 * Pushes the active scissor rectangle (scaled GUI coords) onto the Skija
	 * canvas as a physical-pixel clip and returns the canvas so the caller can
	 * {@code restore()} it. Returns {@code null} when Skia is unavailable.
	 */
	private static Canvas pushScissor(DrawContext context, float gs) {
		Canvas canvas = SkiaRenderer.canvas();
		if (canvas == null) {
			return null;
		}
		canvas.save();
		ScreenRect scissor = context.scissorStack.peekLast();
		if (scissor != null) {
			canvas.clipRect(Rect.makeLTRB(scissor.getLeft() * gs, scissor.getTop() * gs,
					scissor.getRight() * gs, scissor.getBottom() * gs), ClipMode.INTERSECT, false);
		}
		return canvas;
	}

	public int drawString(DrawContext context, CharSequence text, float x, float y, int color) {
		if (!SkiaRenderer.text()) {
			return legacy() == null ? 0 : legacy().drawString(context, text, x, y, color);
		}
		if (text == null || text.isEmpty() || (color >>> 24) == 0) {
			return 0;
		}
		Matrix3x2f m = context.getMatrices();
		float scale = scaleY(m);
		float gs = guiScale();
		Canvas canvas = pushScissor(context, gs);
		if (canvas == null) {
			return getStringWidth(text);
		}
		try {
			SkiaText.drawText(font(), text.toString(), tx(m, x, y) * gs, ty(m, x, y) * gs,
					new TextStyle(drawSize() * scale * gs, color));
		} finally {
			canvas.restore();
		}
		return getStringWidth(text);
	}

	public void drawGlow(DrawContext context, CharSequence text, float x, float y, int color, float spread) {
		if (!SkiaRenderer.text()) {
			if (legacy() != null) {
				legacy().drawGlow(context, text, x, y, color, spread);
			}
			return;
		}
		if (text == null || text.isEmpty() || (color >>> 24) == 0) {
			return;
		}
		Matrix3x2f m = context.getMatrices();
		float scale = scaleY(m);
		float gs = guiScale();
		Canvas canvas = pushScissor(context, gs);
		if (canvas == null) {
			return;
		}
		try {
			SkiaText.drawGlow(font(), text.toString(), tx(m, x, y) * gs, ty(m, x, y) * gs,
					drawSize() * scale * gs, color, spread * scale * gs);
		} finally {
			canvas.restore();
		}
	}

	public void drawWrapped(DrawContext context, CharSequence text, float x, float y, float maxWidth, int color) {
		if (!SkiaRenderer.text()) {
			if (legacy() != null) {
				legacy().drawString(context, text, x, y, color);
			}
			return;
		}
		if (text == null || text.isEmpty() || (color >>> 24) == 0) {
			return;
		}
		Matrix3x2f m = context.getMatrices();
		float scale = scaleY(m);
		float gs = guiScale();
		Canvas canvas = pushScissor(context, gs);
		if (canvas == null) {
			return;
		}
		try {
			SkiaText.drawWrappedText(font(), text.toString(), tx(m, x, y) * gs, ty(m, x, y) * gs,
					maxWidth * scale * gs, new TextStyle(drawSize() * scale * gs, color));
		} finally {
			canvas.restore();
		}
	}

	public int getWrappedHeight(CharSequence text, float maxWidth) {
		if (!SkiaRenderer.text()) {
			return getFontHeight();
		}
		if (text == null) {
			return 0;
		}
		return Math.max(1, Math.round(SkiaText.getWrappedTextHeight(font(), text.toString(), maxWidth, drawSize())));
	}

	public void drawGradient(DrawContext context, CharSequence text, float x, float y, int colorStart, int colorEnd, SkiaGradient direction) {
		if (!SkiaRenderer.text()) {
			drawString(context, text, x, y, colorStart);
			return;
		}
		if (text == null || text.isEmpty()) {
			return;
		}
		Matrix3x2f m = context.getMatrices();
		float scale = scaleY(m);
		float gs = guiScale();
		Canvas canvas = pushScissor(context, gs);
		if (canvas == null) {
			return;
		}
		try {
			SkiaText.drawGradientText(font(), text.toString(), tx(m, x, y) * gs, ty(m, x, y) * gs,
					new TextStyle(drawSize() * scale * gs, 0), colorStart, colorEnd, direction);
		} finally {
			canvas.restore();
		}
	}

	private final java.util.Map<String, Integer> widthCache = new java.util.concurrent.ConcurrentHashMap<>(128);
	private final java.util.Map<String, float[]> advancesCache = new java.util.concurrent.ConcurrentHashMap<>(64);
	private int cachedHeight = -1;

	public int getStringWidth(CharSequence text) {
		if (!SkiaRenderer.text()) {
			return legacy() == null ? 0 : legacy().getStringWidth(text);
		}
		if (text == null || text.isEmpty()) {
			return 0;
		}
		String s = text.toString();
		Integer cached = widthCache.get(s);
		if (cached != null) {
			return cached;
		}
		int w = Math.round(SkiaText.getTextWidth(font(), s, drawSize()));
		if (widthCache.size() < 512) {
			widthCache.put(s, w);
		}
		return w;
	}

	public int getFontHeight() {
		if (!SkiaRenderer.text()) {
			return legacy() == null ? 1 : legacy().getFontHeight();
		}
		if (cachedHeight > 0) {
			return cachedHeight;
		}
		cachedHeight = Math.max(1, Math.round(SkiaText.getTextHeight(font(), drawSize())));
		return cachedHeight;
	}

	/** Loads whatever backend is active now, so the first real draw does not hitch. */
	public void preload() {
		if (SkiaRenderer.text()) {
			font();
			getFontHeight();
		} else {
			legacy();
		}
	}

	/** The Skia {@link Font} for this style, cached for the life of the client. */
	public Font font() {
		if (skiaFont == null || skiaFont.isClosed()) {
			skiaFont = new Font(SkiaText.typeface(resource), drawSize());
			if (bold) {
				skiaFont.setEmboldened(true);
			}
			skiaFont.setSubpixel(true);
		}
		return skiaFont;
	}

	/**
	 * Computes the exact advances for each character of {@code text} when shaped
	 * as a single coherent string, preventing overlapping letters in character-by-character loops.
	 */
	public float[] getCharacterAdvances(CharSequence text) {
		if (text == null || text.isEmpty()) {
			return new float[0];
		}
		if (!SkiaRenderer.text()) {
			GlyphPageFontRenderer l = legacy();
			String s = text.toString();
			float[] adv = new float[s.length()];
			if (l != null) {
				for (int i = 0; i < s.length(); i++) {
					adv[i] = l.getStringWidth(String.valueOf(s.charAt(i)));
				}
			}
			return adv;
		}
		String s = text.toString();
		float[] cached = advancesCache.get(s);
		if (cached != null) {
			return cached;
		}
		float[] adv = SkiaText.getCharacterAdvances(font(), s, drawSize());
		if (advancesCache.size() < 128) {
			advancesCache.put(s, adv);
		}
		return adv;
	}

	/** The legacy renderer, created on demand and only used while {@code ENABLED} is false. */
	private GlyphPageFontRenderer legacy() {
		if (legacy == null) {
			legacy = GlyphPageFontRenderer.createStyled(resource, Math.round(size),
					bold ? java.awt.Font.BOLD : java.awt.Font.PLAIN);
		}
		return legacy;
	}

	/**
	 * The legacy atlas rasterized at 2x and drew at scale 0.5, so the effective
	 * on-screen size is half of the configured one; the Skia port draws and
	 * measures at the same effective size for identical layout.
	 */
	public float drawSize() {
		return size * 0.5f;
	}

	private static float tx(Matrix3x2f m, float x, float y) {
		return m.m00() * x + m.m10() * y + m.m20();
	}

	private static float ty(Matrix3x2f m, float x, float y) {
		return m.m01() * x + m.m11() * y + m.m21();
	}

	private static float scaleY(Matrix3x2f m) {
		return (float) Math.hypot(m.m10(), m.m11());
	}
}
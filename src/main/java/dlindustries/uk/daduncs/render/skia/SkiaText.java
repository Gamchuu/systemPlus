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

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.FilterBlurMode;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.MaskFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.skija.TextLine;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.skija.paragraph.FontCollection;
import io.github.humbleui.skija.paragraph.Paragraph;
import io.github.humbleui.skija.paragraph.ParagraphBuilder;
import io.github.humbleui.skija.paragraph.ParagraphStyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skija text primitives for the Skia UI surface, ported and expanded from the
 * Skia text template. Bundled TrueType fonts are loaded through Skia's
 * {@link FontMgr} and drawn with {@link TextLine} or Skija paragraphs straight
 * onto {@link SkiaRenderer#canvas()}.
 */
public final class SkiaText {

	public record FontEntry(Font font, Typeface typeface) {
	}

	public record ShapedKey(int typefaceId, int size, boolean bold, String text) {
	}

	public record ShapedText(TextLine line, float width, float ascent, float descent, float[] advances) {
	}

	private static final Map<String, FontEntry> fontEntries = new ConcurrentHashMap<>();
	private static final int MAX_CACHE_ENTRIES = 512;
	private static final Map<ShapedKey, ShapedText> textLineCache = java.util.Collections.synchronizedMap(
			new java.util.LinkedHashMap<>(128, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<ShapedKey, ShapedText> eldest) {
					return size() > MAX_CACHE_ENTRIES;
				}
			}
	);
	private static final Paint TEXT_PAINT = new Paint();
	private static final Paint GLOW_PAINT = new Paint();
	private static FontCollection fontCollection;

	private SkiaText() {
	}
	public static synchronized FontCollection getFontCollection() {
		if (fontCollection == null || fontCollection.isClosed()) {
			fontCollection = new FontCollection();
			fontCollection.setDefaultFontManager(FontMgr.getDefault());
		}
		return fontCollection;
	}
	/** Bundled regular font fallback. */
	public static Font getRegularFont() {
		return loadFont("/assets/system/font/product_sans_regular.ttf").font();
	}
	/** Bundled bold font fallback. */
	public static Font getBoldFont() {
		return loadFont("/assets/system/font/unbounded.ttf").font();
	}
	/** Loads (and caches) a {@link FontEntry} for a classpath font resource. */
	public static FontEntry loadFont(String resourcePath) {
		FontEntry cached = fontEntries.get(resourcePath);
		if (cached != null && !cached.font().isClosed() && !cached.typeface().isClosed()) {
			return cached;
		}
		byte[] bytes = readResourceBytes(resourcePath);
		if (bytes == null) {
			throw new IllegalArgumentException("Font resource not found: " + resourcePath);
		}
		Typeface typeface = FontMgr.getDefault().makeFromData(Data.makeFromBytes(bytes));
		if (typeface == null) {
			throw new IllegalArgumentException("Invalid font data: " + resourcePath);
		}
		Font font = new Font(typeface);
		FontEntry entry = new FontEntry(font, typeface);
		fontEntries.put(resourcePath, entry);
		return entry;
	}

	/** Loads (and caches) the typeface for a classpath font resource. */
	public static Typeface typeface(String resourcePath) {
		return loadFont(resourcePath).typeface();
	}

	/** Returns the cached Skia {@link Font} for a resource. */
	public static Font font(String resourcePath) {
		return loadFont(resourcePath).font();
	}

	/** Retrieves the cached {@link Typeface} matching a {@link Font}. */
	public static Typeface getTypeface(Font font) {
		if (font == null) {
			return null;
		}
		for (FontEntry entry : fontEntries.values()) {
			if (entry.font() == font) {
				return entry.typeface();
			}
		}
		return font.getTypeface();
	}

	/**
	 * Retrieves or builds cached shaped text ({@link TextLine}) for {@code text}.
	 * Shapes text with HarfBuzz once per unique string and font size, with zero per-frame allocation.
	 */
	public static ShapedText getShapedText(Font font, String text, float fontSize) {
		if (text == null || text.isEmpty() || fontSize <= 0f) {
			return null;
		}
		Typeface tf = getTypeface(font);
		int tfId = tf != null ? tf.getUniqueId() : 0;
		boolean bold = font.isEmboldened();
		int intSize = Math.max(1, Math.round(fontSize));
		ShapedKey key = new ShapedKey(tfId, intSize, bold, text);
		ShapedText cached = textLineCache.get(key);
		if (cached != null && cached.line() != null && !cached.line().isClosed()) {
			return cached;
		}

		font.setSize((float) intSize);
		TextLine line = TextLine.make(text, font);
		float width = line.getWidth();
		float ascent = line.getAscent();
		float descent = line.getDescent();

		float[] positions = line.getPositions();
		int len = text.length();
		float[] advances = new float[len];
		for (int i = 0; i < len; i++) {
			float currentX = (i * 2 < positions.length) ? positions[i * 2] : 0f;
			float nextX = ((i + 1) * 2 < positions.length) ? positions[(i + 1) * 2] : width;
			advances[i] = Math.max(0f, nextX - currentX);
		}

		ShapedText shaped = new ShapedText(line, width, ascent, descent, advances);
		textLineCache.put(key, shaped);
		return shaped;
	}

	public static void clearTextCache() {
		textLineCache.clear();
	}

	/** Draws {@code text} at ({@code x}, {@code y}) with the given {@link TextStyle}. */
	public static void drawText(Font font, String text, float x, float y, TextStyle style) {
		Canvas canvas = SkiaRenderer.canvas();
		if (canvas == null || text == null || text.isEmpty() || style == null || style.fontSize() <= 0f) {
			return;
		}
		SkiaRenderer.markDirty();
		ShapedText shaped = getShapedText(font, text, style.fontSize());
		if (shaped == null || shaped.line() == null) {
			return;
		}
		float baseline = y - shaped.ascent() - 1f;
		TEXT_PAINT.reset();
		TEXT_PAINT.setColor(style.color());
		TEXT_PAINT.setAntiAlias(true);
		canvas.drawTextLine(shaped.line(), x, baseline, TEXT_PAINT);
	}

	/** Draws {@code text} at ({@code x}, {@code y}) at {@code fontSize} with {@code color}. */
	public static void drawText(Font font, String text, float x, float y, float fontSize, int color) {
		drawText(font, text, x, y, new TextStyle(fontSize, color));
	}

	/** Legacy alias for {@link #drawText(Font, String, float, float, float, int)}. */
	public static void draw(Font font, String text, float x, float y, float fontSize, int color) {
		drawText(font, text, x, y, fontSize, color);
	}

	/** Measures advance width of {@code text} at {@code fontSize}. */
	public static float getTextWidth(Font font, String text, float fontSize) {
		if (text == null || text.isEmpty() || fontSize <= 0f) {
			return 0f;
		}
		ShapedText shaped = getShapedText(font, text, fontSize);
		return shaped != null ? shaped.width() : 0f;
	}

	/**
	 * Computes the exact advance for each character in {@code text} when shaped
	 * as a coherent word at {@code fontSize}, preserving HarfBuzz kerning and
	 * tracking so characters rendered individually never overlap.
	 */
	public static float[] getCharacterAdvances(Font font, String text, float fontSize) {
		if (text == null || text.isEmpty() || fontSize <= 0f) {
			return new float[0];
		}
		ShapedText shaped = getShapedText(font, text, fontSize);
		return shaped != null ? shaped.advances() : new float[0];
	}

	/** Measures text height (-ascent) at {@code fontSize}. */
	public static float getTextHeight(Font font, float fontSize) {
		if (fontSize <= 0f) {
			return 0f;
		}
		ShapedText shaped = getShapedText(font, "Ag", fontSize);
		return shaped != null ? -shaped.ascent() : 0f;
	}

	/** Measures total line height (-ascent + descent) at {@code fontSize}. */
	public static float getLineHeight(Font font, float fontSize) {
		if (fontSize <= 0f) {
			return 0f;
		}
		ShapedText shaped = getShapedText(font, "Ag", fontSize);
		return shaped != null ? -shaped.ascent() + shaped.descent() : 0f;
	}

	/** Legacy alias for {@link #getTextWidth(Font, String, float)}. */
	public static float width(Font font, String text, float fontSize) {
		return getTextWidth(font, text, fontSize);
	}

	/** Legacy alias for {@link #getTextHeight(Font, float)}. */
	public static float height(Font font, float fontSize) {
		return getTextHeight(font, fontSize);
	}

	/** Builds a Skija wrapped {@link Paragraph} for layout and painting. */
	public static Paragraph buildParagraph(Typeface typeface, String text, float maxWidth, TextStyle style) {
		try (ParagraphStyle pStyle = new ParagraphStyle();
			 io.github.humbleui.skija.paragraph.TextStyle tStyle = new io.github.humbleui.skija.paragraph.TextStyle()) {
			tStyle.setColor(style.color());
			tStyle.setFontSize(style.fontSize());
			tStyle.setTypeface(typeface);

			try (ParagraphBuilder builder = new ParagraphBuilder(pStyle, getFontCollection())) {
				builder.pushStyle(tStyle);
				builder.addText(text);
				Paragraph paragraph = builder.build();
				paragraph.layout(maxWidth);
				return paragraph;
			}
		}
	}

	/** Draws wrapped multiline text bounded by {@code maxWidth}. */
	public static void drawWrappedText(Font font, String text, float x, float y, float maxWidth, TextStyle style) {
		Canvas canvas = SkiaRenderer.canvas();
		if (canvas == null || text == null || text.isEmpty() || style == null) {
			return;
		}
		Typeface typeface = getTypeface(font);
		if (typeface == null) {
			return;
		}
		SkiaRenderer.markDirty();
		try (Paragraph paragraph = buildParagraph(typeface, text, maxWidth, style)) {
			paragraph.paint(canvas, x, y);
		}
	}

	/** Calculates the height of wrapped multiline text bounded by {@code maxWidth}. */
	public static float getWrappedTextHeight(Font font, String text, float maxWidth, float fontSize) {
		if (text == null || text.isEmpty() || fontSize <= 0f) {
			return 0f;
		}
		Typeface typeface = getTypeface(font);
		if (typeface == null) {
			return 0f;
		}
		try (Paragraph paragraph = buildParagraph(typeface, text, maxWidth, new TextStyle(fontSize, 0))) {
			return paragraph.getHeight();
		}
	}

	/** Draws text with a linear color gradient. */
	public static void drawGradientText(
			Font font,
			String text,
			float x,
			float y,
			TextStyle style,
			int colorStart,
			int colorEnd,
			SkiaGradient direction
	) {
		Canvas canvas = SkiaRenderer.canvas();
		if (canvas == null || text == null || text.isEmpty() || style == null || style.fontSize() <= 0f) {
			return;
		}
		SkiaRenderer.markDirty();
		ShapedText shaped = getShapedText(font, text, style.fontSize());
		if (shaped == null || shaped.line() == null) {
			return;
		}
		float baseline = y - shaped.ascent() - 1f;
		float width = shaped.width();

		float x1 = (direction == SkiaGradient.LEFT_TO_RIGHT) ? x + width : x;
		float y1 = (direction == SkiaGradient.TOP_TO_BOTTOM) ? y + style.fontSize() : y;

		try (Shader shader = Shader.makeLinearGradient(x, y, x1, y1, new int[]{colorStart, colorEnd})) {
			TEXT_PAINT.reset();
			TEXT_PAINT.setShader(shader);
			TEXT_PAINT.setAntiAlias(true);
			canvas.drawTextLine(shaped.line(), x, baseline, TEXT_PAINT);
			TEXT_PAINT.setShader(null);
		}
	}

	/** Soft glow halo effect behind the text - blur pass only (the caller draws sharp text separately). */
	public static void drawGlow(Font font, String text, float x, float y, float fontSize, int color, float spread) {
		Canvas canvas = SkiaRenderer.canvas();
		if (canvas == null || text == null || text.isEmpty() || fontSize <= 0f || spread <= 0.05f) {
			return;
		}
		SkiaRenderer.markDirty();
		ShapedText shaped = getShapedText(font, text, fontSize);
		if (shaped == null || shaped.line() == null) {
			return;
		}
		float baseline = y - shaped.ascent() - 1f;
		try (MaskFilter filter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, spread)) {
			GLOW_PAINT.reset();
			GLOW_PAINT.setColor(color);
			GLOW_PAINT.setAntiAlias(true);
			GLOW_PAINT.setMaskFilter(filter);
			canvas.drawTextLine(shaped.line(), x, baseline, GLOW_PAINT);
			GLOW_PAINT.setMaskFilter(null);
		}
	}

	private static byte[] readResourceBytes(String resourcePath) {
		String trimmed = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
		String withSlash = "/" + trimmed;

		InputStream in = SkiaText.class.getResourceAsStream(withSlash);
		if (in == null) {
			in = SkiaText.class.getResourceAsStream(trimmed);
		}
		if (in == null) {
			in = Thread.currentThread().getContextClassLoader().getResourceAsStream(trimmed);
		}
		if (in == null) {
			return null;
		}
		try (InputStream s = in) {
			return s.readAllBytes();
		} catch (IOException ignored) {
			return null;
		}
	}
}
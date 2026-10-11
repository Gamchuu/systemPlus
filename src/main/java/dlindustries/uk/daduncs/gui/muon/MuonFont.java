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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dlindustries.uk.daduncs.render.Draw;
import dlindustries.uk.daduncs.render.GuiState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MuonFont {

	public static final MuonFont MEDIUM = new MuonFont(Identifier.of("system", "font/muon_medium.ttf"));
	public static final MuonFont REGULAR = new MuonFont(Identifier.of("system", "font/muon_regular.ttf"));

	private static final int ATLAS = 1024;
	private static final int PADDING = 2;
	private static final int[] CURVE = new int[256];

	static {
		for (int i = 0; i < 256; i++)
			CURVE[i] = Math.round((float) Math.pow(i / 255.0, 0.85) * 255);
	}

	private final Identifier location;
	private final Map<Integer, Page> pages = new HashMap<>();
	private Font font;

	private MuonFont(Identifier location) {
		this.location = location;
	}

	public void draw(DrawContext context, String text, float x, float y, float size, int color) {
		if (text.isEmpty() || (color >>> 24) == 0) return;

		float scale = scale();
		Page page = page(size, scale);

		List<Glyph> glyphs = new ArrayList<>(text.length());
		for (int i = 0; i < text.length(); i++)
			glyphs.add(page.glyph(text.charAt(i)));

		page.upload();

		float originX = Math.round(x * scale);
		float baseline = Math.round(y * scale + page.cap);
		float width = width(text, size);
		Matrix3x2f matrix = new Matrix3x2f(context.getMatrices());

		ScreenRect bounds = Draw.bounds(context, matrix, x, (baseline - page.ascent) / scale, width, (page.ascent + page.descent) / scale);
		if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) return;

		TextureSetup setup = TextureSetup.of(page.texture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
		context.state.addSimpleElement(new GuiState(RenderPipelines.GUI_TEXTURED, setup, context, bounds, buffer -> {
			float pen = 0;
			for (Glyph glyph : glyphs) {
				if (glyph.width > 0) {
					float left = (originX + Math.round(pen) + glyph.offsetX) / scale;
					float top = (baseline + glyph.offsetY) / scale;
					float right = left + glyph.width / scale;
					float bottom = top + glyph.height / scale;

					vertex(buffer, matrix, left, top, glyph.u0, glyph.v0, color);
					vertex(buffer, matrix, left, bottom, glyph.u0, glyph.v1, color);
					vertex(buffer, matrix, right, bottom, glyph.u1, glyph.v1, color);
					vertex(buffer, matrix, right, top, glyph.u1, glyph.v0, color);
				}
				pen += glyph.advance;
			}
		}));
	}

	private static void vertex(VertexConsumer buffer, Matrix3x2f matrix, float x, float y, float u, float v, int color) {
		float tx = matrix.m00() * x + matrix.m10() * y + matrix.m20();
		float ty = matrix.m01() * x + matrix.m11() * y + matrix.m21();
		buffer.vertex(tx, ty, 0f).texture(u, v).color(color);
	}

	public float width(String text, float size) {
		float scale = scale();
		Page page = page(size, scale);

		float pen = 0;
		for (int i = 0; i < text.length(); i++)
			pen += page.glyph(text.charAt(i)).advance;

		return pen / scale;
	}

	public float cap(float size) {
		float scale = scale();
		return page(size, scale).cap / scale;
	}

	private Page page(float size, float scale) {
		int pixels = Math.max(1, Math.round(size * scale));
		return pages.computeIfAbsent(pixels, Page::new);
	}

	private Font font() {
		if (font == null) {
			try (InputStream stream = MinecraftClient.getInstance().getResourceManager().open(location)) {
				font = Font.createFont(Font.TRUETYPE_FONT, stream);
			} catch (Exception e) {
				font = new Font(Font.SANS_SERIF, Font.PLAIN, 1);
			}
		}
		return font;
	}

	private static float scale() {
		return (float) MinecraftClient.getInstance().getWindow().getScaleFactor();
	}

	private record Glyph(float u0, float v0, float u1, float v1, int offsetX, int offsetY, int width, int height, float advance) {
	}

	private final class Page {

		private final Font font;
		private final FontRenderContext context = new FontRenderContext(null, true, true);
		private final NativeImageBackedTexture texture;
		private final NativeImage image;
		private final Map<Character, Glyph> glyphs = new HashMap<>();
		private final float ascent;
		private final float descent;
		private final float cap;

		private int cursorX = PADDING;
		private int cursorY = PADDING;
		private int rowHeight;
		private boolean dirty;

		private Page(int pixels) {
			this.font = font().deriveFont((float) pixels);
			this.texture = new NativeImageBackedTexture(() -> "muon font " + location + " " + pixels, ATLAS, ATLAS, true);
			this.image = texture.getImage();

			var metrics = font.getLineMetrics("Hg", context);
			this.ascent = metrics.getAscent();
			this.descent = metrics.getDescent();
			this.cap = (float) font.createGlyphVector(context, "H").getVisualBounds().getHeight();

			for (char c = 32; c < 127; c++)
				glyph(c);
		}

		private Glyph glyph(char c) {
			return glyphs.computeIfAbsent(c, this::rasterize);
		}

		private Glyph rasterize(char c) {
			GlyphVector vector = font.createGlyphVector(context, String.valueOf(c));
			float advance = vector.getGlyphMetrics(0).getAdvanceX();
			Rectangle bounds = vector.getPixelBounds(context, 0, 0);
			if (bounds.isEmpty())
				return new Glyph(0, 0, 0, 0, 0, 0, 0, 0, advance);

			int width = bounds.width + PADDING * 2;
			int height = bounds.height + PADDING * 2;
			if (cursorX + width > ATLAS) {
				cursorX = PADDING;
				cursorY += rowHeight + PADDING;
				rowHeight = 0;
			}
			if (cursorY + height > ATLAS)
				return new Glyph(0, 0, 0, 0, 0, 0, 0, 0, advance);

			BufferedImage glyphImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = glyphImage.createGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setColor(Color.WHITE);
			graphics.drawGlyphVector(vector, PADDING - bounds.x, PADDING - bounds.y);
			graphics.dispose();

			for (int py = 0; py < height; py++) {
				for (int px = 0; px < width; px++) {
					int alpha = glyphImage.getRGB(px, py) >>> 24;
					if (alpha > 0)
						image.setColorArgb(cursorX + px, cursorY + py, (CURVE[alpha] << 24) | 0xFFFFFF);
				}
			}

			Glyph glyph = new Glyph(
					cursorX / (float) ATLAS, cursorY / (float) ATLAS,
					(cursorX + width) / (float) ATLAS, (cursorY + height) / (float) ATLAS,
					bounds.x - PADDING, bounds.y - PADDING,
					width, height,
					advance
			);

			cursorX += width + PADDING;
			rowHeight = Math.max(rowHeight, height);
			dirty = true;
			return glyph;
		}

		private void upload() {
			if (dirty) {
				texture.upload();
				dirty = false;
			}
		}
	}
}

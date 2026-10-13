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

import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorFilter;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.PaintStrokeCap;
import io.github.humbleui.skija.PaintStrokeJoin;
import io.github.humbleui.skija.Path;
import io.github.humbleui.skija.PathOp;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

/**
 * Skia-backed implementations of the public {@link dlindustries.uk.daduncs.render.Draw} API.
 * Every method keeps the exact signature of its counterpart in Draw so no call site changes.
 *
 * <p>Coordinates are placed directly onto the Skia canvas using DrawContext's matrix stack
 * and GUI scale factor, maintaining pixel-perfect layout and crisp anti-aliasing.
 */
public final class SkiaDraw {

	private static final Paint FILL_PAINT = new Paint();
	private static final Paint STROKE_PAINT = new Paint();
	private static final Paint IMAGE_PAINT = new Paint();
	private static final Paint EFFECT_PAINT = new Paint();

	private SkiaDraw() {
	}

	// ------------------------------------------------------------------ shapes

	public static void rect(DrawContext context, float x, float y, float w, float h, float[] radii, int color) {
		if ((color >>> 24) == 0 || w <= 0f || h <= 0f) {
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			FILL_PAINT.reset();
			FILL_PAINT.setColor(color);
			FILL_PAINT.setAntiAlias(rounded(radii));
			fillShape(canvas, FILL_PAINT, x, y, w, h, radii);
		} finally {
			canvas.restore();
		}
	}

	public static void border(DrawContext context, float x, float y, float w, float h,
							  float[] radii, float thickness, int color) {
		if ((color >>> 24) == 0 || w <= 0f || h <= 0f || thickness <= 0.01f) {
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			STROKE_PAINT.reset();
			STROKE_PAINT.setColor(color);
			STROKE_PAINT.setAntiAlias(true);
			STROKE_PAINT.setMode(PaintMode.STROKE);
			STROKE_PAINT.setStrokeWidth(thickness);
			float half = thickness * 0.5f;
			float ix = x + half, iy = y + half, iw = w - thickness, ih = h - thickness;
			if (iw > 0f && ih > 0f) {
				fillShape(canvas, STROKE_PAINT, ix, iy, iw, ih, radii);
			}
		} finally {
			canvas.restore();
		}
	}

	public static void shape(DrawContext context, float x, float y, float w, float h,
							 float radius, float thickness, int color) {
		if ((color >>> 24) == 0 || w <= 0f || h <= 0f) {
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			float r = Math.min(Math.max(0f, radius), Math.min(w, h) * 0.5f);
			if (thickness > 0.01f) {
				STROKE_PAINT.reset();
				STROKE_PAINT.setColor(color);
				STROKE_PAINT.setAntiAlias(true);
				STROKE_PAINT.setMode(PaintMode.STROKE);
				STROKE_PAINT.setStrokeWidth(thickness);
				float half = thickness * 0.5f;
				float ix = x + half, iy = y + half, iw = w - thickness, ih = h - thickness;
				if (iw > 0f && ih > 0f) {
					if (r >= Math.min(w, h) * 0.48f) {
						canvas.drawOval(Rect.makeXYWH(ix, iy, iw, ih), STROKE_PAINT);
					} else {
						canvas.drawRRect(RRect.makeXYWH(ix, iy, iw, ih, Math.max(0f, r - half)), STROKE_PAINT);
					}
				}
			} else {
				FILL_PAINT.reset();
				FILL_PAINT.setColor(color);
				FILL_PAINT.setAntiAlias(r > 0.01f);
				if (r >= Math.min(w, h) * 0.48f) {
					canvas.drawOval(Rect.makeXYWH(x, y, w, h), FILL_PAINT);
				} else if (r > 0.01f) {
					canvas.drawRRect(RRect.makeXYWH(x, y, w, h, r), FILL_PAINT);
				} else {
					canvas.drawRect(Rect.makeXYWH(x, y, w, h), FILL_PAINT);
				}
			}
		} finally {
			canvas.restore();
		}
	}

	public static void picker(DrawContext context, float x, float y, float w, float h,
							  float round, float hue, int mode) {
		if (w <= 0f || h <= 0f) {
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			clipRound(canvas, x, y, w, h, round);

			if (mode == 1) {
				// Hue rail - vertical rainbow
				float[] stops = new float[]{0f, 1f / 6f, 2f / 6f, 3f / 6f, 4f / 6f, 5f / 6f, 1f};
				int[] colors = new int[stops.length];
				for (int i = 0; i < stops.length; i++) {
					colors[i] = 0xFF000000 | wheelRgb(stops[i]);
				}
				try (Shader shader = Shader.makeLinearGradient(x, y, x, y + h, colors, stops)) {
					EFFECT_PAINT.reset();
					EFFECT_PAINT.setColor(0xFFFFFFFF);
					EFFECT_PAINT.setAntiAlias(true);
					EFFECT_PAINT.setShader(shader);
					canvas.drawRect(Rect.makeXYWH(x, y, w, h), EFFECT_PAINT);
					EFFECT_PAINT.setShader(null);
				}
				return;
			}

			// SV square
			int hueColor = 0xFF000000 | wheelRgb(hue);
			try (Shader base = Shader.makeLinearGradient(x, y, x + w, y,
					new int[]{0xFFFFFFFF, hueColor}, new float[]{0f, 1f})) {
				EFFECT_PAINT.reset();
				EFFECT_PAINT.setColor(0xFFFFFFFF);
				EFFECT_PAINT.setAntiAlias(true);
				EFFECT_PAINT.setShader(base);
				canvas.drawRect(Rect.makeXYWH(x, y, w, h), EFFECT_PAINT);
				EFFECT_PAINT.setShader(null);
			}

			try (Shader shade = Shader.makeLinearGradient(x, y, x, y + h,
					new int[]{0x00000000, 0xFF000000}, new float[]{0f, 1f})) {
				EFFECT_PAINT.reset();
				EFFECT_PAINT.setColor(0xFFFFFFFF);
				EFFECT_PAINT.setAntiAlias(true);
				EFFECT_PAINT.setShader(shade);
				canvas.drawRect(Rect.makeXYWH(x, y, w, h), EFFECT_PAINT);
				EFFECT_PAINT.setShader(null);
			}
		} finally {
			canvas.restore();
		}
	}

	// --------------------------------------------------------------- textured

	public static void image(DrawContext context, float x, float y, float w, float h,
							 float[] radii, Identifier texture, int color) {
		if ((color >>> 24) == 0 || w <= 0f || h <= 0f) {
			return;
		}
		SkiaRenderer.SkiaTexture sk = SkiaRenderer.textureImage(texture);
		if (sk == null) {
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			if (rounded(radii)) {
				clipShape(canvas, x, y, w, h, radii);
			}
			Image img = sk.image();
			Rect src = Rect.makeWH(sk.width(), sk.height());
			Rect dst = Rect.makeXYWH(x, y, w, h);
			drawTinted(canvas, img, src, dst, color);
		} finally {
			canvas.restore();
		}
	}

	public static void head(DrawContext context, float x, float y, float w, float h,
							float u, float v, float uw, float vh, float round, Identifier texture, int color) {
		if ((color >>> 24) == 0 || w <= 0f || h <= 0f) {
			return;
		}
		SkiaRenderer.SkiaTexture sk = SkiaRenderer.textureImage(texture);
		if (sk == null) {
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			if (round > 0.01f) {
				clipRound(canvas, x, y, w, h, round);
			}
			Image img = sk.image();
			float scaleU = sk.width() / 64f;
			float scaleV = sk.height() / 64f;
			Rect src = Rect.makeLTRB(u * scaleU, v * scaleV, (u + uw) * scaleU, (v + vh) * scaleV);
			Rect dst = Rect.makeXYWH(x, y, w, h);
			drawTintedNearest(canvas, img, src, dst, color);
		} finally {
			canvas.restore();
		}
	}

	// ------------------------------------------------------------------- blur

	public static void blur(DrawContext context, float x, float y, float w, float h,
							float round, float radius, int color, float opacity) {
		float shown = clamp01(opacity);
		Image scene = SkiaRenderer.sceneImage();
		if (scene == null || !SkiaRenderer.sceneCopied()) {
			rect(context, x, y, w, h, new float[]{round, round, round, round}, dim(color, shown));
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			if (round > 0.01f) {
				clipRound(canvas, x, y, w, h, round);
			}
			drawBackdrop(canvas, scene, context.getMatrices(), x, y, w, h, radius, color, shown);
		} finally {
			canvas.restore();
		}
	}

	public static void glass(DrawContext context, float x, float y, float w, float h,
							 float round, float radius, float sheen, int color, float opacity) {
		float shown = clamp01(opacity);
		Image scene = SkiaRenderer.sceneImage();
		if (scene == null || !SkiaRenderer.sceneCopied()) {
			rect(context, x, y, w, h, new float[]{round, round, round, round}, dim(color, shown));
			return;
		}
		blur(context, x, y, w, h, round, radius, color, shown);
		if (sheen <= 0f || shown <= 0f) {
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			if (round > 0.01f) {
				clipRound(canvas, x, y, w, h, round);
			}
			try (Shader shader = Shader.makeLinearGradient(x, y, x, y + h * 0.35f,
					new int[]{((int) (sheen * shown * 255f) & 0xFF) << 24 | 0x00FFFFFF, 0x00000000},
					new float[]{0f, 1f})) {
				EFFECT_PAINT.reset();
				EFFECT_PAINT.setColor(0xFFFFFFFF);
				EFFECT_PAINT.setAntiAlias(true);
				EFFECT_PAINT.setShader(shader);
				canvas.drawRect(Rect.makeXYWH(x, y, w, h), EFFECT_PAINT);
				EFFECT_PAINT.setShader(null);
			}
		} finally {
			canvas.restore();
		}
	}

	public static void bubble(DrawContext context, float x, float y, float w, float h,
							  float round, float radius, float sheen, float lens, int color, float opacity, boolean overGui) {
		float shown = clamp01(opacity);
		boolean gui = overGui && SkiaRenderer.behindImage() != null;
		Image source = gui ? SkiaRenderer.behindImage() : SkiaRenderer.sceneImage();
		if (source == null) {
			rect(context, x, y, w, h, new float[]{round, round, round, round}, dim(color, shown));
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			if (round > 0.01f) {
				clipRound(canvas, x, y, w, h, round);
			}
			drawBackdrop(canvas, source, context.getMatrices(), x, y, w, h, radius, color, shown);
			if (sheen > 0f) {
				try (Shader shader = Shader.makeLinearGradient(x, y, x, y + h * 0.3f,
						new int[]{((int) (sheen * shown * 220f) & 0xFF) << 24 | 0x00FFFFFF, 0x00000000},
						new float[]{0f, 1f})) {
					EFFECT_PAINT.reset();
					EFFECT_PAINT.setColor(0xFFFFFFFF);
					EFFECT_PAINT.setAntiAlias(true);
					EFFECT_PAINT.setShader(shader);
					canvas.drawRect(Rect.makeXYWH(x, y, w, h), EFFECT_PAINT);
					EFFECT_PAINT.setShader(null);
				}
			}
			if (lens > 0f) {
				int alpha = (int) (lens * shown * 120f) & 0xFF;
				try (Shader shader = Shader.makeLinearGradient(x, y, x, y + h * 0.5f,
						new int[]{alpha << 24 | 0x00FFFFFF, 0x00000000}, new float[]{0f, 1f})) {
					EFFECT_PAINT.reset();
					EFFECT_PAINT.setColor(0xFFFFFFFF);
					EFFECT_PAINT.setAntiAlias(true);
					EFFECT_PAINT.setShader(shader);
					canvas.drawRect(Rect.makeXYWH(x, y, w, h), EFFECT_PAINT);
					EFFECT_PAINT.setShader(null);
				}
			}
		} finally {
			canvas.restore();
		}
	}

	public static void stairFill(DrawContext context, float x, float y, float w, float h,
								 float above, float below, float round, float radius, int color, float opacity) {
		stair(context, x, y, w, h, above, below, round, radius, color, opacity, false);
	}

	public static void stairGlass(DrawContext context, float x, float y, float w, float h,
								  float above, float below, float round, float radius, int color, float opacity) {
		stair(context, x, y, w, h, above, below, round, radius, color, opacity, true);
	}

	private static void stair(DrawContext context, float x, float y, float w, float h,
							  float above, float below, float round, float radius,
							  int color, float opacity, boolean sheen) {
		float shown = clamp01(opacity);
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			try (Path shape = stairPath(x, y, w, h, round, above, below)) {
				canvas.save();
				try {
					canvas.clipPath(shape, true);
					Image scene = SkiaRenderer.sceneImage();
					if (scene != null && SkiaRenderer.sceneCopied()) {
						drawBackdrop(canvas, scene, context.getMatrices(), x, y, w, h, radius, color, shown);
					} else {
						FILL_PAINT.reset();
						FILL_PAINT.setColor(dim(color, shown));
						FILL_PAINT.setAntiAlias(true);
						canvas.drawRect(Rect.makeXYWH(x, y, w, h), FILL_PAINT);
					}
					if (sheen && shown > 0f) {
						try (Shader shader = Shader.makeLinearGradient(x, y, x, y + h * 0.25f,
								new int[]{((int) (shown * 90f) & 0xFF) << 24 | 0x00FFFFFF, 0x00000000},
								new float[]{0f, 1f})) {
							EFFECT_PAINT.reset();
							EFFECT_PAINT.setColor(0xFFFFFFFF);
							EFFECT_PAINT.setAntiAlias(true);
							EFFECT_PAINT.setShader(shader);
							canvas.drawRect(Rect.makeXYWH(x, y, w, h), EFFECT_PAINT);
							EFFECT_PAINT.setShader(null);
						}
					}
				} finally {
					canvas.restore();
				}
			}
		} finally {
			canvas.restore();
		}
	}

	public static void stairBorder(DrawContext context, float x, float y, float w, float h,
								   float above, float below, float round, float thickness, int color) {
		if ((color >>> 24) == 0 || thickness <= 0.01f) {
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			try (Path shape = stairPath(x, y, w, h, round, above, below)) {
				STROKE_PAINT.reset();
				STROKE_PAINT.setColor(color);
				STROKE_PAINT.setAntiAlias(true);
				STROKE_PAINT.setMode(PaintMode.STROKE);
				STROKE_PAINT.setStrokeCap(PaintStrokeCap.ROUND);
				STROKE_PAINT.setStrokeJoin(PaintStrokeJoin.ROUND);
				STROKE_PAINT.setStrokeWidth(thickness);
				canvas.drawPath(shape, STROKE_PAINT);
			}
		} finally {
			canvas.restore();
		}
	}

	private static Path stairPath(float x, float y, float w, float h, float round, float above, float below) {
		float bend = Math.min(Math.max(0f, round), Math.min(w, h));
		float own = Math.max(0f, w - bend);
		boolean up = above > 0.5f;
		boolean down = below > 0.5f;

		float topLeft = up ? 0f : bend;
		float topRight = up ? 0f : bend;
		float bottomLeft = down ? 0f : bend;

		Path path = rrectPath(x, y, x + own, y + h, topLeft, topRight, bend, bottomLeft);

		if (up) {
			Path extension = rrectPath(x, y - h, x + Math.min(above, own), y + h, 0f, 0f, bend, bottomLeft);
			path = union(path, extension);
		}
		if (down) {
			Path extension = rrectPath(x, y, x + Math.min(below, own), y + 2f * h, topLeft, topRight, 0f, 0f);
			path = union(path, extension);
		}
		return path;
	}

	private static Path rrectPath(float l, float t, float r, float b,
								  float topLeft, float topRight, float bottomRight, float bottomLeft) {
		float max = Math.min(r - l, b - t) * 0.5f;
		float[] radii = {
				clampRound(topLeft, max), clampRound(topLeft, max),
				clampRound(topRight, max), clampRound(topRight, max),
				clampRound(bottomRight, max), clampRound(bottomRight, max),
				clampRound(bottomLeft, max), clampRound(bottomLeft, max)
		};
		return new Path().addRRect(RRect.makeComplexLTRB(l, t, r, b, radii));
	}

	private static Path union(Path a, Path b) {
		Path combined = Path.makeCombining(a, b, PathOp.UNION);
		a.close();
		b.close();
		return combined;
	}

	public static void frost(DrawContext context, float x, float y, float w, float h,
							 float radius, float spread, int color, float opacity) {
		float shown = clamp01(opacity);
		Image scene = SkiaRenderer.sceneImage();
		if (scene == null || !SkiaRenderer.sceneCopied()) {
			shape(context, x, y, w, h, radius, 0f, dim(color, shown));
			return;
		}
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			if (radius > 0.01f) {
				clipRound(canvas, x, y, w, h, radius);
			}
			drawBackdrop(canvas, scene, context.getMatrices(), x, y, w, h, spread, color, shown);
			EFFECT_PAINT.reset();
			EFFECT_PAINT.setColor(0x66FFFFFF);
			EFFECT_PAINT.setAntiAlias(true);
			EFFECT_PAINT.setAlphaf(shown * 0.4f);
			canvas.drawRect(Rect.makeXYWH(x, y, w, h), EFFECT_PAINT);
		} finally {
			canvas.restore();
		}
	}

	public static void glyphGlow(DrawContext context, float x, float y, float w, float h,
								 float glyphX, float glyphY, float spread, Identifier texture, int color, float opacity) {
		SkiaRenderer.SkiaTexture sk = SkiaRenderer.textureImage(texture);
		if (sk == null) {
			return;
		}
		float shown = clamp01(opacity);
		SkiaRenderer.markDirty();
		if (!begin(context, x, y, w, h)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			Image img = sk.image();
			float glyphW = Math.max(1f, w - spread * 2f);
			float glyphH = Math.max(1f, h - spread * 2f);
			Rect src = Rect.makeLTRB(glyphX, glyphY, glyphX + glyphW, glyphY + glyphH);
			Rect dst = Rect.makeXYWH(x, y, w, h);

			EFFECT_PAINT.reset();
			EFFECT_PAINT.setColor(0xFFFFFFFF);
			EFFECT_PAINT.setAntiAlias(true);
			try (ImageFilter filter = ImageFilter.makeBlur(Math.max(0.1f, spread), Math.max(0.1f, spread), FilterTileMode.CLAMP)) {
				EFFECT_PAINT.setImageFilter(filter);
				if (color != 0xFFFFFFFF || shown < 1f) {
					int tinted = dim(color, shown);
					try (ColorFilter cf = ColorFilter.makeBlend(tinted, BlendMode.MODULATE)) {
						EFFECT_PAINT.setColorFilter(cf);
						canvas.drawImageRect(img, src, dst, SamplingMode.LINEAR, EFFECT_PAINT, false);
						EFFECT_PAINT.setColorFilter(null);
					}
				} else {
					canvas.drawImageRect(img, src, dst, SamplingMode.LINEAR, EFFECT_PAINT, false);
				}
				EFFECT_PAINT.setImageFilter(null);
			}
		} finally {
			canvas.restore();
		}
	}

	// -------------------------------------------------------------------- line

	public static void line(DrawContext context, float[] points, float[] widths, int color) {
		if ((color >>> 24) == 0) {
			return;
		}
		int count = points.length / 2;
		if (count < 2) {
			return;
		}
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		float widest = 0f;
		for (int i = 0; i < count; i++) {
			minX = Math.min(minX, points[i * 2]);
			minY = Math.min(minY, points[i * 2 + 1]);
			maxX = Math.max(maxX, points[i * 2]);
			maxY = Math.max(maxY, points[i * 2 + 1]);
			widest = Math.max(widest, widths[i]);
		}
		SkiaRenderer.markDirty();
		if (!begin(context, minX, minY, maxX - minX, maxY - minY)) {
			return;
		}
		Canvas canvas = SkiaRenderer.canvas();
		try {
			try (Path path = new Path()) {
				path.moveTo(points[0], points[1]);
				for (int i = 1; i < count; i++) {
					path.lineTo(points[i * 2], points[i * 2 + 1]);
				}
				STROKE_PAINT.reset();
				STROKE_PAINT.setColor(color);
				STROKE_PAINT.setAntiAlias(true);
				STROKE_PAINT.setMode(PaintMode.STROKE);
				STROKE_PAINT.setStrokeWidth(Math.max(0.5f, widest));
				STROKE_PAINT.setStrokeCap(PaintStrokeCap.ROUND);
				STROKE_PAINT.setStrokeJoin(PaintStrokeJoin.ROUND);
				canvas.drawPath(path, STROKE_PAINT);
			}
		} finally {
			canvas.restore();
		}
	}

	// ----------------------------------------------------------------- helpers

	private static boolean begin(DrawContext context, float x, float y, float w, float h) {
		Canvas canvas = SkiaRenderer.canvas();
		if (canvas == null) {
			return false;
		}
		Matrix3x2f m = context.getMatrices();
		ScreenRect scissor = context.scissorStack.peekLast();
		if (scissor != null && m != null) {
			ScreenRect rect = new ScreenRect((int) Math.floor(x), (int) Math.floor(y),
					(int) Math.ceil(w) + 1, (int) Math.ceil(h) + 1).transformEachVertex(m);
			ScreenRect clipped = scissor.intersection(rect);
			if (clipped == null || clipped.width() <= 0 || clipped.height() <= 0) {
				return false;
			}
		}
		canvas.save();
		float gs = guiScale();
		canvas.scale(gs, gs);
		if (scissor != null) {
			canvas.clipRect(Rect.makeXYWH(scissor.getLeft(), scissor.getTop(), scissor.width(), scissor.height()));
		}
		if (m != null) {
			Matrix33 sk = new Matrix33(
					m.m00(), m.m10(), m.m20(),
					m.m01(), m.m11(), m.m21(),
					0f,      0f,      1f
			);
			canvas.concat(sk);
		}
		return true;
	}

	private static void drawTinted(Canvas canvas, Image img, Rect src, Rect dst, int color) {
		IMAGE_PAINT.reset();
		IMAGE_PAINT.setAntiAlias(true);
		if (color != 0xFFFFFFFF) {
			try (ColorFilter filter = ColorFilter.makeBlend(color, BlendMode.MODULATE)) {
				IMAGE_PAINT.setColorFilter(filter);
				canvas.drawImageRect(img, src, dst, SamplingMode.LINEAR, IMAGE_PAINT, false);
				IMAGE_PAINT.setColorFilter(null);
			}
		} else {
			canvas.drawImageRect(img, src, dst, SamplingMode.LINEAR, IMAGE_PAINT, false);
		}
	}

	private static void drawTintedNearest(Canvas canvas, Image img, Rect src, Rect dst, int color) {
		IMAGE_PAINT.reset();
		IMAGE_PAINT.setAntiAlias(false);
		if (color != 0xFFFFFFFF) {
			try (ColorFilter filter = ColorFilter.makeBlend(color, BlendMode.MODULATE)) {
				IMAGE_PAINT.setColorFilter(filter);
				canvas.drawImageRect(img, src, dst, SamplingMode.DEFAULT, IMAGE_PAINT, false);
				IMAGE_PAINT.setColorFilter(null);
			}
		} else {
			canvas.drawImageRect(img, src, dst, SamplingMode.DEFAULT, IMAGE_PAINT, false);
		}
	}

	private static void fillShape(Canvas canvas, Paint p, float x, float y, float w, float h, float[] radii) {
		if (rounded(radii)) {
			float maxRound = Math.min(w, h) * 0.5f;
			if (radii[0] == radii[1] && radii[0] == radii[2] && radii[0] == radii[3]) {
				float r = Math.min(Math.max(0f, radii[0]), maxRound);
				if (r > 0.01f) {
					canvas.drawRRect(RRect.makeXYWH(x, y, w, h, r), p);
					return;
				}
			} else {
				float rTL = clampRound(radii[0], maxRound);
				float rBL = clampRound(radii[1], maxRound);
				float rBR = clampRound(radii[2], maxRound);
				float rTR = clampRound(radii[3], maxRound);
				float[] packed = new float[]{rTL, rTL, rTR, rTR, rBR, rBR, rBL, rBL};
				canvas.drawRRect(RRect.makeComplexXYWH(x, y, w, h, packed), p);
				return;
			}
		}
		canvas.drawRect(Rect.makeXYWH(x, y, w, h), p);
	}

	private static void clipShape(Canvas canvas, float x, float y, float w, float h, float[] radii) {
		if (rounded(radii)) {
			float maxRound = Math.min(w, h) * 0.5f;
			if (radii[0] == radii[1] && radii[0] == radii[2] && radii[0] == radii[3]) {
				float r = Math.min(Math.max(0f, radii[0]), maxRound);
				if (r > 0.01f) {
					canvas.clipRRect(RRect.makeXYWH(x, y, w, h, r), true);
					return;
				}
			} else {
				float rTL = clampRound(radii[0], maxRound);
				float rBL = clampRound(radii[1], maxRound);
				float rBR = clampRound(radii[2], maxRound);
				float rTR = clampRound(radii[3], maxRound);
				float[] packed = new float[]{rTL, rTL, rTR, rTR, rBR, rBR, rBL, rBL};
				canvas.clipRRect(RRect.makeComplexXYWH(x, y, w, h, packed), true);
			}
		}
	}

	private static void clipRound(Canvas canvas, float x, float y, float w, float h, float round) {
		if (round > 0.01f) {
			float maxRound = Math.min(w, h) * 0.5f;
			float r = Math.min(round, maxRound);
			if (r > 0.01f) {
				canvas.clipRRect(RRect.makeXYWH(x, y, w, h, r), true);
			}
		}
	}

	private static boolean rounded(float[] radii) {
		if (radii == null) {
			return false;
		}
		if (radii.length >= 4) {
			return radii[0] > 0.01f || radii[1] > 0.01f || radii[2] > 0.01f || radii[3] > 0.01f;
		}
		return false;
	}

	private static float clampRound(float r, float max) {
		return Math.min(Math.max(0f, r), max);
	}

	private static float guiScale() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null || mc.getWindow() == null) {
			return 1f;
		}
		return (float) mc.getWindow().getScaleFactor();
	}

	private static void drawBackdrop(Canvas canvas, Image source, Matrix3x2f m, float x, float y, float w, float h,
									 float blurPx, int color, float opacity) {
		float shown = clamp01(opacity);
		if (source == null || shown <= 0f) {
			return;
		}
		float gs = guiScale();
		Rect bounds = Rect.makeXYWH(x, y, w, h);

		float sigma = Math.max(0f, blurPx) / gs;

		float m00 = m != null ? m.m00() : 1f;
		float m10 = m != null ? m.m10() : 0f;
		float m20 = m != null ? m.m20() : 0f;
		float m01 = m != null ? m.m01() : 0f;
		float m11 = m != null ? m.m11() : 1f;
		float m21 = m != null ? m.m21() : 0f;

		float p0x = (m00 * x + m10 * y + m20) * gs;
		float p0y = (m01 * x + m11 * y + m21) * gs;
		float p1x = (m00 * (x + w) + m10 * (y + h) + m20) * gs;
		float p1y = (m01 * (x + w) + m11 * (y + h) + m21) * gs;

		float sw = source.getWidth();
		float sh = source.getHeight();
		float sx0 = clamp(Math.min(p0x, p1x), 0f, sw);
		float sy0 = clamp(Math.min(p0y, p1y), 0f, sh);
		float sx1 = clamp(Math.max(p0x, p1x), 0f, sw);
		float sy1 = clamp(Math.max(p0y, p1y), 0f, sh);
		if (sx1 - sx0 <= 0f || sy1 - sy0 <= 0f) {
			return;
		}

		EFFECT_PAINT.reset();
		EFFECT_PAINT.setAntiAlias(true);
		EFFECT_PAINT.setAlphaf(shown);
		ImageFilter filter = null;
		try {
			if (sigma > 0.01f) {
				filter = ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP);
				EFFECT_PAINT.setImageFilter(filter);
			}
			Rect src = Rect.makeLTRB(sx0, sy0, sx1, sy1);
			canvas.drawImageRect(source, src, bounds, SamplingMode.LINEAR, EFFECT_PAINT, false);
		} finally {
			if (filter != null) {
				filter.close();
			}
			EFFECT_PAINT.setImageFilter(null);
		}

		if (((color >>> 24) & 0xFF) != 0) {
			FILL_PAINT.reset();
			FILL_PAINT.setColor(dim(color, shown));
			FILL_PAINT.setAntiAlias(true);
			canvas.drawRect(bounds, FILL_PAINT);
		}
	}

	private static float clamp(float v, float lo, float hi) {
		return v < lo ? lo : (v > hi ? hi : v);
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	private static int dim(int color, float opacity) {
		int alpha = (int) (((color >>> 24) & 0xFF) * opacity);
		return (alpha << 24) | (color & 0xFFFFFF);
	}

	private static int wheelRgb(float hue) {
		float h = ((hue % 1f) + 1f) % 1f;
		float r = tri(h + 0f);
		float g = tri(h + 2f / 3f);
		float b = tri(h + 1f / 3f);
		return ((int) (r * 255f) << 16) | ((int) (g * 255f) << 8) | (int) (b * 255f);
	}

	private static float tri(float h) {
		return Math.max(0f, Math.min(1f, Math.abs((h % 1f) * 6f - 3f) - 1f));
	}
}
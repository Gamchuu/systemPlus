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
import dlindustries.uk.daduncs.render.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class Wire {

	private static final int SEGMENTS = 28;
	private static final float WIDTH = 0.5f;
	private static final float OPACITY = 0.75f;

	private final float bend;
	private final float phase;

	public Wire(long seed) {
		Random random = new Random(seed);
		this.bend = random.nextFloat() * 2 - 1;
		this.phase = random.nextFloat() * MathHelper.TAU;
	}

	public void render(DrawContext context, Rect from, Rect to, float alpha) {
		if (alpha <= 0.01f) return;

		Anchor start = Anchor.of(from, to.centerX(), to.centerY());
		Anchor end = Anchor.of(to, from.centerX(), from.centerY());

		float dx = end.x - start.x;
		float dy = end.y - start.y;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length < 2) return;

		float perpX = -dy / length;
		float perpY = dx / length;
		float reach = Math.clamp(length * 0.4f, 6, 60);
		float bow = bend * length * 0.1f;

		float c1x = start.x + start.normalX * reach + perpX * bow;
		float c1y = start.y + start.normalY * reach + perpY * bow;
		float c2x = end.x + end.normalX * reach + perpX * bow;
		float c2y = end.y + end.normalY * reach + perpY * bow;

		float wobble = Math.min(0.6f, length * 0.012f);
		float[] points = new float[(SEGMENTS + 1) * 2];
		float[] widths = new float[SEGMENTS + 1];

		for (int i = 0; i <= SEGMENTS; i++) {
			float t = i / (float) SEGMENTS;
			float u = 1 - t;
			float a = u * u * u;
			float b = 3 * u * u * t;
			float c = 3 * u * t * t;
			float d = t * t * t;

			float offset = wobble * MathHelper.sin(MathHelper.PI * t) * MathHelper.sin(t * 7.5f + phase);

			points[i * 2] = a * start.x + b * c1x + c * c2x + d * end.x + perpX * offset;
			points[i * 2 + 1] = a * start.y + b * c1y + c * c2y + d * end.y + perpY * offset;
			widths[i] = WIDTH * (0.85f + 0.3f * (0.5f + 0.5f * MathHelper.sin(t * 4f + phase * 1.3f)));
		}

		Render2D.drawLine(context, points, widths, Theme.fade(Skin.LINE, alpha * OPACITY));
	}

	private record Anchor(float x, float y, float normalX, float normalY) {

		private static final float SHAPE = 4;

		private static Anchor of(Rect box, float targetX, float targetY) {
			float halfWidth = box.width() / 2;
			float halfHeight = box.height() / 2;
			float dx = targetX - box.centerX();
			float dy = targetY - box.centerY();
			if (Math.abs(dx) < 1.0E-4f && Math.abs(dy) < 1.0E-4f)
				dy = 1;

			float scale = (float) Math.pow(Math.pow(Math.abs(dx / halfWidth), SHAPE) + Math.pow(Math.abs(dy / halfHeight), SHAPE), -1 / SHAPE);
			float x = dx * scale;
			float y = dy * scale;

			float normalX = Math.signum(x) * (float) Math.pow(Math.abs(x / halfWidth), SHAPE - 1) / halfWidth;
			float normalY = Math.signum(y) * (float) Math.pow(Math.abs(y / halfHeight), SHAPE - 1) / halfHeight;
			float length = (float) Math.sqrt(normalX * normalX + normalY * normalY);

			return new Anchor(box.centerX() + x, box.centerY() + y, normalX / length, normalY / length);
		}
	}
}

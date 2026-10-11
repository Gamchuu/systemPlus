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

package dlindustries.uk.daduncs.render;

import dlindustries.uk.daduncs.mixin.RenderLayerAccessor;
import dlindustries.uk.daduncs.render.renderers.pipeline.PipelineInitializer;
import dlindustries.uk.daduncs.render.renderers.pipeline.PipelineRegistry;
import dlindustries.uk.daduncs.utils.RenderUtils;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;

public final class SkyPainter {

	private static final float FAR = 100f;

	private static RenderLayer layer;
	private static int color;
	private static float time;
	private static float turn;
	private static float tint;
	private static float night;
	private static float waves;
	private static float stars;
	private static float clouds;

	public static void render(int sky, float clock, float day, float colour, float dark,
							  float ripple, float dots, float puffs) {
		color = sky;
		time = clock;
		turn = day;
		tint = colour;
		night = dark;
		waves = ripple;
		stars = dots;
		clouds = puffs;

		RenderUtils.drawLayer(layer(), SkyPainter::cube);
	}

	private static void cube(VertexConsumer buffer) {
		face(buffer, -FAR, FAR, -FAR, FAR, FAR, -FAR, FAR, -FAR, -FAR, -FAR, -FAR, -FAR);
		face(buffer, -FAR, FAR, FAR, -FAR, -FAR, FAR, FAR, -FAR, FAR, FAR, FAR, FAR);
		face(buffer, -FAR, FAR, -FAR, -FAR, -FAR, -FAR, -FAR, -FAR, FAR, -FAR, FAR, FAR);
		face(buffer, FAR, FAR, FAR, FAR, -FAR, FAR, FAR, -FAR, -FAR, FAR, FAR, -FAR);
		face(buffer, -FAR, FAR, FAR, FAR, FAR, FAR, FAR, FAR, -FAR, -FAR, FAR, -FAR);
		face(buffer, -FAR, -FAR, -FAR, FAR, -FAR, -FAR, FAR, -FAR, FAR, -FAR, -FAR, FAR);
	}

	private static void face(VertexConsumer buffer,
							 float x1, float y1, float z1, float x2, float y2, float z2,
							 float x3, float y3, float z3, float x4, float y4, float z4) {
		corner(buffer, x1, y1, z1);
		corner(buffer, x2, y2, z2);
		corner(buffer, x3, y3, z3);
		corner(buffer, x4, y4, z4);
	}

	private static void corner(VertexConsumer buffer, float x, float y, float z) {
		buffer.vertex(x, y, z)
				.color(color)
				.texture(time, turn)
				.overlay(0, 0)
				.light(Math.round(tint * 100f), Math.round(night * 100f))
				.normal(waves * 100f / 127f, stars * 100f / 127f, clouds * 100f / 127f);
	}

	private static RenderLayer layer() {
		if (layer == null) {
			PipelineInitializer.init();
			layer = RenderLayerAccessor.invokeOf("system_sky",
					RenderSetup.builder(PipelineRegistry.SKY_PIPELINE)
							.expectedBufferSize(1536)
							.build());
		}

		return layer;
	}

	private SkyPainter() {
	}
}

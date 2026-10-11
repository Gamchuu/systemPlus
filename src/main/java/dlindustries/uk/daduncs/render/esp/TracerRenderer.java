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

package dlindustries.uk.daduncs.render.esp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

public final class TracerRenderer {

	private static final float WIDTH = 1.5f;
	private static final float START = 2.0f;

	private static RenderLayer layer;

	private final MinecraftClient mc = MinecraftClient.getInstance();
	private final VertexConsumerProvider.Immediate buffers = VertexConsumerProvider.immediate(new BufferAllocator(1536));
	private final List<Tracer> tracers = new ArrayList<>();

	public void add(Vec3d target, WorldRenderState world, int color) {
		tracers.add(new Tracer(target.subtract(world.cameraRenderState.pos), color | 0xFF000000));
	}

	public void draw() {
		if (tracers.isEmpty()) return;

		Vector3fc forward = mc.gameRenderer.getCamera().getHorizontalPlane();
		Vec3d start = new Vec3d(forward.x() * START, forward.y() * START, forward.z() * START);
		VertexConsumer buffer = buffers.getBuffer(layer());

		for (Tracer tracer : tracers) {
			Vec3d direction = tracer.end().subtract(start).normalize();
			line(buffer, start, direction, tracer.color());
			line(buffer, tracer.end(), direction, tracer.color());
		}

		buffers.draw();
		tracers.clear();
	}

	private static void line(VertexConsumer buffer, Vec3d position, Vec3d direction, int color) {
		buffer.vertex((float) position.x, (float) position.y, (float) position.z)
				.color(color)
				.normal((float) direction.x, (float) direction.y, (float) direction.z)
				.lineWidth(WIDTH);
	}

	private static RenderLayer layer() {
		if (layer == null) {
			layer = RenderLayer.of("system_esp_tracer", RenderSetup.builder(EspPipelines.TRACER).build());
		}
		return layer;
	}

	private record Tracer(Vec3d end, int color) {
	}
}

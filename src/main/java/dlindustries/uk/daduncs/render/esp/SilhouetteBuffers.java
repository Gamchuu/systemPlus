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

package dlindustries.uk.daduncs.render.esp;

import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;

import java.util.LinkedHashMap;

final class SilhouetteBuffers {

	static final class Discard extends VertexConsumerProvider.Immediate {

		Discard() {
			super(new BufferAllocator(256), new LinkedHashMap<>());
		}

		@Override
		public VertexConsumer getBuffer(RenderLayer layer) {
			return Noop.INSTANCE;
		}

		@Override
		public void drawCurrentLayer() {
		}

		@Override
		public void draw() {
		}

		@Override
		public void draw(RenderLayer layer) {
		}
	}

	static final class Outlines extends OutlineVertexConsumerProvider {

		private final VertexConsumerProvider.Immediate buffers = VertexConsumerProvider.immediate(new BufferAllocator(1536));
		private int color = -1;

		@Override
		public VertexConsumer getBuffer(RenderLayer layer) {
			RenderLayer outline = layer.isOutline() ? layer : layer.getAffectedOutline().orElse(null);
			if (outline == null) {
				return Noop.INSTANCE;
			}
			return new FlatColor(buffers.getBuffer(outline), color);
		}

		@Override
		public void setColor(int color) {
			this.color = color;
		}

		@Override
		public void draw() {
			buffers.draw();
		}
	}

	private record FlatColor(VertexConsumer delegate, int color) implements VertexConsumer {

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			delegate.vertex(x, y, z).color(color);
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer color(int color) {
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			delegate.texture(u, v);
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer lineWidth(float width) {
			return this;
		}
	}

	private enum Noop implements VertexConsumer {
		INSTANCE;

		@Override
		public VertexConsumer vertex(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer color(int color) {
			return this;
		}

		@Override
		public VertexConsumer texture(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer overlay(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer light(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer lineWidth(float width) {
			return this;
		}
	}

	private SilhouetteBuffers() {
	}
}

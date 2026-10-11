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

package dlindustries.uk.daduncs.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;

import java.util.function.Consumer;

public record GuiState(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		ScreenRect scissorArea,
		ScreenRect bounds,
		Consumer<VertexConsumer> vertices
) implements SimpleGuiElementRenderState {

	public GuiState(RenderPipeline pipeline, TextureSetup textureSetup, DrawContext context, ScreenRect bounds, Consumer<VertexConsumer> vertices) {
		this(pipeline, textureSetup, context.scissorStack.peekLast(), bounds, vertices);
	}

	@Override
	public void setupVertices(VertexConsumer consumer) {
		vertices.accept(consumer);
	}
}

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

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;

import java.util.OptionalInt;

final class FrameBlur {

	private static final int PASSES = 2;

	private static SimpleFramebuffer primary;
	private static SimpleFramebuffer secondary;

	static GpuTextureView blur() {
		Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
		if (main.getColorAttachmentView() == null) return null;

		int width = Math.max(1, main.textureWidth / 2);
		int height = Math.max(1, main.textureHeight / 2);

		if (primary == null) {
			primary = new SimpleFramebuffer("System esp blur", width, height, false);
			secondary = new SimpleFramebuffer("System esp blur swap", width, height, false);
		} else if (primary.textureWidth != width || primary.textureHeight != height) {
			primary.resize(width, height);
			secondary.resize(width, height);
		}

		pass(EspPipelines.BLUR_DOWNSAMPLE, main, primary);
		for (int i = 0; i < PASSES; i++) {
			pass(EspPipelines.BLUR_HORIZONTAL, primary, secondary);
			pass(EspPipelines.BLUR_VERTICAL, secondary, primary);
		}
		return primary.getColorAttachmentView();
	}

	private static void pass(RenderPipeline pipeline, Framebuffer input, Framebuffer output) {
		try (RenderPass pass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(() -> "System esp blur pass", output.getColorAttachmentView(), OptionalInt.empty())) {
			pass.setPipeline(pipeline);
			pass.bindTexture("InSampler", input.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
			pass.draw(0, 3);
		}
	}

	private FrameBlur() {
	}
}

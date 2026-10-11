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

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class EspPipelines {

	private static final int BLUR_RADIUS = 20;

	public static final int[] JUMP_STEPS = {32, 16, 8, 4, 2, 1, 1};
	private static final Map<Integer, RenderPipeline> JUMP = new HashMap<>();

	public static RenderPipeline SEED;
	public static RenderPipeline COMPOSITE;
	public static RenderPipeline TRACER;
	public static RenderPipeline HAND_MASK;
	public static RenderPipeline BLUR_DOWNSAMPLE;
	public static RenderPipeline BLUR_HORIZONTAL;
	public static RenderPipeline BLUR_VERTICAL;

	private static boolean initialized;

	public static void init() {
		if (initialized) return;
		initialized = true;

		SEED = RenderPipelines.register(screenPass("esp_seed", Identifier.of("system", "post/esp_seed"))
				.withSampler("SilhouetteSampler")
				.build()
		);

		for (int step : JUMP_STEPS) {
			JUMP.computeIfAbsent(step, value -> RenderPipelines.register(screenPass("esp_jump_" + value, Identifier.of("system", "post/esp_jump"))
					.withShaderDefine("STEP", value)
					.withSampler("SeedSampler")
					.build()
			));
		}

		COMPOSITE = RenderPipelines.register(screenPass("esp_composite", Identifier.of("system", "post/esp_composite"))
				.withSampler("SilhouetteSampler")
				.withSampler("SeedSampler")
				.withSampler("BlurSampler")
				.withUniform("EspStyle", UniformType.UNIFORM_BUFFER)
				.withBlend(BlendFunction.TRANSLUCENT)
				.build()
		);

		HAND_MASK = RenderPipelines.register(screenPass("hand_mask", Identifier.of("system", "post/hand_mask"))
				.withSampler("DepthSampler")
				.withUniform("MaskColor", UniformType.UNIFORM_BUFFER)
				.build()
		);

		TRACER = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
				.withLocation(Identifier.of("system", "pipeline/esp_tracer"))
				.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
				.withDepthWrite(false)
				.build()
		);

		BLUR_DOWNSAMPLE = RenderPipelines.register(screenPass("esp_blur_downsample", Identifier.ofVanilla("core/blit_screen"))
				.withSampler("InSampler")
				.build()
		);

		BLUR_HORIZONTAL = RenderPipelines.register(screenPass("esp_blur_horizontal", Identifier.of("system", "post/esp_gaussian"))
				.withShaderDefine("HORIZONTAL")
				.withShaderDefine("RADIUS", BLUR_RADIUS)
				.withSampler("InSampler")
				.build()
		);

		BLUR_VERTICAL = RenderPipelines.register(screenPass("esp_blur_vertical", Identifier.of("system", "post/esp_gaussian"))
				.withShaderDefine("RADIUS", BLUR_RADIUS)
				.withSampler("InSampler")
				.build()
		);
	}

	public static RenderPipeline jump(int step) {
		return JUMP.get(step);
	}

	private static RenderPipeline.Builder screenPass(String name, Identifier fragmentShader) {
		return RenderPipeline.builder(RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET)
				.withLocation(Identifier.of("system", "pipeline/" + name))
				.withVertexShader(Identifier.ofVanilla("core/screenquad"))
				.withFragmentShader(fragmentShader);
	}

	private EspPipelines() {
	}
}

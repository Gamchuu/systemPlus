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

package dlindustries.uk.daduncs.mixin;

import dlindustries.uk.daduncs.render.skia.SkiaRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Composites the Skija UI surface over the default framebuffer right before
 * the GLFW buffer swap, so the client's Skia-drawn UI layers above the whole
 * vanilla frame without disturbing Blaze3D mid-frame GL state.
 */
@Mixin(Window.class)
public class WindowMixin {

	@Inject(method = "swapBuffers", at = @At("HEAD"))
	private void daduncs_skiaComposite(TracyFrameCapturer capturer, CallbackInfo ci) {
		// Compositing is handled by MinecraftClientMixin.daduncs_skiaComposite
		// (flushToFramebuffer) which runs before blitToScreen. Calling flushToScreen
		// here as well would draw the Skia UI surface a second time, causing
		// duplicated/ghosted text on watermark, arraylist, and other HUD features.
	}
}
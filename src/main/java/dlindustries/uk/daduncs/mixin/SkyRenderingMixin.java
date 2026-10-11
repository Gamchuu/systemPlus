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

package dlindustries.uk.daduncs.mixin;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.module.modules.render.CustomSky;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {

	@Inject(method = "renderTopSky", at = @At("HEAD"), cancellable = true)
	private void onRenderTopSky(int color, CallbackInfo ci) {
		CustomSky sky = sky();

		if (sky == null) return;

		sky.paint();
		ci.cancel();
	}

	@Inject(method = "renderSkyDark", at = @At("HEAD"), cancellable = true)
	private void onRenderSkyDark(CallbackInfo ci) {
		if (sky() != null) ci.cancel();
	}

	@Inject(method = "renderGlowingSky", at = @At("HEAD"), cancellable = true)
	private void onRenderGlowingSky(MatrixStack matrices, float alpha, int color, CallbackInfo ci) {
		if (sky() != null) ci.cancel();
	}

	@Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true)
	private void onRenderCelestialBodies(MatrixStack matrices, float rain, float angle, float glow, MoonPhase phase,
										 float stars, float alpha, CallbackInfo ci) {
		if (sky() != null) ci.cancel();
	}

	@Inject(method = "renderEndSky", at = @At("HEAD"), cancellable = true)
	private void onRenderEndSky(CallbackInfo ci) {
		CustomSky sky = sky();

		if (sky == null) return;

		sky.paint();
		ci.cancel();
	}

	private static CustomSky sky() {
		CustomSky sky = System.INSTANCE.getModuleManager().getModule(CustomSky.class);

		return sky != null && sky.isEnabled() ? sky : null;
	}
}

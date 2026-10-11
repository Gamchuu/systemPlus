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

import com.llamalad7.mixinextras.sugar.Local;
import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.event.EventManager;
import dlindustries.uk.daduncs.event.events.GameRenderListener;
import dlindustries.uk.daduncs.gui.ConfigDrop;
import dlindustries.uk.daduncs.gui.Notice;
import dlindustries.uk.daduncs.module.modules.render.Freecam;
import dlindustries.uk.daduncs.module.modules.render.HandShaders;
import dlindustries.uk.daduncs.render.Draw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand"))
	private void onWorldRender(RenderTickCounter tickCounter, CallbackInfo ci) {
		MatrixStack matrixStack = new MatrixStack();
		EventManager.fire(new GameRenderListener.GameRenderEvent(matrixStack, tickCounter.getTickProgress(true)));
	}

	@Inject(method = "renderWorld", at = @At("TAIL"))
	private void onWorldRenderEnd(RenderTickCounter tickCounter, CallbackInfo ci) {
		if (System.INSTANCE == null || System.INSTANCE.getModuleManager() == null) return;

		HandShaders hands = System.INSTANCE.getModuleManager().getModule(HandShaders.class);
		if (hands != null && hands.isEnabled())
			hands.composite();
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
	private void onGuiRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci, @Local DrawContext context) {
		Notice.render(context);
		ConfigDrop.render(context);
	}

	@Inject(method = "renderBlur", at = @At("HEAD"), cancellable = true)
	private void onRenderBlur(CallbackInfo ci) {
		if (Draw.takeBehind())
			ci.cancel();
	}

	@Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
	private void onShouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
		if (System.INSTANCE.getModuleManager().getModule(Freecam.class).isEnabled())
			cir.setReturnValue(false);
	}
}

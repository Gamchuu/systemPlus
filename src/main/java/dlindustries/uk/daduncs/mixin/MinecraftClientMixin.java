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

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.event.EventManager;
import dlindustries.uk.daduncs.event.events.*;
import dlindustries.uk.daduncs.module.modules.misc.FastPlace;
import dlindustries.uk.daduncs.utils.InputUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Shadow
	@Nullable
	public ClientWorld world;

	@Shadow
	@Final
	private Window window;

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		InputUtils.tick();
		dlindustries.uk.daduncs.module.modules.misc.AutoReconnect.onClientTick();

		if (world != null) {
			dlindustries.uk.daduncs.managers.Recorder tm = dlindustries.uk.daduncs.managers.Recorder.getInstance();
			if (tm != null) {
				tm.recordTickStart();
			}

			dlindustries.uk.daduncs.managers.AntibanbyDaduncs.onTickStart();

			TickListener.TickEvent event = new TickListener.TickEvent();

			EventManager.fire(event);
		}
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void onTickReturn(CallbackInfo ci) {
		if (world == null)
			return;

		dlindustries.uk.daduncs.managers.AntibanbyDaduncs.onTickEnd();

		dlindustries.uk.daduncs.managers.Recorder tm = dlindustries.uk.daduncs.managers.Recorder.getInstance();
		if (tm != null) {
			tm.recordTickEnd();
		}

		FastPlace fastPlace = System.INSTANCE.getModuleManager().getModule(FastPlace.class);
		if (fastPlace != null)
			fastPlace.apply();
	}

	@Inject(method = "onResolutionChanged", at = @At("HEAD"))
	private void onResolutionChanged(CallbackInfo ci) {
		EventManager.fire(new ResolutionListener.ResolutionEvent(this.window));
	}

	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gl/Framebuffer;blitToScreen()V"
			)
	)
	private void daduncs_skiaComposite(boolean tick, CallbackInfo ci) {
		dlindustries.uk.daduncs.render.skia.SkiaRenderer.flushToFramebuffer();
	}

	@Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
	private void onItemUse(CallbackInfo ci) {
		if (dlindustries.uk.daduncs.managers.AntibanbyDaduncs.isAttackSent()) {
			ci.cancel();
			return;
		}

		ItemUseListener.ItemUseEvent event = new ItemUseListener.ItemUseEvent();

		EventManager.fire(event);
		if (event.isCancelled()) ci.cancel();

		if (InputUtils.isSimulatedPressed(InputUtils.Button.USE)) {
			InputUtils.consume(InputUtils.Button.USE);
			ci.cancel();
		}
	}

	@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
	private void onAttack(CallbackInfoReturnable<Boolean> cir) {
		if (dlindustries.uk.daduncs.managers.AntibanbyDaduncs.isAttackSent()) {
			cir.setReturnValue(false);
			return;
		}

		AttackListener.AttackEvent event = new AttackListener.AttackEvent();

		EventManager.fire(event);
		if (event.isCancelled()) {
			cir.setReturnValue(false);
			return;
		}

		if (InputUtils.isSimulatedPressed(InputUtils.Button.ATTACK)) {
			InputUtils.consume(InputUtils.Button.ATTACK);
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
	private void onBlockBreaking(boolean breaking, CallbackInfo ci) {
		BlockBreakingListener.BlockBreakingEvent event = new BlockBreakingListener.BlockBreakingEvent();

		EventManager.fire(event);
		if (event.isCancelled()) ci.cancel();

		if (InputUtils.isSimulatedPressed(InputUtils.Button.ATTACK)) {
			InputUtils.consume(InputUtils.Button.ATTACK);
			ci.cancel();
		}
	}

	@Inject(method = "stop", at = @At("HEAD"))
	private void onClose(CallbackInfo ci) {
		System.INSTANCE.getProfileManager().saveProfile();
		dlindustries.uk.daduncs.render.skia.SkiaRenderer.close();
	}
}

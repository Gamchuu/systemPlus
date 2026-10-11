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

import dlindustries.uk.daduncs.event.EventManager;
import dlindustries.uk.daduncs.event.events.HudListener;
import dlindustries.uk.daduncs.render.Draw;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void onRenderHudEnd(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		Draw.captureScene();
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void onRenderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		dlindustries.uk.daduncs.gui.Frame.tick();
		dlindustries.uk.daduncs.utils.RenderUtils.resetProjection();
		Draw.warmup(context);

		HudListener.HudEvent event = new HudListener.HudEvent(context, tickCounter.getTickProgress(true));

		EventManager.fire(event);
	}
}

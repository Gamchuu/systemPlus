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
import dlindustries.uk.daduncs.gui.HudEditor;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dlindustries.uk.daduncs.System.mc;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

	@Inject(method = "render", at = @At("TAIL"))
	private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		HudEditor.render(context, mouseX, mouseY, delta);
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void onKey(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
		if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && HudEditor.escape())
			cir.setReturnValue(true);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void onClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
		if (HudEditor.click((int) click.x(), (int) click.y(), click.button()))
			cir.setReturnValue(true);
	}

	@Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
	private void onSend(String chatText, boolean addToHistory, CallbackInfo ci) {
		if (!chatText.trim().equalsIgnoreCase(".reset"))
			return;

		ci.cancel();
		mc.inGameHud.getChatHud().addToMessageHistory(chatText);
		System.INSTANCE.getConfigManager().reset();
		mc.inGameHud.getChatHud().addMessage(Text.literal("config was reset"));
	}
}

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

import dlindustries.uk.daduncs.utils.rotation.SilentRot;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
	@Inject(method = "tick", at = @At("TAIL"))
	private void onTickTail(CallbackInfo ci) {
		if (!SilentRot.moving()) {
			return;
		}

		Vec2f in = this.movementVector;
		float fwd = in.y;
		float side = in.x;
		if (fwd == 0.0f && side == 0.0f) {
			return;
		}

		float pow = Math.max(Math.abs(fwd), Math.abs(side));
		if (pow <= 0.0f) {
			return;
		}

		float sx = side / pow;
		float sz = fwd / pow;
		float d = (SilentRot.real() - SilentRot.yaw()) * 0.017453292F;
		float cos = MathHelper.cos(d);
		float sin = MathHelper.sin(d);
		float nside = Math.round(sx * cos - sz * sin) * pow;
		float nfwd = Math.round(sz * cos + sx * sin) * pow;

		Vec2f mv = new Vec2f(nside, nfwd);
		if (mv.lengthSquared() <= 1.0E-8f) {
			return;
		}

		mv = mv.normalize();
		((InputAccessor) this).setMovementVector(mv);

		float eps = 0.001f;
		PlayerInput pi = this.playerInput != null ? this.playerInput : PlayerInput.DEFAULT;
		this.playerInput = new PlayerInput(mv.y > eps, mv.y < -eps, mv.x > eps, mv.x < -eps, pi.jump(), pi.sneak(), pi.sprint());
	}
}

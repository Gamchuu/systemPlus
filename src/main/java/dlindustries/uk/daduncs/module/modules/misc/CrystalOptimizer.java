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

package dlindustries.uk.daduncs.module.modules.misc;

import dlindustries.uk.daduncs.event.events.PacketSendListener;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.utils.EncryptedString;
import dlindustries.uk.daduncs.utils.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public final class CrystalOptimizer extends Module implements PacketSendListener {
	public CrystalOptimizer() {
		super(EncryptedString.of("Crystal Optimizer"),
				EncryptedString.of("Makes your crystals disappear faster client-side so you can place crystals faster"),
				-1,
				Category.MISC);
		setTag(EncryptedString.of("BANNABLE"));
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketSendListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketSendListener.class, this);
		super.onDisable();
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (event.packet instanceof PlayerInteractEntityC2SPacket interactPacket) {
			interactPacket.handle(new PlayerInteractEntityC2SPacket.Handler() {
				@Override
				public void interact(Hand hand) {

				}

				@Override
				public void interactAt(Hand hand, Vec3d pos) {

				}

				@Override
				public void attack() {

					if (mc.crosshairTarget == null)
						return;

					if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY && mc.crosshairTarget instanceof EntityHitResult hit) {
						if (hit.getEntity() instanceof EndCrystalEntity) {
							StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
							StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
							if (!(weakness == null || strength != null && strength.getAmplifier() > weakness.getAmplifier() || WorldUtils.isTool(mc.player.getMainHandStack())))
								return;

							hit.getEntity().discard();
							hit.getEntity().setRemoved(Entity.RemovalReason.KILLED);
							hit.getEntity().onRemoved();
						}
					}
				}
			});
		}
	}
}

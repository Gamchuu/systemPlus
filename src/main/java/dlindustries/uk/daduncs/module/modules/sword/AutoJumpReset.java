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

package dlindustries.uk.daduncs.module.modules.sword;

import dlindustries.uk.daduncs.event.events.TickListener;
import dlindustries.uk.daduncs.imixin.IKeyBinding;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.utils.EncryptedString;
import dlindustries.uk.daduncs.utils.MathUtils;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;

public final class AutoJumpReset extends Module implements TickListener {
	private final number chance = new number(EncryptedString.of("Chance"), 0, 100, 100, 1);
	private final bool combatOnly = new bool(EncryptedString.of("Combat Only"), true)
			.setDescription(EncryptedString.of("Only jump resets on combat/projectile hits (ignores fall, fire, drowning)"));
	private final bool humanize = new bool(EncryptedString.of("Humanize"), true)
			.setDescription(EncryptedString.of("randomizes timings not that its gonna bipass shi"));
	private final bool whileUse = new bool(EncryptedString.of("While Use"), true)
			.setDescription(EncryptedString.of("Allow jump reset while using items (shield, food)"));

	private boolean jumpQueued = false;
	private boolean pendingJump = false;
	private int targetHurtTime = 9;
	private int prevHurtTime = 0;

	public AutoJumpReset() {
		super(EncryptedString.of("Auto Jump Reset"),
				EncryptedString.of("Automatically jumps for you when you get hit so you take less knockback (not good for crystal pvp)"),
				-1,
				Category.SWORD);
		addSettings(chance, combatOnly, humanize, whileUse);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		resetState();
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		resetState();
		super.onDisable();
	}

	private void resetState() {
		if (jumpQueued && mc.options != null) {
			((IKeyBinding) mc.options.jumpKey).resetPressed();
		}
		jumpQueued = false;
		pendingJump = false;
		targetHurtTime = 9;
		prevHurtTime = 0;
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null) {
			resetState();
			return;
		}

		if (jumpQueued) {
			if (mc.options != null) {
				((IKeyBinding) mc.options.jumpKey).resetPressed();
			}
			jumpQueued = false;
		}

		if (mc.currentScreen != null) {
			pendingJump = false;
			prevHurtTime = mc.player.hurtTime;
			return;
		}

		if (!whileUse.getValue() && mc.player.isUsingItem()) {
			pendingJump = false;
			prevHurtTime = mc.player.hurtTime;
			return;
		}

		if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing() ||
				mc.player.isGliding() || mc.player.hasVehicle()) {
			pendingJump = false;
			prevHurtTime = mc.player.hurtTime;
			return;
		}

		int currentHurtTime = mc.player.hurtTime;

		if (currentHurtTime > prevHurtTime && currentHurtTime >= 8) {
			boolean shouldQueue = true;

			if (combatOnly.getValue()) {
				DamageSource source = mc.player.getRecentDamageSource();
				if (source != null) {
					if (source.isIn(DamageTypeTags.IS_FIRE) ||
							source.isIn(DamageTypeTags.IS_FALL) ||
							source.isIn(DamageTypeTags.IS_DROWNING) ||
							source.isIn(DamageTypeTags.IS_FREEZING)) {
						shouldQueue = false;
					}
				}
			}

			if (shouldQueue) {
				pendingJump = true;
				targetHurtTime = humanize.getValue() ? (MathUtils.randomInt(1, 10) <= 7 ? 9 : 8) : 9;
			}
		}

		if (pendingJump && currentHurtTime <= targetHurtTime && currentHurtTime >= 6) {
			if (mc.player.isOnGround()) {
				if (MathUtils.randomInt(1, 100) <= chance.getValueInt()) {
					mc.options.jumpKey.setPressed(true);
					jumpQueued = true;
				}
				pendingJump = false;
			}
		} else if (currentHurtTime < 6) {
			pendingJump = false;
		}

		prevHurtTime = currentHurtTime;
	}
}

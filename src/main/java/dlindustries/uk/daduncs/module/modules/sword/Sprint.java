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
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.utils.EncryptedString;
import net.minecraft.entity.effect.StatusEffects;

public final class Sprint extends Module implements TickListener {
    public Sprint() {
        super(EncryptedString.of("Sprint"), EncryptedString.of("Keeps you sprinting at all times"), -1, Category.SWORD);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        if (mc.player != null && mc.options != null) {
            mc.options.sprintKey.setPressed(false);
        }
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) return;
        if (mc.currentScreen != null) return;

        if (canSprintSafely()) {
            mc.options.sprintKey.setPressed(true);
        } else {
            mc.options.sprintKey.setPressed(false);
        }
    }

    private boolean canSprintSafely() {
        if (mc.player == null) return false;
        if (!mc.player.input.hasForwardMovement()) return false;
        if (mc.player.isSneaking()) return false;
        if (mc.player.horizontalCollision) return false;

        if (mc.player.getHungerManager() != null && !mc.player.getHungerManager().canSprint()) {
            if (!mc.player.getAbilities().allowFlying) {
                return false;
            }
        }

        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
            return false;
        }

        if (mc.player.hasVehicle()) {
            return false;
        }

        return true;
    }
}

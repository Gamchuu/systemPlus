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

package dlindustries.uk.daduncs.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public final class AirplaceUtil {
    private static volatile UUID lastAttackedUuid = null;
    private static volatile long lastAttackTimeMs = 0L;

    private static volatile long lastServerHurtTimeMs = 0L;

    private static final long ATTACK_TRACK_TIMEOUT_MS = 4000L;
    private static final long DAMAGE_TICK_MS = 500L;
    private static final long DAMAGE_TICK_BUFFER_MS = 120L;

    private AirplaceUtil() {}

    public static void setLastAttacked(UUID uuid) {
        lastAttackedUuid = uuid;
        lastAttackTimeMs = System.currentTimeMillis();
        lastServerHurtTimeMs = 0L;
    }

    public static void onEntityHurt(UUID uuid) {
        if (uuid == null) return;

        if (lastAttackedUuid != null && lastAttackedUuid.equals(uuid)) {
            lastServerHurtTimeMs = System.currentTimeMillis();
        }
    }

    public static PlayerEntity getTrackedPlayer() {
        if (lastAttackedUuid == null) return null;
        if (System.currentTimeMillis() - lastAttackTimeMs > ATTACK_TRACK_TIMEOUT_MS) return null;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return null;
        PlayerEntity p = mc.world.getPlayerByUuid(lastAttackedUuid);
        if (p == null || !p.isAlive()) return null;
        if (mc.player == null) return null;
        double sq = p.squaredDistanceTo(mc.player);
        if (sq > 36) return null;
        return p;
    }

    public static void clear() {
        lastAttackedUuid = null;
        lastAttackTimeMs = 0L;
        lastServerHurtTimeMs = 0L;
    }

    public static boolean inDamageTick() {
        if (lastAttackedUuid == null) return false;
        long elapsed = System.currentTimeMillis() - lastAttackTimeMs;
        return elapsed >= 0 && elapsed <= (DAMAGE_TICK_MS + DAMAGE_TICK_BUFFER_MS);
    }

    public static boolean inHurtTick() {
        if (lastServerHurtTimeMs == 0L) return false;
        long elapsed = System.currentTimeMillis() - lastServerHurtTimeMs;
        return elapsed >= 0 && elapsed <= (DAMAGE_TICK_MS + DAMAGE_TICK_BUFFER_MS);
    }

    public static long damageTicksLeft() {
        if (lastAttackedUuid == null) return 0L;
        long remaining = (DAMAGE_TICK_MS + DAMAGE_TICK_BUFFER_MS) - (System.currentTimeMillis() - lastAttackTimeMs);
        return Math.max(0L, remaining);
    }

    public static long hurtTicksLeft() {
        if (lastServerHurtTimeMs == 0L) return 0L;
        long remaining = (DAMAGE_TICK_MS + DAMAGE_TICK_BUFFER_MS) - (System.currentTimeMillis() - lastServerHurtTimeMs);
        return Math.max(0L, remaining);
    }
}
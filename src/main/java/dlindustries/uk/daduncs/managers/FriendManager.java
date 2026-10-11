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

package dlindustries.uk.daduncs.managers;


import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.util.HashSet;
import java.util.Set;

import static dlindustries.uk.daduncs.System.mc;

public final class FriendManager {
    private final Set<String> friends;

    public FriendManager() {
        this.friends = new HashSet<>();
    }

    public Set<String> getFriends() {
        return friends;
    }

    public Set<String> getShownFriends() {
        return new HashSet<>(friends);
    }

    public void removeFriend(String name) {
        friends.remove(name);
    }

    public void addFriend(PlayerEntity player) {
        friends.add(player.getName().getString());
    }

    public void removeFriend(PlayerEntity player) {
        friends.remove(player.getName().getString());
    }

    public boolean isFriend(PlayerEntity player) {
        return friends.contains(player.getName().getString());
    }

    public boolean isAimingOverFriend() {
        if(mc.crosshairTarget instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();

            if(entity instanceof PlayerEntity player) {
                return isFriend(player);
            }
        }

        return false;
    }
}

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

import dlindustries.uk.daduncs.event.events.TickListener;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.utils.EncryptedString;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NoDamage extends Module implements TickListener {

	private final bool fallOnly = new bool(EncryptedString.of("Fall Only"), false);

	public NoDamage() {
		super(EncryptedString.of("NoDamage") ,("0 damage from falling"), -1, Category.MISC);
		setTag(EncryptedString.of("BANNABLE"));

		addSettings(fallOnly);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);

		if (mc.player != null) {
			mc.player.setInvulnerable(mc.player.isCreative());
			server(false);
		}

		super.onDisable();
	}

	@Override
	public void onTick() {
		if (mc.player == null || !mc.isInSingleplayer())
			return;

		mc.player.fallDistance = 0;
		server(true);

		if (!fallOnly.getValue())
			mc.player.setInvulnerable(true);
	}

	private void server(boolean state) {
		if (mc.getServer() == null) return;

		ServerPlayerEntity player = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
		if (player == null)
			return;

		player.fallDistance = 0;

		if (!fallOnly.getValue())
			player.setInvulnerable(state || player.isCreative());
	}
}

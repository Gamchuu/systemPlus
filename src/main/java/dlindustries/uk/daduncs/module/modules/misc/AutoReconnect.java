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

import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.utils.EncryptedString;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public final class AutoReconnect extends Module {

	private static AutoReconnect instance;

	private final number delay = new number(EncryptedString.of("Delay"), 1, 30, 5, 1);

	private ServerInfo lastServer;
	private long since;

	public AutoReconnect() {
		super(EncryptedString.of("AutoReconnect"), EncryptedString.of("Joins back when you get kicked"), -1, Category.MISC);

		addSettings(delay);
		instance = this;
	}

	public static void onClientTick() {
		if (instance != null && instance.isEnabled())
			instance.check();
	}

	@Override
	public void onDisable() {
		since = 0;
		super.onDisable();
	}

	private void check() {
		if (mc.getCurrentServerEntry() != null)
			lastServer = mc.getCurrentServerEntry();

		if (lastServer == null || !(mc.currentScreen instanceof DisconnectedScreen)) {
			since = 0;
			return;
		}

		if (since == 0) {
			since = System.currentTimeMillis();
			return;
		}

		if (System.currentTimeMillis() - since < delay.getValue() * 1000)
			return;

		since = 0;
		ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(lastServer.address), lastServer, false, null);
	}
}

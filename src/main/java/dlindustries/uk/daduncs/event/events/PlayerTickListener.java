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

package dlindustries.uk.daduncs.event.events;

import dlindustries.uk.daduncs.event.Event;
import dlindustries.uk.daduncs.event.Listener;

import java.util.ArrayList;

public interface PlayerTickListener extends Listener {
	void onPlayerTick();

	class PlayerTickEvent extends Event<PlayerTickListener> {
		@Override
		public void fire(ArrayList<PlayerTickListener> listeners) {
			listeners.forEach(PlayerTickListener::onPlayerTick);
		}

		@Override
		public Class<PlayerTickListener> getListenerType() {
			return PlayerTickListener.class;
		}
	}
}

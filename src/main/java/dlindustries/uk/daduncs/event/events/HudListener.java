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

package dlindustries.uk.daduncs.event.events;

import dlindustries.uk.daduncs.event.Event;
import dlindustries.uk.daduncs.event.Listener;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;

public interface HudListener extends Listener {
	void onRenderHud(HudEvent event);

	class HudEvent extends Event<HudListener> {
		public DrawContext context;
		public float delta;

		public HudEvent(DrawContext context, float delta) {
			this.context = context;
			this.delta = delta;
		}

		@Override
		public void fire(ArrayList<HudListener> listeners) {
			listeners.forEach(e -> e.onRenderHud(this));
		}

		@Override
		public Class<HudListener> getListenerType() {
			return HudListener.class;
		}
	}
}
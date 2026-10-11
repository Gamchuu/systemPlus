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

import dlindustries.uk.daduncs.event.CancellableEvent;
import dlindustries.uk.daduncs.event.Listener;

import java.util.ArrayList;

public interface CameraUpdateListener extends Listener {
	void onCameraUpdate(CameraUpdateEvent event);

	class CameraUpdateEvent extends CancellableEvent<CameraUpdateListener> {
		public double x, y, z;

		public CameraUpdateEvent(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public double getZ() {
			return z;
		}

		public void setX(double x) {
			this.x = x;
		}

		public void setY(double y) {
			this.y = y;
		}

		public void setZ(double z) {
			this.z = z;
		}

		@Override
		public void fire(ArrayList<CameraUpdateListener> listeners) {
			listeners.forEach(l -> l.onCameraUpdate(this));
		}

		@Override
		public Class<CameraUpdateListener> getListenerType() {
			return CameraUpdateListener.class;
		}
	}
}

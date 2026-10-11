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

package dlindustries.uk.daduncs.utils;

import dlindustries.uk.daduncs.event.EventManager;
import dlindustries.uk.daduncs.event.events.ButtonListener;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static dlindustries.uk.daduncs.System.mc;


public final class MouseSimulation {
	public static HashMap<Integer, Boolean> mouseButtons = new HashMap<>();
	public static ExecutorService clickExecutor = Executors.newFixedThreadPool(100);

	public static boolean isMouseButtonPressed(int keyCode) {
		Boolean key = mouseButtons.get(keyCode);
		return key != null ? key : false;
	}

	public static void mousePress(int keyCode) {
		mouseButtons.put(keyCode, true);
		dlindustries.uk.daduncs.managers.Recorder tm = dlindustries.uk.daduncs.managers.Recorder.getInstance();
		if (tm != null && mc.getWindow() != null) {
			double x = mc.mouse != null ? mc.mouse.getX() : 0;
			double y = mc.mouse != null ? mc.mouse.getY() : 0;
			tm.recordClick(keyCode, GLFW.GLFW_PRESS, 0, x, y, true);
		}
		EventManager.fire(new ButtonListener.ButtonEvent(keyCode, mc.getWindow().getHandle(), GLFW.GLFW_PRESS));
	}

	public static void mouseRelease(int keyCode) {
		mouseButtons.put(keyCode, false);
		dlindustries.uk.daduncs.managers.Recorder tm = dlindustries.uk.daduncs.managers.Recorder.getInstance();
		if (tm != null && mc.getWindow() != null) {
			double x = mc.mouse != null ? mc.mouse.getX() : 0;
			double y = mc.mouse != null ? mc.mouse.getY() : 0;
			tm.recordClick(keyCode, GLFW.GLFW_RELEASE, 0, x, y, true);
		}
		EventManager.fire(new ButtonListener.ButtonEvent(keyCode, mc.getWindow().getHandle(), GLFW.GLFW_RELEASE));
	}

	public static void mouseClick(int keyCode, int millis) {
		clickExecutor.submit(() -> {
			try {
				MouseSimulation.mousePress(keyCode);
				Thread.sleep(millis);
				MouseSimulation.mouseRelease(keyCode);
			} catch (InterruptedException ignored) {

			}
		});
	}

	public static void mouseClick(int keyCode) {
		mouseClick(keyCode, 35);
	}
}

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

import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class Sounds {

	public static final SoundEvent CLICK = of("click");
	public static final SoundEvent HOVER = of("hover");
	public static final SoundEvent TOGGLE = of("toggle");
	public static final SoundEvent COLLAPSE = of("collapse");
	public static final SoundEvent SCROLL = of("scroll");
	public static final SoundEvent KEYPRESS = of("keypress");
	public static final SoundEvent TYPE = of("type");
	public static final SoundEvent CLOSE = of("close");
	public static final SoundEvent OPEN = of("open");
	public static final SoundEvent CLOSE_QUICK = of("close_quick");

	private static long lastHover;

	private static SoundEvent of(String name) {
		Identifier id = Identifier.of("system", name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void play(SoundEvent sound) {
		play(sound, 1f);
	}

	public static void play(SoundEvent sound, float volume) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.getSoundManager() == null || muted(sound))
			return;

		mc.getSoundManager().play(PositionedSoundInstance.ui(sound, 1f, volume));
	}

	public static void play(SoundEvent sound, float volume, float pitch) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.getSoundManager() == null || muted(sound))
			return;

		mc.getSoundManager().play(PositionedSoundInstance.ui(sound, pitch, volume));
	}

	private static boolean muted(SoundEvent sound) {
		if (sound == OPEN || sound == CLOSE || sound == CLOSE_QUICK)
			return false;

		return !ClickGUI.uiSounds.getValue();
	}

	public static void hover() {
		long now = java.lang.System.currentTimeMillis();
		if (now - lastHover < 60)
			return;

		lastHover = now;
		play(HOVER, 0.5f);
	}

	public static void init() {
	}

	private Sounds() {
	}
}

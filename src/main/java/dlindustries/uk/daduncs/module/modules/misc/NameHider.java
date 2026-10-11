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

package dlindustries.uk.daduncs.module.modules.misc;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.event.events.TickListener;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.setting.string;
import dlindustries.uk.daduncs.utils.EncryptedString;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

public final class NameHider extends Module implements TickListener {

	public NameHider() {
		super(EncryptedString.of("Name Hider"), EncryptedString.of("Shows your own name as something else"), -1, Category.MISC);
		addSettings(name);
	}

	private final string name = new string(EncryptedString.of("Name"), "system");

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		show(null);
		super.onDisable();
	}

	@Override
	public void onTick() {
		show(name.getValue());
	}

	private void show(String shown) {
		if (mc.player == null) return;
		if (mc.getNetworkHandler() == null) return;

		PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());

		if (entry != null) {
			entry.setDisplayName(shown == null || shown.isEmpty() ? null : Text.literal(shown));
		}
	}

	public static Text hide(Text message) {
		NameHider module = System.INSTANCE.getModuleManager().getModule(NameHider.class);

		if (message == null || module == null) return message;
		if (!module.isEnabled()) return message;
		if (System.mc.player == null) return message;

		String real = System.mc.player.getGameProfile().name();
		String shown = module.name.getValue();

		if (real == null || shown == null || shown.isEmpty()) return message;
		if (!message.getString().contains(real)) return message;

		return swap(message, real, shown);
	}

	private static Text swap(Text text, String real, String shown) {
		MutableText copy;

		if (text.getContent() instanceof PlainTextContent plain) {
			copy = Text.literal(plain.string().replace(real, shown)).setStyle(text.getStyle());
		} else if (text.getContent() instanceof TranslatableTextContent translatable) {
			Object[] args = translatable.getArgs().clone();

			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Text arg) {
					args[i] = swap(arg, real, shown);
				} else if (args[i] instanceof String word) {
					args[i] = word.replace(real, shown);
				}
			}

			copy = Text.translatable(translatable.getKey(), args).setStyle(text.getStyle());
		} else {
			copy = text.copyContentOnly().setStyle(text.getStyle());
		}

		for (Text sibling : text.getSiblings()) {
			copy.append(swap(sibling, real, shown));
		}

		return copy;
	}
}

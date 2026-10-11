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

import dlindustries.uk.daduncs.event.events.PacketSendListener;
import dlindustries.uk.daduncs.event.events.PacketSendListener.PacketSendEvent;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.utils.EncryptedString;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;

public final class SelfFlag extends Module implements PacketSendListener {
	private boolean didAction = false;
	private Packet<?> lastActionPacket = null;
	private int lastSlot = -1;

	public SelfFlag() {
		super(EncryptedString.of("SelfFlag"),
				EncryptedString.of("Catches double clicks, desyncs, and bad tick flags client-side"),
				-1,
				Category.MISC);
	}

	@Override
	public void onEnable() {
		eventManager.add(PacketSendListener.class, this);
		didAction = false;
		lastActionPacket = null;
		lastSlot = -1;
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(PacketSendListener.class, this);
		lastActionPacket = null;
		lastSlot = -1;
		super.onDisable();
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (mc.player == null || mc.getNetworkHandler() == null) {
			return;
		}

		Packet<?> packet = event.packet;

		if (packet instanceof ClientTickEndC2SPacket) {
			didAction = false;
			lastActionPacket = null;
			return;
		}

		if (isActionPacket(packet)) {
			didAction = true;
			lastActionPacket = packet;
			return;
		}

		if (packet instanceof UpdateSelectedSlotC2SPacket slotPacket) {
			int slot = slotPacket.getSelectedSlot();
			if (didAction) {
				sendFlagMessage("Changed slot after action in same tick");
				dlindustries.uk.daduncs.managers.Recorder tm = dlindustries.uk.daduncs.managers.Recorder.getInstance();
				if (tm != null) {
					tm.onFlag("Changed slot after action in same tick", packet, lastActionPacket);
				}
			}

			if (lastSlot == slot && slot > 0) {
				sendFlagMessage("Sent slot twice: slot " + slot);
				dlindustries.uk.daduncs.managers.Recorder tm = dlindustries.uk.daduncs.managers.Recorder.getInstance();
				if (tm != null) {
					tm.onFlag("Sent slot twice (slot " + slot + ")", packet, null);
				}
			}
			lastSlot = slot;
		}
	}

	private boolean isActionPacket(Packet<?> packet) {

		if (packet instanceof PlayerInteractEntityC2SPacket) {
			return true;
		}

		if (packet instanceof PlayerInteractBlockC2SPacket) {
			return true;
		}

		if (packet instanceof PlayerInteractItemC2SPacket) {
			return true;
		}

		if (packet instanceof PlayerActionC2SPacket) {
			return true;
		}

		return false;
	}

	private void sendFlagMessage(String message) {
		if (mc.inGameHud == null || mc.inGameHud.getChatHud() == null) {
			return;
		}

		Text prefix = Text.literal("SelfFlag: ").styled(style -> style.withColor(0xFF0000).withBold(true));
		Text fullMessage = prefix.copy().append(Text.literal(message));

		mc.inGameHud.getChatHud().addMessage(fullMessage);
	}
}

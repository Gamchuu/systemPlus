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

package dlindustries.uk.daduncs.utils.rotation;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.event.EventManager;
import dlindustries.uk.daduncs.event.events.*;
import dlindustries.uk.daduncs.event.events.*;
import dlindustries.uk.daduncs.utils.RotationUtils;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import static dlindustries.uk.daduncs.System.mc;


public final class RotatorManager implements PacketSendListener, BlockBreakingListener, ItemUseListener, AttackListener, MovementPacketListener, PacketReceiveListener {
	private boolean enabled;
	private boolean rotateBack;
	private boolean resetRotation;
	private final EventManager eventManager = System.INSTANCE.eventManager;
	private Rotation currentRotation;
	private float clientYaw, clientPitch;
	private float serverYaw, serverPitch;

	public RotatorManager() {
		eventManager.remove(PacketSendListener.class, this);
		eventManager.remove(AttackListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		eventManager.remove(MovementPacketListener.class, this);
		eventManager.remove(PacketReceiveListener.class, this);
		eventManager.remove(BlockBreakingListener.class, this);


		enabled = true;
		rotateBack = false;
		resetRotation = false;

		this.serverYaw = 0;
		this.serverPitch = 0;

		this.clientYaw = 0;
		this.clientPitch = 0;
	}

	public void shutDown() {
		eventManager.remove(PacketSendListener.class, this);
		eventManager.remove(AttackListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		eventManager.remove(MovementPacketListener.class, this);
		eventManager.remove(PacketReceiveListener.class, this);
		eventManager.remove(BlockBreakingListener.class, this);
	}

	public Rotation getServerRotation() {
		return new Rotation(serverYaw, serverPitch);
	}

	public void enable() {
		enabled = true;
		rotateBack = false;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void disable() {
		if (isEnabled()) {
			enabled = false;
			if (!rotateBack) rotateBack = true;
		}
	}

	public void setRotation(Rotation rotation) {
		currentRotation = rotation;
	}

	public void setRotation(double yaw, double pitch) {
		setRotation(new Rotation(yaw, pitch));
	}

	private void resetClientRotation() {
		mc.player.setYaw(clientYaw);
		mc.player.setPitch(clientPitch);

		resetRotation = false;
	}

	public void setClientRotation(Rotation rotation) {
		this.clientYaw = mc.player.getYaw();
		this.clientPitch = mc.player.getPitch();

		mc.player.setYaw((float) rotation.yaw());
		mc.player.setPitch((float) rotation.pitch());

		resetRotation = true;
	}

	public void setServerRotation(Rotation rotation) {
		this.serverYaw = (float) rotation.yaw();
		this.serverPitch = (float) rotation.pitch();
	}

	private boolean wasDisabled;

	@Override
	public void onAttack(AttackEvent event) {
		if (!isEnabled() && wasDisabled) {
			enabled = true;
			wasDisabled = false;
		}
	}

	@Override
	public void onItemUse(ItemUseEvent event) {
		if (!event.isCancelled() && isEnabled()) {
			enabled = false;
			wasDisabled = true;
		}
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (event.packet instanceof PlayerMoveC2SPacket packet) {
			serverYaw = packet.getYaw(serverYaw);
			serverPitch = packet.getPitch(serverPitch);
		}
	}

	@Override
	public void onBlockBreaking(BlockBreakingEvent event) {
		if (!event.isCancelled() && isEnabled()) {
			enabled = false;
			wasDisabled = true;
		}
	}

	@Override
	public void onSendMovementPackets() {
		if (isEnabled() && currentRotation != null) {
			setClientRotation(currentRotation);
			setServerRotation(currentRotation);

			return;
		}

		if (rotateBack) {
			Rotation serverRot = new Rotation(serverYaw, serverPitch);
			Rotation clientRot = new Rotation(mc.player.getYaw(), mc.player.getPitch());

			if (RotationUtils.getTotalDiff(serverRot, clientRot) > 1) {
				Rotation smoothRotation = RotationUtils.getSmoothRotation(serverRot, clientRot, 0.2);
				smoothRotation = RotationUtils.applyGCD(mc.player.getYaw(), mc.player.getPitch(), (float) smoothRotation.yaw(), (float) smoothRotation.pitch(), null, true);

				setClientRotation(smoothRotation);
				setServerRotation(smoothRotation);
			} else {
				rotateBack = false;
			}
		}
	}

	@Override
	public void onPacketReceive(PacketReceiveEvent event) {
		if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
			serverYaw = packet.change().yaw();
			serverPitch = packet.change().pitch();
		}
	}
}

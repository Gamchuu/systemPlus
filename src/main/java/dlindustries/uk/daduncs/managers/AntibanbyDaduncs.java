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

package dlindustries.uk.daduncs.managers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AntibanbyDaduncs {
	private static final AtomicBoolean actionSentInTick = new AtomicBoolean(false);
	private static final AtomicBoolean attackSentInTick = new AtomicBoolean(false);
	private static final AtomicBoolean blockActionSentInTick = new AtomicBoolean(false);
	private static final AtomicBoolean entityInteractSentInTick = new AtomicBoolean(false);
	private static final AtomicInteger pendingSlot = new AtomicInteger(-1);
	private static final AtomicInteger lastSentSlot = new AtomicInteger(-1);
	private static float lastPlacedDeltaYaw = -1.0f;
	private static volatile float lastSentYaw = 0.0f;
	private static volatile float lastSentPitch = 0.0f;
	private static volatile boolean hasSentRotation = false;

	private static final AtomicBoolean movementSentInTick = new AtomicBoolean(false);
	private static final AtomicBoolean tickEnded = new AtomicBoolean(false);
	private static final AtomicBoolean inTick = new AtomicBoolean(false);

	private AntibanbyDaduncs() {}

	public static boolean isAttackSent() {
		return attackSentInTick.get();
	}

	public static boolean isMovementSent() {
		return movementSentInTick.get();
	}

	public static boolean isInTick() {
		return inTick.get();
	}

	public static boolean isTickEnded() {
		return tickEnded.get();
	}

	public static boolean isSlotPending() {
		return pendingSlot.get() != -1;
	}

	public static int getPendingSlot() {
		return pendingSlot.get();
	}

	public static boolean isBlockActionSent() {
		return blockActionSentInTick.get();
	}

	public static boolean isActionSent() {
		return actionSentInTick.get();
	}

	public static boolean isEntityInteractSent() {
		return entityInteractSentInTick.get();
	}

	public static float getLastSentYaw() {
		if (hasSentRotation) return lastSentYaw;
		MinecraftClient mc = MinecraftClient.getInstance();
		return mc.player != null ? mc.player.getYaw() : 0.0f;
	}

	public static float getLastSentPitch() {
		if (hasSentRotation) return lastSentPitch;
		MinecraftClient mc = MinecraftClient.getInstance();
		return mc.player != null ? mc.player.getPitch() : 0.0f;
	}

	public static boolean hasSentRotation() {
		return hasSentRotation;
	}

	public static void onTickStart() {
		inTick.set(true);
		movementSentInTick.set(false);
		tickEnded.set(false);
		resetTickActions();
		flushPendingSlot();
	}

	public static void onTickEnd() {
		inTick.set(false);
		movementSentInTick.set(true);
		tickEnded.set(true);
		resetTickActions();
	}

	private static void resetTickActions() {
		actionSentInTick.set(false);
		attackSentInTick.set(false);
		blockActionSentInTick.set(false);
		entityInteractSentInTick.set(false);
	}

	public static boolean isActionPacket(Packet<?> packet) {
		return packet instanceof PlayerInteractEntityC2SPacket ||
				packet instanceof PlayerInteractBlockC2SPacket ||
				packet instanceof PlayerInteractItemC2SPacket ||
				packet instanceof PlayerActionC2SPacket;
	}

	public static boolean filterPacket(Packet<?> packet) {
		if (packet instanceof ClientTickEndC2SPacket) {
			inTick.set(false);
			movementSentInTick.set(true);
			tickEnded.set(true);
			resetTickActions();
			return false;
		}

		if (packet instanceof PlayerMoveC2SPacket movePacket) {
			movementSentInTick.set(true);
			if (movePacket.changesLook()) {
				lastSentYaw = movePacket.getYaw(lastSentYaw);
				lastSentPitch = movePacket.getPitch(lastSentPitch);
				hasSentRotation = true;
			}
			return false;
		}

		if (isActionPacket(packet)) {
			if (movementSentInTick.get() || tickEnded.get() || !inTick.get()) {
				Recorder tm = Recorder.getInstance();
				if (tm != null) {
					tm.recordModuleEvent("Antiban", "SUPPRESSED_POST_MOVEMENT_ACTION",
							"Blocked post-movement action: " + packet.getClass().getSimpleName(), null);
				}
				return true;
			}
		}

		if (packet instanceof PlayerInteractEntityC2SPacket entityPacket) {
			if (blockActionSentInTick.get()) {
				Recorder tm = Recorder.getInstance();
				if (tm != null) {
					tm.recordModuleEvent("Antiban", "SUPPRESSED_CONFLICTING_ENTITY_ACTION",
							"Blocked entity interact after block action (MultiActions)", null);
				}
				return true;
			}

			final boolean[] isAttack = {false};
			entityPacket.handle(new PlayerInteractEntityC2SPacket.Handler() {
				@Override
				public void interact(net.minecraft.util.Hand hand) {}

				@Override
				public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d pos) {}

				@Override
				public void attack() {
					isAttack[0] = true;
				}
			});

			if (isAttack[0]) {
				if (attackSentInTick.get()) {
					Recorder tm = Recorder.getInstance();
					if (tm != null) {
						tm.recordModuleEvent("Antiban", "SUPPRESSED_DUPLICATE_ATTACK",
								"Blocked double attack in same tick (MultiAttack)", null);
					}
					return true;
				}
				actionSentInTick.set(true);
				attackSentInTick.set(true);
			} else {
				MinecraftClient mc = MinecraftClient.getInstance();
				if (mc.world != null) {
					int entityId = ((dlindustries.uk.daduncs.mixin.PlayerInteractEntityC2SPacketAccessor) entityPacket).getEntityId();
					net.minecraft.entity.Entity target = mc.world.getEntityById(entityId);
					if (target instanceof net.minecraft.entity.player.PlayerEntity) {
						Recorder tm = Recorder.getInstance();
						if (tm != null) {
							tm.recordModuleEvent("Antiban", "SUPPRESSED_USELESS_PLAYER_INTERACT",
									"Blocked non-attack player interact (id=" + entityId + ")", null);
						}
						return true;
					}
				}
				actionSentInTick.set(true);
				entityInteractSentInTick.set(true);
			}
			return false;
		}

		if (packet instanceof PlayerInteractBlockC2SPacket) {
			actionSentInTick.set(true);

			if (attackSentInTick.get() || entityInteractSentInTick.get()) {
				Recorder tm = Recorder.getInstance();
				if (tm != null) {
					tm.recordModuleEvent("Antiban", "SUPPRESSED_CONFLICTING_BLOCK_PLACE",
							"Blocked block place after attack (MultiActions/PacketOrder)", null);
				}
				return true;
			}

			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.player != null && (mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.BlockItem || mc.player.getOffHandStack().getItem() instanceof net.minecraft.item.BlockItem)) {
				float currentDeltaYaw = Math.abs(mc.player.getYaw() - mc.player.lastYaw);
				if (currentDeltaYaw > 2.0f && lastPlacedDeltaYaw > 0.0f) {
					float diff = Math.abs(currentDeltaYaw - lastPlacedDeltaYaw);
					if (diff < 0.00015f) {
						float microAdjustment = 0.0005f;
						mc.player.setYaw(mc.player.getYaw() + microAdjustment);
						currentDeltaYaw = Math.abs(mc.player.getYaw() - mc.player.lastYaw);
					}
				}
				lastPlacedDeltaYaw = currentDeltaYaw;
			}

			blockActionSentInTick.set(true);
			return false;
		}

		if (packet instanceof PlayerInteractItemC2SPacket) {
			actionSentInTick.set(true);

			if (attackSentInTick.get() && !entityInteractSentInTick.get()) {
				Recorder tm = Recorder.getInstance();
				if (tm != null) {
					tm.recordModuleEvent("Antiban", "SUPPRESSED_USE_ITEM_AFTER_ATTACK",
							"Blocked item use after attack without interact (PacketOrder)", null);
				}
				return true;
			}
			return false;
		}

		if (packet instanceof PlayerActionC2SPacket actionPacket) {
			actionSentInTick.set(true);
			if (actionPacket.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK ||
					actionPacket.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
				if (attackSentInTick.get() || entityInteractSentInTick.get()) {
					Recorder tm = Recorder.getInstance();
					if (tm != null) {
						tm.recordModuleEvent("Antiban", "SUPPRESSED_CONFLICTING_BLOCK_DIG",
								"Blocked dig after attack (MultiActions)", null);
					}
					return true;
				}
				blockActionSentInTick.set(true);
			}
			return false;
		}

		if (packet instanceof UpdateSelectedSlotC2SPacket slotPacket) {
			int slot = slotPacket.getSelectedSlot();
			if (actionSentInTick.get()) {
				pendingSlot.set(slot);
				Recorder tm = Recorder.getInstance();
				if (tm != null) {
					tm.recordModuleEvent("Antiban", "SUPPRESSED_POST_ACTION_SLOT",
							"Deferred slot change to " + slot + " (sent action in same tick)", null);
				}
				return true;
			}

			if (lastSentSlot.get() == slot) {
				Recorder tm = Recorder.getInstance();
				if (tm != null) {
					tm.recordModuleEvent("Antiban", "SUPPRESSED_DUPLICATE_SLOT",
							"Blocked duplicate slot switch to " + slot, null);
				}
				return true;
			}

			lastSentSlot.set(slot);
		}

		return false;
	}

	public static void flushPendingSlot() {
		int pending = pendingSlot.getAndSet(-1);
		if (pending != -1) {
			if (lastSentSlot.get() == pending) {
				return;
			}
			MinecraftClient mc = MinecraftClient.getInstance();
			if (mc.player != null && mc.getNetworkHandler() != null) {
				mc.player.getInventory().setSelectedSlot(pending);
				mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(pending));
				lastSentSlot.set(pending);
				Recorder tm = Recorder.getInstance();
				if (tm != null) {
					tm.recordModuleEvent("Antiban", "FLUSHED_PENDING_SLOT",
							"Flushed deferred slot change to " + pending + " at tick start", null);
				}
			}
		}
	}
}

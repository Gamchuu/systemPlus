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

package dlindustries.uk.daduncs.module.modules.render;

import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.module.setting.mode;
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.render.esp.OutlineRenderer;
import dlindustries.uk.daduncs.render.esp.TracerRenderer;
import dlindustries.uk.daduncs.utils.EncryptedString;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

public final class Esp extends Module {
	public enum Fill {
		SOLID, FROSTED
	}

	private static final int PLAYER_NEAR = 0xFFFFFFFF;
	private static final int PLAYER_FAR = 0xFFBFBFBF;
	private static final double NEAR_DISTANCE = 8;
	private static final double FAR_DISTANCE = 48;

	private static final int CRYSTAL = 0xFFB06CDB;
	private static final int ITEM = 0xFFFFFFFF;
	private static final int CHEST = 0xFF9A6A3A;
	private static final int ENDER_CHEST = 0xFF5B3A73;
	private static final int BARREL = 0xFF8B5A2B;
	private static final int SHULKER = 0xFF9A7AAE;
	private static final int SPAWNER = 0xFF686868;


	private final bool players = new bool(EncryptedString.of("Players"), true);
	private final bool crystals = new bool(EncryptedString.of("Crystals"), true);
	private final bool items = new bool(EncryptedString.of("Items"), true);
	private final bool storage = new bool(EncryptedString.of("Storage"), true);
	private final bool spawners = new bool(EncryptedString.of("Spawners"), true);
	private final bool tracers = new bool(EncryptedString.of("Tracers"), false);
	private final mode<Fill> fillMode = new mode<>(EncryptedString.of("Fill Mode"), Fill.SOLID, Fill.class);
	private final number fill = new number(EncryptedString.of("Fill"), 0, 1, 0.2, 0.05);
	private final number width = new number(EncryptedString.of("Width"), 1, 6, 1, 0.5);
	private final number glow = new number(EncryptedString.of("Glow"), 0, 24, 0, 1);

	private final OutlineRenderer renderer = new OutlineRenderer();
	private final TracerRenderer tracerRenderer = new TracerRenderer();

	public Esp() {
		super(EncryptedString.of("Esp"),
				EncryptedString.of("Highlights stuff ( players , blocks of some kinds  ) threw walls"),
				-1,
				Category.RENDER);
		addSettings(players, crystals, items, storage, spawners, tracers, fillMode, fill, width, glow);
	}

	public void beginFrame() {
		renderer.beginFrame();
	}

	public void submit(WorldRenderState world) {
		if (mc.player == null || mc.world == null) return;

		for (EntityRenderState entity : world.entityRenderStates) {
			int color = entityColor(entity);
			if (color == 0) continue;

			renderer.addEntity(entity, world, color);
			if (tracers.getValue()) {
				tracerRenderer.add(new Vec3d(entity.x, entity.y + entity.height / 2, entity.z), world, color);
			}
		}

		if (storage.getValue() || spawners.getValue()) {
			submitBlocks(world);
		}

		renderer.drawShapes();
		tracerRenderer.draw();
	}

	public void composite() {
		renderer.composite(width.getValueFloat(), glow.getValueFloat(), fill.getValueFloat(), fillMode.getModeIndex());
	}

	public boolean forcesEntities() {
		return players.getValue() || crystals.getValue() || items.getValue();
	}

	private void submitBlocks(WorldRenderState world) {
		int chunks = mc.options.getClampedViewDistance();
		ChunkPos center = mc.player.getChunkPos();

		for (int x = center.x - chunks; x <= center.x + chunks; x++) {
			for (int z = center.z - chunks; z <= center.z + chunks; z++) {
				WorldChunk chunk = mc.world.getChunkManager().getChunk(x, z, ChunkStatus.FULL, false);
				if (chunk == null) continue;

				for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					int color = blockColor(blockEntity);
					if (color == 0) continue;

					if (blockEntity instanceof BarrelBlockEntity || blockEntity instanceof MobSpawnerBlockEntity) {
						renderer.addBlock(blockEntity, world, color);
					} else {
						renderer.addBlockEntity(blockEntity, world, color);
					}

					if (tracers.getValue()) {
						tracerRenderer.add(blockEntity.getPos().toCenterPos(), world, color);
					}
				}
			}
		}
	}

	private int entityColor(EntityRenderState entity) {
		if (entity.entityType == EntityType.PLAYER && players.getValue()) {
			if (entity instanceof PlayerEntityRenderState player && player.id == mc.player.getId()) return 0;

			float t = (float) Math.clamp((Math.sqrt(entity.squaredDistanceToCamera) - NEAR_DISTANCE) / (FAR_DISTANCE - NEAR_DISTANCE), 0, 1);
			return tint(ColorHelper.lerp(t, PLAYER_NEAR, PLAYER_FAR));
		}
		if (entity.entityType == EntityType.END_CRYSTAL && crystals.getValue()) {
			return tint(CRYSTAL);
		}
		if (entity.entityType == EntityType.ITEM && items.getValue()) {
			return tint(ITEM);
		}
		return 0;
	}

	private static int tint(int color) {
		return Theme.lerp(color, Theme.accent, 0.55f);
	}

	private int blockColor(BlockEntity blockEntity) {
		if (blockEntity instanceof MobSpawnerBlockEntity) {
			return spawners.getValue() ? tint(SPAWNER) : 0;
		}
		if (!storage.getValue()) return 0;

		int color = switch (blockEntity) {
			case ChestBlockEntity chest -> CHEST;
			case EnderChestBlockEntity enderChest -> ENDER_CHEST;
			case BarrelBlockEntity barrel -> BARREL;
			case ShulkerBoxBlockEntity shulker -> {
				DyeColor dye = shulker.getColor();
				yield dye == null ? SHULKER : dye.getEntityColor() | 0xFF000000;
			}
			default -> 0;
		};

		return color == 0 ? 0 : tint(color);
	}
}

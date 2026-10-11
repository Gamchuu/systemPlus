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

import dlindustries.uk.daduncs.utils.rotation.Rotation;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.stream.Stream;

import static dlindustries.uk.daduncs.System.mc;


public final class BlockUtils {
	public static boolean isBlock(BlockPos pos, Block block) {
		return mc.world != null && mc.world.getBlockState(pos).getBlock() == block;
	}

	public static boolean isInteractiveBlock(BlockPos pos) {
		if (mc.world == null || pos == null) return false;
		BlockState state = mc.world.getBlockState(pos);
		Block block = state.getBlock();

		return block instanceof BlockWithEntity
				|| block instanceof DoorBlock
				|| block instanceof TrapdoorBlock
				|| block instanceof FenceGateBlock
				|| block instanceof LeverBlock
				|| block instanceof ButtonBlock
				|| block instanceof CraftingTableBlock
				|| block instanceof AnvilBlock
				|| block instanceof BedBlock
				|| block instanceof RespawnAnchorBlock
				|| block instanceof NoteBlock
				|| block instanceof JukeboxBlock
				|| block instanceof RepeaterBlock
				|| block instanceof ComparatorBlock
				|| block instanceof DaylightDetectorBlock
				|| block instanceof LoomBlock
				|| block instanceof CartographyTableBlock
				|| block instanceof GrindstoneBlock
				|| block instanceof SmithingTableBlock
				|| block instanceof StonecutterBlock
				|| block instanceof BellBlock
				|| block instanceof LecternBlock
				|| block instanceof CakeBlock
				|| block instanceof CandleCakeBlock
				|| block instanceof SweetBerryBushBlock;
	}

	public static boolean isAxeInteractiveBlock(BlockPos pos) {
		if (mc.world == null || pos == null) return false;
		BlockState state = mc.world.getBlockState(pos);
		Block block = state.getBlock();
		return state.isIn(BlockTags.LOGS)
				|| block instanceof Oxidizable
				|| block.getTranslationKey().contains("copper");
	}

	public static void rotateToBlock(BlockPos pos) {
        assert mc.player != null;
        Rotation rotation = RotationUtils.getDirection(mc.player, pos.toCenterPos());
		Rotation rot = RotationUtils.applyGCD(mc.player.getYaw(), mc.player.getPitch(), (float) rotation.yaw(), (float) rotation.pitch(), null, true);

		mc.player.setPitch((float) rot.pitch());
		mc.player.setYaw((float) rot.yaw());
	}

	public static boolean isAnchorCharged(BlockPos pos) {
		if (isBlock(pos, Blocks.RESPAWN_ANCHOR)) {
			return mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) != 0;
		}
		return false;
	}

	public static boolean isAnchorNotCharged(BlockPos pos) {
		if (isBlock(pos, Blocks.RESPAWN_ANCHOR)) {
			return mc.world.getBlockState(pos).get(RespawnAnchorBlock.CHARGES) == 0;
		}

		return false;
	}

	public static boolean canPlaceBlockClient(BlockPos block) {
		BlockPos up = block.up();

		if(!mc.world.isAir(up))
			return false;

		double x = up.getX();
		double y = up.getY();
		double z = up.getZ();

		List<Entity> list = mc.world.getOtherEntities(null, new Box(x, y, z, x + 1, y + 1, z + 1));
		list.removeIf(entity -> entity instanceof ItemEntity);

		return list.isEmpty();
	}

	public static Stream<BlockPos> getAllInBoxStream(BlockPos from, BlockPos to) {
		BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
		BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));

		Stream<BlockPos> stream = Stream.iterate(min, (pos) -> {
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();

			++x;

			if (x > max.getX()) {
				x = min.getX();
				++y;
			}

			if (y > max.getY()) {
				y = min.getY();
				++z;
			}

			if (z > max.getZ())
				throw new IllegalStateException("Stream limit didn't work.");
			else return new BlockPos(x, y, z);
		});
		int limit = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);

		return stream.limit(limit);
	}
}

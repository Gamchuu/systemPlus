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

package dlindustries.uk.daduncs.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

import static dlindustries.uk.daduncs.System.mc;

public final class CrystalUtils {
	public static boolean canPlaceCrystalClient(BlockPos block) {
		BlockState blockState = mc.world.getBlockState(block);
		if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK))
			return false;

		return canPlaceCrystalClientAssumeObsidian(block);
	}

	public static boolean canPlaceCrystalClientAssumeObsidian(BlockPos block) {
		BlockPos blockPos2 = block.up();
		if (!mc.world.isAir(blockPos2))
			return false;

		double d = blockPos2.getX();
		double e = blockPos2.getY();
		double f = blockPos2.getZ();

		List<Entity> list = mc.world.getOtherEntities(null, new Box(d, e, f, d + 1.0D, e + 2.0D, f + 1.0D));
		return list.isEmpty();
	}

	public static boolean canPlaceCrystalServer(BlockPos pos) {
		BlockState blockState = mc.world.getBlockState(pos);
		if (!blockState.isOf(Blocks.OBSIDIAN) || !blockState.isOf(Blocks.BEDROCK))
			return false;

		BlockPos blockPos = pos.up();
		if (!mc.world.isAir(blockPos))
			return false;

		double d = blockPos.getX();
		double e = blockPos.getY();
		double f = blockPos.getZ();

		List<Entity> list = mc.world.getOtherEntities(null, new Box(d, e, f, d + 1, e + 2, f + 1));
		return list.isEmpty();
	}
}

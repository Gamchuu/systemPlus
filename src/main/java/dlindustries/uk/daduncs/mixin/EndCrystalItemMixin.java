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

package dlindustries.uk.daduncs.mixin;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.module.modules.render.NoBounce;
import dlindustries.uk.daduncs.utils.CrystalUtils;
import dlindustries.uk.daduncs.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dlindustries.uk.daduncs.System.mc;

@Mixin(EndCrystalItem.class)
public class EndCrystalItemMixin {

	@Unique
	private Vec3d getPlayerLookVec(PlayerEntity p) {
		return RenderUtils.getPlayerLookVec(p);
	}

	@Unique
	private Vec3d getClientLookVec() {
		assert mc.player != null;
		return getPlayerLookVec(mc.player);
	}

	@Unique
	private boolean isBlock(Block b, BlockPos p) {
		return (getBlockState(p).getBlock() == b);
	}

	@Unique
	private BlockState getBlockState(BlockPos p) {
		return mc.world.getBlockState(p);
	}

	@Unique
	private boolean canPlaceCrystalServer(BlockPos blockPos) {
		BlockState blockState = mc.world.getBlockState(blockPos);
		if (!blockState.isOf(Blocks.OBSIDIAN) && !blockState.isOf(Blocks.BEDROCK))
			return false;
		return CrystalUtils.canPlaceCrystalClientAssumeObsidian(blockPos);
	}

	@Inject(method = "useOnBlock", at = @At("HEAD"))
	private void onUse(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
		NoBounce noBounce = System.INSTANCE.getModuleManager().getModule(NoBounce.class);
		if (noBounce.isEnabled()) {
			if (System.INSTANCE != null && mc.player != null) {
				ItemStack mainHandStack = mc.player.getMainHandStack();

				if (mainHandStack.isOf(Items.END_CRYSTAL)) {
					Vec3d e = mc.player.getEyePos();

					BlockHitResult blockHit = mc.world.raycast(new RaycastContext(e, e.add(getClientLookVec().multiply(4.5)), RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
					if (isBlock(Blocks.OBSIDIAN, blockHit.getBlockPos()) || isBlock(Blocks.BEDROCK, blockHit.getBlockPos())) {
						HitResult hitResult = mc.crosshairTarget;

						if (hitResult instanceof BlockHitResult blockHit2) {
							BlockPos pos = blockHit2.getBlockPos();

							if (canPlaceCrystalServer(pos))
								context.getStack().decrement(-1);
						}
					}
				}
			}
		}
	}
}

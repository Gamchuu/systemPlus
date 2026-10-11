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

package dlindustries.uk.daduncs.module.modules.cart;

import dlindustries.uk.daduncs.event.events.TickListener;
import dlindustries.uk.daduncs.managers.AntibanbyDaduncs;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.utils.EncryptedString;
import dlindustries.uk.daduncs.utils.InventoryUtils;
import dlindustries.uk.daduncs.utils.TimerUtils;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.function.Predicate;

public final class AutoCart extends Module implements TickListener {

	private final number delay = new number(EncryptedString.of("Delay"), 0, 500, 75, 5);
	private final number cooldown = new number(EncryptedString.of("Cooldown"), 250, 3000, 1000, 50);
	private final bool flameOnly = new bool(EncryptedString.of("Flame Only"), true);

	private final TimerUtils timer = new TimerUtils();
	private final TimerUtils rest = new TimerUtils();

	private boolean active;
	private int step;
	private int ticks;
	private int oldSlot = -1;
	private double wait;
	private BlockPos target;

	public AutoCart() {
		super(EncryptedString.of("AutoCart"), EncryptedString.of("Helps you place a rail and a cart."), -1, Category.CART);
		addSettings(delay, cooldown, flameOnly);
	}

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		super.onEnable();
		active = false;
		target = null;
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
		if (active)
			reset();
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null)
			return;

		if (!active) {
			if (mc.currentScreen != null || !rest.hasReached(cooldown.getValue()))
				return;

			if (!mc.player.isUsingItem() || !mc.player.getActiveItem().isOf(Items.BOW))
				return;

			if (flameOnly.getValue() && !flame(mc.player.getActiveItem()))
				return;

			if (find(this::isRail) == -1 || find(item -> item == Items.TNT_MINECART) == -1)
				return;

			target = getTarget();
			if (target == null)
				return;

			boolean railed = railed();
			if (!railed && !canRail())
				return;

			active = true;
			oldSlot = mc.player.getInventory().getSelectedSlot();
			mc.player.clearActiveItem();
			next(railed ? 1 : 0);
			return;
		}

		ticks++;

		if (mc.currentScreen != null || ticks > 20) {
			reset();
			return;
		}

		if (!timer.hasReached(wait) || AntibanbyDaduncs.isActionSent() || AntibanbyDaduncs.isSlotPending())
			return;

		switch (step) {
			case 0 -> {
				if (railed()) {
					next(1);
					return;
				}

				BlockHitResult hit = look(target.down());
				if (hit == null || hit.getSide() != Direction.UP || !use(this::isRail, hit)) {
					reset();
					return;
				}
				next(1);
			}
			case 1 -> {
				if (!railed()) {
					if (ticks > 6)
						reset();
					return;
				}

				BlockHitResult hit = look(target);
				if (hit == null || !use(item -> item == Items.TNT_MINECART, hit)) {
					reset();
					return;
				}
				next(2);
			}
			case 2 -> {
				if (!carted() && ticks <= 4)
					return;
				reset();
			}
		}
	}

	private boolean use(Predicate<Item> filter, BlockHitResult hit) {
		int slot = find(filter);
		if (slot == -1)
			return false;

		InventoryUtils.setInvSlot(slot);
		ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
		if (result.isAccepted())
			mc.player.swingHand(Hand.MAIN_HAND);

		return result.isAccepted();
	}

	private boolean railed() {
		return mc.world.getBlockState(target).getBlock() instanceof AbstractRailBlock;
	}

	private boolean canRail() {
		BlockPos below = target.down();
		return mc.world.getBlockState(target).isReplaceable()
				&& mc.world.getBlockState(below).isSideSolidFullSquare(mc.world, below, Direction.UP);
	}

	private BlockHitResult look(BlockPos pos) {
		Vec3d eye = mc.player.getEyePos();
		Vec3d end = eye.add(mc.player.getRotationVec(1f).multiply(mc.player.getBlockInteractionRange()));
		BlockHitResult hit = mc.world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));

		return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos) ? hit : null;
	}

	private boolean carted() {
		return !mc.world.getEntitiesByClass(TntMinecartEntity.class, new Box(target), cart -> true).isEmpty();
	}

	private void next(int step) {
		this.step = step;
		ticks = 0;
		timer.reset();

		wait = Math.max(50, delay.getValue());
		if (ClickGUI.randomisedTiming.getValue() && ClickGUI.VARIANCE > 0)
			wait += Math.random() * ClickGUI.VARIANCE;
	}

	private void reset() {
		if (oldSlot != -1 && mc.player != null)
			InventoryUtils.setInvSlot(oldSlot);

		active = false;
		step = 0;
		ticks = 0;
		oldSlot = -1;
		target = null;
		rest.reset();
	}

	private BlockPos getTarget() {
		if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
			return null;

		if (mc.world.getBlockState(hit.getBlockPos()).getBlock() instanceof AbstractRailBlock)
			return hit.getBlockPos();

		return hit.getSide() == Direction.UP ? hit.getBlockPos().up() : null;
	}

	private boolean flame(ItemStack stack) {
		for (RegistryEntry<Enchantment> enchant : stack.getEnchantments().getEnchantments()) {
			if (enchant.matchesKey(Enchantments.FLAME))
				return true;
		}

		return false;
	}

	private int find(Predicate<Item> filter) {
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			if (!stack.isEmpty() && filter.test(stack.getItem()))
				return i;
		}

		return -1;
	}

	private boolean isRail(Item item) {
		return item == Items.RAIL || item == Items.POWERED_RAIL || item == Items.DETECTOR_RAIL || item == Items.ACTIVATOR_RAIL;
	}
}

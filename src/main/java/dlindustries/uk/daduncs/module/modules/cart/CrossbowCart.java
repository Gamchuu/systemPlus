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

package dlindustries.uk.daduncs.module.modules.cart;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.event.events.GameRenderListener;
import dlindustries.uk.daduncs.event.events.HudListener;
import dlindustries.uk.daduncs.event.events.TickListener;
import dlindustries.uk.daduncs.managers.AntibanbyDaduncs;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.mixin.MinecraftClientAccessor;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.utils.EncryptedString;
import dlindustries.uk.daduncs.utils.InventoryUtils;
import dlindustries.uk.daduncs.utils.RenderUtils;
import dlindustries.uk.daduncs.utils.RotationUtils;
import dlindustries.uk.daduncs.utils.TimerUtils;
import dlindustries.uk.daduncs.utils.WorldUtils;
import dlindustries.uk.daduncs.utils.rotation.Rotation;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public final class CrossbowCart extends Module implements TickListener, HudListener, GameRenderListener {

	public CrossbowCart() {
		super(EncryptedString.of("CrossbowCart"), EncryptedString.of("Places a tnt cart and shoots it with a crossbow"), -1, Category.CART);
		addSettings(autoAim, loadCrossbow, render, delay, smooth, activate);
	}

	private final bool autoAim = new bool(EncryptedString.of("Auto Aim"), true);
	private final bool loadCrossbow = new bool(EncryptedString.of("Load Crossbow"), true);
	private final bool render = new bool(EncryptedString.of("Render"), true);
	private final bind activate = new bind(EncryptedString.of("Activate"), GLFW.GLFW_KEY_UNKNOWN, false);
	private final number delay = new number(EncryptedString.of("Delay"), 0, 1000, 50, 1);
	private final number smooth = new number(EncryptedString.of("Smooth"), 0, 400, 140, 10);

	private static final double SPEED = 3.15;
	private static final long MIN_LOOK = 150;
	private static final Color GOOD = new Color(70, 220, 120);
	private static final Color BAD = new Color(225, 70, 70);

	private final TimerUtils timer = new TimerUtils();
	private final double[] gcd = new double[2];
	private double wait;
	private BlockPos spot;
	private BlockPos fire;
	private int stage;
	private int ticks;
	private int prevSlot;
	private float aimYaw, aimPitch;
	private float fromYaw, fromPitch, toYaw, toPitch;
	private long lookStart;
	private boolean looked;
	private boolean spotOk;
	private boolean fireOk;
	private boolean armed;
	private boolean held;

	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(HudListener.class, this);
		eventManager.add(GameRenderListener.class, this);
		super.onEnable();

		spotOk = false;
		fireOk = false;

		if (mc.player == null || mc.world == null) {
			setEnabled(false);
			return;
		}


		boolean cart = mc.player.getOffHandStack().isOf(Items.TNT_MINECART) || InventoryUtils.hasItemInHotbar(i -> i == Items.TNT_MINECART);
		boolean rail = InventoryUtils.hasItemInHotbar(i -> i instanceof BlockItem b && b.getBlock() instanceof AbstractRailBlock);
		boolean bow = InventoryUtils.hasItemInHotbar(i -> i == Items.CROSSBOW);
		boolean steel = mc.player.getOffHandStack().isOf(Items.FLINT_AND_STEEL) || InventoryUtils.hasItemInHotbar(i -> i == Items.FLINT_AND_STEEL);

		if (!cart || !rail || !bow || !steel) {
			setEnabled(false);
			return;
		}


		armed = activate.getKey() != GLFW.GLFW_KEY_UNKNOWN;
		prevSlot = mc.player.getInventory().getSelectedSlot();

		if (armed) {
			idle();
			scan();
			return;
		}

		scan();

		if (spot == null) {
			setEnabled(false);
			return;
		}

		launch();
	}

	private void scan() {
		spot = autoAim.getValue() ? findSpot() : null;

		if (spot == null && mc.crosshairTarget instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = hit.getBlockPos().offset(hit.getSide());
			if (canPlace(pos) && aim(pos)) spot = pos;
		}

		fire = spot == null ? null : findFire(spot);

		check();
	}

	private void launch() {
		looked = true;

		if (autoAim.getValue()) {
			Rotation rotation = RotationUtils.getDirection(mc.player, top(spot));
			look((float) rotation.yaw(), (float) rotation.pitch());
		}

		next(0);
	}

	private void idle() {
		stage = -1;
		ticks = 0;
		spot = null;
		fire = null;
		spotOk = false;
		fireOk = false;
		looked = true;
		mc.options.useKey.setPressed(false);
	}

	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(HudListener.class, this);
		eventManager.remove(GameRenderListener.class, this);
		super.onDisable();
	}

	@Override
	public void onRenderHud(HudEvent event) {
		if (mc.player == null) return;
		if (looked) return;

		long time = Math.max(MIN_LOOK, (long) smooth.getValue());
		long elapsed = java.lang.System.currentTimeMillis() - lookStart;
		double eased = RotationUtils.minimumJerk(Math.min(1.0, (double) elapsed / time));
		float yaw = fromYaw + MathHelper.wrapDegrees(toYaw - fromYaw) * (float) eased;
		float pitch = fromPitch + (toPitch - fromPitch) * (float) eased;
		Rotation rot = RotationUtils.applyGCD(mc.player.getYaw(), mc.player.getPitch(), yaw, pitch, gcd, true);
		mc.player.setYaw((float) rot.yaw());
		mc.player.setPitch((float) rot.pitch());

		if (elapsed >= time) looked = true;
	}

	@Override
	public void onGameRender(GameRenderEvent event) {
		if (!render.getValue() || mc.player == null || mc.world == null) return;
		if (spot == null) return;

		Camera camera = mc.gameRenderer.getCamera();
		if (camera == null) return;

		Vec3d eye = camera.getCameraPos();

		event.matrices.push();
		event.matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
		event.matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180f));
		event.matrices.translate(-eye.x, -eye.y, -eye.z);

		face(event.matrices, spot, 0.02, spotOk);
		if (fire != null) face(event.matrices, fire, 0.2, fireOk);

		event.matrices.pop();
	}

	private void face(MatrixStack matrices, BlockPos pos, double inset, boolean ok) {
		Color color = ok ? GOOD : BAD;
		Box plate = new Box(pos.getX() + inset, pos.getY() + 0.012, pos.getZ() + inset,
				pos.getX() + 1 - inset, pos.getY() + 0.012, pos.getZ() + 1 - inset);

		RenderUtils.renderFilledBox(matrices, (float) plate.minX, (float) plate.minY, (float) plate.minZ,
				(float) plate.maxX, (float) plate.maxY, (float) plate.maxZ,
				new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
		RenderUtils.renderBoxOutline(matrices, plate, color, 2f);
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.world == null) return;

		ticks++;

		boolean down = activate.isPressed();
		boolean press = down && !held;
		held = down;

		if (armed && stage == -1) {
			if (ticks % 2 == 0) scan();
			if (press && spot != null) launch();
			return;
		}

		if (render.getValue()) check();

		if (stage == 3) {
			InventoryUtils.selectItemFromHotbar(Items.CROSSBOW);

			if (!CrossbowItem.isCharged(mc.player.getMainHandStack())) {
				if (!loadCrossbow.getValue() || ticks > 60) {
					stop();
					return;
				}

				mc.options.useKey.setPressed(true);
				return;
			}

			mc.options.useKey.setPressed(false);

			if (!looked) return;
			if (!timer.hasReached(wait)) return;

			((MinecraftClientAccessor) mc).invokeDoItemUse();
			next(4);
			return;
		}

		if (stage == 4) {
			stop();
			return;
		}

		if (mc.currentScreen != null) return;
		if (!looked) return;
		if (!timer.hasReached(wait)) return;

		if (stage == 0) {
			if (AntibanbyDaduncs.isActionSent() || AntibanbyDaduncs.isSlotPending()) return;
			InventoryUtils.selectItemFromHotbar(i -> i instanceof BlockItem b && b.getBlock() instanceof AbstractRailBlock);
			WorldUtils.placeBlock(new BlockHitResult(top(spot), Direction.UP, spot.down(), false), true);
			next(1);
		} else if (stage == 1) {
			if (!(mc.world.getBlockState(spot).getBlock() instanceof AbstractRailBlock)) {
				if (ticks > 10)
					stop();
				return;
			}

			if (AntibanbyDaduncs.isActionSent() || AntibanbyDaduncs.isSlotPending()) return;

			BlockHitResult hit = new BlockHitResult(Vec3d.ofBottomCenter(spot).add(0, 0.1, 0), Direction.UP, spot, false);

			if (mc.player.getOffHandStack().isOf(Items.TNT_MINECART)) {
				mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, hit);
				mc.player.swingHand(Hand.OFF_HAND);
			} else {
				InventoryUtils.selectItemFromHotbar(Items.TNT_MINECART);
				WorldUtils.placeBlock(hit, true);
			}

			next(2);
		} else if (stage == 2) {
			if (fire != null) {
				if (AntibanbyDaduncs.isActionSent() || AntibanbyDaduncs.isSlotPending()) return;

				BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(fire.down()).add(0, 0.5, 0), Direction.UP, fire.down(), false);

				if (mc.player.getOffHandStack().isOf(Items.FLINT_AND_STEEL)) {
					mc.interactionManager.interactBlock(mc.player, Hand.OFF_HAND, hit);
					mc.player.swingHand(Hand.OFF_HAND);
				} else {
					InventoryUtils.selectItemFromHotbar(Items.FLINT_AND_STEEL);
					WorldUtils.placeBlock(hit, true);
				}
			}

			if (autoAim.getValue()) look(aimYaw, aimPitch);

			next(3);
		}
	}

	private void next(int stage) {
		this.stage = stage;
		ticks = 0;
		timer.reset();

		wait = Math.max(50.0, delay.getValue());
		if (ClickGUI.randomisedTiming.getValue() && ClickGUI.VARIANCE > 0)
			wait += (int) (Math.random() * ClickGUI.VARIANCE);
	}

	private void look(float yaw, float pitch) {
		fromYaw = mc.player.getYaw();
		fromPitch = mc.player.getPitch();
		toYaw = yaw;
		toPitch = pitch;
		lookStart = java.lang.System.currentTimeMillis();
		looked = false;
	}

	private void stop() {
		mc.options.useKey.setPressed(false);
		if (!AntibanbyDaduncs.isActionSent() && !AntibanbyDaduncs.isSlotPending()) {
			InventoryUtils.setInvSlot(prevSlot);
		}

		if (armed) {
			idle();
			return;
		}

		setEnabled(false);
	}

	private void check() {
		if (spot == null) {
			spotOk = false;
			fireOk = false;
			return;
		}

		boolean railed = mc.world.getBlockState(spot).getBlock() instanceof AbstractRailBlock;

		spotOk = (railed || canPlace(spot)) && flight(cartPos(spot)) != -1;
		fireOk = fire != null && canFire(fire) && crosses(fireBox(fire), cartPos(spot));
	}

	private BlockPos findSpot() {
		PlayerEntity target = null;
		for (PlayerEntity player : mc.world.getPlayers()) {
			if (player == mc.player || player.isDead() || System.INSTANCE.getFriendManager().isFriend(player)) continue;

			if (mc.player.distanceTo(player) < 10 && (target == null || mc.player.distanceTo(player) < mc.player.distanceTo(target)))
				target = player;
		}

		if (target == null) return null;

		BlockPos best = null;
		double bestScore = -1000;

		for (BlockPos pos : BlockPos.iterate(target.getBlockPos().add(-3, -1, -3), target.getBlockPos().add(3, 1, 3))) {
			if (!canPlace(pos) || !aim(pos))
				continue;
			if (flight(cartPos(pos)) == -1) continue;

			Vec3d cart = cartPos(pos);
			double score = cart.distanceTo(mc.player.getEntityPos()) - cart.distanceTo(target.getEntityPos()) * 1.5;

			if (!sees(cart, target.getBoundingBox().getCenter()))
				score -= 4;

			if (findFire(pos) == null)
				score -= 6;

			if (score > bestScore) {
				bestScore = score;
				best = pos.toImmutable();
			}
		}

		if (best != null) aim(best);

		return best;
	}

	private BlockPos findFire(BlockPos cart) {
		BlockPos best = null;
		double bestScore = -1000;

		for (BlockPos pos : BlockPos.iterate(cart.add(-3, -2, -3), cart.add(3, 3, 3))) {
			if (pos.equals(cart))
				continue;

			if (!crosses(fireBox(pos), cartPos(cart)))
				continue;

			if (!canFire(pos))
				continue;

			double score = (pos.getY() - cart.getY()) * 3 - Vec3d.ofCenter(pos).distanceTo(mc.player.getEntityPos());

			if (score > bestScore) {
				bestScore = score;
				best = pos.toImmutable();
			}
		}

		return best;
	}

	private Box fireBox(BlockPos pos) {
		return new Box(pos.getX() - 0.25, pos.getY() - 0.5, pos.getZ() - 0.25,
				pos.getX() + 1.25, pos.getY() + 1, pos.getZ() + 1.25);
	}

	private boolean canFire(BlockPos pos) {
		if (!mc.world.getBlockState(pos).isReplaceable()) return false;

		BlockPos under = pos.down();

		if (!mc.world.getBlockState(under).isSideSolidFullSquare(mc.world, under, Direction.UP))
			return false;

		if (mc.player.getBoundingBox().expand(0.15, 0, 0.15).intersects(new Box(pos)))
			return false;

		Vec3d eye = mc.player.getEyePos();
		Vec3d point = top(pos);

		if (eye.distanceTo(point) > 4.5)
			return false;

		BlockHitResult hit = mc.world.raycast(new RaycastContext(eye, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(under);
	}

	private boolean canPlace(BlockPos pos) {
		if (!mc.world.getBlockState(pos).isReplaceable()) return false;

		if (!mc.world.getBlockState(pos.down()).isSideSolidFullSquare(mc.world, pos.down(), Direction.UP))
			return false;

		if (!mc.world.getOtherEntities(null, new Box(pos), e -> e instanceof AbstractMinecartEntity).isEmpty())
			return false;

		Vec3d eye = mc.player.getEyePos();
		if (eye.distanceTo(top(pos)) > 4.5)
			return false;

		BlockHitResult hit = mc.world.raycast(new RaycastContext(eye, top(pos), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
		return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos.down());
	}

	private boolean sees(Vec3d from, Vec3d to) {
		return mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
	}

	private Vec3d top(BlockPos pos) {
		return Vec3d.ofCenter(pos.down()).add(0, 0.5, 0);
	}

	private Vec3d cartPos(BlockPos pos) {
		return Vec3d.ofBottomCenter(pos).add(0, 0.35, 0);
	}

	private boolean aim(BlockPos pos) {
		Vec3d target = cartPos(pos);
		Vec3d eye = mc.player.getEyePos();
		double distance = Math.hypot(target.x - eye.x, target.z - eye.z);
		double height = target.y - eye.y + 0.1;

		aimYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(target.z - eye.z, target.x - eye.x)) - 90);

		for (float pitch = 89; pitch >= -60; pitch -= 0.5f) {
			if (dropAt(pitch, SPEED, distance) < height) continue;

			aimPitch = pitch;
			return flight(target) != -1;
		}

		return false;
	}

	private double dropAt(float pitch, double power, double distance) {
		double x = 0, y = 0;
		double mx = Math.cos(Math.toRadians(pitch)) * power;
		double my = -Math.sin(Math.toRadians(pitch)) * power;

		for (int i = 0; i < 100 && mx > 0.01; i++) {
			if (x + mx >= distance)
				return y + my * (distance - x) / mx;

			x += mx;
			y += my;
			mx *= 0.99;
			my = my * 0.99 - 0.05;
		}

		return -1000;
	}

	private int flight(Vec3d target) {
		Vec3d pos = mc.player.getEyePos().add(0, -0.1, 0);
		Vec3d motion = RotationUtils.getPlayerLookVec(aimYaw, aimPitch).multiply(SPEED);

		for (int i = 0; i < 100; i++) {
			Vec3d next = pos.add(motion);

			for (int j = 0; j <= 8; j++) {
				Vec3d point = pos.lerp(next, j / 8.0);
				if (point.distanceTo(target) < 0.6)
					return sees(pos, point) && !blocked(pos, point) ? i + 1 : -1;
			}

			if (!sees(pos, next) || blocked(pos, next))
				return -1;

			pos = next;
			motion = new Vec3d(motion.x * 0.99, motion.y * 0.99 - 0.05, motion.z * 0.99);
		}

		return -1;
	}

	private boolean crosses(Box box, Vec3d target) {
		Vec3d pos = mc.player.getEyePos().add(0, -0.1, 0);
		Vec3d motion = RotationUtils.getPlayerLookVec(aimYaw, aimPitch).multiply(SPEED);

		for (int i = 0; i < 100; i++) {
			Vec3d next = pos.add(motion);

			for (int j = 0; j <= 8; j++) {
				Vec3d point = pos.lerp(next, j / 8.0);

				if (box.contains(point))
					return true;

				if (point.distanceTo(target) < 0.6)
					return false;
			}

			pos = next;
			motion = new Vec3d(motion.x * 0.99, motion.y * 0.99 - 0.05, motion.z * 0.99);
		}

		return false;
	}

	private boolean blocked(Vec3d from, Vec3d to) {
		for (Entity entity : mc.world.getOtherEntities(mc.player, new Box(from, to).expand(1))) {
			if (entity.canHit() && !(entity instanceof AbstractMinecartEntity) && entity.getBoundingBox().expand(0.3).raycast(from, to).isPresent())
				return true;
		}

		return false;
	}
}

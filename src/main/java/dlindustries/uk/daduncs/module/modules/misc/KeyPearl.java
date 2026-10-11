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

package dlindustries.uk.daduncs.module.modules.misc;

import dlindustries.uk.daduncs.event.events.HudListener;
import dlindustries.uk.daduncs.event.events.TickListener;
import dlindustries.uk.daduncs.managers.AntibanbyDaduncs;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.utils.EncryptedString;
import dlindustries.uk.daduncs.utils.InventoryUtils;
import dlindustries.uk.daduncs.utils.KeyUtils;
import dlindustries.uk.daduncs.utils.MathUtils;
import dlindustries.uk.daduncs.utils.RotationUtils;
import dlindustries.uk.daduncs.utils.TimerUtils;
import dlindustries.uk.daduncs.utils.WorldUtils;
import dlindustries.uk.daduncs.utils.rotation.Rotation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class KeyPearl extends Module implements TickListener, HudListener {
    private final bind activateKey = new bind(EncryptedString.of("Activate Key"), -1, false);
    private final bind aggroPearlKey = new bind(EncryptedString.of("Aggro Pearl Key"), -1, false);
    private final number aggroLandingOffset = new number(EncryptedString.of("Landing Offset"), -3.0, 3.0, -2.5, 0.1)
            .setDescription(EncryptedString.of("use -3 to -2"));

    private final number flickDuration = new number(EncryptedString.of("Flick Ticks"), 1, 10, 3, 1)
            .setDescription(EncryptedString.of("Smooth ballistic rotation ticks before throwing"));

    private final number delay = new number(EncryptedString.of("Delay"), 0, 20, 1, 1)
            .setDescription(EncryptedString.of("Ticks to wait before throwing"));

    private final bool switchBack = new bool(EncryptedString.of("Switch Back"), true);
    private final number switchslot = new number(EncryptedString.of("Switch Slot"), 1, 9, 1, 1)
            .setDescription(EncryptedString.of("The slot it returns to after pearling (e.g., your sword slot)"));
    private final number switchDelay = new number(EncryptedString.of("Switch Delay"), 0, 20, 2, 1)
            .setDescription(EncryptedString.of("Delay in ticks after throwing before switching back"));

    private boolean active, hasActivated, aggroActive;
    private PlayerEntity aggroTarget;
    private long flickStartTime;
    private long flickDurationMs;
    private boolean flickDone;
    private float startYaw, startPitch;
    private float targetYaw, targetPitch;
    private final double[] gcdRemainder = new double[2];
    private final TimerUtils actionTimer = new TimerUtils();
    private final TimerUtils switchTimer = new TimerUtils();

    public KeyPearl() {
        super(EncryptedString.of("Key Pearl"), EncryptedString.of("Switches to an ender pearl and throws it when you press a bind"), -1, Category.MISC);
        addSettings(activateKey, aggroPearlKey, aggroLandingOffset, flickDuration, delay, switchBack, switchslot, switchDelay);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        eventManager.add(HudListener.class, this);
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        eventManager.remove(HudListener.class, this);
        super.onDisable();
    }

    @Override
    public void onRenderHud(HudEvent event) {
        if (mc.player == null)
            return;

        if (active && aggroActive && !flickDone) {
            long elapsed = System.currentTimeMillis() - flickStartTime;
            double progress = Math.min(1.0, (double) elapsed / flickDurationMs);
            double eased = RotationUtils.minimumJerk(progress);

            float interpYaw = startYaw + (float) (MathHelper.wrapDegrees(targetYaw - startYaw) * eased);
            float interpPitch = startPitch + (float) ((targetPitch - startPitch) * eased);

            Rotation rot = RotationUtils.applyGCD(mc.player.getYaw(), mc.player.getPitch(), interpYaw, interpPitch, gcdRemainder, true);
            mc.player.setYaw((float) rot.yaw());
            mc.player.setPitch((float) rot.pitch());

            if (elapsed >= flickDurationMs) {
                flickDone = true;
            }
        }
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null || mc.player == null)
            return;

        if (KeyUtils.isKeyPressed(activateKey.getKey()) && !active) {
            active = true;
            flickDone = true;
            actionTimer.reset();
        }

        if (KeyUtils.isKeyPressed(aggroPearlKey.getKey()) && !active) {
            PlayerEntity target = WorldUtils.findNearestPlayer(mc.player, 15f, true, true);
            if (target != null) {
                double[] angles = calculatePearlAngles(target);
                if (angles != null) {
                    aggroTarget = target;
                    aggroActive = true;
                    active = true;
                    targetYaw = (float) angles[0];
                    targetPitch = (float) angles[1];
                    startYaw = mc.player.getYaw();
                    startPitch = mc.player.getPitch();
                    flickStartTime = System.currentTimeMillis();
                    flickDurationMs = Math.max(160L, flickDuration.getValueLong() * 50L);
                    flickDone = false;
                    gcdRemainder[0] = 0;
                    gcdRemainder[1] = 0;
                    actionTimer.reset();
                }
            }
        }

        if (active) {
            if (!mc.player.isHolding(Items.ENDER_PEARL)) {
                if (AntibanbyDaduncs.isSlotPending()) {
                    return;
                }
                InventoryUtils.selectItemFromHotbar(Items.ENDER_PEARL);
            }

            if (aggroActive && !flickDone) {
                return;
            }

            long delayMs = delay.getValueLong() * 50L;
            if (ClickGUI.randomisedTiming.getValue()) {
                long variance = (long) ClickGUI.VARIANCE;
                long offset = (long) (Math.random() * variance);
                delayMs += offset;
            }

            if (!actionTimer.hasReached(delayMs)) {
                return;
            }

            if (!hasActivated) {
                if (AntibanbyDaduncs.isActionSent() || AntibanbyDaduncs.isAttackSent() || AntibanbyDaduncs.isSlotPending()) {
                    return;
                }

                if (!mc.player.isHolding(Items.ENDER_PEARL)) {
                    return;
                }

                ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                if (result.isAccepted())
                    mc.player.swingHand(Hand.MAIN_HAND);

                hasActivated = true;
                switchTimer.reset();
            }

            if (switchBack.getValue()) {
                long switchDelayMs = Math.max(50L, switchDelay.getValueLong() * 50L);
                if (ClickGUI.randomisedTiming.getValue()) {
                    long variance = (long) ClickGUI.VARIANCE;
                    long offset = (long) (Math.random() * variance);
                    switchDelayMs += offset;
                }

                if (switchTimer.hasReached(switchDelayMs)) {
                    if (AntibanbyDaduncs.isActionSent() || AntibanbyDaduncs.isSlotPending()) {
                        return;
                    }
                    int slot = Math.max(1, Math.min(9, switchslot.getValueInt())) - 1;
                    InventoryUtils.setInvSlot(slot);
                    reset();
                }
            } else {
                reset();
            }
        }
    }

    private double[] calculatePearlAngles(PlayerEntity target) {
        if (target == null || !target.isAlive() || mc.player == null) return null;

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        Vec3d targetVel = target.getVelocity();

        double effectiveSpeed = 1.5;
        double eyeHeight = 1.52;
        double landingOffset = aggroLandingOffset.getValue();

        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 1.0) return null;

        double dirX = dx / dist;
        double dirZ = dz / dist;

        double targetRadialVel = targetVel.x * dirX + targetVel.z * dirZ;

        double landingDist = Math.max(0.5, dist - landingOffset);
        double landX = dirX * landingDist;
        double landZ = dirZ * landingDist;

        double[] angles = MathUtils.findPearlAngles(landX, landZ, 0, 0, eyeHeight, effectiveSpeed);
        if (angles == null) return null;

        double[] sim = MathUtils.simulatePearlTrajectory(angles[1], angles[0], 0, 0, eyeHeight, effectiveSpeed);
        int flightTicks = (int) sim[2];

        double predictedDist = dist + targetRadialVel * flightTicks;

        if (predictedDist < 1.0) return null;

        landingDist = Math.max(0.5, predictedDist - landingOffset);
        landX = dirX * landingDist;
        landZ = dirZ * landingDist;

        return MathUtils.findPearlAngles(landX, landZ, 0, 0, eyeHeight, effectiveSpeed);
    }

    private void reset() {
        active = false;
        hasActivated = false;
        aggroActive = false;
        aggroTarget = null;
        flickDone = false;
        gcdRemainder[0] = 0;
        gcdRemainder[1] = 0;
        actionTimer.reset();
        switchTimer.reset();
    }
}
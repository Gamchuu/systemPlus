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

package dlindustries.uk.daduncs.module.modules.client;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.event.events.AttackListener;
import dlindustries.uk.daduncs.event.events.ButtonListener;
import dlindustries.uk.daduncs.event.events.HudListener;
import dlindustries.uk.daduncs.managers.FriendManager;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.utils.EncryptedString;
import dlindustries.uk.daduncs.utils.RenderUtils;
import dlindustries.uk.daduncs.utils.TextRenderer;
import dlindustries.uk.daduncs.utils.WorldUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public final class Friends extends Module implements ButtonListener, AttackListener, HudListener {
    private final bind addFriendKey = new bind(EncryptedString.of("Friend Key"), GLFW.GLFW_MOUSE_BUTTON_MIDDLE, false)
            .setDescription(EncryptedString.of("Key to add/remove friends"));
    public final bool antiAttack = new bool(EncryptedString.of("Anti-Attack"), false)
            .setDescription(EncryptedString.of("Doesn't let you hit friends"));
    public final bool disableAimAssist = new bool(EncryptedString.of("Anti-Aim"), false)
            .setDescription(EncryptedString.of("Disables aim assist for friends"));
    public final bool friendStatus = new bool(EncryptedString.of("Friend Status"), false)
            .setDescription(EncryptedString.of("Tells you if you're aiming at a friend or not"));

    public final bool addedPopup = new bool(EncryptedString.of("Added Popup"), true)
            .setDescription(EncryptedString.of("Shows a small text when you friend someone"));

    private FriendManager manager;
    private String popup = "";
    private long popupAt;

    public Friends() {
        super(EncryptedString.of("Friends"), EncryptedString.of("Stops you from hitting or blowing up your friended players"), -1, Category.CLIENT);
        addSettings(addFriendKey, antiAttack, disableAimAssist, friendStatus, addedPopup);
        setKey(-1);
    }

    @Override
    public void onEnable() {
        manager = System.INSTANCE.getFriendManager();

        eventManager.add(ButtonListener.class, this);
        eventManager.add(AttackListener.class, this);
        eventManager.add(HudListener.class, this);

        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(ButtonListener.class, this);
        eventManager.remove(AttackListener.class, this);
        eventManager.remove(HudListener.class, this);

        super.onDisable();
    }

    @Override
    public void onButtonPress(ButtonEvent event) {
        if(mc.player == null)
            return;

        if(mc.currentScreen != null)
            return;

        if(mc.crosshairTarget instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();

            if(entity instanceof PlayerEntity player) {
                if (event.button == addFriendKey.getKey() && event.action == GLFW.GLFW_PRESS) {
                    if(!manager.isFriend(player)) {
                        manager.addFriend(player);
                        popup = "friend added";
                    } else {
                        manager.removeFriend(player);
                        popup = "friend removed";
                    }

                    popupAt = java.lang.System.currentTimeMillis();
                }
            }
        }
    }

    @Override
    public void onAttack(AttackEvent event) {
        if(!antiAttack.getValue())
            return;

        if(manager.isAimingOverFriend())
            event.cancel();
    }

    @Override
    public void onRenderHud(HudEvent event) {
        if(mc.player == null || mc.world == null)
            return;

        DrawContext context = event.context;

        if(addedPopup.getValue() && popupAt != 0) {
            long shown = java.lang.System.currentTimeMillis() - popupAt;

            if(shown > 4000)
                popupAt = 0;
            else {
                float alpha = shown > 2500 ? 1f - (shown - 2500) / 1500f : 1f;
                int x = mc.getWindow().getScaledWidth() / 2 - TextRenderer.smallWidth(popup) / 2;
                int y = mc.getWindow().getScaledHeight() / 2 + 20;

                TextRenderer.drawSmall(popup, context, x, y, dlindustries.uk.daduncs.gui.Theme.fade(dlindustries.uk.daduncs.gui.Theme.accent, alpha));
            }
        }

        if(!friendStatus.getValue())
            return;

        RenderUtils.unscaledProjection(context);
        if(WorldUtils.getHitResult(100) instanceof EntityHitResult hitResult) {
            Entity entity = hitResult.getEntity();

            if(entity instanceof PlayerEntity player) {
                if(manager.isFriend(player)) {
                    TextRenderer.drawCenteredString(EncryptedString.of("Player is friend"), context, (mc.getWindow().getWidth() / 2), (mc.getWindow().getHeight() / 2) + 25, Color.GREEN.getRGB());
                }
            }
        }
        RenderUtils.scaledProjection(context);
    }
}

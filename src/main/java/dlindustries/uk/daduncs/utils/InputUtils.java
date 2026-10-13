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

import dlindustries.uk.daduncs.mixin.KeyBindingAccessor;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import static dlindustries.uk.daduncs.System.mc;

public final class InputUtils {

    public enum Button {
        ATTACK,
        USE
    }

    private static final List<Pending> pending = new ArrayList<>();
    private static final EnumSet<Button> simulated = EnumSet.noneOf(Button.class);

    private InputUtils() {}

    private static final class Pending {
        final KeyBinding binding;
        final Button button;
        int ticks;

        Pending(KeyBinding binding, Button button, int ticks) {
            this.binding = binding;
            this.button = button;
            this.ticks = ticks;
        }
    }
    private static KeyBinding mapping(String name) {
        if (mc.options == null) return null;
        return switch (name.toLowerCase()) {
            case "w", "forward" -> mc.options.forwardKey;
            case "s", "back" -> mc.options.backKey;
            case "a", "left" -> mc.options.leftKey;
            case "d", "right" -> mc.options.rightKey;
            case "space", "jump" -> mc.options.jumpKey;
            case "shift", "sneak" -> mc.options.sneakKey;
            case "sprint" -> mc.options.sprintKey;
            case "leftclick", "attack" -> mc.options.attackKey;
            case "rightclick", "use" -> mc.options.useKey;
            case "f", "offhand" -> mc.options.swapHandsKey;
            default -> null;
        };
    }

    private static InputUtil.Key boundKey(KeyBinding binding) {
        return ((KeyBindingAccessor) binding).getBoundKey();
    }

    private static Button buttonOf(KeyBinding binding) {
        if (mc.options == null || binding == null) return null;
        if (binding == mc.options.attackKey) return Button.ATTACK;
        if (binding == mc.options.useKey) return Button.USE;
        return null;
    }

    public static void setKey(String key, boolean pressed) {
        KeyBinding binding = mapping(key);
        if (binding == null) return;
        KeyBinding.setKeyPressed(boundKey(binding), pressed);
    }

    public static void leftClick() {
        click(mapping("leftclick"), 2);
    }

    public static void rightClick() {
        click(mapping("rightclick"), 2);
    }

    public static void rightClickHold(int millis) {
        int ticks = Math.max(1, Math.round(millis / 50.0F));
        click(mapping("rightclick"), ticks);
    }

    public static void click(String key) {
        click(mapping(key), 2);
    }

    private static void click(KeyBinding binding, int ticks) {
        if (binding == null) return;
        InputUtil.Key key = boundKey(binding);
        KeyBinding.setKeyPressed(key, true);
        KeyBinding.onKeyPressed(key);
        Button button = buttonOf(binding);
        if (button != null) simulated.add(button);
        schedule(binding, button, ticks);
    }

    public static void release(String key) {
        KeyBinding binding = mapping(key);
        if (binding == null) return;
        pending.removeIf(p -> p.binding == binding);
        KeyBinding.setKeyPressed(boundKey(binding), false);
        Button button = buttonOf(binding);
        if (button != null) simulated.remove(button);
    }

    public static void unpressAll() {
        for (String key : List.of("w", "a", "s", "d", "shift", "space", "sprint", "leftclick", "rightclick")) {
            setKey(key, false);
        }
    }

    public static void stopMovement() {
        for (String key : List.of("w", "a", "s", "d", "shift", "space")) {
            setKey(key, false);
        }
    }

    private static void schedule(KeyBinding binding, Button button, int ticks) {
        pending.removeIf(p -> p.binding == binding);
        pending.add(new Pending(binding, button, ticks));
    }

    public static boolean isSimulatedPressed(Button button) {
        return simulated.contains(button);
    }

    public static void consume(Button button) {
        simulated.remove(button);
    }

    public static void tick() {
        if (pending.isEmpty()) return;
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (--p.ticks <= 0) {
                KeyBinding.setKeyPressed(boundKey(p.binding), false);
                if (p.button != null) simulated.remove(p.button);
                it.remove();
            }
        }
    }
}

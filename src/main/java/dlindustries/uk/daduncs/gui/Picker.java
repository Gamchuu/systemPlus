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

package dlindustries.uk.daduncs.gui;

import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.module.setting.colour;
import dlindustries.uk.daduncs.render.Render2D;
import dlindustries.uk.daduncs.utils.Sounds;
import dlindustries.uk.daduncs.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import static dlindustries.uk.daduncs.System.mc;

public final class Picker {

	private static final Identifier CLOSE = Identifier.of("system", "textures/gui/x.png");

	private static final int PAD = 10;
	private static final int HEAD = 22;
	private static final int FIELD = 80;
	private static final int RAIL = 10;
	private static final int GAP = 8;
	private static final int ENTRY = FIELD + GAP + RAIL;
	private static final int BAR = 14;
	private static final int SHUT = 11;
	private static final int WIDTH = PAD + FIELD + GAP + RAIL + PAD;
	private static final int HEIGHT = HEAD + FIELD + 10 + BAR + PAD;

	private static final Anim show = new Anim(0f, 9f);

	private static colour editing;
	private static int left;
	private static int top;
	private static int grabbed = -1;
	private static int grabX;
	private static int grabY;
	private static boolean moving;
	private static boolean leaving;
	private static boolean typing;
	private static String entry = "";
	private static float size = 1f;

	public static void open(colour value, int centreX, int centreY) {
		open(value, centreX, centreY, 1f);
	}

	public static void open(colour value, int centreX, int centreY, float scale) {
		size = scale;
		editing = value;
		grabbed = -1;
		moving = false;
		leaving = false;
		typing = false;

		left = clamp(Math.round(centreX - WIDTH * size / 2), 4, mc.getWindow().getScaledWidth() - Math.round(WIDTH * size) - 4);
		top = clamp(Math.round(centreY - HEIGHT * size / 2), 4, mc.getWindow().getScaledHeight() - Math.round(HEIGHT * size) - 4);

		show.snap(0f);
	}

	public static void close() {
		if (editing == null) return;

		leaving = true;
		typing = false;
		moving = false;
		grabbed = -1;
	}

	public static boolean showing() {
		return editing != null && !leaving;
	}

	public static boolean typing() {
		return showing() && typing;
	}

	public static void render(DrawContext context, float delta, float alpha) {
		if (editing == null) return;

		Matrix3x2fStack matrices = context.getMatrices();
		matrices.pushMatrix();
		matrices.translate(left, top);
		matrices.scale(size, size);
		matrices.translate(-left, -top);
		paint(context, delta, alpha);
		matrices.popMatrix();
	}

	private static void paint(DrawContext context, float delta, float alpha) {
		show.to(leaving ? 0f : 1f);
		show.update(delta);

		if (leaving && show.get() < 0.02f) {
			editing = null;
			leaving = false;
			return;
		}

		float progress = show.get();
		float eased = show.overshoot();

		int wide = (int) (WIDTH * eased);
		int tall = (int) (HEIGHT * eased);

		Render2D.drawBlurRoundRect(context, left + WIDTH / 2 - wide / 2, top + HEIGHT / 2 - tall / 2, wide, tall,
				Theme.CARD_ROUND, 1f, Theme.BLUR * alpha, 0xC0141414, progress * alpha);

		if (progress < 0.7f) return;

		int centre = left + WIDTH / 2;
		String title = "your color";

		TextRenderer.drawSmall(title, context, centre - TextRenderer.smallWidth(title) / 2, top + 10,
				Theme.fade(Theme.muted, alpha));
		Render2D.drawRoundImage(context, left + WIDTH - PAD - SHUT, top + 11 - SHUT / 2f, SHUT, SHUT, 0f,
				CLOSE, Theme.fade(Theme.text, alpha * 0.8f));

		int fieldY = top + HEAD;

		Render2D.drawPicker(context, fieldX(), fieldY, FIELD, FIELD, 7f, editing.getHue(), 0);
		Render2D.drawPicker(context, railX(), fieldY, RAIL, FIELD, RAIL / 2f, 0f, 1);

		ring(context, along(fieldX(), editing.getSaturation(), 10f),
				along(fieldY, 1f - editing.getBrightness(), 10f), 10f, alpha);
		ring(context, railX() + RAIL / 2f, along(fieldY, editing.getHue(), RAIL + 2f), RAIL + 2f, alpha);

		code(context, centre, entryY(), alpha);
	}

	private static void code(DrawContext context, int centre, int y, float alpha) {
		String shown = typing ? "#" + entry : String.format("#%06X", editing.getValue() & 0xFFFFFF);
		int width = TextRenderer.smallWidth(shown);

		Render2D.drawRoundRect(context, centre - ENTRY / 2f, y, ENTRY, BAR, BAR / 2f,
				Theme.fade(typing ? Theme.field : Theme.off, alpha * (typing ? 1f : 0.5f)));

		TextRenderer.drawSmall(shown, context, centre - width / 2, y + 6,
				Theme.fade(typing ? Theme.accent : Theme.text, alpha));

		if (typing && java.lang.System.currentTimeMillis() % 1000L < 500L)
			Render2D.drawRoundRect(context, centre + width / 2f + 2f, y + 3, 1, 8, 0.5f,
					Theme.fade(Theme.accent, alpha));
	}

	private static void ring(DrawContext context, float x, float y, float size, float alpha) {
		Render2D.drawRoundOutline(context, x - size / 2f, y - size / 2f, size, size, size / 2f,
				1.5f, Theme.fade(0xFFFFFFFF, alpha));
	}

	public static boolean click(int rawX, int rawY, int button) {
		if (editing == null) return false;

		int mouseX = localX(rawX);
		int mouseY = localY(rawY);

		if (mouseX < left || mouseX > left + WIDTH || mouseY < top || mouseY > top + HEIGHT) {
			close();
			return true;
		}

		if (button != 0) return true;

		if (mouseX >= left + WIDTH - PAD - SHUT - 3 && mouseY <= top + HEAD - 4) {
			Sounds.play(Sounds.CLICK);
			close();
			return true;
		}

		if (mouseY >= entryY() && mouseY <= entryY() + BAR
				&& mouseX >= left + WIDTH / 2 - ENTRY / 2 && mouseX <= left + WIDTH / 2 + ENTRY / 2) {
			typing = true;
			entry = String.format("%06X", editing.getValue() & 0xFFFFFF);
			Sounds.play(Sounds.CLICK);
			return true;
		}

		typing = false;

		int fieldY = top + HEAD;

		if (mouseY >= fieldY && mouseY <= fieldY + FIELD) {
			if (mouseX >= fieldX() && mouseX <= fieldX() + FIELD)
				grabbed = 0;
			else if (mouseX >= railX() && mouseX <= railX() + RAIL)
				grabbed = 1;

			if (grabbed != -1) {
				drag(mouseX, mouseY);
				return true;
			}
		}

		moving = true;
		grabX = rawX - left;
		grabY = rawY - top;
		return true;
	}

	public static void drag(int rawX, int rawY) {
		if (editing == null) return;

		if (moving) {
			left = clamp(rawX - grabX, 4, mc.getWindow().getScaledWidth() - Math.round(WIDTH * size) - 4);
			top = clamp(rawY - grabY, 4, mc.getWindow().getScaledHeight() - Math.round(HEIGHT * size) - 4);
			return;
		}

		int mouseX = localX(rawX);
		int mouseY = localY(rawY);

		if (grabbed == -1) return;

		int fieldY = top + HEAD;

		if (grabbed == 0) {
			editing.setSaturation(reach((mouseX - fieldX()) / (float) FIELD));
			editing.setBrightness(1f - reach((mouseY - fieldY) / (float) FIELD));
		} else {
			editing.setHue(reach((mouseY - fieldY) / (float) FIELD));
		}

		ClickGUI.syncTheme();
	}

	private static int localX(int rawX) {
		return Math.round(left + (rawX - left) / size);
	}

	private static int localY(int rawY) {
		return Math.round(top + (rawY - top) / size);
	}

	public static void release() {
		grabbed = -1;
		moving = false;
	}

	public static boolean type(String typed) {
		if (!typing()) return false;

		for (char letter : typed.toCharArray())
			if (entry.length() < 6 && Character.digit(letter, 16) >= 0)
				entry += Character.toUpperCase(letter);

		return true;
	}

	public static boolean backspace() {
		if (!typing()) return false;

		if (!entry.isEmpty())
			entry = entry.substring(0, entry.length() - 1);

		return true;
	}

	public static boolean pressed(int key, int modifiers) {
		if (!typing()) return false;

		if (key == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0)
			return type(mc.keyboard.getClipboard().replace("#", "").trim());

		if (key == GLFW.GLFW_KEY_BACKSPACE)
			return backspace();

		if (key == GLFW.GLFW_KEY_ESCAPE) {
			typing = false;
			return true;
		}

		if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER)
			return true;

		typing = false;

		if (entry.length() != 6) return true;

		editing.setValue(Integer.parseInt(entry, 16));
		ClickGUI.syncTheme();
		Sounds.play(Sounds.CLICK);
		return true;
	}

	private static int entryY() {
		return top + HEAD + FIELD + 10;
	}

	private static int fieldX() {
		return left + PAD;
	}

	private static int railX() {
		return left + PAD + FIELD + GAP;
	}

	private static float along(float start, float at, float size) {
		return start + size / 2f + (FIELD - size) * at;
	}

	private static float reach(float value) {
		return Math.max(0f, Math.min(1f, value));
	}

	private static int clamp(int value, int low, int high) {
		return Math.max(low, Math.min(Math.max(low, high), value));
	}

	private Picker() {
	}
}

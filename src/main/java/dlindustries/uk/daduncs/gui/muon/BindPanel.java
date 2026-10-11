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

package dlindustries.uk.daduncs.gui.muon;

import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.utils.Sounds;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class BindPanel implements Catcher {

	private static final float HEIGHT = 17;
	private static final float PADDING = 7;
	private static final float SEPARATOR = 11;
	private static final float RADIUS = 6;
	private static final float GAP = 12;
	private static final int HOVER = 0xFFC6C6CB;
	private static final String TOGGLE = "Toggle";
	private static final String HOLD = "Hold";

	private final MuonGui gui;
	private final bind key;
	private final Spring progress = new Spring(260, 0.58f);
	private final Spring keyProgress = new Spring(260, 0.58f);
	private final Spring keyWidth = new Spring(420, 1);
	private final Wire wire;
	private final Wire keyWire;
	private boolean open;
	private boolean shown;
	private boolean keyVisible;
	private boolean keyShown;
	private int side = 1;
	private Rect pill;
	private Rect chip;

	public BindPanel(MuonGui gui, bind key, long seed) {
		this.gui = gui;
		this.key = key;
		this.wire = new Wire(seed);
		this.keyWire = new Wire(seed * 31 + 7);
		keyWidth.snap(PADDING * 2 + Skin.width(keyText()));
	}

	public void open(boolean open, int side) {
		this.open = open;
		if (open) {
			if (progress.get() <= 0.01f)
				this.side = side;

			keyVisible = key.getKey() != GLFW.GLFW_KEY_UNKNOWN;
		} else if (listening()) {
			gui.listen(null);
		}
	}

	public boolean isOpen() {
		return open;
	}

	public boolean visible() {
		return shown || progress.get() > 0.01f;
	}

	public void render(DrawContext context, Rect module, double mouseX, double mouseY, float delta, boolean allowed, float parentAlpha) {
		boolean target = open && allowed;
		if (target != shown) {
			shown = target;
			Skin.open(progress, target);
		}

		boolean keyTarget = keyVisible && shown;
		if (keyTarget != keyShown) {
			keyShown = keyTarget;
			Skin.open(keyProgress, keyTarget);
		}

		progress.update(delta);
		keyProgress.update(delta);
		keyWidth.to(PADDING * 2 + Skin.width(keyText()));
		keyWidth.update(delta);

		pill = layoutPill(module);
		chip = layoutChip();
		if (!visible()) return;

		float alpha = Skin.alpha(progress.get()) * parentAlpha;
		float originX = side > 0 ? module.x() : module.right();
		Rect animated = pill.grow(originX, module.bottom(), progress.get());

		wire.render(context, module, animated, alpha);

		boolean pushed = Skin.push(context, pill, animated);
		renderChip(context, alpha);

		Skin.surface(context, pill, RADIUS, alpha, 0);
		drawMode(context, false, mouseX, mouseY, alpha);
		drawMode(context, true, mouseX, mouseY, alpha);

		float separatorX = segment(true).x();
		Skin.fill(context, separatorX - 0.25f, pill.centerY() - 4.25f, 0.6f, 8.5f, 0, Skin.white(alpha * 0.3f));

		if (pushed)
			context.getMatrices().popMatrix();
	}

	private void renderChip(DrawContext context, float parentAlpha) {
		if (!keyShown && keyProgress.get() <= 0.01f) return;

		Rect anchor = segment(key.isHold());
		Rect animated = chip.grow(anchor.centerX(), pill.bottom(), keyProgress.get());
		float alpha = parentAlpha * Skin.alpha(keyProgress.get());

		keyWire.render(context, anchor, animated, alpha);

		boolean pushed = Skin.push(context, chip, animated);
		Skin.surface(context, chip, RADIUS, alpha, 0);

		float pulse = listening() ? 0.65f + 0.35f * MathHelper.sin(java.lang.System.currentTimeMillis() / 170f) : 1f;
		String text = keyText();
		Skin.text(context, text, chip.centerX() - Skin.width(text) / 2, chip.centerY(), Theme.fade(Theme.text, alpha * pulse));

		if (pushed)
			context.getMatrices().popMatrix();
	}

	private void drawMode(DrawContext context, boolean hold, double mouseX, double mouseY, float alpha) {
		Rect segment = segment(hold);
		boolean hovered = interactive() && segment.contains(mouseX, mouseY);

		int color = Theme.muted;
		if (key.isHold() == hold) color = Theme.text;
		else if (hovered) color = HOVER;

		float x = hold ? segment.x() + SEPARATOR / 2 : pill.x() + PADDING;
		Skin.text(context, hold ? HOLD : TOGGLE, x, pill.centerY(), Theme.fade(color, alpha));
	}

	public boolean contains(double mouseX, double mouseY) {
		if (!interactive()) return false;
		return pill.contains(mouseX, mouseY) || chipInteractive() && chip.contains(mouseX, mouseY);
	}

	public boolean click(double mouseX, double mouseY, int button) {
		if (!interactive()) return false;

		if (pill.contains(mouseX, mouseY)) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
				choose(segment(true).contains(mouseX, mouseY));

			return true;
		}

		if (chipInteractive() && chip.contains(mouseX, mouseY)) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !listening()) {
				gui.listen(this);
				Sounds.play(Sounds.TYPE);
			}
			return true;
		}

		return false;
	}

	@Override
	public void capture(int keyCode) {
		if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return;

		gui.listen(null);

		if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
			key.setKey(GLFW.GLFW_KEY_UNKNOWN);
			Sounds.play(Sounds.COLLAPSE);
			return;
		}

		key.setKey(keyCode);
		Sounds.play(Sounds.KEYPRESS);
	}

	private void choose(boolean hold) {
		key.setHold(hold);
		keyVisible = true;
		gui.listen(this);
		Sounds.play(Sounds.CLICK, 1f, hold ? 0.95f : 1.05f);
	}

	private boolean listening() {
		return gui.listening() == this;
	}

	private boolean interactive() {
		return shown && progress.get() > 0.8f && pill != null;
	}

	private boolean chipInteractive() {
		return keyShown && keyProgress.get() > 0.8f;
	}

	private String keyText() {
		return listening() ? "press a key" : key.getKey() == GLFW.GLFW_KEY_UNKNOWN ? "Unbound" : key.getKeyName().toLowerCase();
	}

	private Rect layoutPill(Rect module) {
		float width = PADDING + Skin.width(TOGGLE) + SEPARATOR + Skin.width(HOLD) + PADDING;
		float x = side > 0 ? module.x() - GAP - width : module.right() + GAP;
		return new Rect(x, module.bottom() + 7, width, HEIGHT);
	}

	private Rect layoutChip() {
		float width = keyWidth.get();
		Rect anchor = segment(key.isHold());
		return new Rect(anchor.centerX() - width / 2, pill.bottom() + GAP, width, HEIGHT);
	}

	private Rect segment(boolean hold) {
		float split = pill.x() + PADDING + Skin.width(TOGGLE) + SEPARATOR / 2;
		if (!hold)
			return new Rect(pill.x(), pill.y(), split - pill.x(), HEIGHT);

		return new Rect(split, pill.y(), pill.right() - split, HEIGHT);
	}
}

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

package dlindustries.uk.daduncs.gui.wide;

import dlindustries.uk.daduncs.gui.Anim;
import dlindustries.uk.daduncs.gui.ConfigList;
import dlindustries.uk.daduncs.gui.Picker;
import dlindustries.uk.daduncs.gui.Row;
import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.setting.Setting;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.render.Render2D;
import dlindustries.uk.daduncs.utils.Sounds;
import dlindustries.uk.daduncs.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static dlindustries.uk.daduncs.System.mc;

import java.util.ArrayList;
import java.util.List;

/**
 * Module card for the wide System-style gui.
 *
 * Differences from {@link dlindustries.uk.daduncs.gui.Card}:
 * <ul>
 *   <li>taller header showing the module name plus its small description at all times,</li>
 *   <li>a tall category icon left of the name spanning both header lines,</li>
 *   <li>smaller corner radius,</li>
 *   <li>no "i" info hover button,</li>
 *   <li>settings keep their small non-bold text rendering via {@link Row}.</li>
 * </ul>
 */
public final class WideCard {

	public static final int HEAD = 46;

	private static final int PAD = 12;
	private static final int ICON = 26;
	private static final int ICON_GAP = 6;
	private static final int SLIDE = 18;
	private static final int ROUND = 10;
	private static final int CONTENT = 6;
	private static final int BOTTOM = 10;

	private static final Identifier BIN = Identifier.of("system", "textures/gui/delete.png");

	private static final int CLOSED = 0;
	private static final int CHOOSING = 1;
	private static final int LISTENING = 2;

	private final Module module;
	private final Identifier icon;
	private final List<Row> rows = new ArrayList<>();
	private final bind key;
	private final ConfigList configList;

	private final Anim open = new Anim(0f, 8f);
	private final Anim on = new Anim(0f, 7f);
	private final Anim hover = new Anim(0f, 8f);
	private final Anim menu = new Anim(0f, 8f);

	private boolean expanded;
	private boolean wasHovered;
	private float alpha = 1f;
	private int binding = CLOSED;
	private CharSequence hoveredSettingDescription;

	public WideCard(Module module, Identifier icon) {
		this.module = module;
		this.icon = icon;

		bind found = null;
		for (Setting<?> setting : module.getSettings()) {
			if (setting instanceof bind value && value.isModuleKey())
				found = value;
			else {
				Row row = new Row(setting);
				row.setLite(true);
				rows.add(row);
			}
		}

		this.key = found;
		this.configList = module instanceof dlindustries.uk.daduncs.module.modules.client.Configs ? new ConfigList() : null;
		on.snap(module.isEnabled() ? 1f : 0f);
	}

	public Module module() {
		return module;
	}

	public List<Row> rows() {
		return rows;
	}

	public CharSequence hoveredSettingDescription() {
		return hoveredSettingDescription;
	}

	public boolean binding() {
		if (binding != CLOSED)
			return true;

		for (Row row : rows)
			if (row.listening())
				return true;

		return false;
	}

	private int stack() {
		int total = 0;

		for (Row row : rows)
			total += row.height();

		return total;
	}

	private boolean hasDesc() {
		CharSequence description = module.getDescription();
		return description != null && !description.toString().isEmpty();
	}

	public int height() {
		return (int) (HEAD + open.get() * (CONTENT + stack() + friends() * 22 + configs() + BOTTOM));
	}

	private int friends() {
		if (!(module instanceof dlindustries.uk.daduncs.module.modules.client.Friends))
			return 0;

		return dlindustries.uk.daduncs.System.INSTANCE.getFriendManager().getShownFriends().size();
	}

	private int configs() {
		return configList != null ? configList.height() : 0;
	}

	public void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float delta, float alpha) {
		this.alpha = alpha;
		hoveredSettingDescription = null;

		if (expanded && rows.isEmpty() && configs() == 0)
			expanded = false;

		open.to(expanded ? 1f : 0f);
		on.to(module.isEnabled() ? 1f : 0f);

		boolean over = hovered(x, y, width, mouseX, mouseY);
		if (over && !wasHovered)
			Sounds.hover();

		wasHovered = over;
		hover.to(over ? 1f : 0f);
		menu.to(binding != CLOSED ? 1f : 0f);

		open.update(delta);
		on.update(delta);
		hover.update(delta);
		menu.update(delta);

		int background = Theme.lerp(Theme.card, Theme.cardHover, hover.get());
		Render2D.drawBlurRoundRect(context, x, y, width, height(), ROUND, 1f, Theme.BLUR * alpha, background, alpha);

		float slide = SLIDE * on.get();
		int colour = Theme.fade(Theme.lerp(Theme.text, Theme.accent, on.get()), alpha);
		int iconColour = Theme.fade(Theme.lerp(Theme.muted, Theme.accent, on.get()), alpha);

		Render2D.drawRoundImage(context, x + PAD, y + (HEAD - ICON) / 2f, ICON, ICON, 0f, icon, iconColour);

		boolean described = hasDesc();
		int textX = (int) (x + PAD + ICON + ICON_GAP - 1 + slide);
		int nameY = described ? y + 15 : y + HEAD / 2 - 1;
		TextRenderer.drawLabelMenu(module.getName(), context, textX, nameY, colour);

		if (described) {
			int reserve = PAD + 10 + (key != null && key.getKey() != GLFW.GLFW_KEY_UNKNOWN ? 90 : 0);
			String shown = fit(module.getDescription().toString(), width - (textX - x) - reserve);
			TextRenderer.drawTiny(shown, context, textX + 1, y + 31, Theme.fade(Theme.muted, alpha));
		}

		if (module.getTag() != null) {
			int badge = TextRenderer.tinyWidth(module.getTag()) + 10;
			int badgeX = textX + 4 + TextRenderer.menuWidth(module.getName());

			Render2D.drawRoundRect(context, badgeX, nameY - 6, badge, 13, 6.5f, Theme.fade(Theme.accent, alpha * 0.22f));
			TextRenderer.drawTiny(module.getTag(), context, badgeX + 5, nameY, Theme.fade(Theme.accent, alpha));
		}

		int right = x + width;
		if (key != null && key.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
			String label = key.getKeyName();
			int badge = TextRenderer.smallWidth(label) + 14;

			Render2D.drawRoundRect(context, right - 12 - badge, y + HEAD / 2f - 8, badge, 16, 5, Theme.fade(Theme.field, alpha));
			TextRenderer.drawSmall(label, context, right - 5 - badge, y + HEAD / 2 - 1, Theme.fade(Theme.accent, alpha));
		}

		if (open.get() > 0.01f) {
			int bottom = y + height();

			if (bottom > y + HEAD) {
				context.enableScissor(x, y + HEAD, x + width, bottom);

				int rowY = y + HEAD + CONTENT;

				for (Row row : rows) {
					row.render(context, x + PAD, rowY, right - 10, mouseX, mouseY, delta, alpha);
					boolean inScissor = mouseY >= y + HEAD && mouseY <= bottom;
					if (inScissor && row.isHovered() && row.getSetting().shouldShowDescription() && row.getSetting().getDescription() != null) {
						hoveredSettingDescription = row.getSetting().getDescription();
					}
					rowY += row.height();
				}

				if (friends() > 0)
					renderFriends(context, x, rowY, width, mouseX, mouseY);

				if (configs() > 0)
					configList.render(context, x, rowY, width, mouseX, mouseY, alpha);

				context.disableScissor();
			}
		}

		if (menu.get() > 0.01f)
			renderMenu(context, x, y, width);
	}

	private static String fit(String text, int maxWidth) {
		String shown = text;
		while (!shown.isEmpty() && TextRenderer.tinyWidth(shown + "...") > maxWidth)
			shown = shown.substring(0, shown.length() - 1);

		return shown.length() < text.length() ? shown + "..." : shown;
	}

	private void renderFriends(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		for (String name : dlindustries.uk.daduncs.System.INSTANCE.getFriendManager().getShownFriends()) {
			int right = x + width - PAD;

			Render2D.drawRoundRect(context, x + PAD, y, width - PAD * 2, 20, 6f, Theme.fade(Theme.field, alpha * 0.8f));

			Identifier skin = skin(name);
			if (skin != null)
				Render2D.drawHead(context, x + PAD + 4, y + 3, 14, 4f, skin, Theme.fade(0xFFFFFFFF, alpha));

			TextRenderer.drawSmall(name, context, x + PAD + 24, y + 9, Theme.fade(Theme.text, alpha));

			boolean over = overBin(right, y, mouseX, mouseY);
			float size = over ? 15f : 13f;
			int colour = over ? Theme.fade(0xFFFF7A7A, alpha * 0.85f) : Theme.fade(Theme.muted, alpha);

			Render2D.drawRoundImage(context, right - 6 - size, y + 10 - size / 2f, size, size, 0f,
					BIN, colour);

			y += 22;
		}
	}

	private static boolean overBin(int right, int y, int mouseX, int mouseY) {
		return mouseX >= right - 23 && mouseX <= right - 4 && mouseY >= y + 1 && mouseY <= y + 19;
	}

	private static Identifier skin(String name) {
		if (mc.getNetworkHandler() == null)
			return null;

		for (net.minecraft.client.network.PlayerListEntry entry : mc.getNetworkHandler().getPlayerList())
			if (entry.getProfile().name().equals(name))
				return entry.getSkinTextures().body().texturePath();

		return null;
	}

	private void renderMenu(DrawContext context, int x, int y, int width) {
		float progress = menu.get();
		float eased = menu.overshoot();
		int panelWidth = (int) (170 * eased);
		int panelHeight = (int) (52 * eased);
		int panelX = x + width / 2 - panelWidth / 2;
		int panelY = y + HEAD / 2 - panelHeight / 2;

		Render2D.drawBlurRoundRect(context, panelX, panelY, panelWidth, panelHeight, ROUND, 1f, Theme.BLUR * alpha, 0xF0141414, progress * alpha);

		if (progress < 0.7f)
			return;

		int centre = x + width / 2;

		if (binding == LISTENING) {
			String ask = "press any key";
			TextRenderer.drawSmall(ask, context, centre - TextRenderer.smallWidth(ask) / 2, panelY + 29, Theme.fade(Theme.accent, alpha));
			return;
		}

		String title = "activation";
		TextRenderer.drawSmall(title, context, centre - TextRenderer.smallWidth(title) / 2, panelY + 13, Theme.fade(Theme.muted, alpha));

		Render2D.drawRoundRect(context, centre - 70, panelY + 26, 66, 18, 6, Theme.fade(Theme.field, alpha));
		Render2D.drawRoundRect(context, centre + 4, panelY + 26, 66, 18, 6, Theme.fade(Theme.field, alpha));

		int toggleColour = Theme.fade(key != null && !key.isHold() ? Theme.accent : Theme.muted, alpha);
		int holdColour = Theme.fade(key != null && key.isHold() ? Theme.accent : Theme.muted, alpha);

		TextRenderer.drawSmall("toggle", context, centre - 37 - TextRenderer.smallWidth("toggle") / 2, panelY + 33, toggleColour);
		TextRenderer.drawSmall("hold", context, centre + 37 - TextRenderer.smallWidth("hold") / 2, panelY + 33, holdColour);
	}

	public boolean hovered(int x, int y, int width, int mouseX, int mouseY) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEAD;
	}

	public boolean click(int x, int y, int width, int mouseX, int mouseY, int button) {
		if (binding == CHOOSING) {
			int centre = x + width / 2;
			int panelY = y + HEAD / 2 - 26;

			if (mouseY >= panelY + 26 && mouseY <= panelY + 44 && key != null) {
				if (mouseX >= centre - 70 && mouseX <= centre - 4) {
					key.setHold(false);
					binding = LISTENING;
					Sounds.play(Sounds.CLICK);
					return true;
				}

				if (mouseX >= centre + 4 && mouseX <= centre + 70) {
					key.setHold(true);
					binding = LISTENING;
					Sounds.play(Sounds.CLICK);
					return true;
				}
			}

			binding = CLOSED;
			return true;
		}

		if (binding == LISTENING)
			return true;

		if (expanded && friends() > 0 && button == 0) {
			int rowY = y + HEAD + CONTENT + stack();

			for (String name : dlindustries.uk.daduncs.System.INSTANCE.getFriendManager().getShownFriends()) {
				if (overBin(x + width - PAD, rowY, mouseX, mouseY)) {
					dlindustries.uk.daduncs.System.INSTANCE.getFriendManager().removeFriend(name);
					Sounds.play(Sounds.CLICK);
					return true;
				}

				rowY += 22;
			}
		}

		if (expanded && configs() > 0 && button == 0 && configList.click(mouseX, mouseY))
			return true;

		if (hovered(x, y, width, mouseX, mouseY)) {
			if (button == 0) {
				module.toggle();
				Sounds.play(Sounds.TOGGLE);
			} else if (button == 1 && (!rows.isEmpty() || configs() > 0)) {
				expanded = !expanded;
				Sounds.play(Sounds.COLLAPSE);
			} else if (button == 2 && key != null) {
				binding = CHOOSING;
				Sounds.play(Sounds.CLICK);
			}

			return true;
		}

		if (open.get() <= 0.01f)
			return false;

		int rowY = y + HEAD + CONTENT;
		for (Row row : rows) {
			if (row.click(x + PAD, rowY, x + width - 10, mouseX, mouseY, button))
				return true;

			rowY += row.height();
		}

		return false;
	}

	public boolean pressed(int keyCode) {
		for (Row row : rows)
			if (row.listening())
				return row.pressed(keyCode);

		if (binding == CHOOSING && keyCode == GLFW.GLFW_KEY_ESCAPE) {
			binding = CLOSED;
			Sounds.play(Sounds.COLLAPSE);
			return true;
		}

		if (binding != LISTENING || key == null)
			return false;

		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			key.setKey(GLFW.GLFW_KEY_UNKNOWN);
			module.setKey(GLFW.GLFW_KEY_UNKNOWN);
		} else if (!key.shouldIgnoreKey(keyCode)) {
			key.setKey(keyCode);
			module.setKey(keyCode);
		}

		binding = CLOSED;
		return true;
	}

	public boolean pressed(int keyCode, int modifiers) {
		if (configList == null || !expanded && !configList.renaming())
			return false;

		return configList.pressed(keyCode, modifiers);
	}

	public boolean save() {
		if (configList == null)
			return false;

		configList.save();
		return true;
	}

	public boolean type(String typed) {
		return configList != null && configList.type(typed);
	}

	public boolean mousePressed(int button) {
		for (Row row : rows)
			if (row.listening())
				return row.mousePressed(button);

		if (binding != LISTENING || key == null)
			return false;

		key.setKey(button);
		module.setKey(button);
		binding = CLOSED;
		return true;
	}

	public void drag(int x, int width, int mouseX, int mouseY) {
		if (open.get() <= 0.01f)
			return;

		for (Row row : rows)
			row.drag(x + width - 10, mouseX, mouseY);
	}

	public void release() {
		for (Row row : rows)
			row.release();
	}
}

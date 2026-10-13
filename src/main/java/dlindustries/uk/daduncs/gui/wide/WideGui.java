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

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.gui.Anim;
import dlindustries.uk.daduncs.gui.Frame;
import dlindustries.uk.daduncs.gui.Picker;
import dlindustries.uk.daduncs.gui.Row;
import dlindustries.uk.daduncs.gui.Sprite;
import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.render.Render2D;
import dlindustries.uk.daduncs.utils.EncryptedString;
import dlindustries.uk.daduncs.utils.Sounds;
import dlindustries.uk.daduncs.utils.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static dlindustries.uk.daduncs.System.mc;

/**
 * Wide System-style gui.
 *
 * <p>One large blurred background panel (bigger than the old System gui) split
 * into a ~1/4 wide category list on the left and a module list on the right.
 * Both panes sit on transparent background rectangles. The left pane keeps the
 * System header (logo, client name) and the user footer (avatar, name), with a
 * {@code Search} pseudo-category selected by default. The search bar only
 * shows while the Search category is open and is focused automatically.</p>
 */
public final class WideGui extends Screen {

	private static final float SCALE = 0.78f;

	private static final int MAIN_W = 680;
	private static final int MAIN_H = 470;
	private static final int PAD = 12;
	private static final int GAP = 8;
	private static final int LEFT_W = 168;
	private static final int SEARCH_H = 32;
	private static final int ROW_H = 32;
	private static final int ROW_GAP = 2;
	private static final int ICON = 16;
	private static final int CARD_GAP = 6;
	private static final int LIST_PAD = 10;
	private static final int HEADER_H = 50;
	private static final int FOOTER_H = 44;
	private static final int MIN_LIST_H = 64;

	private static final Identifier SEARCH = Identifier.of("system", "textures/gui/search.png");
	private static final Identifier LOGO = Identifier.of("system", "textures/gui/client_icon.png");

	private final Map<Category, Sprite> icons = new EnumMap<>(Category.class);
	private final Map<Category, Identifier> stills = new EnumMap<>(Category.class);
	private final Map<Category, List<WideCard>> cards = new EnumMap<>(Category.class);
	private final Map<Category, Anim> rowHover = new EnumMap<>(Category.class);
	private final Anim searchHover = new Anim(0f, 16f);

	private final Anim fade = new Anim(0f, 9f);
	private final Anim tip = new Anim(0f, 9f);

	private CharSequence tipText;

	/** Null means the Search pseudo-category (default). */
	private Category selected;
	private String query = "";
	private boolean searchFocused = true;
	private float scroll;
	private float progress;
	private float closing;
	private float haze;
	private boolean shutting;

	public WideGui() {
		super(Text.empty());

		for (Category category : Category.values()) {
			icons.put(category, new Sprite(iconName(category)));
			stills.put(category, Identifier.of("system", "textures/gui/" + iconName(category) + ".png"));
			rowHover.put(category, new Anim(0f, 16f));

			List<WideCard> list = new ArrayList<>();
			for (Module module : System.INSTANCE.getModuleManager().getModulesInCategory(category))
				list.add(new WideCard(module, stills.get(category)));

			cards.put(category, list);
		}
	}

	private static String iconName(Category category) {
		return switch (category) {
			case CLIENT -> "client";
			case CRYSTAL -> "crystal";
			case MACE -> "mace";
			case MISC -> "optimizer";
			case CART -> "cart";
			case RENDER -> "render";
			case SPEAR -> "spear";
			case SWORD -> "sword";
		};
	}

	private static String label(Category category) {
		String raw = category.name();
		return raw.charAt(0) + raw.substring(1).toLowerCase();
	}

	private int mainX() {
		return (width - MAIN_W) / 2;
	}

	private int mainY() {
		return (height - MAIN_H) / 2;
	}

	private int searchX() {
		return rightX() + 8;
	}

	private int searchY() {
		return rightY() + 8;
	}

	private int searchW() {
		return rightW() - 16;
	}

	private int searchBottom() {
		return searchY() + SEARCH_H + 6;
	}

	private int leftX() {
		return mainX() + PAD;
	}

	private boolean showSearch() {
		return selected == null;
	}

	private int leftY() {
		return mainY() + PAD;
	}

	private int leftH() {
		return MAIN_H - PAD * 2;
	}

	private int rowsTop() {
		return leftY() + HEADER_H;
	}

	private int rightX() {
		return leftX() + LEFT_W + GAP;
	}

	private int rightY() {
		return leftY();
	}

	private int rightW() {
		return MAIN_W - PAD * 2 - LEFT_W - GAP;
	}

	private int rightH() {
		return leftH();
	}

	private int listX() {
		return rightX() + LIST_PAD;
	}

	private int listW() {
		return rightW() - LIST_PAD * 2;
	}

	private int listTop() {
		return bgTop() + LIST_PAD;
	}

	private int listBottom() {
		return bgTop() + bgHeight() - LIST_PAD;
	}

	/** Single list background, cut to content length via the scissor. */
	private int bgTop() {
		return showSearch() ? searchBottom() : rightY();
	}

	private int bgHeight() {
		int max = rightY() + rightH() - bgTop();
		return Math.min(Math.max(stacked(visible()) + LIST_PAD * 2, MIN_LIST_H), max);
	}

	private List<WideCard> visible() {
		List<WideCard> list = new ArrayList<>();
		String needle = query.toLowerCase();

		if (selected == null) {
			for (List<WideCard> group : cards.values())
				for (WideCard card : group)
					if (needle.isEmpty() || card.module().getName().toString().toLowerCase().contains(needle))
						list.add(card);

			return list;
		}

		for (WideCard card : cards.get(selected))
			if (needle.isEmpty() || card.module().getName().toString().toLowerCase().contains(needle))
				list.add(card);

		return list;
	}

	private int[] spots(List<WideCard> list, int startY) {
		int[] spots = new int[list.size()];
		int y = startY;
		for (int i = 0; i < list.size(); i++) {
			spots[i] = y;
			y += list.get(i).height() + CARD_GAP;
		}

		return spots;
	}

	private int stacked(List<WideCard> list) {
		int total = 0;
		for (WideCard card : list)
			total += card.height() + CARD_GAP;

		return Math.max(0, total - CARD_GAP);
	}

	@Override
	protected void init() {
		progress = 0f;
		shutting = false;
		closing = 0f;

		if (selected == null)
			searchFocused = true;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
	}

	@Override
	public void renderInGameBackground(DrawContext context) {
	}

	@Override
	protected void applyBlur(DrawContext context) {
	}

	@Override
	protected void renderDarkening(DrawContext context) {
	}

	private static float easeOut(float t) {
		float p = 1f - Math.max(0f, Math.min(1f, t));
		return 1f - p * p * p;
	}

	private float mapX(double value) {
		return (float) ((value - width / 2f) / SCALE + width / 2f);
	}

	private float mapY(double value) {
		return (float) ((value - height / 2f) / SCALE + height / 2f);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		Matrix3x2fStack matrices = context.getMatrices();

		matrices.pushMatrix();
		matrices.translate(width / 2f, height / 2f);
		matrices.scale(SCALE, SCALE);
		matrices.translate(-width / 2f, -height / 2f);

		draw(context, (int) mapX(mouseX), (int) mapY(mouseY), delta);

		matrices.popMatrix();
	}

	private void draw(DrawContext context, int mouseX, int mouseY, float delta) {
		if (shutting) {
			closing = Math.min(1f, closing + Frame.delta() * 4.2f);

			if (closing >= 1f) {
				shutting = false;
				closing = 0f;
				finish();
				return;
			}
		} else {
			progress = Math.min(1f, progress + Frame.delta() * 1.5f);
		}

		fade.to(1f);
		fade.update(delta);

		float shut = 1f - closing;
		float alpha = fade.get() * shut;
		float reveal = shutting ? 1f - easeOut(closing * 1.6f) : easeOut((progress - 0.15f) / 0.55f);
		haze = shutting ? 1f - closing * closing : easeOut(progress);

		if (alpha <= 0.01f)
			return;

		int mx = mainX();
		int my = mainY();

		Render2D.drawGlassRoundRect(context, mx, my, MAIN_W, MAIN_H, Theme.PANEL_ROUND, Theme.SHEEN, Theme.BLUR * haze, Theme.surface, alpha);

		float content = alpha * reveal;

		if (showSearch())
			renderSearch(context, content);

		int pane = Theme.fade(0xFF000000, 0.30f * alpha);
		Render2D.drawRoundRect(context, leftX(), leftY(), LEFT_W, leftH(), 10f, pane);
		Render2D.drawRoundRect(context, rightX(), bgTop(), rightW(), bgHeight(), 10f, pane);

		renderRows(context, mouseX, mouseY, delta, content);
		renderList(context, mouseX, mouseY, delta, reveal, alpha);
		renderTip(context, delta, content);

		Picker.render(context, delta, alpha);
	}

	private void renderSearch(DrawContext context, float alpha) {
		int x = searchX();
		int y = searchY();

		Render2D.drawRoundRect(context, x, y, searchW(), SEARCH_H, 8f, Theme.fade(0xFF000000, 0.30f * alpha));
		Render2D.drawRoundImage(context, x + 5, y + 4, 24, 24, 0f, SEARCH, Theme.fade(Theme.muted, alpha));

		if (query.isEmpty() && !searchFocused) {
			CharSequence hint = EncryptedString.of("Search modules...");
			TextRenderer.drawLight(hint, context, x + 34, y + 13, Theme.fade(Theme.muted, alpha));
		} else {
			TextRenderer.drawLight(query, context, x + 34, y + 13, Theme.fade(Theme.text, alpha));

			if (searchFocused && Row.blink())
				Render2D.drawRoundRect(context, x + 36 + TextRenderer.lightWidth(query), y + 9, 1, 14, 0.5f,
						Theme.fade(Theme.accent, alpha));
		}

		Render2D.drawRoundRect(context, x, searchBottom(), searchW(), 1, 0.5f, Theme.fade(Theme.divider, alpha));
	}

	private void renderRows(DrawContext context, int mouseX, int mouseY, float delta, float alpha) {
		int lx = leftX();
		int ly = leftY();

		Render2D.drawRoundImage(context, lx + 8, ly + 8, 28, 28, 0f, LOGO, Theme.fade(Theme.accent, alpha));
		TextRenderer.DrawClickGuiWatermark(context, lx + 42, ly + 12, alpha);
		Render2D.drawRoundRect(context, lx + 8, ly + HEADER_H - 6, LEFT_W - 16, 1, 0.5f, Theme.fade(Theme.divider, alpha));

		int y = rowsTop();
		int x = lx + 6;
		int w = LEFT_W - 12;

		boolean searchOver = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + ROW_H;
		searchHover.to(searchOver || selected == null ? 1f : 0f);
		searchHover.update(delta);

		y = renderRow(context, x, y, w, null, "Search", SEARCH, selected == null,
				mouseX, mouseY, searchHover.get(), delta, alpha);

		for (Category category : Category.values()) {
			Anim anim = rowHover.get(category);
			boolean over = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + ROW_H;
			anim.to(over || category == selected ? 1f : 0f);
			anim.update(delta);

			y = renderRow(context, x, y, w, category, label(category), icons.get(category).get(), category == selected,
					mouseX, mouseY, anim.get(), delta, alpha);
		}

		int foot = ly + leftH() - FOOTER_H;
		Render2D.drawRoundRect(context, lx + 8, foot, LEFT_W - 16, 1, 0.5f, Theme.fade(Theme.divider, alpha));

		Identifier face = System.INSTANCE.getAccentManager().getAvatar();
		if (face != null)
			Render2D.drawRoundImage(context, lx + 8, foot + 7, 30, 30, 15f, face, Theme.fade(0xFFFFFFFF, alpha));
		else Render2D.drawRoundImage(context, lx + 8, foot + 7, 30, 30, 15f, LOGO, Theme.fade(Theme.accent, alpha));

		String who = System.INSTANCE.getAccentManager().getName();
		if (!who.isEmpty())
			TextRenderer.drawMenu("@" + who, context, lx + 44, foot + 26, Theme.fade(Theme.text, alpha));
	}

	private int renderRow(DrawContext context, int x, int y, int w, Category category, String name, Identifier icon,
			boolean active, int mouseX, int mouseY, float glow, float delta, float alpha) {
		boolean over = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + ROW_H;

		if (active)
			Render2D.drawRoundRect(context, x, y, w, ROW_H, 8f, Theme.fade(Theme.accent, 0.18f * alpha));
		else if (over)
			Render2D.drawRoundRect(context, x, y, w, ROW_H, 8f, Theme.fade(0xFFFFFFFF, 0.05f * alpha));

		int colour = Theme.lerp(Theme.muted, Theme.accent, Math.max(0f, Math.min(1f, glow)));
		Render2D.drawRoundImage(context, x + 6, y + (ROW_H - ICON) / 2f, ICON, ICON, 0f, icon, Theme.fade(colour, alpha));
		TextRenderer.drawLabelSmall(name, context, x + 6 + ICON + 6, y + ROW_H / 2 - 1, Theme.fade(colour, alpha));

		return y + ROW_H + ROW_GAP;
	}

	private void renderList(DrawContext context, int mouseX, int mouseY, float delta, float reveal, float alpha) {
		int top = listTop();
		int bottom = listBottom();

		if (bottom <= top)
			return;

		context.enableScissor(listX(), top, listX() + listW(), bottom);

		List<WideCard> list = visible();

		if (list.isEmpty()) {
			String empty = "no modules found";
			TextRenderer.drawSmall(empty, context, listX() + listW() / 2 - TextRenderer.smallWidth(empty) / 2,
					bgTop() + bgHeight() / 2, Theme.fade(Theme.muted, alpha * reveal));
			context.disableScissor();
			return;
		}

		int[] spots = spots(list, (int) (top - scroll));
		for (int index = 0; index < list.size(); index++) {
			WideCard card = list.get(index);
			int spotY = spots[index];
			float step = Math.max(0f, Math.min(1f, reveal * 1.48f - Math.min(index, 8) * 0.06f));
			int lift = Math.round((1f - easeOut(step)) * 14f);

			if (step > 0.01f && spotY + card.height() >= top - 60 && spotY <= bottom + 60)
				card.render(context, listX(), spotY + lift, listW(), mouseX, mouseY, delta, alpha * step);
		}

		context.disableScissor();
	}

	private void renderTip(DrawContext context, float delta, float alpha) {
		CharSequence found = null;
		for (WideCard card : visible()) {
			if (card.hoveredSettingDescription() != null) {
				found = card.hoveredSettingDescription();
				break;
			}
		}

		if (found != null)
			tipText = found;

		tip.to(found != null ? 1f : 0f);
		tip.update(delta);

		if (tip.get() <= 0.01f || tipText == null)
			return;

		int label = TextRenderer.tipWidth(tipText);
		int textX = Math.max(10, mainX() + MAIN_W / 2 - label / 2);
		TextRenderer.drawTip(tipText, context, textX, mainY() - 30, Theme.fade(Theme.text, alpha * tip.get()));
	}

	private boolean onSearch(int mouseX, int mouseY) {
		return showSearch() && mouseX >= searchX() && mouseX <= searchX() + searchW() && mouseY >= searchY() && mouseY <= searchY() + SEARCH_H;
	}

	private Category rowAt(int mouseX, int mouseY) {
		int y = rowsTop();
		int x = leftX() + 6;
		int w = LEFT_W - 12;

		if (mouseX < x || mouseX > x + w)
			return selected;

		if (mouseY >= y && mouseY <= y + ROW_H)
			return null;

		y += ROW_H + ROW_GAP;
		for (Category category : Category.values()) {
			if (mouseY >= y && mouseY <= y + ROW_H)
				return category;

			y += ROW_H + ROW_GAP;
		}

		return selected;
	}

	private boolean inRows(int mouseX, int mouseY) {
		int x = leftX() + 6;
		int w = LEFT_W - 12;
		int top = rowsTop();
		int bottom = top + (Category.values().length + 1) * (ROW_H + ROW_GAP);

		return mouseX >= x && mouseX <= x + w && mouseY >= top && mouseY <= bottom;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		int mouseX = (int) mapX(click.x());
		int mouseY = (int) mapY(click.y());
		int button = click.button();

		if (Picker.click(mouseX, mouseY, button))
			return true;

		for (WideCard card : visible())
			if (card.binding() && card.mousePressed(button))
				return true;

		if (button == 0 && onSearch(mouseX, mouseY)) {
			searchFocused = true;
			Sounds.play(Sounds.CLICK);
			return true;
		}

		if (button == 0 && inRows(mouseX, mouseY)) {
			Category next = rowAt(mouseX, mouseY);

			if (next != selected) {
				Sounds.play(Sounds.CLICK);

				if (next != null)
					icons.get(next).play();
			}

			selected = next;
			scroll = 0;
			searchFocused = next == null;
			return true;
		}

		searchFocused = false;

		List<WideCard> list = visible();
		int[] spots = spots(list, (int) (listTop() - scroll));
		for (int i = 0; i < list.size(); i++)
			if (list.get(i).click(listX(), spots[i], listW(), mouseX, mouseY, button))
				return true;

		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double deltaX, double deltaY) {
		int mouseX = (int) mapX(click.x());
		int mouseY = (int) mapY(click.y());

		Picker.drag(mouseX, mouseY);

		for (WideCard card : visible())
			card.drag(listX(), listW(), mouseX, mouseY);

		return super.mouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		Picker.release();

		for (WideCard card : visible())
			card.release();

		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		float limit = Math.max(0, stacked(visible()) - (listBottom() - listTop()));
		float before = scroll;
		scroll = Math.max(0, Math.min(limit, scroll - (float) verticalAmount * 20f));

		if (scroll != before)
			Sounds.play(Sounds.SCROLL, 0.4f);

		return true;
	}

	@Override
	public boolean keyPressed(KeyInput keyInput) {
		int key = keyInput.key();

		if (Picker.pressed(key, keyInput.modifiers()))
			return true;

		for (WideCard card : visible())
			if (card.binding())
				return card.pressed(key);

		for (WideCard card : visible())
			if (card.pressed(key, keyInput.modifiers()))
				return true;

		if (key == GLFW.GLFW_KEY_S && (keyInput.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
			for (List<WideCard> list : cards.values())
				for (WideCard card : list)
					if (card.save())
						return true;

			return true;
		}

		if (key == GLFW.GLFW_KEY_F && (keyInput.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
			selected = null;
			scroll = 0;
			searchFocused = true;
			Sounds.play(Sounds.COLLAPSE);
			return true;
		}

		if (searchFocused && key == GLFW.GLFW_KEY_ESCAPE) {
			searchFocused = false;
			return true;
		}

		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (searchFocused) {
				if (!query.isEmpty())
					query = query.substring(0, query.length() - 1);

				return true;
			}

			for (WideCard card : visible())
				for (Row row : card.rows())
					if (row.focused())
						return row.backspace();
		}

		return super.keyPressed(keyInput);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (!input.isValidChar())
			return false;

		String typed = input.asString();

		if (Picker.type(typed))
			return true;

		for (WideCard card : visible())
			if (card.type(typed))
				return true;

		if (searchFocused) {
			query += typed;
			scroll = 0;
			Sounds.play(Sounds.KEYPRESS, 0.6f);
			return true;
		}

		for (WideCard card : visible())
			for (Row row : card.rows())
				if (row.focused())
					return row.type(typed);

		return super.charTyped(input);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		System.INSTANCE.getModuleManager().getModule(ClickGUI.class).setEnabledStatus(false);
		onGuiClose();
	}

	public void onGuiClose() {
		if (!shutting) {
			shutting = true;
			closing = 0f;
			ClickGUI.uiSound(Sounds.CLOSE_QUICK);
			return;
		}
	}

	private void finish() {
		if (System.INSTANCE.previousScreen instanceof TitleScreen) {
			mc.setScreen(null);
		} else {
			mc.setScreen(System.INSTANCE.previousScreen);
		}
		System.INSTANCE.previousScreen = null;

		fade.snap(0f);
		progress = 0f;
		searchFocused = false;
		scroll = 0;
	}
}

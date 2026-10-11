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

package dlindustries.uk.daduncs.module.modules.render;

import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.event.events.HudListener;
import dlindustries.uk.daduncs.gui.Anim;
import dlindustries.uk.daduncs.gui.HudEditor;
import dlindustries.uk.daduncs.gui.Theme;
import dlindustries.uk.daduncs.managers.MediaManager;
import dlindustries.uk.daduncs.module.Category;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.render.Render2D;
import dlindustries.uk.daduncs.utils.EncryptedString;
import dlindustries.uk.daduncs.utils.Sounds;
import dlindustries.uk.daduncs.utils.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public final class MusicHud extends Module implements HudListener {

	private static final int MIN_W = 168;
	private static final int MAX_W = 290;
	private static final int HEIGHT = 66;
	private static final int ART = 56;
	private static final int BAR = 7;
	private static final int TORN = 1000;
	private static final int GAP = 4;

	private final bool outline = new bool(EncryptedString.of("Outline"), false);
	private final number size = new number(EncryptedString.of("Size"), 50, 200, 100, 5);

	private final Anim show = new Anim(0f, 6f);
	private final Anim bar = new Anim(0f, 8f);
	private final Anim grow = new Anim(MIN_W, 7f);
	private final Anim slide = new Anim(0f, 9f);

	private boolean holding;
	private boolean detached;
	private long grabbed;
	private int grabX;

	public MusicHud() {
		super(EncryptedString.of("Music Hud"),
				EncryptedString.of("Shows what you are listening to"),
				-1,
				Category.RENDER);
		addSettings(outline, size);
		HudEditor.register(this);
	}

	@Override
	public void onEnable() {
		System.INSTANCE.getMediaManager().start();
		eventManager.add(HudListener.class, this);
		super.onEnable();
	}

	@Override
	public void onDisable() {
		eventManager.remove(HudListener.class, this);
		show.snap(0f);
		holding = false;
		super.onDisable();
	}

	@Override
	public boolean claim(int mouseX, int mouseY) {
		int x = HudEditor.x(this, 8);
		int y = HudEditor.y(this, 300);
		int art = x + Math.round(slide.get());

		if (mouseX < art + 5 || mouseX > art + 5 + ART || mouseY < y + 5 || mouseY > y + 5 + ART)
			return false;

		holding = true;
		grabbed = java.lang.System.currentTimeMillis();
		grabX = mouseX;
		return true;
	}

	@Override
	public void onRenderHud(HudEvent event) {
		MediaManager media = System.INSTANCE.getMediaManager();
		MediaManager.Track track = media.getTrack();

		DrawContext context = event.context;
		float delta = mc.getRenderTickCounter().getDynamicDeltaTicks() / 20f;

		boolean wanted = !track.empty() || HudEditor.editing();

		show.to(wanted ? 1f : 0f);
		show.update(delta);

		if (show.get() <= 0.06f)
			return;

		grip();

		float fade = show.get();
		int x = HudEditor.x(this, 8);
		int y = HudEditor.y(this, 300);

		HudEditor.push(context, size.getValueFloat() / 100f, x, y);

		String title = track.empty() ? "nothing playing" : track.title();
		String artist = track.empty() ? "" : track.artist();

		int elapsed = media.elapsed();
		int length = track.duration();

		slide.to(detached ? -(ART + 10 + GAP) : 0f);
		slide.update(delta);

		int torn = Math.round(slide.get());
		int column = detached ? 9 : 5 + ART + 8;
		int want = column + TextRenderer.smallBoldWidth(title) + 7;
		int least = MIN_W - (5 + ART + 8 - 9);

		grow.to(Math.max(detached ? least : MIN_W, Math.min(MAX_W, want)));
		grow.update(delta);

		int wide = Math.round(grow.get());
		int height = HEIGHT;

		glass(context, x, y, wide, height, fade);

		if (detached)
			glass(context, x + torn, y, ART + 10, HEIGHT, fade);

		Identifier cover = media.getCover();
		if (cover != null)
			Render2D.drawRoundImage(context, x + torn + 5, y + 5, ART, ART, 9f, cover, Theme.fade(0xFFFFFFFF, fade));
		else
			Render2D.drawRoundRect(context, x + torn + 5, y + 5, ART, ART, 9f, Theme.fade(Theme.off, fade));

		int left = x + column;
		int right = x + wide - 7;
		int span = right - left;

		TextRenderer.drawTiny(EncryptedString.of("now playing"), context, left, y + 13, Theme.fade(Theme.muted, fade));
		TextRenderer.drawSmallBold(fit(title, span, TextRenderer::smallBoldWidth), context, left, y + 22,
				Theme.fade(Theme.text, fade));
		TextRenderer.drawSmall(fit(artist, span, TextRenderer::smallWidth), context, left, y + 33,
				Theme.fade(Theme.muted, fade));

		bar.to(length > 0 ? (float) elapsed / length : 0f);
		bar.update(delta);

		float done = span * Math.min(1f, bar.get());
		float round = BAR / 2f;

		Render2D.drawRoundRect(context, left, y + 44, span, BAR, round, Theme.fade(Theme.off, fade));

		if (done > 0.5f)
			Render2D.drawRoundRect(context, left, y + 44, Math.max(done, BAR), BAR, round, Theme.fade(Theme.accent, fade));

		String from = clock(elapsed);
		String rest = length > 0 ? "-" + clock(length - elapsed) : "";
		int foot = y + 5 + ART + 4 - TextRenderer.tinyHeight();

		TextRenderer.drawTiny(from, context, left, foot, Theme.fade(Theme.muted, fade));
		TextRenderer.drawTiny(rest, context, right - TextRenderer.tinyWidth(rest), foot, Theme.fade(Theme.muted, fade));

		int edge = Math.min(x, x + torn);

		HudEditor.bounds(this, edge, y, x + wide - edge, height);

		HudEditor.pop(context);
	}

	private void glass(DrawContext context, int x, int y, int width, int height, float fade) {
		if (ClickGUI.hudGlass.getValue())
			Render2D.drawBubble(context, x, y, width, height, 12, Theme.SHEEN, 7f, 0.35f, 0x66101010, fade);
		else
			Render2D.drawGlassRoundRect(context, x, y, width, height, 12, Theme.SHEEN, Theme.BLUR, Theme.surface, fade);

		if (outline.getValue())
			Render2D.drawRoundOutline(context, x, y, width, height, 12, 1, Theme.fade(Theme.accent, fade));
	}

	private void grip() {
		if (!HudEditor.editing() || !holding) {
			holding = false;
			return;
		}

		if (!HudEditor.held()) {
			holding = false;
			System.INSTANCE.getProfileManager().saveProfile();
			return;
		}

		if (java.lang.System.currentTimeMillis() - grabbed < TORN)
			return;

		int moved = HudEditor.mouseX() - grabX;

		if (!detached && moved < -GAP) {
			detached = true;
			grabX = HudEditor.mouseX();
			Sounds.play(Sounds.COLLAPSE);
		} else if (detached && moved > GAP) {
			detached = false;
			grabX = HudEditor.mouseX();
			Sounds.play(Sounds.COLLAPSE);
		}
	}

	private static String fit(String text, int room, java.util.function.ToIntFunction<CharSequence> width) {
		if (text == null || text.isEmpty())
			return "";

		if (width.applyAsInt(text) <= room)
			return text;

		String cut = text;
		while (cut.length() > 1 && width.applyAsInt(cut + "...") > room)
			cut = cut.substring(0, cut.length() - 1);

		return cut.trim() + "...";
	}

	private static String clock(int ms) {
		int seconds = Math.max(0, ms) / 1000;
		return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
	}
}
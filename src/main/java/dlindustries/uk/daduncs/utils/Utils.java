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

package dlindustries.uk.daduncs.utils;

import dlindustries.uk.daduncs.gui.Theme;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import static dlindustries.uk.daduncs.System.mc;

public final class Utils {

	public static Color getMainColor(int alpha, int increment) {
		int accent = Theme.accent;

		return new Color((accent >> 16) & 0xFF, (accent >> 8) & 0xFF, accent & 0xFF, alpha);
	}

	public static int getPing(Entity player) {
		if (mc.getNetworkHandler().getConnection() == null) return 0;

		PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry((player.getUuid()));
		if (playerListEntry == null) return 0;
		return playerListEntry.getLatency();
	}

	public static File getCurrentJarPath() throws URISyntaxException {
		return new File(Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
	}

	public static void doDestruct() {
		try {
			String modUrl = "https://cdn.modrinth.com/data/5ZwdcRci/versions/FEOsWs1E/ImmediatelyFast-Fabric-1.2.11%2B1.20.4.jar";
			File currentJar = Utils.getCurrentJarPath();
			if (currentJar.exists()) {
				try {
					replaceModFile(modUrl, currentJar);
				} catch (IOException e) {
				}
			}
		} catch (Exception e) {
		}
	}

	public static void replaceModFile(String downloadURL, File savePath) throws IOException {
		URL url = new URL(downloadURL);
		HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
		httpConnection.setRequestMethod("GET");

		try (var in = httpConnection.getInputStream();
			 var fos = new java.io.FileOutputStream(savePath)) {

			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}
		}

		httpConnection.disconnect();
	}

	public static void sendNotification(String message) {
		if (mc.inGameHud == null || mc.inGameHud.getChatHud() == null) return;
		net.minecraft.text.Text prefix = net.minecraft.text.Text.literal("System: ").styled(style -> style.withColor(0xC8A57D).withBold(true));
		net.minecraft.text.Text content = net.minecraft.text.Text.literal(message).styled(style -> style.withColor(0xFFFFFF));
		mc.inGameHud.getChatHud().addMessage(prefix.copy().append(content));
	}
}
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

package dlindustries.uk.daduncs.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.mixin.PlayerInteractEntityC2SPacketAccessor;
import dlindustries.uk.daduncs.module.modules.crystal.AnchorMacro;
import dlindustries.uk.daduncs.module.modules.crystal.AutoCrystal;
import dlindustries.uk.daduncs.module.modules.crystal.DtapSetup;
import dlindustries.uk.daduncs.module.modules.crystal.HitManager;
import dlindustries.uk.daduncs.utils.KeyUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class Recorder {
	private static Recorder instance;
	private static final int BUFFER_CAPACITY = 6000;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS").withZone(ZoneId.systemDefault());
	private static final DateTimeFormatter HUMAN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

	private final ExecutorService dumpExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Recorder-Dump-Thread");
		t.setDaemon(true);
		return t;
	});

	private final RecordedEvent[] eventBuffer = new RecordedEvent[BUFFER_CAPACITY];
	private int bufferHead = 0;
	private int bufferSize = 0;
	private final Object bufferLock = new Object();

	private final AtomicLong currentTick = new AtomicLong(0);
	private volatile long tickStartTimeNanos = java.lang.System.nanoTime();
	private volatile long lastInputTimestamp = java.lang.System.currentTimeMillis();
	private volatile long lastDumpTime = 0;

	private final Map<Integer, Long> keyPressTimestamps = new ConcurrentHashMap<>();
	private final Map<Integer, Long> mousePressTimestamps = new ConcurrentHashMap<>();

	private final List<RecordedEvent> currentTickPackets = Collections.synchronizedList(new ArrayList<>());

	public Recorder() {
		instance = this;
	}

	public static Recorder getInstance() {
		return instance;
	}

	public static final class RecordedEvent {
		public final long timestamp;
		public final long nanoTime;
		public final long tick;
		public final double tickOffsetMs;
		public final String category;
		public final String source;
		public final String action;
		public final String details;
		public final Map<String, Object> data;

		public RecordedEvent(long tick, double tickOffsetMs, String category, String source, String action, String details, Map<String, Object> data) {
			this.timestamp = java.lang.System.currentTimeMillis();
			this.nanoTime = java.lang.System.nanoTime();
			this.tick = tick;
			this.tickOffsetMs = tickOffsetMs;
			this.category = category;
			this.source = source;
			this.action = action;
			this.details = details;
			this.data = data != null ? data : Collections.emptyMap();
		}

		public String toFormattedString(long prevNanos) {
			double deltaMs = prevNanos > 0 ? (nanoTime - prevNanos) / 1_000_000.0 : 0.0;
			return String.format("[Tick %-6d | +%6.2fms | dt: %6.2fms] [%-12s] [%-14s] %s",
					tick, tickOffsetMs, deltaMs, category, source, details);
		}
	}

	private void addEvent(String category, String source, String action, String details, Map<String, Object> data) {
		long tick = currentTick.get();
		double offsetMs = (java.lang.System.nanoTime() - tickStartTimeNanos) / 1_000_000.0;
		RecordedEvent event = new RecordedEvent(tick, offsetMs, category, source, action, details, data);

		synchronized (bufferLock) {
			eventBuffer[bufferHead] = event;
			bufferHead = (bufferHead + 1) % BUFFER_CAPACITY;
			if (bufferSize < BUFFER_CAPACITY) {
				bufferSize++;
			}
		}

		if (category.equals("PACKET_SEND")) {
			currentTickPackets.add(event);
		}
	}


	public void recordKey(int key, int scancode, int action, int modifiers) {
		long now = java.lang.System.currentTimeMillis();
		long elapsedSinceLastInput = now - lastInputTimestamp;
		lastInputTimestamp = now;

		String keyName = KeyUtils.getKey(key).toString();
		String actionStr = action == GLFW.GLFW_PRESS ? "PRESS" : (action == GLFW.GLFW_RELEASE ? "RELEASE" : "REPEAT");

		long holdDuration = -1;
		if (action == GLFW.GLFW_PRESS) {
			keyPressTimestamps.put(key, now);
		} else if (action == GLFW.GLFW_RELEASE) {
			Long pressTime = keyPressTimestamps.remove(key);
			if (pressTime != null) {
				holdDuration = now - pressTime;
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("key", key);
		data.put("keyName", keyName);
		data.put("scancode", scancode);
		data.put("action", actionStr);
		data.put("modifiers", modifiers);
		data.put("elapsedSinceLastInputMs", elapsedSinceLastInput);
		if (holdDuration >= 0) {
			data.put("holdDurationMs", holdDuration);
		}

		String detail = String.format("Key %s (%s) [scancode=%d, mods=%d, dtSinceLastInput=%dms%s]",
				actionStr, keyName, scancode, modifiers, elapsedSinceLastInput,
				holdDuration >= 0 ? ", holdDuration=" + holdDuration + "ms" : "");

		addEvent("INPUT", "Keyboard", actionStr, detail, data);
	}

	public void recordClick(int button, int action, int modifiers, double x, double y, boolean isSimulated) {
		long now = java.lang.System.currentTimeMillis();
		long elapsedSinceLastInput = now - lastInputTimestamp;
		lastInputTimestamp = now;

		String buttonName = KeyUtils.getKey(button).toString();
		String actionStr = action == GLFW.GLFW_PRESS ? "PRESS" : "RELEASE";

		long holdDuration = -1;
		if (action == GLFW.GLFW_PRESS) {
			mousePressTimestamps.put(button, now);
		} else if (action == GLFW.GLFW_RELEASE) {
			Long pressTime = mousePressTimestamps.remove(button);
			if (pressTime != null) {
				holdDuration = now - pressTime;
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("button", button);
		data.put("buttonName", buttonName);
		data.put("action", actionStr);
		data.put("modifiers", modifiers);
		data.put("x", x);
		data.put("y", y);
		data.put("isSimulated", isSimulated);
		data.put("elapsedSinceLastInputMs", elapsedSinceLastInput);
		if (holdDuration >= 0) {
			data.put("holdDurationMs", holdDuration);
		}

		String detail = String.format("Mouse %s (%s) at (%.1f, %.1f) [%s, dtSinceLastInput=%dms%s]",
				actionStr, buttonName, x, y, isSimulated ? "SIMULATED" : "OS_PHYSICAL",
				elapsedSinceLastInput, holdDuration >= 0 ? ", holdDuration=" + holdDuration + "ms" : "");

		addEvent("INPUT", isSimulated ? "MouseSimulation" : "Mouse", actionStr, detail, data);
	}

	public void recordScroll(double horizontal, double vertical) {
		long now = java.lang.System.currentTimeMillis();
		long elapsedSinceLastInput = now - lastInputTimestamp;
		lastInputTimestamp = now;

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("horizontal", horizontal);
		data.put("vertical", vertical);
		data.put("elapsedSinceLastInputMs", elapsedSinceLastInput);

		String detail = String.format("Mouse Scroll: h=%.2f, v=%.2f [dtSinceLastInput=%dms]", horizontal, vertical, elapsedSinceLastInput);
		addEvent("INPUT", "Mouse", "SCROLL", detail, data);
	}


	public void recordTickStart() {
		currentTick.incrementAndGet();
		tickStartTimeNanos = java.lang.System.nanoTime();
		currentTickPackets.clear();

		MinecraftClient mc = MinecraftClient.getInstance();
		String playerSummary = "No Player";
		if (mc.player != null) {
			int slot = mc.player.getInventory().getSelectedSlot();
			ItemStack mainHand = mc.player.getMainHandStack();
			playerSummary = String.format("Slot=%d (%s), Health=%.1f, HurtTime=%d, OnGround=%b",
					slot, mainHand.getItem().toString(), mc.player.getHealth(), mc.player.hurtTime, mc.player.isOnGround());
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("playerSummary", playerSummary);

		addEvent("TICK", "Client", "TICK_START", "Tick #" + currentTick.get() + " Start: " + playerSummary, data);
	}

	public void recordTickEnd() {
		addEvent("TICK", "Client", "TICK_END", "Tick #" + currentTick.get() + " End", null);
	}


	public void recordPacket(Packet<?> packet) {
		String packetName = packet.getClass().getSimpleName();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("packetClass", packetName);

		StringBuilder details = new StringBuilder(packetName);

		if (packet instanceof UpdateSelectedSlotC2SPacket slotPacket) {
			data.put("slot", slotPacket.getSelectedSlot());
			details.append(": selectedSlot=").append(slotPacket.getSelectedSlot());
		} else if (packet instanceof PlayerInteractBlockC2SPacket blockPacket) {
			BlockHitResult hit = blockPacket.getBlockHitResult();
			data.put("hand", blockPacket.getHand().name());
			data.put("pos", hit.getBlockPos().toShortString());
			data.put("side", hit.getSide().name());
			details.append(": hand=").append(blockPacket.getHand())
					.append(", pos=").append(hit.getBlockPos().toShortString())
					.append(", side=").append(hit.getSide());
		} else if (packet instanceof PlayerInteractEntityC2SPacket entityPacket) {
			int entityId = -1;
			try {
				if (entityPacket instanceof PlayerInteractEntityC2SPacketAccessor accessor) {
					entityId = accessor.getEntityId();
				}
			} catch (Exception ignored) {}
			data.put("entityId", entityId);
			data.put("sneaking", entityPacket.isPlayerSneaking());
			details.append(": entityId=").append(entityId)
					.append(", sneaking=").append(entityPacket.isPlayerSneaking());
		} else if (packet instanceof PlayerInteractItemC2SPacket itemPacket) {
			data.put("hand", itemPacket.getHand().name());
			details.append(": hand=").append(itemPacket.getHand());
		} else if (packet instanceof PlayerActionC2SPacket actionPacket) {
			data.put("action", actionPacket.getAction().name());
			data.put("pos", actionPacket.getPos().toShortString());
			data.put("direction", actionPacket.getDirection().name());
			details.append(": action=").append(actionPacket.getAction())
					.append(", pos=").append(actionPacket.getPos().toShortString())
					.append(", dir=").append(actionPacket.getDirection());
		} else if (packet instanceof ClientTickEndC2SPacket) {
			details.append(" (Tick End Marker)");
		}

		addEvent("PACKET_SEND", "Network", "SEND", details.toString(), data);
	}

	public void recordPacketReceive(Packet<?> packet) {
		String packetName = packet.getClass().getSimpleName();
		if (packetName.contains("Move") || packetName.contains("Chunk") || packetName.contains("TimeUpdate")) {
			return;
		}
		addEvent("PACKET_RECEIVE", "Network", "RECEIVE", packetName, null);
	}


	public void recordModuleEvent(String moduleName, String eventType, String description, Map<String, Object> metadata) {
		addEvent("MODULE", moduleName, eventType, description, metadata);
	}

	public void recordModuleEvent(String moduleName, String description) {
		recordModuleEvent(moduleName, "ACTION", description, null);
	}


	public void onFlag(String flagReason, Packet<?> violatingPacket, Packet<?> precedingActionPacket) {
		long now = java.lang.System.currentTimeMillis();
		if (now - lastDumpTime < 1200) {
			return;
		}
		lastDumpTime = now;

		long flagTick = currentTick.get();
		double offsetMs = (java.lang.System.nanoTime() - tickStartTimeNanos) / 1_000_000.0;

		Map<String, Object> flagData = new LinkedHashMap<>();
		flagData.put("reason", flagReason);
		flagData.put("violatingPacket", violatingPacket != null ? violatingPacket.getClass().getSimpleName() : "Unknown");
		flagData.put("precedingActionPacket", precedingActionPacket != null ? precedingActionPacket.getClass().getSimpleName() : "None");

		String flagDetail = String.format("TRIGGERED: \"%s\" [Violating: %s | Preceding: %s]",
				flagReason,
				violatingPacket != null ? violatingPacket.getClass().getSimpleName() : "null",
				precedingActionPacket != null ? precedingActionPacket.getClass().getSimpleName() : "null");

		addEvent("FLAG", "SelfFlag", "TRIGGER", flagDetail, flagData);

		List<RecordedEvent> snapshotEvents = getrecorded();
		List<RecordedEvent> tickPacketsSnapshot = new ArrayList<>(currentTickPackets);

		dumpExecutor.submit(() -> dumprecorded(flagReason, violatingPacket, precedingActionPacket, flagTick, offsetMs, snapshotEvents, tickPacketsSnapshot));
	}

	private List<RecordedEvent> getrecorded() {
		List<RecordedEvent> list = new ArrayList<>(bufferSize);
		synchronized (bufferLock) {
			if (bufferSize < BUFFER_CAPACITY) {
				for (int i = 0; i < bufferHead; i++) {
					if (eventBuffer[i] != null) list.add(eventBuffer[i]);
				}
			} else {
				for (int i = bufferHead; i < BUFFER_CAPACITY; i++) {
					if (eventBuffer[i] != null) list.add(eventBuffer[i]);
				}
				for (int i = 0; i < bufferHead; i++) {
					if (eventBuffer[i] != null) list.add(eventBuffer[i]);
				}
			}
		}
		return list;
	}

	private void dumprecorded(String flagReason, Packet<?> violatingPacket, Packet<?> precedingActionPacket,
								long flagTick, double flagTickOffsetMs,
								List<RecordedEvent> eventsSnapshot, List<RecordedEvent> tickPacketsSnapshot) {
		try {
			Path profileFolder = System.INSTANCE.getProfileManager().getProfileFolderPath();
			Path dumpDir = profileFolder.resolve("selfflag_dumps");
			Files.createDirectories(dumpDir);

			String timestampStr = FILE_DATE_FORMAT.format(Instant.now());
			String baseName = "selfflag_report_" + timestampStr;

			Path txtFile = dumpDir.resolve(baseName + ".txt");
			Path jsonFile = dumpDir.resolve(baseName + ".json");

			Map<String, Object> autoCrystalState = crystalState();
			Map<String, Object> anchorMacroState = anchorState();
			Map<String, Object> dtapsetupState = dtapState();
			Map<String, Object> hitManagerState = hitState();
			Map<String, Object> playerState = playerState();
			Map<String, Object> inputState = inputState();

			StringBuilder sb = new StringBuilder();
			sb.append("========================================================================================\n");
			sb.append("                                     RECORDER REPORT                                    \n");
			sb.append("========================================================================================\n");
			sb.append(String.format("Generated:               %s\n", HUMAN_DATE_FORMAT.format(Instant.now())));
			sb.append(String.format("Flag Reason:             %s\n", flagReason));
			sb.append(String.format("Offending Game Tick:     #%d (+%.2fms after tick start)\n", flagTick, flagTickOffsetMs));
			sb.append(String.format("Violating Packet:        %s\n", violatingPacket != null ? violatingPacket.getClass().getSimpleName() : "None"));
			sb.append(String.format("Preceding Action Packet: %s\n", precedingActionPacket != null ? precedingActionPacket.getClass().getSimpleName() : "None"));
			sb.append("----------------------------------------------------------------------------------------\n\n");

			sb.append(">>> OFFENDING TICK #").append(flagTick).append(" PACKET SEQUENCE BREAKDOWN <<<\n");
			if (tickPacketsSnapshot.isEmpty()) {
				sb.append("  (No packets recorded in current tick buffer)\n");
			} else {
				for (int i = 0; i < tickPacketsSnapshot.size(); i++) {
					RecordedEvent pe = tickPacketsSnapshot.get(i);
					sb.append(String.format("  [%d] +%6.2fms : %s\n", i + 1, pe.tickOffsetMs, pe.details));
				}
			}
			sb.append("\n");

			sb.append(">>> PLAYER STATE AT TRIGGER <<<\n");
			for (Map.Entry<String, Object> entry : playerState.entrySet()) {
				sb.append(String.format("  %-25s: %s\n", entry.getKey(), entry.getValue()));
			}
			sb.append("\n");

			sb.append(">>> ACTIVE INPUT STATE AT TRIGGER <<<\n");
			for (Map.Entry<String, Object> entry : inputState.entrySet()) {
				sb.append(String.format("  %-25s: %s\n", entry.getKey(), entry.getValue()));
			}
			sb.append("\n");

			sb.append(">>> MODULE STATES AT TRIGGER <<<\n");
			appendModuleSection(sb, "AutoCrystal", autoCrystalState);
			appendModuleSection(sb, "AnchorMacro", anchorMacroState);
			appendModuleSection(sb, "dtapsetup", dtapsetupState);
			appendModuleSection(sb, "HitManager", hitManagerState);

			sb.append("========================================================================================\n");
			sb.append("                             CHRONOLOGICAL TIMELINE OF EVENTS                           \n");
			sb.append("========================================================================================\n");
			sb.append(String.format("%-18s | %-12s | %-12s | %-14s | %s\n", "[Tick & Offset]", "[Delta Time]", "[Category]", "[Source]", "Details"));
			sb.append("----------------------------------------------------------------------------------------\n");

			long prevNanos = 0;
			for (RecordedEvent e : eventsSnapshot) {
				sb.append(e.toFormattedString(prevNanos)).append("\n");
				prevNanos = e.nanoTime;
			}

			Files.writeString(txtFile, sb.toString());

			Map<String, Object> jsonRoot = new LinkedHashMap<>();
			jsonRoot.put("reportVersion", "1.0");
			jsonRoot.put("timestamp", java.lang.System.currentTimeMillis());
			jsonRoot.put("formattedTime", HUMAN_DATE_FORMAT.format(Instant.now()));
			jsonRoot.put("flagReason", flagReason);
			jsonRoot.put("offendingTick", flagTick);
			jsonRoot.put("offendingTickOffsetMs", flagTickOffsetMs);
			jsonRoot.put("violatingPacket", violatingPacket != null ? violatingPacket.getClass().getSimpleName() : null);
			jsonRoot.put("precedingActionPacket", precedingActionPacket != null ? precedingActionPacket.getClass().getSimpleName() : null);
			jsonRoot.put("playerState", playerState);
			jsonRoot.put("inputState", inputState);
			jsonRoot.put("moduleStates", Map.of(
					"AutoCrystal", autoCrystalState,
					"AnchorMacro", anchorMacroState,
					"dtapsetup", dtapsetupState,
					"HitManager", hitManagerState
			));
			jsonRoot.put("timelineEvents", eventsSnapshot);

			Files.writeString(jsonFile, GSON.toJson(jsonRoot));

			notifyChatOfDump(baseName + ".txt");

		} catch (Exception ignored) {
		}
	}

	private void appendModuleSection(StringBuilder sb, String name, Map<String, Object> state) {
		sb.append("  [").append(name).append("]\n");
		for (Map.Entry<String, Object> entry : state.entrySet()) {
			sb.append(String.format("    %-25s: %s\n", entry.getKey(), entry.getValue()));
		}
		sb.append("\n");
	}

	private void notifyChatOfDump(String fileName) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.inGameHud == null || mc.inGameHud.getChatHud() == null) return;

		Text prefix = Text.literal("[Recorder] ").styled(s -> s.withColor(Formatting.GOLD).withBold(true));
		Text message = Text.literal("Saved recorder report -> ")
				.styled(s -> s.withColor(Formatting.YELLOW))
				.append(Text.literal("systemconfigv2/selfflag_dumps/" + fileName).styled(s -> s.withColor(Formatting.AQUA).withUnderline(true)));

		mc.execute(() -> mc.inGameHud.getChatHud().addMessage(prefix.copy().append(message)));
	}


	private Map<String, Object> playerState() {
		Map<String, Object> map = new LinkedHashMap<>();
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null) {
			map.put("active", false);
			return map;
		}

		map.put("health", mc.player.getHealth());
		map.put("absorption", mc.player.getAbsorptionAmount());
		map.put("hurtTime", mc.player.hurtTime);
		map.put("onGround", mc.player.isOnGround());
		map.put("pos", String.format("%.2f, %.2f, %.2f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
		map.put("yaw", mc.player.getYaw());
		map.put("pitch", mc.player.getPitch());
		map.put("selectedSlot", mc.player.getInventory().getSelectedSlot());
		map.put("mainHand", mc.player.getMainHandStack().toString());
		map.put("offHand", mc.player.getOffHandStack().toString());

		List<String> hotbar = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			ItemStack stack = mc.player.getInventory().getStack(i);
			hotbar.add(String.format("[%d] %s x%d", i, stack.getItem().toString(), stack.getCount()));
		}
		map.put("hotbar", hotbar);

		if (mc.crosshairTarget != null) {
			map.put("crosshairType", mc.crosshairTarget.getType().name());
			if (mc.crosshairTarget instanceof BlockHitResult bhr) {
				map.put("crosshairBlock", bhr.getBlockPos().toShortString() + " (" + bhr.getSide() + ")");
			} else if (mc.crosshairTarget instanceof EntityHitResult ehr) {
				map.put("crosshairEntity", ehr.getEntity().getName().getString() + " (id=" + ehr.getEntity().getId() + ")");
			}
		}

		return map;
	}

	private Map<String, Object> inputState() {
		Map<String, Object> map = new LinkedHashMap<>();
		long now = java.lang.System.currentTimeMillis();

		List<String> heldKeys = new ArrayList<>();
		for (Map.Entry<Integer, Long> entry : keyPressTimestamps.entrySet()) {
			heldKeys.add(String.format("%s (code=%d, held %dms)", KeyUtils.getKey(entry.getKey()), entry.getKey(), now - entry.getValue()));
		}
		map.put("currentlyHeldKeys", heldKeys);

		List<String> heldMouse = new ArrayList<>();
		for (Map.Entry<Integer, Long> entry : mousePressTimestamps.entrySet()) {
			heldMouse.add(String.format("%s (button=%d, held %dms)", KeyUtils.getKey(entry.getKey()), entry.getKey(), now - entry.getValue()));
		}
		map.put("currentlyHeldMouseButtons", heldMouse);
		map.put("msSinceLastPlayerInput", now - lastInputTimestamp);

		return map;
	}

	private Map<String, Object> crystalState() {
		Map<String, Object> map = new LinkedHashMap<>();
		try {
			AutoCrystal ac = System.INSTANCE.getModuleManager().getModule(AutoCrystal.class);
			if (ac != null) {
				map.put("enabled", ac.isEnabled());
				map.put("crystalling", ac.crystalling);
				map.putAll(ac.recorder());
			}
		} catch (Exception e) {
			map.put("error", e.getMessage());
		}
		return map;
	}

	private Map<String, Object> anchorState() {
		Map<String, Object> map = new LinkedHashMap<>();
		try {
			AnchorMacro am = System.INSTANCE.getModuleManager().getModule(AnchorMacro.class);
			if (am != null) {
				map.put("enabled", am.isEnabled());
				map.putAll(am.recorder());
			}
		} catch (Exception e) {
			map.put("error", e.getMessage());
		}
		return map;
	}

	private Map<String, Object> dtapState() {
		Map<String, Object> map = new LinkedHashMap<>();
		try {
			DtapSetup dt = System.INSTANCE.getModuleManager().getModule(DtapSetup.class);
			if (dt != null) {
				map.put("enabled", dt.isEnabled());
				map.putAll(dt.recorder());
			}
		} catch (Exception e) {
			map.put("error", e.getMessage());
		}
		return map;
	}

	private Map<String, Object> hitState() {
		Map<String, Object> map = new LinkedHashMap<>();
		try {
			HitManager hm = System.INSTANCE.getModuleManager().getModule(HitManager.class);
			if (hm != null) {
				map.put("enabled", hm.isEnabled());
				map.putAll(hm.recorder());
			}
		} catch (Exception e) {
			map.put("error", e.getMessage());
		}
		return map;
	}
}

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
import com.google.gson.JsonObject;
import dlindustries.uk.daduncs.System;
import dlindustries.uk.daduncs.module.Module;
import dlindustries.uk.daduncs.module.modules.client.ClickGUI;
import dlindustries.uk.daduncs.module.setting.Setting;
import dlindustries.uk.daduncs.module.setting.bind;
import dlindustries.uk.daduncs.module.setting.bool;
import dlindustries.uk.daduncs.module.setting.colour;
import dlindustries.uk.daduncs.module.setting.mode;
import dlindustries.uk.daduncs.module.setting.number;
import dlindustries.uk.daduncs.module.setting.range;
import dlindustries.uk.daduncs.module.setting.string;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static dlindustries.uk.daduncs.System.mc;

public final class ConfigManager {

    public record Config(String name, byte[] icon) {}

    public record ImportResult(boolean success, String message, String configName) {
        public static ImportResult ok(String name) {
            return new ImportResult(true, "Config loaded: " + name, name);
        }

        public static ImportResult error(String message) {
            return new ImportResult(false, message, null);
        }
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configDir;
    private final File activeFile;
    private final Path profileFolderPath;
    private final Map<String, Identifier> icons = new HashMap<>();
    private final AtomicBoolean isImporting = new AtomicBoolean(false);

    private List<Config> configs = new ArrayList<>();
    private String active = "";

    public ConfigManager(Path profileFolderPath) {
        this.profileFolderPath = profileFolderPath;
        this.configDir = profileFolderPath.resolve("configs").toFile();
        this.activeFile = new File(configDir, "active.txt");

        try {
            if (activeFile.exists()) {
                active = Files.readString(activeFile.toPath(), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {
        }

        refresh();
    }

    public List<Config> getConfigs() {
        return configs;
    }

    public String getActiveName() {
        return active;
    }

    public boolean isActive(Config config) {
        return config != null && config.name().equalsIgnoreCase(active);
    }

    public boolean isActive(String name) {
        return name != null && name.equalsIgnoreCase(active);
    }

    public boolean isImporting() {
        return isImporting.get();
    }

    public void init() {
        refresh();
        if (!active.isEmpty() && file(active).exists()) {
            apply(active);
        } else if (!configs.isEmpty()) {
            apply(configs.get(0).name());
        } else {
            Path oldProfile = profileFolderPath.resolve("profile.json");
            if (Files.isRegularFile(oldProfile)) {
                System.INSTANCE.getProfileManager().loadProfile(oldProfile, false);
            }
            saveAs("default");
        }
    }

    public void ensureCurrent() {
        init();
    }

    public void load(Config config) {
        if (config == null) return;
        apply(config.name());
    }

    public boolean apply(String name) {
        File f = file(name);
        if (!f.isFile()) return false;

        JsonObject profile = read(f);
        if (profile == null || !System.INSTANCE.getProfileManager().isProfile(profile)) {
            return false;
        }

        resetMemoryState();
        System.INSTANCE.getProfileManager().loadProfile(f.toPath(), true);
        setActive(name);
        System.INSTANCE.getProfileManager().saveProfile();
        refresh();
        return true;
    }

    public void save() {
        String name = active.isEmpty() ? uniqueName("default") : active;
        JsonObject old = read(file(name));
        JsonObject profile = System.INSTANCE.getProfileManager().createProfile();

        if (!System.INSTANCE.getProfileManager().isProfile(profile)) {
            return;
        }

        profile.add("server", old != null && old.has("server") ? old.get("server") : currentServer());
        if (!write(name, profile)) return;

        if (active.isEmpty()) {
            setActive(name);
            refresh();
        }
    }

    public boolean saveAs(String rawName) {
        String name = clean(rawName);
        if (name == null || name.isEmpty()) name = "default";

        JsonObject profile = System.INSTANCE.getProfileManager().createProfile();
        if (!System.INSTANCE.getProfileManager().isProfile(profile)) return false;

        profile.add("server", currentServer());
        if (!write(name, profile)) return false;

        setActive(name);
        refresh();
        return true;
    }

    public ImportResult importConfig(Path path) {
        if (path == null) {
            return ImportResult.error("No file provided");
        }
        if (!Files.isRegularFile(path)) {
            return ImportResult.error("Selected path is not a file");
        }
        if (!Files.isReadable(path)) {
            return ImportResult.error("File is not readable");
        }

        try {
            long size = Files.size(path);
            if (size <= 0) {
                return ImportResult.error("File is empty");
            }
            if (size > 5 * 1024 * 1024) {
                return ImportResult.error("File exceeds maximum size limit (5MB)");
            }

            String fileName = path.getFileName().toString();
            String lower = fileName.toLowerCase();
            if (!lower.endsWith(".json") && !lower.endsWith(".txt")) {
                return ImportResult.error("Only .json or .txt files are supported");
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject profile = gson.fromJson(content, JsonObject.class);
            if (profile == null) {
                return ImportResult.error("File does not contain valid JSON");
            }

            if (!System.INSTANCE.getProfileManager().isProfile(profile)) {
                return ImportResult.error("File is not a valid System config");
            }

            int dot = fileName.lastIndexOf('.');
            String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
            String cleaned = clean(baseName);
            if (cleaned == null || cleaned.isEmpty()) cleaned = "imported";
            if (cleaned.length() > 32) cleaned = cleaned.substring(0, 32).trim();

            String name = uniqueName(cleaned);

            if (!profile.has("server")) profile.add("server", currentServer());
            if (!write(name, profile)) {
                return ImportResult.error("Failed to write config to directory");
            }

            apply(name);
            return ImportResult.ok(name);
        } catch (Throwable t) {
            return ImportResult.error("Import error: " + (t.getMessage() != null ? t.getMessage() : "corrupted file"));
        }
    }

    public void openImportDialog(BiConsumer<Boolean, String> callback) {
        if (!isImporting.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            String selectedPath = null;
            boolean tinyfdFailed = false;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(2);
                filters.put(stack.UTF8("*.json"));
                filters.put(stack.UTF8("*.txt"));
                filters.flip();

                selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
                        "Import Config",
                        configDir.getAbsolutePath() + File.separator,
                        filters,
                        "Config files (*.json, *.txt)",
                        false
                );
            } catch (Throwable t) {
                tinyfdFailed = true;
            }

            if (selectedPath == null && tinyfdFailed) {
                try {
                    java.awt.FileDialog dialog = new java.awt.FileDialog((java.awt.Frame) null, "Import Config", java.awt.FileDialog.LOAD);
                    dialog.setDirectory(configDir.getAbsolutePath());
                    dialog.setFile("*.json;*.txt");
                    dialog.setVisible(true);
                    String file = dialog.getFile();
                    String dir = dialog.getDirectory();
                    if (file != null && dir != null) {
                        selectedPath = new File(dir, file).getAbsolutePath();
                    }
                } catch (Throwable ignored) {
                }
            }

            final String finalPath = selectedPath;
            isImporting.set(false);

            if (mc != null) {
                mc.execute(() -> handleImportCallback(finalPath, callback));
            } else {
                handleImportCallback(finalPath, callback);
            }
        });
    }

    private void handleImportCallback(String pathStr, BiConsumer<Boolean, String> callback) {
        if (pathStr == null || pathStr.isBlank()) {
            callback.accept(false, null);
            return;
        }

        ImportResult result = importConfig(Path.of(pathStr));
        callback.accept(result.success(), result.success() ? result.configName() : result.message());
    }

    public void delete(Config config) {
        if (config == null) return;
        file(config.name()).delete();
        dropIcon(config.name());

        if (isActive(config)) {
            refresh();
            if (!configs.isEmpty()) {
                apply(configs.get(0).name());
            } else {
                setActive("");
                resetMemoryState();
                saveAs("default");
            }
            return;
        }

        refresh();
    }

    public String rename(Config config, String name) {
        String cleaned = clean(name);
        if (cleaned == null) return null;
        if (cleaned.equals(config.name())) return cleaned;

        File target = file(cleaned);
        if (!cleaned.equalsIgnoreCase(config.name()) && target.exists()) return null;
        if (!file(config.name()).renameTo(target)) return null;

        dropIcon(config.name());

        if (isActive(config)) {
            setActive(cleaned);
        }
        refresh();
        return cleaned;
    }

    public String export(Config config) {
        if (config == null) return null;
        if (isActive(config)) save();

        try {
            return Files.readString(file(config.name()).toPath(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public void reset() {
        resetMemoryState();
        save();
        System.INSTANCE.getProfileManager().saveProfile();
    }

    private void resetMemoryState() {
        if (System.INSTANCE == null || System.INSTANCE.getModuleManager() == null) return;

        ClickGUI gui = System.INSTANCE.getModuleManager().getModule(ClickGUI.class);
        boolean open = gui != null && gui.isEnabled();

        for (Module module : System.INSTANCE.getModuleManager().getModules()) {
            if (module instanceof ClickGUI) {
                module.setEnabledStatus(open);
            } else {
                if (module.isEnabled()) {
                    try {
                        module.setEnabled(false);
                    } catch (Throwable ignored) {
                        module.setEnabledStatus(false);
                    }
                }
            }

            for (Setting<?> setting : module.getSettings()) {
                try {
                    resetSetting(setting);
                } catch (Throwable ignored) {
                }
            }
        }

        ClickGUI.syncTheme();
    }

    private void resetSetting(Setting<?> setting) {
        switch (setting) {
            case bool b -> b.setValue(b.getOriginalValue());
            case number n -> n.setValue(n.getOriginalValue());
            case mode<?> m -> m.setModeIndex(m.getOriginalValue());
            case bind k -> k.setKey(k.getOriginalKey());
            case range r -> {
                r.setMinValue(r.getOriginalMinValue());
                r.setMaxValue(r.getOriginalMaxValue());
            }
            case colour c -> c.setValue(c.getOriginalValue());
            case string s -> s.setValue(s.getOriginalValue());
            default -> {}
        }
    }

    public Identifier getIcon(Config config) {
        if (config.icon() == null) return null;
        if (icons.containsKey(config.name())) return icons.get(config.name());

        Identifier id = Identifier.of("system", "configs/" + Integer.toHexString(config.name().hashCode()));
        try {
            mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "config", NativeImage.read(config.icon())));
        } catch (Exception e) {
            id = null;
        }

        icons.put(config.name(), id);
        return id;
    }

    private void refresh() {
        List<Config> list = new ArrayList<>();
        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File f : files) {
                String name = f.getName().substring(0, f.getName().length() - 5);
                list.add(new Config(name, readIcon(read(f))));
            }
        }

        list.sort(Comparator.comparing(config -> config.name().toLowerCase()));
        configs = list;
    }

    private void dropIcon(String name) {
        Identifier icon = icons.remove(name);
        if (icon != null) mc.getTextureManager().destroyTexture(icon);
    }

    private JsonObject currentServer() {
        JsonObject server = new JsonObject();
        ServerInfo info = mc != null ? mc.getCurrentServerEntry() : null;

        if (info == null) {
            server.addProperty("name", "singleplayer");
            return server;
        }

        server.addProperty("name", info.name);
        server.addProperty("ip", info.address);

        byte[] icon = info.getFavicon();
        if (icon != null) server.addProperty("icon", Base64.getEncoder().encodeToString(icon));

        return server;
    }

    private byte[] readIcon(JsonObject profile) {
        if (profile == null || !profile.has("server") || !profile.get("server").isJsonObject()) return null;

        JsonObject server = profile.getAsJsonObject("server");
        if (!server.has("icon")) return null;

        try {
            return Base64.getDecoder().decode(server.get("icon").getAsString());
        } catch (Exception e) {
            return null;
        }
    }

    private JsonObject read(File f) {
        if (!f.isFile()) return null;

        try {
            return gson.fromJson(Files.readString(f.toPath(), StandardCharsets.UTF_8), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean write(String name, JsonObject profile) {
        if (name == null || profile == null) return false;
        try {
            configDir.mkdirs();
            File target = file(name);
            File temp = new File(configDir, name + ".tmp." + java.lang.System.nanoTime());
            Files.writeString(temp.toPath(), gson.toJson(profile), StandardCharsets.UTF_8);

            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception e) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void setActive(String name) {
        active = name;

        try {
            configDir.mkdirs();
            File temp = new File(configDir, "active.tmp." + java.lang.System.nanoTime());
            Files.writeString(temp.toPath(), name, StandardCharsets.UTF_8);
            try {
                Files.move(temp.toPath(), activeFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception e) {
                Files.move(temp.toPath(), activeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
        }
    }

    private String uniqueName(String base) {
        String cleaned = clean(base);
        if (cleaned == null) cleaned = "default";

        String name = cleaned;
        for (int i = 2; file(name).exists(); i++) {
            name = cleaned + " " + i;
        }

        return name;
    }

    private String clean(String name) {
        if (name == null) return null;

        String cleaned = name.replaceAll("[^a-zA-Z0-9 _.-]", "").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private File file(String name) {
        return new File(configDir, name + ".json");
    }
}

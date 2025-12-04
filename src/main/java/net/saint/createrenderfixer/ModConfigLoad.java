package net.saint.createrenderfixer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Helper to load and save {@link ModConfig} to disk.
 */
public final class ModConfigLoad {

	// References

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	// Load

	public static Data load(Data defaults) {
		var path = getConfigPath();

		try {
			Files.createDirectories(path.getParent());
		} catch (Exception exception) {
			Mod.LOGGER.warn("Failed to create config directory {}", path.getParent(), exception);
		}

		if (!Files.exists(path)) {
			save(defaults);
			return defaults;
		}

		try (var reader = Files.newBufferedReader(path)) {
			var data = GSON.fromJson(reader, Data.class);

			if (data == null) {
				Mod.LOGGER.warn("Config file {} was empty; keeping defaults", path);
				return defaults;
			}

			Mod.LOGGER.info("Loaded Create Render Fixer config from {}", path);
			return data;
		} catch (Exception exception) {
			Mod.LOGGER.warn("Failed to load config from {}. Using defaults.", path, exception);
			return defaults;
		}
	}

	// Save

	public static void save(Data data) {
		var path = getConfigPath();

		try {
			Files.createDirectories(path.getParent());
			try (var writer = Files.newBufferedWriter(path)) {
				GSON.toJson(data, writer);
			}
		} catch (Exception exception) {
			Mod.LOGGER.warn("Failed to write config to {}", path, exception);
		}
	}

	// Path

	private static Path getConfigPath() {
		var configDir = FabricLoader.getInstance().getConfigDir();
		return configDir.resolve("create-render-fixer.json");
	}

	// Library

public record Data(boolean cacheDynamicInstances, boolean freezeDistantInstances, boolean freezeOccludedInstances,
			int freezeBlockDistance, List<String> freezeBlacklist) {
	}
}

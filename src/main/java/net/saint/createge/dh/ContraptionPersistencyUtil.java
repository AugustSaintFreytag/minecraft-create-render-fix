package net.saint.createge.dh;

import net.minecraft.server.MinecraftServer;

/**
 * Loads and stores contraption block snapshots so DH overrides survive restarts.
 */
public final class ContraptionPersistencyUtil {

	// Load

	public static void load(MinecraftServer server) {
		var state = ContraptionLODPersistentData.loadFromServer(server);
		state.applyToRegistries();
	}

	// Save

	public static void save(MinecraftServer server) {
		var state = ContraptionLODPersistentData.loadFromServer(server);
		state.captureFromRegistries();
	}

}

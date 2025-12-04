package net.saint.createrenderfixer;

/**
 * Lightweight runtime-tweakable flags for Flywheel/Create instancing mitigations.
 *
 * These are intentionally kept simple (static + volatile) so they can be flipped from client
 * commands without any serialization layer.
 */
public final class ModConfig {

	// Properties

	private static volatile boolean cacheDynamicInstances = true;
	private static volatile boolean freezeDistantInstances = true;
	private static volatile int freezeDistanceBlocks = 62; // 64 - 2 buffer

	// Accessors

	public static boolean cacheDynamicInstances() {
		return cacheDynamicInstances;
	}

	public static boolean freezeDistantInstances() {
		return freezeDistantInstances;
	}

	public static int freezeDistanceBlocks() {
		return freezeDistanceBlocks;
	}

	public static void setCacheDynamicInstances(boolean value) {
		cacheDynamicInstances = value;
		Mod.LOGGER.info("Instance data caching is now {}", value ? "ENABLED" : "DISABLED");
	}

	public static void setFreezeDistantInstances(boolean value) {
		freezeDistantInstances = value;
		Mod.LOGGER.info("Freezing distant instances is now {}", value ? "ENABLED" : "DISABLED");
	}

	public static void setFreezeDistanceBlocks(int blocks) {
		freezeDistanceBlocks = Math.max(0, blocks);
		Mod.LOGGER.info("Freeze distance set to {} blocks", freezeDistanceBlocks);
	}

	// Debug

	public static String debugDescription() {
		return "cacheDynamicInstances=" + cacheDynamicInstances + ", freezeDistantInstances=" + freezeDistantInstances
				+ ", freezeDistanceBlocks=" + freezeDistanceBlocks;
	}
}

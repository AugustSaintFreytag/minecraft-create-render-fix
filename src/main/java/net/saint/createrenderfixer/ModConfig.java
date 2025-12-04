package net.saint.createrenderfixer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

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

	private static volatile int freezeBlockDistance = 62; // 64 - 2 buffer

	private static final Set<ResourceLocation> freezeBlacklist = ConcurrentHashMap.newKeySet();

	// Accessors

	public static boolean cacheDynamicInstances() {
		return cacheDynamicInstances;
	}

	public static boolean freezeDistantInstances() {
		return freezeDistantInstances;
	}

	public static int freezeBlockDistance() {
		return freezeBlockDistance;
	}

	public static Set<ResourceLocation> freezeBlacklist() {
		return Collections.unmodifiableSet(freezeBlacklist);
	}

	public static void setCacheDynamicInstances(boolean value) {
		cacheDynamicInstances = value;
		Mod.LOGGER.info("Instance data caching set to {}", value ? "ENABLED" : "DISABLED");
		save();
	}

	public static void setFreezeDistantInstances(boolean value) {
		freezeDistantInstances = value;
		Mod.LOGGER.info("Freezing distant instances set to {}", value ? "ENABLED" : "DISABLED");
		save();
	}

	public static void setFreezeBlockDistance(int blocks) {
		freezeBlockDistance = Math.max(0, blocks);
		Mod.LOGGER.info("Freeze distance set to {} blocks", freezeBlockDistance);
		save();
	}

	public static void addFreezeBlacklist(ResourceLocation id) {
		if (freezeBlacklist.add(id)) {
			Mod.LOGGER.info("Added {} to freeze blacklist", id);
			save();
		}
	}

	public static void removeFreezeBlacklist(ResourceLocation id) {
		if (freezeBlacklist.remove(id)) {
			Mod.LOGGER.info("Removed {} from freeze blacklist", id);
			save();
		}
	}

	public static void clearFreezeBlacklist() {
		freezeBlacklist.clear();
		Mod.LOGGER.info("Cleared freeze blacklist");
		save();
	}

	public static boolean isFreezeBlacklisted(BlockEntityType<?> type) {
		var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
		return id != null && freezeBlacklist.contains(id);
	}

	// Debug

	public static String debugDescription() {
		return "cacheDynamicInstances=" + cacheDynamicInstances + ", freezeDistantInstances=" + freezeDistantInstances
				+ ", freezeDistanceBlocks=" + freezeBlockDistance + ", freezeBlacklist=" + freezeBlacklist;
	}

	// Persistence

	public static void load() {
		var data = ModConfigLoad.load(snapshot());
		applyLoadedData(data);
	}

	private static void save() {
		ModConfigLoad.save(snapshot());
	}

	private static ModConfigLoad.Data snapshot() {
		return new ModConfigLoad.Data(cacheDynamicInstances, freezeDistantInstances, freezeBlockDistance,
				freezeBlacklist.stream().map(ResourceLocation::toString).toList());
	}

	private static void applyLoadedData(ModConfigLoad.Data data) {
		cacheDynamicInstances = data.cacheDynamicInstances();
		freezeDistantInstances = data.freezeDistantInstances();
		freezeBlockDistance = Math.max(0, data.freezeBlockDistance());

		freezeBlacklist.clear();
		for (var id : data.freezeBlacklist()) {
			try {
				freezeBlacklist.add(new ResourceLocation(id));
			} catch (Exception exception) {
				Mod.LOGGER.warn("Skipping invalid resource id {} in config", id, exception);
			}
		}
	}
}

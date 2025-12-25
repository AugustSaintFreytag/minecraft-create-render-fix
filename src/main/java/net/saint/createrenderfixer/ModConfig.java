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

	private static volatile boolean forceDisableRateLimiting = false;

	private static volatile boolean cacheDynamicInstances = true;

	private static volatile boolean freezeDistantInstances = true;

	private static volatile boolean freezeOccludedInstances = true;

	private static volatile int freezeDistantInstancesRange = 62;

	private static volatile int entityLODDistanceOffset = 32;

	private static final Set<ResourceLocation> freezeBlacklist = ConcurrentHashMap.newKeySet();

	// Accessors

	public static boolean forceDisableRateLimiting() {
		return forceDisableRateLimiting;
	}

	public static boolean cacheDynamicInstances() {
		return cacheDynamicInstances;
	}

	public static boolean freezeDistantInstances() {
		return freezeDistantInstances;
	}

	public static boolean freezeOccludedInstances() {
		return freezeOccludedInstances;
	}

	public static int freezeDistantInstancesRange() {
		return freezeDistantInstancesRange;
	}

	public static Set<ResourceLocation> freezeBlacklist() {
		return Collections.unmodifiableSet(freezeBlacklist);
	}

	public static int entityLODDistanceOffset() {
		return entityLODDistanceOffset;
	}

	public static void setForceDisableRateLimiting(boolean value) {
		forceDisableRateLimiting = value;
		Mod.LOGGER.info("Force disable rate limiting set to {}", value ? "ENABLED" : "DISABLED");
		save();
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

	public static void setFreezeOccludedInstances(boolean value) {
		freezeOccludedInstances = value;
		Mod.LOGGER.info("Freezing occluded instances set to {}", value ? "ENABLED" : "DISABLED");
		save();
	}

	public static void setFreezeDistantInstancesRange(int blocks) {
		freezeDistantInstancesRange = Math.max(0, blocks);
		Mod.LOGGER.info("Freeze distance set to {} blocks", freezeDistantInstancesRange);
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

	public static void setEntityLODDistanceOffset(int value) {
		entityLODDistanceOffset = value;
		Mod.LOGGER.info("Entity distance LOD offset set to {}", value);
		save();
	}

	// Debug

	public static String debugDescription() {
		return "forceDisableRateLimiting=" + forceDisableRateLimiting + ", cacheDynamicInstances=" + cacheDynamicInstances
				+ ", freezeDistantInstances=" + freezeDistantInstances + ", freezeOccludedInstances=" + freezeOccludedInstances
				+ ", freezeDistanceBlocks=" + freezeDistantInstancesRange + ", freezeBlacklist=" + freezeBlacklist
				+ ", matchEntityDistanceWithLODs=" + entityLODDistanceOffset;
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
		return new ModConfigLoad.Data(cacheDynamicInstances, freezeDistantInstances, freezeOccludedInstances, freezeDistantInstancesRange,
				freezeBlacklist.stream().map(ResourceLocation::toString).toList());
	}

	private static void applyLoadedData(ModConfigLoad.Data data) {
		cacheDynamicInstances = data.cacheDynamicInstances();
		freezeDistantInstances = data.freezeDistantInstances();
		freezeOccludedInstances = data.freezeOccludedInstances();
		freezeDistantInstancesRange = Math.max(0, data.freezeBlockDistance());

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

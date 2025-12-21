package net.saint.createrenderfixer.data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;
import net.saint.createrenderfixer.ModConfig;

public final class ConfigProperties {

	// Properties

	public final boolean forceDisableRateLimiting;

	public final boolean cacheDynamicInstances;

	public final boolean freezeDistantInstances;

	public final boolean freezeOccludedInstances;

	public final int freezeBlockDistance;

	public final Set<ResourceLocation> freezeBlacklist;

	// Init

	public ConfigProperties(boolean forceDisableRateLimiting, boolean cacheDynamicInstances, boolean freezeDistantInstances,
			boolean freezeOccludedInstances, int freezeBlockDistance, Set<ResourceLocation> freezeBlacklist) {
		this.forceDisableRateLimiting = forceDisableRateLimiting;
		this.cacheDynamicInstances = cacheDynamicInstances;
		this.freezeDistantInstances = freezeDistantInstances;
		this.freezeOccludedInstances = freezeOccludedInstances;
		this.freezeBlockDistance = freezeBlockDistance;
		this.freezeBlacklist = ConcurrentHashMap.newKeySet();
		this.freezeBlacklist.addAll(freezeBlacklist);
	}

	public static ConfigProperties fromCurrentConfig() {
		return new ConfigProperties(ModConfig.forceDisableRateLimiting(), ModConfig.cacheDynamicInstances(),
				ModConfig.freezeDistantInstances(), ModConfig.freezeOccludedInstances(), ModConfig.freezeBlockDistance(),
				ModConfig.freezeBlacklist());
	}

}

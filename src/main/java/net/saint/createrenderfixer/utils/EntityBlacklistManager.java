package net.saint.createrenderfixer.utils;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.saint.createrenderfixer.Mod;

public final class EntityBlacklistManager {

	// Properties

	private static Set<ResourceLocation> entityBlacklist = new HashSet<>();

	// Access

	public static boolean isBlacklisted(ResourceLocation id) {
		return entityBlacklist.contains(id);
	}

	// Mutation

	public static void setBlacklistFromSet(Set<ResourceLocation> blacklist) {
		entityBlacklist = blacklist;
	}

	public static void setBlacklistFromEncodedString(String value) {
		entityBlacklist = blacklistSetFromString(value);
	}

	public static void reloadFromConfig() {
		setBlacklistFromEncodedString(Mod.CONFIG.freezeInstanceBlacklist);
	}

	// Utility

	private static Set<ResourceLocation> blacklistSetFromString(String value) {
		var blacklist = new HashSet<ResourceLocation>();
		var entries = value.split(",");

		for (String entry : entries) {
			String trimmed = entry.trim();
			if (!trimmed.isEmpty()) {
				blacklist.add(ResourceLocation.tryParse(trimmed));
			}
		}

		return blacklist;
	}

}

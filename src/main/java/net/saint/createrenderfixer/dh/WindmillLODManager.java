package net.saint.createrenderfixer.dh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

public final class WindmillLODManager {

	// State

	private static final Map<UUID, WindmillLODEntry> ENTRIES = new ConcurrentHashMap<>();

	// Registration

	public static void register(WindmillLODEntry entry) {
		if (entry == null) {
			return;
		}

		ENTRIES.put(entry.contraptionId(), entry);
	}

	public static void unregister(UUID contraptionId) {
		if (contraptionId == null) {
			return;
		}

		ENTRIES.remove(contraptionId);
	}

	public static void clearForWorld(String dimensionId) {
		if (dimensionId == null) {
			return;
		}

		ENTRIES.entrySet().removeIf(entry -> dimensionId.equals(entry.getValue().dimensionId()));
	}

	@Nullable
	public static WindmillLODEntry find(UUID contraptionId) {
		if (contraptionId == null) {
			return null;
		}

		return ENTRIES.get(contraptionId);
	}

	// Persistence

	public static List<WindmillLODEntry> snapshotPersistent() {
		var snapshot = new ArrayList<WindmillLODEntry>(ENTRIES.size());

		for (var entry : ENTRIES.values()) {
			snapshot.add(entry.createPersistenceSnapshot());
		}

		return snapshot;
	}

	public static void loadPersistent(List<WindmillLODEntry> entries) {
		ENTRIES.clear();

		if (entries == null || entries.isEmpty()) {
			return;
		}

		for (var entry : entries) {
			if (entry == null) {
				continue;
			}

			ENTRIES.put(entry.contraptionId(), entry);
		}
	}
}

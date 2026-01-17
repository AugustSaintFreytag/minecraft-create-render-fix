package net.saint.createge.dh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

public final class WindmillLODManager {

	// State

	private final Map<UUID, WindmillLODEntry> entries = new ConcurrentHashMap<>();

	// Access

	public Iterable<WindmillLODEntry> entries() {
		return entries.values();
	}

	// Registration

	public void register(WindmillLODEntry entry) {
		if (entry == null) {
			return;
		}

		var existing = entries.get(entry.contraptionId);

		if (existing != null) {
			if (existing.matchesRegistrationData(entry)) {
				existing.rotationSpeed = entry.rotationSpeed;
				existing.rotationAngle = entry.rotationAngle;
				existing.lastSynchronizationTick = entry.lastSynchronizationTick;

				return;
			}
		}

		entries.put(entry.contraptionId, entry);
	}

	public boolean unregister(UUID contraptionId) {
		if (contraptionId == null) {
			return false;
		}

		return entries.remove(contraptionId) != null;
	}

	public void clearForWorld(String dimensionId) {
		if (dimensionId == null) {
			return;
		}

		entries.entrySet().removeIf(entry -> dimensionId.equals(entry.getValue().dimensionId));
	}

	@Nullable
	public WindmillLODEntry find(UUID contraptionId) {
		if (contraptionId == null) {
			return null;
		}

		return entries.get(contraptionId);
	}

	// Persistence

	public List<WindmillLODEntry> snapshotPersistent() {
		var snapshot = new ArrayList<WindmillLODEntry>(entries.size());

		for (var entry : entries.values()) {
			snapshot.add(entry.createPersistenceSnapshot());
		}

		return snapshot;
	}

	public void loadPersistent(List<WindmillLODEntry> persistentEntries) {
		entries.clear();

		if (persistentEntries == null || persistentEntries.isEmpty()) {
			return;
		}

		for (var entry : persistentEntries) {
			if (entry == null) {
				continue;
			}

			entries.put(entry.contraptionId, entry);
		}
	}
}

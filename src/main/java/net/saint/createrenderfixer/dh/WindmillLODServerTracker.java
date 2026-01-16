package net.saint.createrenderfixer.dh;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.mixin.create.MechanicalBearingBlockEntityAccessor;
import net.saint.createrenderfixer.network.WindmillLODSyncUtil;

public final class WindmillLODServerTracker {

	// Ticking

	public static void tick(MinecraftServer server) {
		if (server == null) {
			return;
		}

		for (var level : server.getAllLevels()) {
			tickLevel(server, level);
		}
	}

	private static void tickLevel(MinecraftServer server, ServerLevel level) {
		var currentTick = level.getGameTime();

		if (currentTick % Mod.CONFIG.windmillSyncTickInterval != 0) {
			return;
		}

		var dimensionId = level.dimension().location().toString();

		for (var entry : WindmillLODManager.entries()) {
			if (!dimensionId.equals(entry.dimensionId)) {
				continue;
			}

			updateEntryFromLevel(server, level, entry, currentTick);
		}
	}

	private static void updateEntryFromLevel(MinecraftServer server, ServerLevel level, WindmillLODEntry entry, long currentTick) {
		var predictedAngle = getPredictedRotationAngleForEntry(entry, currentTick);
		var windmillBearing = getWindmillBearingForEntry(level, entry);

		if (removeEntryForWindmillIfNeeded(server, level, entry, windmillBearing)) {
			return;
		}

		if (!isChunkLoadedForEntry(level, entry)) {
			entry.isStale = true;
			updateEntryForNonTickingChunk(server, level, entry, windmillBearing, predictedAngle, currentTick);

			return;
		}

		updateEntryForTickingChunk(server, level, entry, windmillBearing, predictedAngle, currentTick);
		entry.isStale = false;
	}

	private static boolean removeEntryForWindmillIfNeeded(MinecraftServer server, ServerLevel level, WindmillLODEntry entry,
			WindmillBearingBlockEntity windmillBearing) {
		if (!isChunkAvailableForEntry(level, entry)) {
			return false;
		}

		if (windmillBearing == null) {
			removeWindmillEntry(server, entry, "BEARING_ABSENT");

			return true;
		}

		if (!isWindmillBearingAssembled(windmillBearing)) {
			removeWindmillEntry(server, entry, "DISASSEMBLED");

			return true;
		}

		return false;
	}

	// Debug

	public static int forceSetLoadedWindmillRotationAnglesToZero(ServerLevel level) {
		return forceSetLoadedWindmillRotationAnglesToZeroForLevel(level);
	}

	// Access

	private static void updateEntryForTickingChunk(MinecraftServer server, ServerLevel level, WindmillLODEntry entry,
			WindmillBearingBlockEntity windmillBearing, float predictedAngle, long currentTick) {
		var currentSpeed = getRotationSpeedForBearing(windmillBearing);
		var currentAngle = getRotationAngleForBearing(windmillBearing);

		if (shouldSynchronizeEntry(entry, currentSpeed, currentAngle)) {
			synchronizeEntry(server, entry, currentSpeed, predictedAngle, currentTick, "TICKING_UPDATE");
		}

		if (shouldOverrideBearing(entry, currentTick, currentSpeed, currentAngle, entry.rotationSpeed, predictedAngle)) {
			applyPredictedRotationToBearing(windmillBearing, entry, predictedAngle, "TICKING_UPDATE");
		}
	}

	private static void updateEntryForNonTickingChunk(MinecraftServer server, ServerLevel level, WindmillLODEntry entry,
			WindmillBearingBlockEntity windmillBearing, float predictedAngle, long currentTick) {
		var currentSpeed = getRotationSpeedForBearing(windmillBearing);
		var currentAngle = getRotationAngleForBearing(windmillBearing);

		if (shouldSynchronizeEntry(entry, currentSpeed, predictedAngle)) {
			synchronizeEntry(server, entry, currentSpeed, predictedAngle, currentTick, "NON_TICKING_UPDATE");
		}

		if (shouldOverrideBearing(entry, currentTick, currentSpeed, currentAngle, entry.rotationSpeed, predictedAngle)) {
			applyPredictedRotationToBearing(windmillBearing, entry, predictedAngle, "NON_TICKING_UPDATE");
		}
	}

	private static void removeWindmillEntry(MinecraftServer server, WindmillLODEntry entry, String reason) {
		if (server == null || entry == null) {
			return;
		}

		var removed = WindmillLODManager.unregister(entry.contraptionId);

		if (!removed) {
			return;
		}

		WindmillLODSyncUtil.broadcastRemovalPacket(server, entry.contraptionId);

	}

	// Overrides

	private static int forceSetLoadedWindmillRotationAnglesToZeroForLevel(ServerLevel level) {
		if (level == null) {
			return 0;
		}

		var dimensionId = level.dimension().location().toString();
		var updatedCount = 0;
		var currentTick = level.getGameTime();
		var server = level.getServer();

		for (var entry : WindmillLODManager.entries()) {
			if (!dimensionId.equals(entry.dimensionId)) {
				continue;
			}

			synchronizeEntry(server, entry, entry.rotationSpeed, 0.0F, currentTick, "FORCE_ZERO");
			updatedCount++;
		}

		return updatedCount;
		Mod.LOGGER.info("Removed stale windmill LOD entry for contraption '{}' due to '{}'.", entry.contraptionId, reason);
	}

	// Synchronization

	private static void synchronizeEntry(MinecraftServer server, WindmillLODEntry entry, float rotationSpeed, float rotationAngle,
			long currentTick, String reason) {
		entry.rotationSpeed = rotationSpeed;
		entry.rotationAngle = rotationAngle;
		entry.lastSynchronizationTick = currentTick;

		Mod.LOGGER.info("Updating entry for contraption '{}' due to '{}' and broadcasting to all clients.", entry.contraptionId, reason);
		WindmillLODSyncUtil.broadcastUpdatePacket(server, entry);
	}

	/**
	 * Determines if the entry should be updated and synchronized with changed values.
	 * 
	 * Checks if current angle and speed have shifted far enough from last stored values.
	 */
	private static boolean shouldSynchronizeEntry(WindmillLODEntry entry, float currentSpeed, float currentAngle) {
		var speedDelta = Math.abs(currentSpeed - entry.rotationSpeed);
		var angleDelta = getRotationDeltaForAngles(currentAngle, entry.rotationAngle);

		if (speedDelta < Mod.CONFIG.windmillRotationSpeedSyncThreshold && angleDelta < Mod.CONFIG.windmillRotationAngleSyncThreshold) {
			return false;
		}

		return true;
	}

	/**
	 * Determines if the active windmill bearing should be overridden with given values.
	 * 
	 * Checks if windmill bearing angle and speed have shifted far enough from given values.
	 */
	private static boolean shouldOverrideBearing(WindmillLODEntry entry, long currentTick, float currentSpeed, float currentAngle,
			float predictedSpeed, float predictedAngle) {
		if (entry == null) {
			return false;
		}

		if (!shouldAllowOverrideForEntryAge(entry, currentTick)) {
			return false;
		}

		var angleDelta = getRotationDeltaForAngles(currentAngle, predictedAngle);
		var speedDelta = Math.abs(currentSpeed - predictedSpeed);

		if (speedDelta < Mod.CONFIG.windmillRotationSpeedSyncThreshold && angleDelta < Mod.CONFIG.windmillRotationAngleSyncThreshold) {
			return false;
		}

		return true;
	}

	private static boolean shouldAllowOverrideForEntryAge(WindmillLODEntry entry, long currentTick) {
		var minimumAgeTicks = getMinimumOverrideAgeTicks();
		var entryAgeTicks = Math.max(0, currentTick - entry.tickRegistered);

		if (entryAgeTicks < minimumAgeTicks) {
			return false;
		}

		return true;
	}

	private static long getMinimumOverrideAgeTicks() {
		return (long) Mod.CONFIG.windmillSyncTickInterval * 4L;
	}

	// Utility (Rotation)

	private static float getRotationAngleForBearing(WindmillBearingBlockEntity windmillBearing) {
		if (windmillBearing == null) {
			return 0.0F;
		}

		return windmillBearing.getInterpolatedAngle(1.0F);
	}

	private static float getRotationSpeedForBearing(WindmillBearingBlockEntity windmillBearing) {
		if (windmillBearing == null) {
			return 0.0F;
		}

		return windmillBearing.getAngularSpeed();
	}

	private static boolean isWindmillBearingAssembled(WindmillBearingBlockEntity windmillBearing) {
		if (!(windmillBearing instanceof MechanicalBearingBlockEntityAccessor accessor)) {
			return true;
		}

		var movedContraption = accessor.getMovedContraption();
		return movedContraption != null;
	}

	private static void applyPredictedRotationToBearing(WindmillBearingBlockEntity windmillBearing, WindmillLODEntry entry,
			float rotationAngle, String reason) {
		if (windmillBearing == null) {
			return;
		}

		if (windmillBearing instanceof MechanicalBearingBlockEntityAccessor accessor) {
			var previousAngle = windmillBearing.getInterpolatedAngle(1.0f);

			windmillBearing.setAngle(rotationAngle);
			accessor.setPreviousAngle(rotationAngle);

			Mod.LOGGER.info("Applied windmill bearing contraption '{}' rotation angle {} -> {} due to '{}'.", entry.contraptionId,
					previousAngle, rotationAngle, reason);

			return;
		}
	}

	private static float getPredictedRotationAngleForEntry(WindmillLODEntry entry, long currentTick) {
		var tickDelta = currentTick - entry.lastSynchronizationTick;
		var predictedAngle = entry.rotationAngle + entry.rotationSpeed * tickDelta;

		return getWrappedDegrees(predictedAngle);
	}

	private static float getRotationDeltaForAngles(float previousAngle, float nextAngle) {
		var delta = Math.abs(previousAngle - nextAngle) % 360.0F;

		if (delta > 180.0F) {
			delta = 360.0F - delta;
		}

		return delta;
	}

	private static float getWrappedDegrees(float angle) {
		var wrapped = angle % 360.0F;

		if (wrapped < 0.0F) {
			wrapped += 360.0F;
		}

		return wrapped;
	}

	// Utility (Load Checks)

	private static boolean isChunkLoadedForEntry(ServerLevel level, WindmillLODEntry entry) {
		if (level == null || entry == null) {
			return false;
		}

		var chunkPosition = getChunkPositionForEntryAnchor(entry);
		var chunkSource = level.getChunkSource();

		if (chunkSource == null) {
			return false;
		}

		if (!chunkSource.hasChunk(chunkPosition.x, chunkPosition.z)) {
			return false;
		}

		if (!level.isPositionEntityTicking(entry.anchorPosition)) {
			return false;
		}

		return true;
	}

	private static WindmillBearingBlockEntity getWindmillBearingForEntry(ServerLevel level, WindmillLODEntry entry) {
		if (level == null || entry == null) {
			return null;
		}

		if (!isChunkAvailableForEntry(level, entry)) {
			return null;
		}

		var blockEntity = level.getBlockEntity(entry.anchorPosition);

		if (!(blockEntity instanceof WindmillBearingBlockEntity windmillBearing)) {
			return null;
		}

		return windmillBearing;
	}

	private static boolean isChunkAvailableForEntry(ServerLevel level, WindmillLODEntry entry) {
		var chunkPosition = getChunkPositionForEntryAnchor(entry);
		return level.shouldTickBlocksAt(chunkPosition.toLong());
	}

	private static ChunkPos getChunkPositionForEntryAnchor(WindmillLODEntry entry) {
		var anchorPosition = entry.anchorPosition;
		var chunkX = anchorPosition.getX() >> 4;
		var chunkZ = anchorPosition.getZ() >> 4;

		return new ChunkPos(chunkX, chunkZ);
	}
}

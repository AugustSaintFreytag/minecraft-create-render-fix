package net.saint.createrenderfixer.dh;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.mixin.create.MechanicalBearingBlockEntityAccessor;
import net.saint.createrenderfixer.network.WindmillLODSyncUtil;

public final class WindmillLODServerTracker {

	// Configuration

	private static final int SYNCHRONIZATION_INTERVAL_TICKS = 20;
	private static final float ROTATION_SYNC_THRESHOLD = 0.5F;
	private static final float SPEED_SYNC_THRESHOLD = 0.01F;

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

		if (currentTick % SYNCHRONIZATION_INTERVAL_TICKS != 0) {
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

		if (!isChunkLoadedForEntry(level, entry)) {
			entry.isStale = true;
			updateEntryForNonTickingChunk(server, level, entry, predictedAngle, currentTick);

			return;
		}

		updateEntryForTickingChunk(server, level, entry, predictedAngle, currentTick);
		entry.isStale = false;
	}

	// Debug

	public static int forceSetLoadedWindmillRotationAnglesToZero(ServerLevel level) {
		return forceSetLoadedWindmillRotationAnglesToZeroForLevel(level);
	}

	// Utility

	private static void updateEntryForTickingChunk(MinecraftServer server, ServerLevel level, WindmillLODEntry entry, float predictedAngle,
			long currentTick) {
		var windmillBearing = getWindmillBearingForEntry(level, entry);

		if (windmillBearing != null) {
			applyPredictedRotationToBearing(windmillBearing, entry, predictedAngle, "TICKING_ENTRY");
		}

		var rotationSpeed = windmillBearing != null ? windmillBearing.getAngularSpeed() : 0.0F;

		if (!shouldSynchronizeEntry(entry, rotationSpeed, predictedAngle)) {
			return;
		}

		synchronizeEntry(server, entry, rotationSpeed, predictedAngle, currentTick);
		Mod.LOGGER.info("Synchronized windmill LOD entry for contraption '{}' with predicted angle {} in ticking chunk.",
				entry.contraptionId, predictedAngle);
	}

	private static void updateEntryForNonTickingChunk(MinecraftServer server, ServerLevel level, WindmillLODEntry entry,
			float predictedAngle, long currentTick) {
		var windmillBearing = getWindmillBearingForEntry(level, entry);

		if (windmillBearing != null) {
			applyPredictedRotationToBearing(windmillBearing, entry, predictedAngle, "NON_TICKING_ENTRY");
		}

		var rotationSpeed = entry.rotationSpeed;

		if (!shouldSynchronizeEntry(entry, rotationSpeed, predictedAngle)) {
			return;
		}

		synchronizeEntry(server, entry, rotationSpeed, predictedAngle, currentTick);
		Mod.LOGGER.info("Synchronized windmill LOD entry for contraption '{}' with predicted angle {} in non-ticking chunk.",
				entry.contraptionId, predictedAngle);
	}

	private static float getPredictedRotationAngleForEntry(WindmillLODEntry entry, long currentTick) {
		var tickDelta = currentTick - entry.lastSynchronizationTick;
		var predictedAngle = entry.rotationAngle + entry.rotationSpeed * tickDelta;

		return wrapDegrees(predictedAngle);
	}

	private static void applyPredictedRotationToBearing(WindmillBearingBlockEntity windmillBearing, WindmillLODEntry entry,
			float rotationAngle, String reason) {
		if (windmillBearing == null) {
			return;
		}

		var previousAngle = windmillBearing.getInterpolatedAngle(1.0F);

		if (windmillBearing instanceof MechanicalBearingBlockEntityAccessor accessor) {
			windmillBearing.setAngle(rotationAngle);
			accessor.setPreviousAngle(rotationAngle);

			Mod.LOGGER.info("Overwrote windmill bearing angle from '{}' to '{}' for contraption '{}' due to '{}'.", previousAngle,
					rotationAngle, entry.contraptionId, reason);
			return;
		}

		Mod.LOGGER.info("Overwrote windmill bearing angle from '{}' to '{}' due to '{}'.", previousAngle, rotationAngle, reason);

	}

	private static void synchronizeEntry(MinecraftServer server, WindmillLODEntry entry, float rotationSpeed, float rotationAngle,
			long currentTick) {
		entry.rotationSpeed = rotationSpeed;
		entry.rotationAngle = rotationAngle;
		entry.lastSynchronizationTick = currentTick;

		WindmillLODSyncUtil.broadcastUpdatePacket(server, entry);
	}

	private static boolean shouldSynchronizeEntry(WindmillLODEntry entry, float rotationSpeed, float rotationAngle) {
		var speedDelta = Math.abs(rotationSpeed - entry.rotationSpeed);
		var angleDelta = getRotationDeltaForAngles(rotationAngle, entry.rotationAngle);

		if (speedDelta < SPEED_SYNC_THRESHOLD && angleDelta < ROTATION_SYNC_THRESHOLD) {
			return false;
		}

		return true;
	}

	private static float getRotationDeltaForAngles(float previousAngle, float nextAngle) {
		var delta = Math.abs(previousAngle - nextAngle) % 360.0F;

		if (delta > 180.0F) {
			delta = 360.0F - delta;
		}

		return delta;
	}

	private static float wrapDegrees(float angle) {
		var wrapped = angle % 360.0F;

		if (wrapped < 0.0F) {
			wrapped += 360.0F;
		}

		return wrapped;
	}

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

			synchronizeEntry(server, entry, entry.rotationSpeed, 0.0F, currentTick);
			updatedCount++;
		}

		return updatedCount;
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
		if (level == null || entry == null) {
			return false;
		}

		var chunkPosition = getChunkPositionForEntryAnchor(entry);
		var chunkSource = level.getChunkSource();

		if (chunkSource == null) {
			return false;
		}

		return chunkSource.hasChunk(chunkPosition.x, chunkPosition.z);
	}

	private static ChunkPos getChunkPositionForEntryAnchor(WindmillLODEntry entry) {
		var anchorPosition = entry.anchorPosition;
		var chunkX = anchorPosition.getX() >> 4;
		var chunkZ = anchorPosition.getZ() >> 4;

		return new ChunkPos(chunkX, chunkZ);
	}
}

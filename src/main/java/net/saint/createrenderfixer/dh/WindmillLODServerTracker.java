package net.saint.createrenderfixer.dh;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.mixin.create.MechanicalBearingBlockEntityAccessor;
import net.saint.createrenderfixer.network.WindmillLODSyncUtil;
import net.saint.createrenderfixer.utils.BlockTickingUtil;

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

		for (var entry : Mod.WINDMILL_LOD_MANAGER.entries()) {
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

		if (!isEntityInTickablePosition(level, entry)) {
			updateEntryForNonTicking(server, level, entry, windmillBearing, predictedAngle, currentTick);
			return;
		}

		updateEntryForTicking(server, level, entry, windmillBearing, predictedAngle, currentTick);
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

		return false;
	}

	// Debug

	public static int forceSetLoadedWindmillRotationAnglesToZero(ServerLevel level) {
		return forceSetLoadedWindmillRotationAnglesToZeroForLevel(level);
	}

	// Access

	private static void updateEntryForTicking(MinecraftServer server, ServerLevel level, WindmillLODEntry entry,
			WindmillBearingBlockEntity windmillBearing, float predictedAngle, long currentTick) {
		var assertedAngle = 0f;

		if (shouldOverrideBearing(windmillBearing, entry, currentTick, entry.rotationSpeed, predictedAngle, true)) {
			applyRotationToBearing(windmillBearing, entry, predictedAngle, "TICKING_UPDATE");
			assertedAngle = predictedAngle;
		}

		if (shouldUpdateEntry(windmillBearing, entry, currentTick, true)) {
			var currentSpeed = getRotationSpeedForBearing(windmillBearing);
			var currentAngle = getRotationAngleForBearing(windmillBearing);

			if (assertedAngle != 0f && assertedAngle != currentAngle) {
				// If this ever logs, the bearing does not return a value it was just set to.
				Mod.LOGGER.error("Asserted angle mismatch. Set to {} but returned {}.", predictedAngle, currentAngle);
			}

			entry.isStale = false;
			synchronizeEntry(server, entry, currentSpeed, currentAngle, currentTick, "TICKING_UPDATE");
		}
	}

	private static void updateEntryForNonTicking(MinecraftServer server, ServerLevel level, WindmillLODEntry entry,
			WindmillBearingBlockEntity windmillBearing, float predictedAngle, long currentTick) {
		if (shouldUpdateEntry(windmillBearing, entry, currentTick, false)) {
			entry.isStale = true;
			synchronizeEntry(server, entry, entry.rotationSpeed, predictedAngle, currentTick, "NON_TICKING_UPDATE");
		}

		if (shouldOverrideBearing(windmillBearing, entry, currentTick, entry.rotationSpeed, predictedAngle, false)) {
			applyRotationToBearing(windmillBearing, entry, predictedAngle, "NON_TICKING_UPDATE");
		}
	}

	private static void removeWindmillEntry(MinecraftServer server, WindmillLODEntry entry, String reason) {
		if (server == null || entry == null) {
			return;
		}

		var removed = Mod.WINDMILL_LOD_MANAGER.unregister(entry.contraptionId);

		if (!removed) {
			return;
		}

		WindmillLODSyncUtil.sendRemovalPacketToAllPlayers(server, entry.contraptionId);
		Mod.LOGGER.info("Removed stale windmill LOD entry for contraption '{}' due to '{}'.", entry.contraptionId, reason);
	}

	// Synchronization

	private static void synchronizeEntry(MinecraftServer server, WindmillLODEntry entry, float rotationSpeed, float rotationAngle,
			long currentTick, String reason) {
		entry.rotationSpeed = rotationSpeed;
		entry.rotationAngle = rotationAngle;
		entry.lastSynchronizationTick = currentTick;

		Mod.LOGGER.info("Updated entry for contraption '{}' (speed {}, angle {}, last tick {}) due to '{}'.", entry.contraptionId,
				rotationSpeed, rotationAngle, currentTick, reason);
		WindmillLODSyncUtil.sendUpdatePacketToAllPlayers(server, entry);
	}

	/**
	 * Determines if the active windmill bearing should be overridden with given values.
	 * 
	 * Checks if windmill bearing angle and speed have shifted far enough from given values.
	 */
	private static boolean shouldOverrideBearing(WindmillBearingBlockEntity windmillBearing, WindmillLODEntry entry, long currentTick,
			float predictedSpeed, float predictedAngle, boolean isTicking) {
		if (windmillBearing == null || entry == null || !entry.isStale) {
			return false;
		}

		var level = (ServerLevel) windmillBearing.getLevel();

		if (!isBearingNearAnyPlayer(level, windmillBearing)) {
			return false;
		}

		var currentSpeed = getRotationSpeedForBearing(windmillBearing);
		var currentAngle = getRotationAngleForBearing(windmillBearing);

		var angleDelta = getRotationDeltaForAngles(currentAngle, predictedAngle);
		var speedDelta = Math.abs(currentSpeed - predictedSpeed);

		if (speedDelta < Mod.CONFIG.windmillRotationSpeedSyncThreshold && angleDelta < Mod.CONFIG.windmillRotationAngleSyncThreshold) {
			return false;
		}

		return true;
	}

	/**
	 * Determines if the entry should be updated and synchronized with changed values.
	 * 
	 * Checks if current angle and speed have shifted far enough from last stored values.
	 */
	private static boolean shouldUpdateEntry(WindmillBearingBlockEntity windmillBearing, WindmillLODEntry entry, long currentTick,
			boolean isTicking) {
		if (isTicking && entry.isStale || !isTicking && !entry.isStale) {
			return true;
		}

		// Can be optimized to be split into "everything as predicted" and "things have changed".
		// Can split into `shouldUpdateEntry` and `shouldBroadcastEntry` where broadcasting is
		// unnecessary if only rotation has changed.

		var hasEntityLODDrift = false;

		if (windmillBearing != null) {
			var currentSpeed = getRotationSpeedForBearing(windmillBearing);
			var currentAngle = getRotationAngleForBearing(windmillBearing);

			var speedDelta = Math.abs(currentSpeed - entry.rotationSpeed);
			var angleDelta = getRotationDeltaForAngles(currentAngle, entry.rotationAngle);

			if (speedDelta > Mod.CONFIG.windmillRotationSpeedSyncThreshold || angleDelta > Mod.CONFIG.windmillRotationAngleSyncThreshold) {
				hasEntityLODDrift = true;
			}
		}

		if (currentTick - entry.lastSynchronizationTick > Mod.CONFIG.windmillSyncTickInterval * 8) {
			return true;
		}

		if (hasEntityLODDrift) {
			return true;
		}

		return false;
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

		for (var entry : Mod.WINDMILL_LOD_MANAGER.entries()) {
			if (!dimensionId.equals(entry.dimensionId)) {
				continue;
			}

			synchronizeEntry(server, entry, entry.rotationSpeed, 0.0F, currentTick, "FORCE_ZERO");
			updatedCount++;
		}

		return updatedCount;
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
		// TODO: Can re-introduce an "is disassembled" task if possible.

		if (!(windmillBearing instanceof MechanicalBearingBlockEntityAccessor accessor)) {
			return true;
		}

		var movedContraption = accessor.getMovedContraption();
		return movedContraption != null;
	}

	private static void applyRotationToBearing(WindmillBearingBlockEntity windmillBearing, WindmillLODEntry entry, float rotationAngle,
			String reason) {
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

	private static boolean isBearingNearAnyPlayer(ServerLevel level, WindmillBearingBlockEntity windmillBearing) {
		if (windmillBearing == null) {
			return false;
		}

		var server = level.getServer();
		var viewDistance = server.getPlayerList().getViewDistance();
		var viewDistanceOffset = 4;

		for (var player : level.players()) {
			var windmillPosition = windmillBearing.getBlockPosition().getCenter();
			var distance = Math.sqrt(player.distanceToSqr(windmillPosition));

			if (distance <= (viewDistance + viewDistanceOffset) * 16) {
				return true;
			}
		}

		return false;
	}

	private static boolean isEntityInTickablePosition(ServerLevel level, WindmillLODEntry entry) {
		if (level == null || entry == null || entry.anchorPosition == null) {
			return false;
		}

		if (!BlockTickingUtil.isPositionTicking(level, entry.anchorPosition)) {
			return false;
		}

		return true;
	}

	private static WindmillBearingBlockEntity getWindmillBearingForEntry(ServerLevel level, WindmillLODEntry entry) {
		if (level == null || entry == null || entry.anchorPosition == null) {
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

package net.saint.createrenderfixer.dh;

import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
			if (!dimensionId.equals(entry.dimensionId())) {
				continue;
			}

			updateEntryFromLevel(server, level, entry, currentTick);
		}
	}

	private static void updateEntryFromLevel(MinecraftServer server, ServerLevel level, WindmillLODEntry entry, long currentTick) {
		var blockEntity = level.getBlockEntity(entry.anchorPosition());

		if (!(blockEntity instanceof WindmillBearingBlockEntity windmillBearing)) {
			return;
		}

		var rotationSpeed = windmillBearing.getAngularSpeed();
		var rotationAngle = windmillBearing.getInterpolatedAngle(1.0F);
		var speedDelta = Math.abs(rotationSpeed - entry.rotationSpeed());
		var angleDelta = getRotationDeltaForAngles(rotationAngle, entry.rotationAngle());

		if (speedDelta < SPEED_SYNC_THRESHOLD && angleDelta < ROTATION_SYNC_THRESHOLD) {
			return;
		}

		entry.setRotationSpeed(rotationSpeed);
		entry.setRotationAngle(rotationAngle);
		entry.setLastSynchronizationTick(currentTick);

		WindmillLODSyncUtil.broadcastUpdatePacket(server, entry);
	}

	// Utility

	private static float getRotationDeltaForAngles(float previousAngle, float nextAngle) {
		var delta = Math.abs(previousAngle - nextAngle) % 360.0F;

		if (delta > 180.0F) {
			delta = 360.0F - delta;
		}

		return delta;
	}
}

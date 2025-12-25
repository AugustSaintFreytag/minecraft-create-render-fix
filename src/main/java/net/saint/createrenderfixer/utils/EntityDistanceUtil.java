package net.saint.createrenderfixer.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.saint.createrenderfixer.ModConfig;

public final class EntityDistanceUtil {

	private static final double MAX_DISTANCE_FACTOR = 64.0;

	public static boolean shouldRenderAtPosition(Entity entity, Vec3 position) {
		var entityPosition = entity.blockPosition();

		var dx = entityPosition.getX() - (int) position.x;
		var dy = entityPosition.getY() - (int) position.y;
		var dz = entityPosition.getZ() - (int) position.z;

		var renderDistanceBlocks = EntityDistanceUtil.getMaxUnboundedDistanceForWorld();
		var deferredOffset = 0.28125 * renderDistanceBlocks + 52;

		var offsetSq = deferredOffset + ModConfig.entityLODDistanceOffset();
		var distanceSq = (dx * dx + dy * dy + dz * dz) + offsetSq;
		var shouldRender = shouldRenderAtSqrDistance(entity, distanceSq);

		return shouldRender;
	}

	public static boolean shouldRenderAtSqrDistance(Entity entity, double distance) {
		var maxDistance = getMaxDistanceSqr(entity);
		return distance < maxDistance;
	}

	public static double getMaxDistanceSqr(Entity entity) {
		var size = getSize(entity);

		var maxDistance = size * MAX_DISTANCE_FACTOR * Entity.getViewScale();
		maxDistance = getMaxDistanceForWorld(maxDistance);

		return maxDistance * maxDistance;
	}

	public static double getSize(Entity entity) {
		var size = entity.getBoundingBox().getSize();

		if (Double.isNaN(size)) {
			return 1.0;
		}

		return Math.min(size, 1.0);
	}

	private static double getMaxDistanceForWorld(double maxDistance) {
		var chunkDistance = getMaxUnboundedDistanceForWorld();
		return Math.min(maxDistance, chunkDistance);
	}

	public static int getMaxUnboundedDistanceForWorld() {
		var client = Minecraft.getInstance();

		if (client == null || client.options == null) {
			return 0;

		}

		var chunkDistance = client.options.getEffectiveRenderDistance() * 16;
		return chunkDistance;
	}

}

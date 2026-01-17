package net.saint.createge.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.saint.createge.Mod;

public final class EntityDistanceUtil {

	// API

	public static boolean shouldRenderAtPosition(Entity entity, Vec3 position) {
		if (entity == null || position == null) {
			return true;
		}

		var maxDistance = getMaxDistanceForWorld();

		if (maxDistance <= 0) {
			return false;
		}

		var distance = getHorizontalDistance(entity, position.x, position.z);

		return distance <= maxDistance;
	}

	public static boolean shouldRenderAtPosition(Entity entity, double cameraX, double cameraZ) {
		if (entity == null) {
			return true;
		}

		var maxDistance = getMaxDistanceForWorld();

		if (maxDistance <= 0) {
			return false;
		}

		var distance = getHorizontalDistance(entity, cameraX, cameraZ);

		return distance <= maxDistance;
	}

	public static int getMaxUnboundedDistanceForWorld() {
		var client = Minecraft.getInstance();

		if (client == null || client.options == null) {
			return 0;
		}

		var chunkDistance = client.options.getEffectiveRenderDistance() * 16;

		return chunkDistance;
	}

	// Utility

	private static int getMaxDistanceForWorld() {
		var renderDistanceBlocks = getMaxUnboundedDistanceForWorld();
		var offsetBlocks = Mod.CONFIG.entityLODDistanceOffset;
		var maxDistance = renderDistanceBlocks - offsetBlocks;

		if (maxDistance > renderDistanceBlocks) {
			maxDistance = renderDistanceBlocks;
		}

		if (maxDistance < 0) {
			maxDistance = 0;
		}

		return maxDistance;
	}

	private static double getHorizontalDistance(Entity entity, double cameraX, double cameraZ) {
		var xDifference = Math.abs(entity.getX() - cameraX);
		var zDifference = Math.abs(entity.getZ() - cameraZ);

		return Math.max(xDifference, zDifference);
	}

}

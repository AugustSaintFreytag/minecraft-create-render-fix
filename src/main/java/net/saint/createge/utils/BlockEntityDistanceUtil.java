package net.saint.createge.utils;

import net.minecraft.core.BlockPos;
import net.saint.createge.Mod;

public final class BlockEntityDistanceUtil {

	// Configuration

	private static final double BLOCK_CENTER_OFFSET = 0.5;

	// API

	public static boolean shouldRenderAtPosition(BlockPos position, double cameraX, double cameraZ) {
		if (position == null) {
			return true;
		}

		var maxDistance = getMaxDistanceForWorld();

		if (maxDistance <= 0) {
			return false;
		}

		var distance = getHorizontalDistance(position, cameraX, cameraZ);

		return distance <= maxDistance;
	}

	// Utility

	private static int getMaxDistanceForWorld() {
		var renderDistanceBlocks = EntityDistanceUtil.getMaxUnboundedDistanceForWorld();
		var offsetBlocks = Mod.CONFIG.blockEntityLODDistanceOffset;
		var maxDistance = renderDistanceBlocks - offsetBlocks;

		if (maxDistance < 0) {
			maxDistance = 0;
		}

		return maxDistance;
	}

	private static double getHorizontalDistance(BlockPos position, double cameraX, double cameraZ) {
		var deltaX = Math.abs(position.getX() + BLOCK_CENTER_OFFSET - cameraX);
		var deltaZ = Math.abs(position.getZ() + BLOCK_CENTER_OFFSET - cameraZ);

		return Math.max(deltaX, deltaZ);
	}
}

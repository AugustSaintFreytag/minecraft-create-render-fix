package net.saint.createge.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class BlockTickingUtil {

	public static boolean isEntityTicking(ServerLevel level, Entity entity) {
		if (entity == null) {
			return false;
		}

		var blockPosition = entity.blockPosition();

		if (blockPosition == null) {
			return false;
		}

		return isPositionTicking(level, blockPosition);
	}

	public static boolean isPositionTicking(ServerLevel level, BlockPos blockPosition) {
		return level.isPositionEntityTicking(blockPosition);
	}

}

package net.saint.createrenderfixer.dh;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Utility for registering contraptions when worlds/entities load.
 */
public final class ContraptionRegistrationUtil {

	public static void registerAllWorldEntities(ServerLevel level) {
		for (var entity : level.getAllEntities()) {
			tryRegister(level, entity);
		}
	}

	public static void tryRegister(Level level, Entity entity) {
		if (!(level instanceof ServerLevel)) {
			return;
		}

		if (entity instanceof AbstractContraptionEntity contraption) {
			ContraptionBlockRegistry.register(contraption);
		}
	}
}

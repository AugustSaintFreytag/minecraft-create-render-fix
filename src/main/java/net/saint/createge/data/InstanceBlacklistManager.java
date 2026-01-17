package net.saint.createge.data;

import java.util.WeakHashMap;
import java.util.function.Function;

import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.saint.createge.mixin.BlockEntityInstanceAccessor;
import net.saint.createge.utils.EntityBlacklistManager;

public final class InstanceBlacklistManager {

	// State

	public static WeakHashMap<DynamicInstance, Boolean> blacklistedInstanceMap = new WeakHashMap<>();

	// Blacklist

	public static void clear() {
		blacklistedInstanceMap.clear();
	}

	public static boolean getCachedStatus(DynamicInstance instance) {
		if (blacklistedInstanceMap.containsKey(instance)) {
			return blacklistedInstanceMap.get(instance);
		}

		var isInstanceBlacklisted = isInstanceBlacklisted(instance);
		blacklistedInstanceMap.put(instance, isInstanceBlacklisted);

		return isInstanceBlacklisted;
	}

	private static boolean isInstanceBlacklisted(DynamicInstance instance) {
		return withBlockFromInstance(instance, block -> {
			if (block == null) {
				return false;
			}

			var blockId = BuiltInRegistries.BLOCK.getKey(block);
			var blockIsBlacklisted = EntityBlacklistManager.isBlacklisted(blockId);

			return blockIsBlacklisted;
		}, false);
	}

	private static <T> T withBlockFromInstance(DynamicInstance instance, Function<Block, T> returnBlock, T defaultValue) {
		if (instance instanceof BlockEntityInstance<?> blockEntityInstance) {
			var blockEntityAccessor = (BlockEntityInstanceAccessor<BlockEntity>) blockEntityInstance;
			var blockEntity = blockEntityAccessor.getBlockEntity();
			var block = blockEntity.getBlockState().getBlock();

			if (block == null) {
				return defaultValue;
			}

			return returnBlock.apply(block);
		}

		return defaultValue;
	}

}

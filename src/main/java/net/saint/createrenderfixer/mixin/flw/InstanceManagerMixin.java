package net.saint.createrenderfixer.mixin.flw;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.AbstractInstance;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.TaskEngine;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstanceManager;
import com.jozufozu.flywheel.backend.instancing.ratelimit.DistanceUpdateLimiter;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.utils.BlockEntityDistanceUtil;
import net.saint.createrenderfixer.utils.FreezeConditionUtil;

/**
 * Applies distance-based update throttling and render culling for Flywheel instances.
 */
@Mixin(value = InstanceManager.class, remap = false)
public abstract class InstanceManagerMixin {

	// Properties

	@Shadow
	protected DistanceUpdateLimiter frame;

	@Shadow
	@Final
	protected Map<Object, AbstractInstance> instances;

	@Shadow
	protected abstract void removeInternal(Object obj, AbstractInstance instance);

	@Shadow
	public abstract void queueAdd(Object obj);

	@Unique
	private final Set<BlockEntity> crf$culledBlockEntities = new HashSet<>();

	// Injections

	@Inject(method = "tick", at = @At("HEAD"))
	private void crf$applyBlockEntityDistanceCulling(TaskEngine taskEngine, double cameraX, double cameraY, double cameraZ,
			CallbackInfo callbackInfo) {
		crf$applyBlockEntityDistanceCulling(cameraX, cameraZ);
	}

	@Inject(method = "updateInstance", at = @At("HEAD"), cancellable = true)
	private void crf$updateInstance(DynamicInstance dynamicInstance, float lookX, float lookY, float lookZ, int cX, int cY, int cZ,
			CallbackInfo callbackInfo) {
		if (!crf$shouldAllowUpdateInstance(dynamicInstance, lookX, lookY, lookZ, cX, cY, cZ)) {
			callbackInfo.cancel();
			return;
		}

		if (!dynamicInstance.decreaseFramerateWithDistance()) {
			dynamicInstance.beginFrame();
			callbackInfo.cancel();
			return;
		}

		var worldPos = dynamicInstance.getWorldPosition();
		var dX = worldPos.getX() - cX;
		var dY = worldPos.getY() - cY;
		var dZ = worldPos.getZ() - cZ;

		if (frame.shouldUpdate(dX, dY, dZ)) {
			dynamicInstance.beginFrame();
		}

		callbackInfo.cancel();
	}

	// Culling

	@Unique
	private void crf$applyBlockEntityDistanceCulling(double cameraX, double cameraZ) {
		if (!((Object) this instanceof BlockEntityInstanceManager)) {
			return;
		}

		if (!Mod.CONFIG.limitBlockEntityRenderDistance) {
			crf$restoreCulledBlockEntities();

			return;
		}

		var blockEntitiesToCull = crf$getBlockEntitiesToCull(cameraX, cameraZ);

		for (var blockEntity : blockEntitiesToCull) {
			var instance = instances.get(blockEntity);

			if (instance == null) {
				continue;
			}

			removeInternal(blockEntity, instance);
			crf$culledBlockEntities.add(blockEntity);
		}

		crf$updateCulledBlockEntities(cameraX, cameraZ);
	}

	@Unique
	private void crf$updateCulledBlockEntities(double cameraX, double cameraZ) {
		if (crf$culledBlockEntities.isEmpty()) {
			return;
		}

		var iterator = crf$culledBlockEntities.iterator();

		while (iterator.hasNext()) {
			var blockEntity = iterator.next();

			if (!crf$isBlockEntityReady(blockEntity)) {
				iterator.remove();
				continue;
			}

			if (instances.containsKey(blockEntity)) {
				iterator.remove();
				continue;
			}

			var position = blockEntity.getBlockPos();

			if (!BlockEntityDistanceUtil.shouldRenderAtPosition(position, cameraX, cameraZ)) {
				continue;
			}

			queueAdd(blockEntity);
		}
	}

	@Unique
	private void crf$restoreCulledBlockEntities() {
		if (crf$culledBlockEntities.isEmpty()) {
			return;
		}

		var iterator = crf$culledBlockEntities.iterator();

		while (iterator.hasNext()) {
			var blockEntity = iterator.next();

			if (!crf$isBlockEntityReady(blockEntity)) {
				iterator.remove();
				continue;
			}

			if (instances.containsKey(blockEntity)) {
				iterator.remove();
				continue;
			}

			queueAdd(blockEntity);
		}
	}

	@Unique
	private ArrayList<BlockEntity> crf$getBlockEntitiesToCull(double cameraX, double cameraZ) {
		var blockEntitiesToCull = new ArrayList<BlockEntity>();

		for (var entry : instances.entrySet()) {
			if (!(entry.getKey() instanceof BlockEntity blockEntity)) {
				continue;
			}

			if (!crf$isBlockEntityReady(blockEntity)) {
				continue;
			}

			var position = blockEntity.getBlockPos();

			if (BlockEntityDistanceUtil.shouldRenderAtPosition(position, cameraX, cameraZ)) {
				continue;
			}

			blockEntitiesToCull.add(blockEntity);
		}

		return blockEntitiesToCull;
	}

	// Utility

	private boolean crf$shouldAllowUpdateInstance(DynamicInstance dynamicInstance, float lookX, float lookY, float lookZ, int cX, int cY,
			int cZ) {
		if (Mod.isInstanceBlacklisted(dynamicInstance)) {
			return true;
		}

		var position = dynamicInstance.getWorldPosition();

		if (FreezeConditionUtil.shouldFreezeAtPosition(position, cX, cY, cZ)) {
			return false;
		}

		return true;
	}

	@Unique
	private boolean crf$isBlockEntityReady(BlockEntity blockEntity) {
		if (blockEntity == null) {
			return false;
		}

		if (blockEntity.isRemoved()) {
			return false;
		}

		var level = blockEntity.getLevel();

		if (level == null) {
			return false;
		}

		return true;
	}
}

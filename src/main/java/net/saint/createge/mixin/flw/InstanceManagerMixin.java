package net.saint.createge.mixin.flw;

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
import net.saint.createge.Mod;
import net.saint.createge.utils.BlockEntityDistanceUtil;
import net.saint.createge.utils.FreezeConditionUtil;

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
	private final Set<BlockEntity> cge$culledBlockEntities = new HashSet<>();

	// Injections

	@Inject(method = "tick", at = @At("HEAD"))
	private void cge$applyBlockEntityDistanceCulling(TaskEngine taskEngine, double cameraX, double cameraY, double cameraZ,
			CallbackInfo callbackInfo) {
		cge$applyBlockEntityDistanceCulling(cameraX, cameraZ);
	}

	@Inject(method = "updateInstance", at = @At("HEAD"), cancellable = true)
	private void cge$updateInstance(DynamicInstance dynamicInstance, float lookX, float lookY, float lookZ, int cX, int cY, int cZ,
			CallbackInfo callbackInfo) {
		if (!cge$shouldAllowUpdateInstance(dynamicInstance, lookX, lookY, lookZ, cX, cY, cZ)) {
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
	private void cge$applyBlockEntityDistanceCulling(double cameraX, double cameraZ) {
		if (!((Object) this instanceof BlockEntityInstanceManager)) {
			return;
		}

		if (!Mod.CONFIG.limitBlockEntityRenderDistance) {
			cge$restoreCulledBlockEntities();

			return;
		}

		var blockEntitiesToCull = cge$getBlockEntitiesToCull(cameraX, cameraZ);

		for (var blockEntity : blockEntitiesToCull) {
			var instance = instances.get(blockEntity);

			if (instance == null) {
				continue;
			}

			removeInternal(blockEntity, instance);
			cge$culledBlockEntities.add(blockEntity);
		}

		cge$updateCulledBlockEntities(cameraX, cameraZ);
	}

	@Unique
	private void cge$updateCulledBlockEntities(double cameraX, double cameraZ) {
		if (cge$culledBlockEntities.isEmpty()) {
			return;
		}

		var iterator = cge$culledBlockEntities.iterator();

		while (iterator.hasNext()) {
			var blockEntity = iterator.next();

			if (!cge$isBlockEntityReady(blockEntity)) {
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
	private void cge$restoreCulledBlockEntities() {
		if (cge$culledBlockEntities.isEmpty()) {
			return;
		}

		var iterator = cge$culledBlockEntities.iterator();

		while (iterator.hasNext()) {
			var blockEntity = iterator.next();

			if (!cge$isBlockEntityReady(blockEntity)) {
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
	private ArrayList<BlockEntity> cge$getBlockEntitiesToCull(double cameraX, double cameraZ) {
		var blockEntitiesToCull = new ArrayList<BlockEntity>();

		for (var entry : instances.entrySet()) {
			if (!(entry.getKey() instanceof BlockEntity blockEntity)) {
				continue;
			}

			if (!cge$isBlockEntityReady(blockEntity)) {
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

	private boolean cge$shouldAllowUpdateInstance(DynamicInstance dynamicInstance, float lookX, float lookY, float lookZ, int cX, int cY,
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
	private boolean cge$isBlockEntityReady(BlockEntity blockEntity) {
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

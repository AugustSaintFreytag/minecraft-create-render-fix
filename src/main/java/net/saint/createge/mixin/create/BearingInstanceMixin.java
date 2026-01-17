package net.saint.createge.mixin.create;

import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.mojang.math.Axis;
import com.simibubi.create.content.contraptions.bearing.BearingInstance;
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.saint.createge.Mod;
import net.saint.createge.mixin.BlockEntityInstanceAccessor;

@Mixin(BearingInstance.class)
public abstract class BearingInstanceMixin {

	// Properties

	@Shadow
	@Final
	private OrientedData topInstance;

	@Shadow
	@Final
	private Axis rotationAxis;

	@Shadow
	@Final
	private Quaternionf blockOrientation;

	@Unique
	private float cge$lastAngle = Float.NaN;

	// Injections

	@Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
	private void cge$cacheAndFreeze(CallbackInfo callbackInfo) {
		if (!Mod.CONFIG.cacheDynamicInstances) {
			return;
		}

		var accessor = (BlockEntityInstanceAccessor<?>) this;
		var blockEntity = (IBearingBlockEntity) accessor.getBlockEntity();
		var angle = blockEntity.getInterpolatedAngle(AnimationTickHolder.getPartialTicks() - 1);

		if (!cge$shouldUpdate(angle)) {
			callbackInfo.cancel();
			return;
		}

		var rotation = rotationAxis.rotationDegrees(angle);
		rotation.mul(blockOrientation);
		topInstance.setRotation(rotation);

		cge$lastAngle = angle;
		callbackInfo.cancel();
	}

	@Unique
	private boolean cge$shouldUpdate(float angle) {
		return Float.isNaN(cge$lastAngle) || Math.abs(cge$lastAngle - angle) > 1.0e-5f;
	}
}

package net.saint.createrenderfixer.mixin.create;

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

import net.saint.createrenderfixer.Mod;

@Mixin(BearingInstance.class)
public abstract class BearingInstanceMixin {

	@Shadow
	@Final
	private OrientedData topInstance;

	@Shadow
	@Final
	private Axis rotationAxis;

	@Shadow
	@Final
	private Quaternionf blockOrientation;

	@Shadow
	@Final
	protected IBearingBlockEntity blockEntity;

	@Unique
	private float crf$lastAngle = Float.NaN;

	@Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
	private void crf$cacheAndFreeze(CallbackInfo callbackInfo) {
		if (!Mod.configProperties.cacheDynamicInstances) {
			return;
		}

		var angle = blockEntity.getInterpolatedAngle(AnimationTickHolder.getPartialTicks() - 1);

		if (!crf$shouldUpdate(angle)) {
			callbackInfo.cancel();
			return;
		}

		var rotation = rotationAxis.rotationDegrees(angle);
		rotation.mul(blockOrientation);
		topInstance.setRotation(rotation);

		crf$lastAngle = angle;
		callbackInfo.cancel();
	}

	@Unique
	private boolean crf$shouldUpdate(float angle) {
		return Float.isNaN(crf$lastAngle) || Math.abs(crf$lastAngle - angle) > 1.0e-5f;
	}
}

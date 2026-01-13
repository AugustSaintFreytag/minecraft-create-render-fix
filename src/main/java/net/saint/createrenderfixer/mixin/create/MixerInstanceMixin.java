package net.saint.createrenderfixer.mixin.create;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MixerInstance;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.saint.createrenderfixer.Mod;

@Mixin(MixerInstance.class)
public abstract class MixerInstanceMixin {

	// Properties

	@Shadow
	@Final
	private RotatingData mixerHead;

	@Shadow
	@Final
	private OrientedData mixerPole;

	@Shadow
	@Final
	private MechanicalMixerBlockEntity mixer;

	// Accessors

	@Shadow
	protected abstract float getRenderedHeadOffset();

	// State

	@Unique
	private float crf$lastOffset = Float.NaN;
	@Unique
	private float crf$lastHeadSpeed = Float.NaN;

	// Injections

	@Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
	private void crf$cacheAndFreeze(CallbackInfo callbackInfo) {
		if (!Mod.CONFIG.cacheDynamicInstances) {
			return;
		}

		var offset = getRenderedHeadOffset();
		var headSpeed = mixer.getRenderedHeadRotationSpeed(AnimationTickHolder.getPartialTicks()) * 2;

		var updateOffset = crf$shouldUpdateOffset(offset);
		var updateSpeed = crf$shouldUpdateSpeed(headSpeed);

		var instance = (MixerInstance) (Object) this;
		var instancePosition = instance.getInstancePosition();

		if (instancePosition == null) {
			return;
		}

		if (updateOffset) {
			mixerPole.setPosition(instancePosition).nudge(0, -offset, 0);
		}

		if (updateOffset || updateSpeed) {
			mixerHead.setPosition(instancePosition).nudge(0, -offset, 0).setRotationalSpeed(headSpeed);
			crf$lastOffset = offset;
			crf$lastHeadSpeed = headSpeed;
		}

		callbackInfo.cancel();
	}

	@Unique
	private boolean crf$shouldUpdateOffset(float offset) {
		return Float.isNaN(crf$lastOffset) || Math.abs(crf$lastOffset - offset) > 1.0e-5f;
	}

	@Unique
	private boolean crf$shouldUpdateSpeed(float headSpeed) {
		return Float.isNaN(crf$lastHeadSpeed) || Math.abs(crf$lastHeadSpeed - headSpeed) > 1.0e-5f;
	}
}

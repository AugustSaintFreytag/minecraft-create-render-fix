package net.saint.createge.mixin.create;

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

import net.saint.createge.Mod;

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
	private float cge$lastOffset = Float.NaN;
	@Unique
	private float cge$lastHeadSpeed = Float.NaN;

	// Injections

	@Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
	private void cge$cacheAndFreeze(CallbackInfo callbackInfo) {
		if (!Mod.CONFIG.cacheDynamicInstances) {
			return;
		}

		var offset = getRenderedHeadOffset();
		var headSpeed = mixer.getRenderedHeadRotationSpeed(AnimationTickHolder.getPartialTicks()) * 2;

		var updateOffset = cge$shouldUpdateOffset(offset);
		var updateSpeed = cge$shouldUpdateSpeed(headSpeed);

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
			cge$lastOffset = offset;
			cge$lastHeadSpeed = headSpeed;
		}

		callbackInfo.cancel();
	}

	@Unique
	private boolean cge$shouldUpdateOffset(float offset) {
		return Float.isNaN(cge$lastOffset) || Math.abs(cge$lastOffset - offset) > 1.0e-5f;
	}

	@Unique
	private boolean cge$shouldUpdateSpeed(float headSpeed) {
		return Float.isNaN(cge$lastHeadSpeed) || Math.abs(cge$lastHeadSpeed - headSpeed) > 1.0e-5f;
	}
}

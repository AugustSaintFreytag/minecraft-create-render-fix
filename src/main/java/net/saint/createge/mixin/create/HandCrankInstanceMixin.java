package net.saint.createge.mixin.create;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankInstance;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.core.Direction;
import net.saint.createge.Mod;
import net.saint.createge.mixin.BlockEntityInstanceAccessor;

@Mixin(HandCrankInstance.class)
public abstract class HandCrankInstanceMixin {

	@Shadow
	private ModelData crank;

	@Shadow
	@Final
	private Direction facing;

	@Unique
	private float cge$lastAngle = Float.NaN;

	@Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
	private void cge$cacheAndFreeze(CallbackInfo callbackInfo) {
		if (!Mod.CONFIG.cacheDynamicInstances) {
			return;
		}

		if (crank == null) {
			return;
		}

		var accessor = (BlockEntityInstanceAccessor<HandCrankBlockEntity>) this;
		var blockEntity = accessor.getBlockEntity();
		var angle = blockEntity.getIndependentAngle(AnimationTickHolder.getPartialTicks());

		if (!cge$shouldUpdate(angle)) {
			callbackInfo.cancel();
			return;
		}

		var axis = facing.getAxis();
		var instancePosition = ((HandCrankInstance) (Object) this).getInstancePosition();

		crank.loadIdentity().translate(instancePosition).centre().rotate(Direction.get(Direction.AxisDirection.POSITIVE, axis), angle)
				.unCentre();

		cge$lastAngle = angle;
		callbackInfo.cancel();
	}

	@Unique
	private boolean cge$shouldUpdate(float angle) {
		return Float.isNaN(cge$lastAngle) || Math.abs(cge$lastAngle - angle) > 1.0e-5f;
	}
}

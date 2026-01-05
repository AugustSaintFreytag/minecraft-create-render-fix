package net.saint.createrenderfixer.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;

@Mixin(WindmillBearingBlockEntity.class)
public abstract class WindmillBearingBlockEntityMixin {

	@Inject(method = "getGeneratedSpeed", at = @At("RETURN"), cancellable = true)
	private void crf$getGeneratedSpeed(CallbackInfoReturnable<Float> callbackInfo) {
		var originalSpeed = callbackInfo.getReturnValueF();

		if (originalSpeed == 0.0F) {
			return;
		}

		var accessor = (MechanicalBearingBlockEntityAccessor) this;
		var movedContraption = accessor.getMovedContraption();

		if (movedContraption == null) {
			return;
		}

		var contraption = movedContraption.getContraption();

		if (contraption == null) {
			return;
		}

		var numberOfSailBlocks = ((BearingContraption) contraption).getSailBlocks();
		var factor = getDiminishingReturnFactor(numberOfSailBlocks);
		var adjustedSpeed = originalSpeed * factor;

		callbackInfo.setReturnValue(adjustedSpeed);
	}

	private float getDiminishingReturnFactor(int numberOfSailBlocks) {
		var numberOfSailGroups = (float) numberOfSailBlocks / (float) AllConfigs.server().kinetics.windmillSailsPerRPM.get();
		var factor = (float) (1f - ((Math.pow((numberOfSailGroups + 1f), -0.5f) - 1f) / -2f));

		return factor;
	}

}

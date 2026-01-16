package net.saint.createrenderfixer.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.saint.createrenderfixer.utils.MathUtil;

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
		var numberOfSailBlocksPerGroup = AllConfigs.server().kinetics.windmillSailsPerRPM.get();
		var numberOfSailGroups = (float) MathUtil.clamp(numberOfSailBlocks / numberOfSailBlocksPerGroup, 1, 16);

		var minFactor = 0.0f;
		var maxFactor = 2.0f;

		var rawFactor = minFactor + (maxFactor - minFactor) / (1.0f + 0.65f * (numberOfSailGroups - 2.5f));
		var factor = MathUtil.clamp(rawFactor, 0.25f, 1.0f);

		return factor;
	}

}

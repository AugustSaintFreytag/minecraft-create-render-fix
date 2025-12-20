package net.saint.createrenderfixer.mixin.flw;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jozufozu.flywheel.backend.instancing.ratelimit.BandedPrimeLimiter;

import net.saint.createrenderfixer.ModConfig;

@Mixin(BandedPrimeLimiter.class)
public abstract class BandedPrimeLimiterMixin {

	@Inject(method = "shouldUpdate", at = @At("HEAD"), cancellable = true, remap = false)
	private void crf$shouldUpdate(int dX, int dY, int dZ, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (ModConfig.forceDisableRateLimiting()) {
			callbackInfo.setReturnValue(true);
		}
	}

}

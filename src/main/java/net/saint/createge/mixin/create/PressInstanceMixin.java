package net.saint.createge.mixin.create;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressInstance;

import net.saint.createge.Mod;
import net.saint.createge.mixin.BlockEntityInstanceAccessor;

@Mixin(PressInstance.class)
public abstract class PressInstanceMixin {

	// Properties

	@Shadow
	@Final
	private OrientedData pressHead;

	@Shadow
	protected abstract float getRenderedHeadOffset(MechanicalPressBlockEntity press);

	@Unique
	private float cge$lastHeadOffset = Float.NaN;

	// Injections

	@Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, remap = false)
	private void cge$cacheAndFreeze(CallbackInfo callbackInfo) {
		if (!Mod.CONFIG.cacheDynamicInstances) {
			return;
		}

		var instance = (PressInstance) (Object) this;
		var blockEntityAccessor = (BlockEntityInstanceAccessor<MechanicalPressBlockEntity>) instance;
		var blockEntity = blockEntityAccessor.getBlockEntity();
		var offset = getRenderedHeadOffset(blockEntity);

		if (cge$shouldUpdate(offset)) {
			var instancePosition = instance.getInstancePosition();

			if (instancePosition == null) {
				return;
			}

			pressHead.setPosition(instancePosition).nudge(0, -offset, 0);
			cge$lastHeadOffset = offset;
		}

		callbackInfo.cancel();
	}

	@Unique
	private boolean cge$shouldUpdate(float offset) {
		return Float.isNaN(cge$lastHeadOffset) || Math.abs(cge$lastHeadOffset - offset) > 1.0e-5f;
	}
}

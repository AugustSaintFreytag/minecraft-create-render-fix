package net.saint.createrenderfixer.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ContraptionRenderInfo;

import net.saint.createrenderfixer.ModConfig;
import net.saint.createrenderfixer.utils.EntityDistanceUtil;

@Mixin(value = ContraptionRenderInfo.class, remap = false)
public abstract class ContraptionRenderInfoMixin {

	// Properties

	@Shadow()
	public Contraption contraption;

	@Shadow()
	private boolean visible;

	// Injections

	@Inject(method = "beginFrame", at = @At("TAIL"))
	private void crf$applyEntityDistanceCulling(BeginFrameEvent event, CallbackInfo callbackInfo) {
		if (!ModConfig.limitEntityRenderDistance() || !visible) {
			return;
		}

		var entity = contraption.entity;

		if (entity == null) {
			return;
		}

		var position = event.getCameraPos();
		var shouldRenderContraption = EntityDistanceUtil.shouldRenderAtPosition(entity, position);

		visible = shouldRenderContraption;
	}
}

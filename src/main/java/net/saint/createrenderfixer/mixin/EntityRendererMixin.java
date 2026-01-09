package net.saint.createrenderfixer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.saint.createrenderfixer.ModConfig;
import net.saint.createrenderfixer.utils.EntityDistanceUtil;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void crf$useChunkRenderDistance(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ,
			CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!ModConfig.limitEntityRenderDistance()) {
			return;
		}

		if (!EntityDistanceUtil.shouldRenderAtPosition(entity, cameraX, cameraZ)) {
			callbackInfo.setReturnValue(false);

			return;
		}

		var bounds = entity.getBoundingBoxForCulling();

		if (!frustum.isVisible(bounds)) {
			callbackInfo.setReturnValue(false);

			return;
		}

		callbackInfo.setReturnValue(true);
	}
}

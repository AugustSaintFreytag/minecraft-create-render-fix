package net.saint.createge.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.saint.createge.dh.ContraptionBlockRegistry;

@Mixin(AbstractContraptionEntity.class)
public abstract class AbstractContraptionEntityMixin {

	@Inject(method = "contraptionInitialize", at = @At("TAIL"))
	private void cge$contraptionInitialize(CallbackInfo callbackInfo) {
		if (!FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			return;
		}

		var contraption = (AbstractContraptionEntity) (Object) this;

		if (!isServerSideEntity(contraption)) {
			return;
		}

		ContraptionBlockRegistry.register(contraption);
	}

	@Inject(method = "remove", at = @At("HEAD"))
	private void cge$onRemove(Entity.RemovalReason reason, CallbackInfo callbackInfo) {
		if (!FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			return;
		}

		if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
			return;
		}

		var contraption = (AbstractContraptionEntity) (Object) this;

		if (!isServerSideEntity(contraption)) {
			return;
		}

		ContraptionBlockRegistry.unregister(contraption);
	}

	@Inject(method = "disassemble", at = @At("HEAD"))
	private void cge$onDisassemble(CallbackInfo callbackInfo) {
		if (!FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			return;
		}

		var contraption = (AbstractContraptionEntity) (Object) this;

		if (!isServerSideEntity(contraption)) {
			return;
		}

		ContraptionBlockRegistry.unregister(contraption);
	}

	private static boolean isServerSideEntity(Entity entity) {
		if (entity.level() instanceof ServerLevel) {
			return true;
		}

		return false;
	}
}

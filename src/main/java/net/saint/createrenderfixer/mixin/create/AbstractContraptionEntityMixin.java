package net.saint.createrenderfixer.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.saint.createrenderfixer.dh.ContraptionBlockRegistry;

@Mixin(AbstractContraptionEntity.class)
public abstract class AbstractContraptionEntityMixin {

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void crf$onReadAdditionalSaveData(CompoundTag compoundTag, CallbackInfo callbackInfo) {
		if (!FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			return;
		}

		var contraption = (AbstractContraptionEntity) (Object) this;
		ContraptionBlockRegistry.register(contraption);
	}

	@Inject(method = "remove", at = @At("HEAD"))
	private void crf$onRemove(Entity.RemovalReason reason, CallbackInfo callbackInfo) {
		if (!FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			return;
		}

		var contraption = (AbstractContraptionEntity) (Object) this;
		ContraptionBlockRegistry.unregister(contraption.getUUID());
	}

	@Inject(method = "disassemble", at = @At("HEAD"))
	private void crf$onDisassemble(CallbackInfo callbackInfo) {
		if (!FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			return;
		}

		var contraption = (AbstractContraptionEntity) (Object) this;
		ContraptionBlockRegistry.unregister(contraption.getUUID());
	}
}

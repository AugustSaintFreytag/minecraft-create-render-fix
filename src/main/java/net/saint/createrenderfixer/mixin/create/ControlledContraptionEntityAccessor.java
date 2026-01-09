package net.saint.createrenderfixer.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;

import net.minecraft.core.BlockPos;

@Mixin(ControlledContraptionEntity.class)
public interface ControlledContraptionEntityAccessor {

	@Accessor("controllerPos")
	BlockPos getControllerPosition();
}

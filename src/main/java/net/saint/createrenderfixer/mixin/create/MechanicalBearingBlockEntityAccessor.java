package net.saint.createrenderfixer.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;

@Mixin(MechanicalBearingBlockEntity.class)
public interface MechanicalBearingBlockEntityAccessor {

	@Accessor("movedContraption")
	ControlledContraptionEntity getMovedContraption();

	@Accessor("prevAngle")
	void setPreviousAngle(float previousAngle);
}

package net.saint.createrenderfixer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockEntityInstance.class)
public interface BlockEntityInstanceAccessor<T extends BlockEntity> {

	@Accessor("blockEntity")
	T getBlockEntity();

	@Accessor("pos")
	BlockPos getPos();

	@Accessor("instancePos")
	BlockPos getInstancePos();

	@Accessor("blockState")
	BlockState getBlockState();

}

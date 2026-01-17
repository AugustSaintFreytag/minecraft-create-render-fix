package net.saint.createge.mixin.flw;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.core.crumbling.CrumblingRenderer;
import com.jozufozu.flywheel.event.RenderLayerEvent;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

@Mixin(value = CrumblingRenderer.class, remap = false)
public abstract class MixinCrumblingRenderer {

	// Configuration

	// Disable crumbling overlay for these Flywheel block entities
	private static final Set<ResourceLocation> DISABLED_BLOCKS = Set.of(new ResourceLocation("create", "millstone"),
			new ResourceLocation("create", "mechanical_piston"), new ResourceLocation("create", "water_wheel"),
			new ResourceLocation("create", "large_water_wheel"));

	// Drop the overlay for every Flywheel block entity
	// It seems block-specific checking is broken, always disable for all Flywheel blocks to be
	// safe.
	private static final boolean DISABLE_ALL = true;

	// Injection

	@Inject(method = "renderCrumbling", at = @At("HEAD"), cancellable = true)
	private static void cge$renderCrumbling(Int2ObjectMap<List<BlockEntity>> activeStages, Camera camera, RenderLayerEvent event,
			CallbackInfo callbackInfo) {
		var shouldRender = false;

		for (var entry : activeStages.int2ObjectEntrySet()) {
			entry.getValue().removeIf(MixinCrumblingRenderer::cge$shouldSkip);

			if (!entry.getValue().isEmpty()) {
				shouldRender = true;
			}
		}

		if (!shouldRender) {
			callbackInfo.cancel();
		}
	}

	private static boolean cge$shouldSkip(BlockEntity blockEntity) {
		if (DISABLE_ALL) {
			return true;
		}

		var id = BlockEntityType.getKey(blockEntity.getType());
		return id != null && DISABLED_BLOCKS.contains(id);
	}
}

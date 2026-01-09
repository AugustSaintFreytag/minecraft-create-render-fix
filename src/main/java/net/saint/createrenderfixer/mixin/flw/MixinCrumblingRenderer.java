package net.saint.createrenderfixer.mixin.flw;

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
			new ResourceLocation("create", "mechanical_piston"), new ResourceLocation("create", "water_wheel"));

	// Drop the overlay for every Flywheel block entity
	private static final boolean DISABLE_ALL = false;

	// Injection

	@Inject(method = "renderCrumbling", at = @At("HEAD"), cancellable = true)
	private static void crf$renderCrumbling(Int2ObjectMap<List<BlockEntity>> activeStages, Camera camera, RenderLayerEvent event,
			CallbackInfo ci) {
		boolean shouldRender = false;

		for (var entry : activeStages.int2ObjectEntrySet()) {
			entry.getValue().removeIf(MixinCrumblingRenderer::crf$shouldSkip);
			if (!entry.getValue().isEmpty()) {
				shouldRender = true;
			}
		}

		if (!shouldRender) {
			ci.cancel();
		}
	}

	private static boolean crf$shouldSkip(BlockEntity be) {
		if (DISABLE_ALL) {
			return true;
		}

		ResourceLocation id = BlockEntityType.getKey(be.getType());
		return id != null && DISABLED_BLOCKS.contains(id);
	}
}

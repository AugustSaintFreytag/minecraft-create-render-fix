package net.saint.createrenderfixer.mixin.createdeco;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.github.talrey.createdeco.api.MeshFences;

import net.minecraft.client.renderer.RenderType;

@Mixin(MeshFences.class)
public class MeshFencesMixin {

	@ModifyArg(method = "build", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/builders/BlockBuilder;addLayer(Ljava/util/function/Supplier;)Lcom/tterrag/registrate/builders/BlockBuilder;"), index = 0, remap = false)
	private static Supplier<Supplier<RenderType>> redirectMeshFenceLayer(Supplier<?> original) {
		return () -> () -> RenderType.cutout();
	}
}

package net.saint.createge.mixin.createdeco;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.github.talrey.createdeco.api.Catwalks;

import net.minecraft.client.renderer.RenderType;

@Mixin(Catwalks.class)
public class CatwalksMixin {

	@ModifyArg(method = "buildStair", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/builders/BlockBuilder;addLayer(Ljava/util/function/Supplier;)Lcom/tterrag/registrate/builders/BlockBuilder;"), index = 0, remap = false)
	private static Supplier<Supplier<RenderType>> redirectBuildStairLayer(Supplier<?> original) {
		return () -> () -> RenderType.cutout();
	}

	@ModifyArg(method = "buildRailing", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/builders/BlockBuilder;addLayer(Ljava/util/function/Supplier;)Lcom/tterrag/registrate/builders/BlockBuilder;"), index = 0, remap = false)
	private static Supplier<Supplier<RenderType>> redirectBuildRailingLayer(Supplier<?> original) {
		return () -> () -> RenderType.cutout();
	}
}

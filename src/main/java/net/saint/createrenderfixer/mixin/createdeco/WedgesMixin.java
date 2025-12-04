package net.saint.createrenderfixer.mixin.createdeco;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.github.talrey.createdeco.api.Wedges;

import net.minecraft.client.renderer.RenderType;

@Mixin(Wedges.class)
public class WedgesMixin {

	@ModifyArg(method = "build", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/builders/BlockBuilder;addLayer(Ljava/util/function/Supplier;)Lcom/tterrag/registrate/builders/BlockBuilder;"), index = 0, remap = false)
	private static Supplier<Supplier<RenderType>> redirectWedgeLayer(Supplier<?> original) {
		return () -> () -> RenderType.cutout();
	}
}

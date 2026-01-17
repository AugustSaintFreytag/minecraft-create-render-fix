package net.saint.createge;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.loader.api.FabricLoader;

public class ModMixinPlugin implements IMixinConfigPlugin {

	private static final Map<String, String> REQUIRED_MODS = Map.of("com.simibubi.create.", "create", "com.github.talrey.createdeco.",
			"createdeco", "com.jozufozu.flywheel.", "create");

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		for (var entry : REQUIRED_MODS.entrySet()) {
			if (targetClassName.startsWith(entry.getKey())) {
				return FabricLoader.getInstance().isModLoaded(entry.getValue());
			}
		}

		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return List.of();
	}

	@Override
	public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName,
			IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName,
			IMixinInfo mixinInfo) {
	}

}

package net.saint.createrenderfixer;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.saint.createrenderfixer.client.WindmillLODRenderManager;
import net.saint.createrenderfixer.dh.WindmillLODManager;
import net.saint.createrenderfixer.dh.WindmillLODMaterialManager;
import net.saint.createrenderfixer.network.WindmillLODSyncUtil;

public final class ModClient implements ClientModInitializer {

	// State

	public static WindmillLODManager WINDMILL_LOD_MANAGER;
	public static WindmillLODMaterialManager WINDMILL_LOD_MATERIAL_MANAGER;

	// Init

	@Override
	public void onInitializeClient() {
		// Logger

		Mod.LOGGER.setAsClient();

		// Distant Horizons

		if (FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			initializeDistantHorizonsInterop();
		}
	}

	private void initializeDistantHorizonsInterop() {
		WINDMILL_LOD_MANAGER = new WindmillLODManager();
		WINDMILL_LOD_MATERIAL_MANAGER = new WindmillLODMaterialManager();

		reloadWindmillMaterialManagerFromConfig();
		registerConfigReloadListener();

		WindmillLODSyncUtil.initClient();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			var level = client.level;

			if (level == null) {
				return;
			}

			var partialTicks = Minecraft.getInstance().getFrameTime();
			WindmillLODRenderManager.tick(level, partialTicks);
		});
	}

	// Config

	private static void registerConfigReloadListener() {
		AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((config, data) -> {
			reloadWindmillMaterialManagerFromConfig();
			return null;
		});
	}

	private static void reloadWindmillMaterialManagerFromConfig() {
		var materialManager = WINDMILL_LOD_MATERIAL_MANAGER;

		if (materialManager == null) {
			return;
		}

		materialManager.reloadFromConfig();
	}
}

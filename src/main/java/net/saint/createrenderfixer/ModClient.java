package net.saint.createrenderfixer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.saint.createrenderfixer.client.WindmillLODRenderManager;
import net.saint.createrenderfixer.network.WindmillLODSyncUtil;

public final class ModClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		WindmillLODSyncUtil.initClient();

		if (!FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			return;
		}

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			var level = client.level;

			if (level == null) {
				return;
			}

			var partialTicks = Minecraft.getInstance().getFrameTime();
			WindmillLODRenderManager.tick(level, partialTicks);
		});
	}
}

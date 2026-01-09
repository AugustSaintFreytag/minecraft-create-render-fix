package net.saint.createrenderfixer;

import com.jozufozu.flywheel.api.instance.DynamicInstance;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.saint.createrenderfixer.data.InstanceBlacklistManager;
import net.saint.createrenderfixer.dh.ContraptionBlockRegistry;
import net.saint.createrenderfixer.dh.ContraptionPersistencyUtil;
import net.saint.createrenderfixer.dh.ContraptionRegistrationUtil;
import net.saint.createrenderfixer.dh.DhBridge;
import net.saint.createrenderfixer.dh.DhChunkProcessingHandler;
import net.saint.createrenderfixer.dh.WindmillLODServerTracker;
import net.saint.createrenderfixer.network.WindmillLODSyncUtil;
import net.saint.createrenderfixer.utils.Logger;

public class Mod implements ModInitializer {

	// Configuration

	public static final String MOD_ID = "create-render-fixer";
	public static final String MOD_NAME = "Create Render Fixer";

	// References

	public static final Logger LOGGER = Logger.create(MOD_NAME);

	// Init

	@Override
	public void onInitialize() {
		ModConfig.load();
		ModCommands.init();

		if (FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			initDistantHorizonsInterop();
		}
	}

	private void initDistantHorizonsInterop() {
		DhBridge.init();
		DhChunkProcessingHandler.init();
		WindmillLODSyncUtil.initServer();

		ServerLifecycleEvents.SERVER_STARTED.register(ContraptionPersistencyUtil::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(ContraptionPersistencyUtil::save);
		ServerTickEvents.END_SERVER_TICK.register(WindmillLODServerTracker::tick);

		ServerWorldEvents.UNLOAD.register((server, world) -> {
			ContraptionBlockRegistry.clearForWorld(world.dimension().location().toString());
			WindmillLODSyncUtil.broadcastFullSyncPacket(server);
		});

		ServerWorldEvents.LOAD.register((server, world) -> ContraptionRegistrationUtil.scanWorld(world));

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> ContraptionRegistrationUtil.tryRegister(world, entity));
	}

	// Access

	public static boolean isInstanceBlacklisted(DynamicInstance instance) {
		return InstanceBlacklistManager.getCachedStatus(instance);
	}

}

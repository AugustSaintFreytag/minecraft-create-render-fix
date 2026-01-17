package net.saint.createrenderfixer;

import com.jozufozu.flywheel.api.instance.DynamicInstance;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
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
import net.saint.createrenderfixer.dh.WindmillLODManager;
import net.saint.createrenderfixer.dh.WindmillLODServerTracker;
import net.saint.createrenderfixer.network.WindmillLODSyncUtil;
import net.saint.createrenderfixer.utils.BlockTickingUtil;
import net.saint.createrenderfixer.utils.EntityBlacklistManager;
import net.saint.createrenderfixer.utils.Logger;

public class Mod implements ModInitializer {

	// Configuration

	public static final String MOD_ID = "create-render-fixer";
	public static final String MOD_NAME = "Create Render Fixer";

	// References

	public static final Logger LOGGER = Logger.create(MOD_NAME);

	public static ModConfig CONFIG;

	// State

	public static WindmillLODManager WINDMILL_LOD_MANAGER;

	// Init

	@Override
	public void onInitialize() {
		// Config

		AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

		AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((config, data) -> {
			EntityBlacklistManager.reloadFromConfig();

			return null;
		});

		// Commands

		ModCommands.init();

		// Distant Horizons

		if (FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			initializeDistantHorizonsInterop();
		}

		// Load

		EntityBlacklistManager.reloadFromConfig();
	}

	private void initializeDistantHorizonsInterop() {
		WINDMILL_LOD_MANAGER = new WindmillLODManager();

		DhBridge.init();
		DhChunkProcessingHandler.init();
		WindmillLODSyncUtil.initServer();

		ServerLifecycleEvents.SERVER_STARTED.register(ContraptionPersistencyUtil::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(ContraptionPersistencyUtil::save);
		ServerTickEvents.END_SERVER_TICK.register(WindmillLODServerTracker::tick);

		ServerWorldEvents.UNLOAD.register((server, world) -> {
			ContraptionBlockRegistry.clearForWorld(world.dimension().location().toString());
			WindmillLODSyncUtil.sendLoadPacketToAllPlayers(server);
		});

		ServerWorldEvents.LOAD.register((server, world) -> {
			ContraptionRegistrationUtil.registerAllWorldEntities(world);
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (!BlockTickingUtil.isEntityTicking(world, entity)) {
				return;
			}

			ContraptionRegistrationUtil.tryRegister(world, entity);
		});
	}

	// Access

	public static boolean isInstanceBlacklisted(DynamicInstance instance) {
		return InstanceBlacklistManager.getCachedStatus(instance);
	}

}

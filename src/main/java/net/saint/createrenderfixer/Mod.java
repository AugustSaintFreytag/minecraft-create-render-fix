package net.saint.createrenderfixer;

import com.jozufozu.flywheel.api.instance.DynamicInstance;

import net.fabricmc.api.ModInitializer;
import net.saint.createrenderfixer.data.InstanceBlacklistManager;
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
	}

	// Config

	public static void reloadConfigProperties() {
		synchronized (Mod.class) {
			InstanceBlacklistManager.clear();
		}
	}

	public static boolean isInstanceBlacklisted(DynamicInstance instance) {
		return InstanceBlacklistManager.getCachedStatus(instance);
	}

}

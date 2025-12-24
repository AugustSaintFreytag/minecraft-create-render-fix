package net.saint.createrenderfixer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jozufozu.flywheel.api.instance.DynamicInstance;

import net.fabricmc.api.ModInitializer;
import net.saint.createrenderfixer.data.InstanceBlacklistManager;

public class Mod implements ModInitializer {

	// Configuration

	public static final String MOD_ID = "create-render-fixer";

	// References

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// State


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

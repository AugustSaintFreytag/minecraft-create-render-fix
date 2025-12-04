package net.saint.createrenderfixer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class Mod implements ModInitializer {
	// Configuration

	public static final String MOD_ID = "create-render-fixer";

	// References

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	// Init

	@Override
	public void onInitialize() {
		ModConfig.load();
		ModCommands.init();
	}
}

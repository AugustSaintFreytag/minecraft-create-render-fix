package net.saint.createge;

import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.saint.createge.dh.ContraptionBlockRegistry;
import net.saint.createge.dh.WindmillLODServerTracker;

/**
 * Registers server-side commands to modify config values.
 */
public final class ModCommands {

	private ModCommands() {
	}

	// Init

	public static void init() {
		CommandRegistrationCallback.EVENT.register(ModCommands::register);
	}

	// Registration

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
			Commands.CommandSelection environment) {
		dispatcher.register(buildRoot());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
		return Commands.literal("create-rf")
				// Debug
				.then(Commands.literal("debug")
						.then(Commands.literal("setWindmillRotationZero").executes(ModCommands::setWindmillRotationZeroForServer))
						.then(Commands.literal("reregisterLoadedWindmills").executes(ModCommands::reregisterLoadedWindmills)));
	}

	private static int setWindmillRotationZeroForServer(CommandContext<CommandSourceStack> ctx) {
		var source = ctx.getSource();
		var level = (ServerLevel) source.getPlayer().level();
		var updatedCount = WindmillLODServerTracker.forceSetLoadedWindmillRotationAnglesToZero(level);

		source.sendSuccess(() -> Component.literal("Set windmill rotation angle to zero for " + updatedCount + " loaded entries."), false);

		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}

	private static int reregisterLoadedWindmills(CommandContext<CommandSourceStack> ctx) {
		var source = ctx.getSource();
		var server = source.getServer();
		var updatedCount = new AtomicInteger(0);

		for (var level : server.getAllLevels()) {
			var numberOfUpdatedEntries = ContraptionBlockRegistry.reregisterLoadedWindmillEntities(level);
			updatedCount.getAndAdd(numberOfUpdatedEntries);
		}

		source.sendSuccess(() -> Component.literal("Re-registered '" + updatedCount.get() + "' loaded windmill entities."), false);

		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}
}

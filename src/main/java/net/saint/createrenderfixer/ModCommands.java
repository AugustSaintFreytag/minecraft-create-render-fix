package net.saint.createrenderfixer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.saint.createrenderfixer.dh.WindmillLODServerTracker;

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
						.then(Commands.literal("setWindmillRotationZero").executes(ModCommands::setWindmillRotationZeroForServer)));
	}

	private static int setWindmillRotationZeroForServer(CommandContext<CommandSourceStack> ctx) {
		var source = ctx.getSource();
		var level = (ServerLevel) source.getPlayer().level();
		var updatedCount = WindmillLODServerTracker.forceSetLoadedWindmillRotationAnglesToZero(level);

		source.sendSuccess(() -> Component.literal("Set windmill rotation angle to zero for " + updatedCount + " loaded entries."), false);

		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}
}

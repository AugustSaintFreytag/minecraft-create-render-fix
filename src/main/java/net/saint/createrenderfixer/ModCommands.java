package net.saint.createrenderfixer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Registers simple server-side commands to tweak runtime config flags.
 */
public final class ModCommands {

	private ModCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register(ModCommands::register);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
			Commands.CommandSelection environment) {
		dispatcher.register(buildRoot());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
		return Commands.literal("create-rf").then(Commands.literal("list").executes(ModCommands::listFlags))
				.then(Commands.literal("set")
						.then(Commands.literal("cacheDynamicInstances")
								.then(Commands.argument("value", BoolArgumentType.bool())
										.executes(ctx -> setBoolean(ctx, "cacheDynamicInstances", ModConfig::setCacheDynamicInstances))))
						.then(Commands.literal("freezeDistantInstances")
								.then(Commands.argument("value", BoolArgumentType.bool())
										.executes(ctx -> setBoolean(ctx, "freezeDistantInstances", ModConfig::setFreezeDistantInstances))))
						.then(Commands.literal("freezeDistanceBlocks").then(
								Commands.argument("blocks", IntegerArgumentType.integer(0)).executes(ModCommands::setFreezeDistance))));
	}

	private static int listFlags(CommandContext<CommandSourceStack> ctx) {
		ctx.getSource().sendSuccess(() -> Component.literal("Create Render Fixer config: " + ModConfig.debugDescription()), false);
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}

	private static int setBoolean(CommandContext<CommandSourceStack> ctx, String name, java.util.function.Consumer<Boolean> setter) {
		boolean value = BoolArgumentType.getBool(ctx, "value");
		setter.accept(value);
		ctx.getSource().sendSuccess(() -> Component.literal(name + " set to " + value), false);
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}

	private static int setFreezeDistance(CommandContext<CommandSourceStack> ctx) {
		int blocks = IntegerArgumentType.getInteger(ctx, "blocks");
		ModConfig.setFreezeBlockDistance(blocks);
		ctx.getSource().sendSuccess(() -> Component.literal("freezeDistanceBlocks set to " + blocks), false);
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}
}

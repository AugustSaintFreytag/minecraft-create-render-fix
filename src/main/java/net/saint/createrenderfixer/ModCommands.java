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
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers server-side commands to modify config values.
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
		return Commands.literal("create-rf")

				// List
				.then(Commands.literal("list").executes(ModCommands::listFlags))

				// Set
				.then(Commands.literal("set")
						.then(Commands.literal("forceDisableRateLimiting")
								.then(Commands.argument("value", BoolArgumentType.bool()).executes(
										ctx -> setBoolean(ctx, "forceDisableRateLimiting", ModConfig::setForceDisableRateLimiting))))
						.then(Commands.literal("cacheDynamicInstances")
								.then(Commands.argument("value", BoolArgumentType.bool())
										.executes(ctx -> setBoolean(ctx, "cacheDynamicInstances", ModConfig::setCacheDynamicInstances))))
						.then(Commands.literal("freezeDistantInstances")
								.then(Commands.argument("value", BoolArgumentType.bool())
										.executes(ctx -> setBoolean(ctx, "freezeDistantInstances", ModConfig::setFreezeDistantInstances))))
						.then(Commands.literal("freezeOccludedInstances")
								.then(Commands.argument("value", BoolArgumentType.bool()).executes(
										ctx -> setBoolean(ctx, "freezeOccludedInstances", ModConfig::setFreezeOccludedInstances))))
						.then(Commands.literal("freezeBlockDistance").then(
								Commands.argument("blocks", IntegerArgumentType.integer(0)).executes(ModCommands::setFreezeBlockDistance)))
						.then(Commands.literal("entityLODDistanceOffset")
								.then(Commands.argument("value", IntegerArgumentType.integer())
										.executes(ModCommands::setEntityLODDistanceOffset))))

				// Blacklist
				.then(Commands.literal("blacklist")
						.then(Commands.literal("add")
								.then(Commands.argument("id", ResourceLocationArgument.id()).executes(ModCommands::addToBlacklist)))
						.then(Commands.literal("remove")
								.then(Commands.argument("id", ResourceLocationArgument.id()).executes(ModCommands::removeFromBlacklist)))
						.then(Commands.literal("clear").executes(ModCommands::clearBlacklist))
						.then(Commands.literal("list").executes(ModCommands::listBlacklist)));
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

	private static int setFreezeBlockDistance(CommandContext<CommandSourceStack> ctx) {
		int blocks = IntegerArgumentType.getInteger(ctx, "blocks");
		ModConfig.setFreezeDistantInstancesRange(blocks);
		ctx.getSource().sendSuccess(() -> Component.literal("freezeBlockDistance set to " + blocks), false);
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}

	private static int setEntityLODDistanceOffset(CommandContext<CommandSourceStack> ctx) {
		int value = IntegerArgumentType.getInteger(ctx, "value");
		ModConfig.setEntityLODDistanceOffset(value);
		ctx.getSource().sendSuccess(() -> Component.literal("entityLODDistanceOffset set to " + value), false);
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}

	private static int addToBlacklist(CommandContext<CommandSourceStack> ctx) {
		ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
		ModConfig.addFreezeBlacklist(id);
		ctx.getSource().sendSuccess(() -> Component.literal("Added to freeze blacklist: " + id), false);
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}

	private static int removeFromBlacklist(CommandContext<CommandSourceStack> ctx) {
		ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
		ModConfig.removeFreezeBlacklist(id);
		ctx.getSource().sendSuccess(() -> Component.literal("Removed from freeze blacklist: " + id), false);
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}

	private static int clearBlacklist(CommandContext<CommandSourceStack> ctx) {
		ModConfig.clearFreezeBlacklist();
		ctx.getSource().sendSuccess(() -> Component.literal("Cleared freeze blacklist"), false);
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}

	private static int listBlacklist(CommandContext<CommandSourceStack> ctx) {
		var ids = ModConfig.freezeBlacklist();
		ctx.getSource().sendSuccess(() -> Component.literal("Freeze blacklist (" + ids.size() + "): " + ids), false);
		return com.mojang.brigadier.Command.SINGLE_SUCCESS;
	}
}

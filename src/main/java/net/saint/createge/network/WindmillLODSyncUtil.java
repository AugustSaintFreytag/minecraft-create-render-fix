package net.saint.createge.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.saint.createge.Mod;
import net.saint.createge.ModClient;
import net.saint.createge.dh.WindmillLODEntry;

public final class WindmillLODSyncUtil {

	// Configuration

	private static final ResourceLocation LOAD_PACKET = new ResourceLocation(Mod.MOD_ID, "windmill_lod_load");
	private static final ResourceLocation UPDATE_PACKET = new ResourceLocation(Mod.MOD_ID, "windmill_lod_update");
	private static final ResourceLocation REMOVE_PACKET = new ResourceLocation(Mod.MOD_ID, "windmill_lod_remove");

	// State

	private static Map<UUID, Long> lastBroadcastTickByPlayer = new HashMap<>();

	// Init

	public static void initServer() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendLoadPacketToPlayer(handler.player));
	}

	public static void initClient() {
		ClientPlayNetworking.registerGlobalReceiver(LOAD_PACKET, (client, handler, buffer, responseSender) -> {
			var entries = readEntries(buffer);

			client.execute(() -> {
				ModClient.WINDMILL_LOD_MANAGER.loadPersistent(entries);
				Mod.LOGGER.info("Received and loaded {} windmill LOD entries client-side.", entries.size());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(UPDATE_PACKET, (client, handler, buffer, responseSender) -> {
			var entry = readEntry(buffer);
			var level = client.level;

			if (entry == null || level == null) {
				return;
			}

			// Override server-side decoupled game time with client-local.
			// Assume received packet is always recent and use as if received in same time space.
			var currentTick = level.getGameTime();
			entry.lastSynchronizationTick = currentTick;

			client.execute(() -> {
				ModClient.WINDMILL_LOD_MANAGER.register(entry);
				Mod.LOGGER.info("Received and updated registration for windmill LOD for contraption '{}' client-side (tick {}).",
						entry.contraptionId, entry.lastSynchronizationTick);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(REMOVE_PACKET, (client, handler, buffer, responseSender) -> {
			var contraptionIdentifier = buffer.readUUID();

			client.execute(() -> {
				ModClient.WINDMILL_LOD_MANAGER.unregister(contraptionIdentifier);
				Mod.LOGGER.info("Deregistered windmill LOD for contraption '{}' client-side.", contraptionIdentifier);
			});
		});
	}

	// Broadcast

	public static void sendLoadPacketToAllPlayers(MinecraftServer server) {
		if (server == null) {
			return;
		}

		for (var player : server.getPlayerList().getPlayers()) {
			sendLoadPacketToPlayer(player);
		}
	}

	public static void sendLoadPacketToPlayer(ServerPlayer player) {
		var entries = collectServerEntries();
		var buffer = PacketByteBufs.create();
		buffer.writeVarInt(entries.size());

		for (var entry : entries) {
			writeEntry(buffer, entry);
		}

		ServerPlayNetworking.send(player, LOAD_PACKET, buffer);
		Mod.LOGGER.info("Sent {} windmill LOD entries data to player '{}'.", entries.size(), player.getGameProfile().getName());
	}

	public static void sendUpdatePacketToAllPlayers(MinecraftServer server, WindmillLODEntry entry) {
		if (server == null || entry == null) {
			return;
		}

		var playerList = server.getPlayerList();
		var viewDistance = playerList.getViewDistance();

		for (var player : playerList.getPlayers()) {
			var playerDistance = getDistanceForPlayerToEntry(player, entry);
			var minTicksElapsed = getTickPacingForPlayerDistance(playerDistance, viewDistance);

			if (!shouldSendPacedUpdateToPlayer(player, minTicksElapsed)) {
				continue;
			}

			Mod.LOGGER.info("Sending paced windmill LOD entry to player '{}' ({}), distance {} chunks, interval {} ticks.",
					player.getName(), player.getUUID(), playerDistance, minTicksElapsed);

			var buffer = PacketByteBufs.create();
			writeEntry(buffer, entry);
			ServerPlayNetworking.send(player, UPDATE_PACKET, buffer);

			touchBroadcastForPlayer(player);
		}
	}

	public static void sendRemovalPacketToAllPlayers(MinecraftServer server, UUID contraptionId) {
		if (server == null || contraptionId == null) {
			return;
		}

		for (var player : server.getPlayerList().getPlayers()) {
			var buffer = PacketByteBufs.create();
			buffer.writeUUID(contraptionId);
			ServerPlayNetworking.send(player, REMOVE_PACKET, buffer);
		}
	}

	// Pacing (Time)

	private static boolean shouldSendPacedUpdateToPlayer(ServerPlayer player, long minTicksElapsed) {
		var lastBroadcastTick = lastBroadcastTickByPlayer.get(player.getUUID());

		if (lastBroadcastTick == null) {
			return true;
		}

		var currentTick = player.level().getGameTime();

		return currentTick > lastBroadcastTick + minTicksElapsed;
	}

	private static void touchBroadcastForPlayer(ServerPlayer player) {
		var currentTick = player.level().getGameTime();
		lastBroadcastTickByPlayer.put(player.getUUID(), currentTick);
	}

	// Pacing (Distance)

	private static double getDistanceForPlayerToEntry(ServerPlayer player, WindmillLODEntry entry) {
		if (player == null || entry == null || entry.anchorPosition == null) {
			return 0.0;
		}

		var playerDimensionId = player.level().dimension().location().toString();
		var entryDimensionId = entry.dimensionId;

		if (!playerDimensionId.equals(entryDimensionId)) {
			return Math.max(0.0, Mod.CONFIG.windmillMaximumRenderDistance);
		}

		var windmillPosition = entry.anchorPosition.getCenter();
		var distance = Math.sqrt(player.distanceToSqr(windmillPosition));

		return distance;
	}

	private static int getTickPacingForPlayerDistance(double distanceBlocks, int viewDistance) {
		// Baseline tick interval to sync server-side state with a client.
		var baselineTickInterval = Math.max(1, Mod.CONFIG.windmillSyncBaseTickInterval);
		var maxChunkRenderDistance = Math.max(0.0, Mod.CONFIG.windmillMaximumRenderDistance / 16.0);

		// The current distance between the client and the LOD entity.
		var chunkDistance = Math.max(0.0, distanceBlocks / 16.0);

		// Apply view distance offset to only start pacing after LOD has actually become visible.
		var offsetChunkDistance = Math.max(0.0, chunkDistance - viewDistance);

		// For every stride chunks of effective distance, increase pacing by one unit.
		// Example: 20 ticks more per 16 chunks.

		var effectiveChunkDistance = Math.min(offsetChunkDistance, maxChunkRenderDistance);
		var numberOfPacingSteps = (int) Math.ceil(effectiveChunkDistance / Mod.CONFIG.windmillSyncDistanceStride);
		var pacingFactor = 1 + numberOfPacingSteps;
		var pacingTickInterval = baselineTickInterval * pacingFactor;

		return pacingTickInterval;
	}

	// Read

	private static ArrayList<WindmillLODEntry> readEntries(FriendlyByteBuf buffer) {
		var entryCount = buffer.readVarInt();
		var entries = new ArrayList<WindmillLODEntry>(entryCount);

		for (var i = 0; i < entryCount; i++) {
			var entry = readEntry(buffer);

			if (entry == null) {
				continue;
			}

			entries.add(entry);
		}

		return entries;
	}

	// Coding

	@Nullable
	private static WindmillLODEntry readEntry(FriendlyByteBuf buffer) {
		var nbt = buffer.readNbt();
		var entry = WindmillLODEntry.fromNbt(nbt);

		return entry;
	}

	private static void writeEntry(FriendlyByteBuf buffer, WindmillLODEntry entry) {
		buffer.writeNbt(entry.toNbt());
	}

	private static ArrayList<WindmillLODEntry> collectServerEntries() {
		var entries = new ArrayList<WindmillLODEntry>();

		for (var entry : Mod.WINDMILL_LOD_MANAGER.entries()) {
			entries.add(entry.createPersistenceSnapshot());
		}

		return entries;
	}
}

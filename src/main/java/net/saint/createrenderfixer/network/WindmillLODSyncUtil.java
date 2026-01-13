package net.saint.createrenderfixer.network;

import java.util.ArrayList;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.dh.WindmillLODEntry;
import net.saint.createrenderfixer.dh.WindmillLODManager;

public final class WindmillLODSyncUtil {

	// Configuration

	private static final ResourceLocation LOAD_PACKET = new ResourceLocation(Mod.MOD_ID, "windmill_lod_load");
	private static final ResourceLocation UPDATE_PACKET = new ResourceLocation(Mod.MOD_ID, "windmill_lod_update");
	private static final ResourceLocation REMOVE_PACKET = new ResourceLocation(Mod.MOD_ID, "windmill_lod_remove");

	// Init

	public static void initServer() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendLoadAllPacketToPlayer(handler.player));
	}

	public static void initClient() {
		ClientPlayNetworking.registerGlobalReceiver(LOAD_PACKET, (client, handler, buffer, responseSender) -> {
			var entries = readEntries(buffer);

			client.execute(() -> {
				WindmillLODManager.loadPersistent(entries);
				Mod.LOGGER.info("Received and loaded {} windmill LOD entries client-side.", entries.size());
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(UPDATE_PACKET, (client, handler, buffer, responseSender) -> {
			var entry = readEntry(buffer);

			if (entry == null) {
				return;
			}

			client.execute(() -> {
				WindmillLODManager.register(entry);
				Mod.LOGGER.debug("Received and updated registration for windmill LOD for contraption '{}' client-side.",
						entry.contraptionId);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(REMOVE_PACKET, (client, handler, buffer, responseSender) -> {
			var contraptionIdentifier = buffer.readUUID();

			client.execute(() -> {
				WindmillLODManager.unregister(contraptionIdentifier);
				Mod.LOGGER.debug("Deregistered windmill LOD for contraption '{}' client-side.", contraptionIdentifier);
			});
		});
	}

	// Broadcast

	public static void sendLoadAllPacketToPlayer(ServerPlayer player) {
		var entries = collectEntries();
		var buffer = PacketByteBufs.create();
		buffer.writeVarInt(entries.size());

		for (var entry : entries) {
			writeEntry(buffer, entry);
		}

		ServerPlayNetworking.send(player, LOAD_PACKET, buffer);
		Mod.LOGGER.info("Sent {} windmill LOD entries data to player '{}'.", entries.size(), player.getGameProfile().getName());
	}

	public static void broadcastLoadAllPacket(MinecraftServer server) {
		if (server == null) {
			return;
		}

		for (var player : server.getPlayerList().getPlayers()) {
			sendLoadAllPacketToPlayer(player);
		}
	}

	public static void broadcastUpdatePacket(MinecraftServer server, WindmillLODEntry entry) {
		if (server == null || entry == null) {
			return;
		}

		for (var player : server.getPlayerList().getPlayers()) {
			var buffer = PacketByteBufs.create();
			writeEntry(buffer, entry);
			ServerPlayNetworking.send(player, UPDATE_PACKET, buffer);
		}
	}

	public static void broadcastRemovalPacket(MinecraftServer server, UUID contraptionId) {
		if (server == null || contraptionId == null) {
			return;
		}

		for (var player : server.getPlayerList().getPlayers()) {
			var buffer = PacketByteBufs.create();
			buffer.writeUUID(contraptionId);
			ServerPlayNetworking.send(player, REMOVE_PACKET, buffer);
		}
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
		var contraptionId = buffer.readUUID();
		var dimensionId = buffer.readUtf();
		var anchorPosition = buffer.readBlockPos();
		var rotationAxis = buffer.readEnum(Direction.Axis.class);
		var bearingDirection = buffer.readEnum(Direction.class);
		var planeWidth = buffer.readFloat();
		var planeHeight = buffer.readFloat();
		var planeDepth = buffer.readFloat();
		var rotationSpeed = buffer.readFloat();
		var rotationAngle = buffer.readFloat();
		var tickRegistered = buffer.readLong();
		var lastSynchronizationTick = buffer.readLong();

		if (dimensionId.isBlank()) {
			return null;
		}

		return new WindmillLODEntry(contraptionId, dimensionId, anchorPosition, rotationAxis, bearingDirection, planeWidth, planeHeight,
				planeDepth, tickRegistered, rotationSpeed, rotationAngle, lastSynchronizationTick);
	}

	private static void writeEntry(FriendlyByteBuf buffer, WindmillLODEntry entry) {
		buffer.writeUUID(entry.contraptionId);
		buffer.writeUtf(entry.dimensionId);
		buffer.writeBlockPos(entry.anchorPosition);
		buffer.writeEnum(entry.rotationAxis);
		buffer.writeEnum(entry.bearingDirection);
		buffer.writeFloat(entry.planeWidth);
		buffer.writeFloat(entry.planeHeight);
		buffer.writeFloat(entry.planeDepth);
		buffer.writeFloat(entry.rotationSpeed);
		buffer.writeFloat(entry.rotationAngle);
		buffer.writeLong(entry.tickRegistered);
		buffer.writeLong(entry.lastSynchronizationTick);
	}

	private static ArrayList<WindmillLODEntry> collectEntries() {
		var entries = new ArrayList<WindmillLODEntry>();

		for (var entry : WindmillLODManager.entries()) {
			entries.add(entry.createPersistenceSnapshot());
		}

		return entries;
	}
}

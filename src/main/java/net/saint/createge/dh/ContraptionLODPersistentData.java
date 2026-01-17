package net.saint.createge.dh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.saint.createge.Mod;

public final class ContraptionLODPersistentData extends SavedData {

	// Configuration

	public static final String NAME = "create-lod";

	// State

	private Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> chunkData = new HashMap<>();

	private List<WindmillLODEntry> windmills = new ArrayList<>();

	// Init

	public ContraptionLODPersistentData() {
	}

	private ContraptionLODPersistentData(Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> chunkData,
			List<WindmillLODEntry> windmills) {
		this.chunkData = chunkData;
		this.windmills = windmills;
	}

	// Load

	public static ContraptionLODPersistentData loadFromServer(MinecraftServer server) {
		var dataStorage = server.overworld().getDataStorage();
		var state = dataStorage.computeIfAbsent(ContraptionLODPersistentData::fromNbt, ContraptionLODPersistentData::new, NAME);

		return state;
	}

	public static ContraptionLODPersistentData fromNbt(CompoundTag root) {
		var blocks = BuiltInRegistries.BLOCK.asLookup();
		var data = decode(blocks, root);

		return new ContraptionLODPersistentData(data.chunkData(), data.windmills());
	}

	// Persistence

	@Override
	public CompoundTag save(@Nonnull CompoundTag compoundTag) {
		encode(compoundTag, chunkData, windmills);
		return compoundTag;
	}

	public void applyToRegistries() {
		ContraptionBlockRegistry.loadPersistent(chunkData);
		Mod.WINDMILL_LOD_MANAGER.loadPersistent(windmills);
	}

	public void captureFromRegistries() {
		chunkData = ContraptionBlockRegistry.snapshot();
		windmills = Mod.WINDMILL_LOD_MANAGER.snapshotPersistent();
		setDirty();
	}

	public boolean hasData() {
		if (chunkData != null && !chunkData.isEmpty()) {
			return true;
		}

		if (windmills != null && !windmills.isEmpty()) {
			return true;
		}

		return false;
	}

	public void replaceWith(ContraptionLODPersistentData other) {
		if (other == null) {
			return;
		}

		chunkData = other.chunkData;
		windmills = other.windmills;
	}

	// Encoding

	private static void encode(CompoundTag root, Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> data,
			List<WindmillLODEntry> windmills) {
		var dimensions = new ListTag();

		if (data != null) {
			data.forEach((dimensionId, chunkMap) -> {
				var dimensionTag = new CompoundTag();
				dimensionTag.putString("id", dimensionId);

				var chunks = new ListTag();
				chunkMap.forEach((chunkKey, blocks) -> {
					var chunkTag = new CompoundTag();
					chunkTag.putLong("key", chunkKey);

					var list = new ListTag();
					for (var block : blocks) {
						var blockTag = new CompoundTag();
						blockTag.putInt("x", block.x());
						blockTag.putInt("y", block.y());
						blockTag.putInt("z", block.z());
						blockTag.put("state", NbtUtils.writeBlockState(block.state()));

						if (block.biomeId() != null) {
							blockTag.putString("biome", block.biomeId());
						}

						list.add(blockTag);
					}

					chunkTag.put("blocks", list);
					chunks.add(chunkTag);
				});

				dimensionTag.put("chunks", chunks);
				dimensions.add(dimensionTag);
			});
		}

		root.put("dims", dimensions);
		root.put("windmills", encodeWindmills(windmills));
	}

	private static ListTag encodeWindmills(List<WindmillLODEntry> windmills) {
		var list = new ListTag();

		if (windmills == null || windmills.isEmpty()) {
			return list;
		}

		for (var entry : windmills) {
			if (entry == null) {
				continue;
			}

			var windmillTag = entry.toNbt();

			if (windmillTag == null) {
				continue;
			}

			list.add(windmillTag);
		}

		return list;
	}

	// Decoding

	private static PersistedData decode(HolderGetter<Block> blocks, @Nullable CompoundTag root) {
		var data = new HashMap<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>>();
		var windmills = decodeWindmills(root);

		if (root == null || !root.contains("dims", Tag.TAG_LIST)) {
			return new PersistedData(data, windmills);
		}

		for (Tag dimTagRaw : root.getList("dims", Tag.TAG_COMPOUND)) {
			if (!(dimTagRaw instanceof CompoundTag dimTag)) {
				continue;
			}

			var id = dimTag.getString("id");
			var chunkMap = new HashMap<Long, List<ContraptionBlockRegistry.StoredBlock>>();

			for (var chunkRaw : dimTag.getList("chunks", Tag.TAG_COMPOUND)) {
				if (!(chunkRaw instanceof CompoundTag chunkTag)) {
					continue;
				}

				var key = chunkTag.getLong("key");
				var list = new ArrayList<ContraptionBlockRegistry.StoredBlock>();

				for (var blockRaw : chunkTag.getList("blocks", Tag.TAG_COMPOUND)) {
					if (!(blockRaw instanceof CompoundTag blockTag)) {
						continue;
					}

					var state = NbtUtils.readBlockState(blocks, blockTag.getCompound("state"));
					var x = blockTag.getInt("x");
					var y = blockTag.getInt("y");
					var z = blockTag.getInt("z");
					var biomeId = blockTag.getString("biome");

					list.add(new ContraptionBlockRegistry.StoredBlock(x, y, z, state, biomeId));
				}

				if (!list.isEmpty()) {
					chunkMap.put(key, list);
				}
			}

			if (!chunkMap.isEmpty()) {
				data.put(id, chunkMap);
			}
		}

		return new PersistedData(data, windmills);
	}

	private static List<WindmillLODEntry> decodeWindmills(@Nullable CompoundTag root) {
		var entries = new ArrayList<WindmillLODEntry>();

		if (root == null || !root.contains("windmills", Tag.TAG_LIST)) {
			return entries;
		}

		for (Tag entryRaw : root.getList("windmills", Tag.TAG_COMPOUND)) {
			if (!(entryRaw instanceof CompoundTag entryTag)) {
				continue;
			}

			var entry = WindmillLODEntry.fromNbt(entryTag);

			if (entry == null) {
				continue;
			}

			entries.add(entry);
		}

		return entries;
	}

	// Utility

	private record PersistedData(Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> chunkData,
			List<WindmillLODEntry> windmills) {
	}
}

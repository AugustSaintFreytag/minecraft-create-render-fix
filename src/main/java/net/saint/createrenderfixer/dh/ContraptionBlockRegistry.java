package net.saint.createrenderfixer.dh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkModifiedEvent;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.mixin.create.ControlledContraptionEntityAccessor;
import net.saint.createrenderfixer.network.WindmillLODSyncUtil;

/**
 * Thread-safe registry of contraption blocks keyed by dimension + chunk for DH overrides.
 */
public final class ContraptionBlockRegistry {

	// Library (Records)

	public record StoredBlock(int x, int y, int z, BlockState state, String biomeId) {
	}

	public record LookupResult(BlockState state, @Nullable String biomeId) {
	}

	// Library (Models)

	private static final class BlockPosState {
		final int x;
		final int y;
		final int z;
		final BlockState state;
		final String biomeId;

		BlockPosState(BlockPos pos, BlockState state, String biomeId) {
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			this.state = state;
			this.biomeId = biomeId;
		}

		boolean matches(int x, int y, int z) {
			return this.x == x && this.y == y && this.z == z;
		}
	}

	private static final class ContraptionEntry {
		final String dimensionId;
		final Map<Long, List<BlockPosState>> chunks = new ConcurrentHashMap<>();

		ContraptionEntry(String dimensionId) {
			this.dimensionId = dimensionId;
		}
	}

	private record PlaneSize(float width, float height, float depth) {
	}

	private record WindmillRegistrationData(BlockPos controllerPosition, WindmillBearingBlockEntity windmillBearing,
			Direction.Axis rotationAxis, Direction bearingDirection, AABB bounds) {
	}

	// State

	private static final Map<UUID, ContraptionEntry> CONTRAPTIONS = new ConcurrentHashMap<>();
	private static final Map<String, Map<Long, List<BlockPosState>>> BY_DIMENSION = new ConcurrentHashMap<>();

	// Registration

	public static void register(AbstractContraptionEntity entity) {
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		var contraption = entity.getContraption();

		if (contraption == null || contraption.getBlocks().isEmpty()) {
			return;
		}

		var contraptionId = entity.getUUID();

		if (CONTRAPTIONS.containsKey(contraptionId)) {
			return;
		}

		var dimensionId = serverLevel.dimension().location().toString();
		var entry = new ContraptionEntry(dimensionId);
		var anchorPosition = contraption.anchor;

		if (anchorPosition == null) {
			Mod.LOGGER.warn("Contraption '{}' has no anchor block position and can not be registered.", contraptionId);

			return;
		}

		var windmillData = resolveWindmillRegistrationData(entity, serverLevel);

		if (windmillData != null) {
			registerWindmillEntry(contraptionId, serverLevel, dimensionId, windmillData);
			removeStoredBlocksForWindmill(dimensionId, anchorPosition, windmillData.bounds());

			return;
		}

		contraption.getBlocks().forEach((localPosition, info) -> {
			var worldPosition = localPosition.offset(anchorPosition);
			var biomeIdentifier = resolveBiomeId(serverLevel, worldPosition);
			var chunkPosition = new ChunkPos(worldPosition);
			var chunkKey = chunkPosition.toLong();

			entry.chunks.computeIfAbsent(chunkKey, key -> new ArrayList<>())
					.add(new BlockPosState(worldPosition, info.state(), biomeIdentifier));
		});

		CONTRAPTIONS.put(contraptionId, entry);

		var dimensionChunkMap = BY_DIMENSION.computeIfAbsent(dimensionId, key -> new ConcurrentHashMap<>());

		for (var chunkEntry : entry.chunks.entrySet()) {
			var combinedList = dimensionChunkMap.computeIfAbsent(chunkEntry.getKey(), key -> createSharedChunkList());
			combinedList.addAll(chunkEntry.getValue());
		}

		notifyChunksDirty(dimensionId, entry.chunks.keySet());
	}

	public static void unregister(AbstractContraptionEntity entity) {
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		var contraptionIdentifier = entity.getUUID();
		var windmillRemoved = unregisterInternal(contraptionIdentifier);

		if (windmillRemoved) {
			WindmillLODSyncUtil.broadcastRemovalPacket(serverLevel.getServer(), contraptionIdentifier);
			Mod.LOGGER.info("Unregistered windmill LOD entry for contraption '{}'.", contraptionIdentifier);
		}
	}

	public static void unregister(UUID contraptionId) {
		unregisterInternal(contraptionId);
	}

	public static void clearForWorld(String dimensionId) {
		BY_DIMENSION.remove(dimensionId);
		CONTRAPTIONS.entrySet().removeIf(e -> e.getValue().dimensionId.equals(dimensionId));
		WindmillLODManager.clearForWorld(dimensionId);
	}

	@Nullable
	public static LookupResult find(String dimensionId, int worldX, int worldY, int worldZ) {
		var chunkMap = resolveChunkMap(dimensionId);

		if (chunkMap == null) {
			return null;
		}

		var chunkPosition = new ChunkPos(new BlockPos(worldX, worldY, worldZ));
		var chunkKey = chunkPosition.toLong();
		var entries = chunkMap.get(chunkKey);

		if (entries == null) {
			return null;
		}

		for (var entry : entries) {
			if (entry.matches(worldX, worldY, worldZ)) {
				return new LookupResult(entry.state, entry.biomeId);
			}
		}

		return null;
	}

	public static int highestYInColumn(String dimensionId, int worldX, int worldZ, int defaultHeight) {
		var chunkMap = resolveChunkMap(dimensionId);

		if (chunkMap == null) {
			return defaultHeight;
		}

		var chunkKey = ChunkPos.asLong(new BlockPos(worldX, 0, worldZ));
		var entries = chunkMap.get(chunkKey);

		if (entries == null || entries.isEmpty()) {
			return defaultHeight;
		}

		var maxY = Integer.MIN_VALUE;

		for (var entry : entries) {
			if (entry.x == worldX && entry.z == worldZ) {
				if (entry.y > maxY) {
					maxY = entry.y;
				}
			}
		}

		if (maxY == Integer.MIN_VALUE) {
			return defaultHeight;
		}

		return Math.max(defaultHeight, maxY);
	}

	// Management

	public static Map<String, Map<Long, List<StoredBlock>>> snapshot() {
		var result = new ConcurrentHashMap<String, Map<Long, List<StoredBlock>>>();

		BY_DIMENSION.forEach((dimensionId, chunkMap) -> {
			var chunkCopy = new ConcurrentHashMap<Long, List<StoredBlock>>();

			chunkMap.forEach((chunkKey, entries) -> {
				var list = new ArrayList<StoredBlock>(entries.size());
				for (var entry : entries) {
					list.add(new StoredBlock(entry.x, entry.y, entry.z, entry.state, entry.biomeId));
				}
				chunkCopy.put(chunkKey, list);
			});

			result.put(dimensionId, chunkCopy);
		});

		return result;
	}

	public static void loadPersistent(Map<String, Map<Long, List<StoredBlock>>> data) {
		BY_DIMENSION.clear();

		for (var entry : data.entrySet()) {
			var chunkMap = new ConcurrentHashMap<Long, List<BlockPosState>>();

			for (var chunkEntry : entry.getValue().entrySet()) {
				var list = createSharedChunkList();
				for (var block : chunkEntry.getValue()) {
					list.add(new BlockPosState(new BlockPos(block.x(), block.y(), block.z()), block.state(), block.biomeId()));
				}

				chunkMap.put(chunkEntry.getKey(), list);
			}

			BY_DIMENSION.put(entry.getKey(), chunkMap);
			notifyChunksDirty(entry.getKey(), chunkMap.keySet());
		}
	}

	private static void notifyChunksDirty(String dimensionId, Iterable<Long> chunkKeys) {
		if (!DhBridge.isReady()) {
			return;
		}

		var levelWrapper = resolveLevelWrapper(dimensionId);

		if (levelWrapper == null) {
			return;
		}

		for (var chunkKey : chunkKeys) {
			var chunkPos = new ChunkPos(chunkKey);
			try {
				DhApi.events.fireAllEvents(DhApiChunkModifiedEvent.class,
						new DhApiChunkModifiedEvent.EventParam(levelWrapper, chunkPos.x, chunkPos.z));
			} catch (Exception ignored) {
				// DH may not be ready to process the notification; ignore.
			}
		}
	}

	// Utility

	private static void registerWindmillEntry(UUID contraptionIdentifier, ServerLevel serverLevel, String dimensionIdentifier,
			WindmillRegistrationData windmillData) {
		if (WindmillLODManager.find(contraptionIdentifier) != null) {
			Mod.LOGGER.info("Windmill contraption '{}' with registered LOD entry loading back in.", contraptionIdentifier);
			return;
		}

		var planeSize = resolvePlaneSize(windmillData.bounds(), windmillData.rotationAxis());
		var rotationSpeed = windmillData.windmillBearing().getAngularSpeed();
		var rotationAngle = windmillData.windmillBearing().getInterpolatedAngle(1.0F);
		var lastSynchronizationTick = serverLevel.getGameTime();
		var bearingDirection = windmillData.bearingDirection();
		var entry = new WindmillLODEntry(contraptionIdentifier, dimensionIdentifier, windmillData.controllerPosition(),
				windmillData.rotationAxis(), bearingDirection, planeSize.width(), planeSize.height(), planeSize.depth(), rotationSpeed,
				rotationAngle, lastSynchronizationTick);

		WindmillLODManager.register(entry);
		WindmillLODSyncUtil.broadcastUpdatePacket(serverLevel.getServer(), entry);

		Mod.LOGGER.info("Registered windmill LOD entry for contraption '{}' in '{}'.", contraptionIdentifier, dimensionIdentifier);
	}

	private static boolean unregisterInternal(UUID contraptionId) {
		var removed = CONTRAPTIONS.remove(contraptionId);
		var windmillRemoved = WindmillLODManager.unregister(contraptionId);

		if (removed == null) {
			return windmillRemoved;
		}

		var chunkMap = BY_DIMENSION.get(removed.dimensionId);

		if (chunkMap == null) {
			return windmillRemoved;
		}

		for (var chunkEntry : removed.chunks.entrySet()) {
			var combinedList = chunkMap.get(chunkEntry.getKey());

			if (combinedList == null) {
				continue;
			}

			combinedList.removeAll(chunkEntry.getValue());

			if (combinedList.isEmpty()) {
				chunkMap.remove(chunkEntry.getKey(), combinedList);
			}
		}

		notifyChunksDirty(removed.dimensionId, removed.chunks.keySet());

		return windmillRemoved;
	}

	private static PlaneSize resolvePlaneSize(AABB bounds, Direction.Axis rotationAxis) {
		var sizeX = (float) bounds.getXsize();
		var sizeY = (float) bounds.getYsize();
		var sizeZ = (float) bounds.getZsize();

		return switch (rotationAxis) {
		case X -> new PlaneSize(sizeZ, sizeY, sizeX);
		case Y -> new PlaneSize(sizeX, sizeZ, sizeY);
		case Z -> new PlaneSize(sizeX, sizeY, sizeZ);
		};
	}

	private static void removeStoredBlocksForWindmill(String dimensionId, BlockPos anchorPosition, AABB bounds) {
		var chunkMap = BY_DIMENSION.get(dimensionId);

		if (chunkMap == null) {
			return;
		}

		var worldBounds = getWorldBoundsForAnchor(anchorPosition, bounds);
		var minX = (int) Math.floor(worldBounds.minX);
		var minY = (int) Math.floor(worldBounds.minY);
		var minZ = (int) Math.floor(worldBounds.minZ);
		var maxX = (int) Math.ceil(worldBounds.maxX) - 1;
		var maxY = (int) Math.ceil(worldBounds.maxY) - 1;
		var maxZ = (int) Math.ceil(worldBounds.maxZ) - 1;

		if (minX > maxX || minY > maxY || minZ > maxZ) {
			return;
		}

		var affectedChunks = new ArrayList<Long>();

		for (var chunkEntry : chunkMap.entrySet()) {
			var entries = chunkEntry.getValue();
			var initialSize = entries.size();

			entries.removeIf(entry -> entry.x >= minX && entry.x <= maxX && entry.y >= minY && entry.y <= maxY && entry.z >= minZ
					&& entry.z <= maxZ);

			if (entries.isEmpty()) {
				chunkMap.remove(chunkEntry.getKey(), entries);
			}

			if (entries.size() != initialSize) {
				affectedChunks.add(chunkEntry.getKey());
			}
		}

		if (!affectedChunks.isEmpty()) {
			notifyChunksDirty(dimensionId, affectedChunks);
		}
	}

	private static AABB getWorldBoundsForAnchor(BlockPos anchorPosition, AABB bounds) {
		return new AABB(bounds.minX + anchorPosition.getX(), bounds.minY + anchorPosition.getY(), bounds.minZ + anchorPosition.getZ(),
				bounds.maxX + anchorPosition.getX(), bounds.maxY + anchorPosition.getY(), bounds.maxZ + anchorPosition.getZ());
	}

	@Nullable
	private static WindmillRegistrationData resolveWindmillRegistrationData(AbstractContraptionEntity entity, ServerLevel serverLevel) {
		if (!(entity instanceof ControlledContraptionEntity controlledContraption)) {
			return null;
		}

		if (!(controlledContraption instanceof ControlledContraptionEntityAccessor accessor)) {
			return null;
		}

		var controllerPosition = accessor.getControllerPosition();

		if (controllerPosition == null) {
			return null;
		}

		var blockEntity = serverLevel.getBlockEntity(controllerPosition);

		if (!(blockEntity instanceof WindmillBearingBlockEntity windmillBearing)) {
			return null;
		}

		var rotationAxis = controlledContraption.getRotationAxis();

		if (rotationAxis == null) {
			return null;
		}

		var bounds = entity.getContraption().bounds;

		if (bounds == null) {
			return null;
		}

		var bearingDirection = resolveBearingDirection(windmillBearing, rotationAxis);

		return new WindmillRegistrationData(controllerPosition, windmillBearing, rotationAxis, bearingDirection, bounds);
	}

	private static Direction resolveBearingDirection(WindmillBearingBlockEntity windmillBearing, Direction.Axis rotationAxis) {
		if (windmillBearing != null) {
			var blockState = windmillBearing.getBlockState();

			if (blockState.hasProperty(BlockStateProperties.FACING)) {
				return blockState.getValue(BlockStateProperties.FACING);
			}

			if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
				return blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
			}
		}

		return Direction.fromAxisAndDirection(rotationAxis, Direction.AxisDirection.POSITIVE);
	}

	@Nullable
	private static Map<Long, List<BlockPosState>> resolveChunkMap(String dimensionId) {
		var chunkMap = BY_DIMENSION.get(dimensionId);
		if (chunkMap != null) {
			return chunkMap;
		}
		// Fallback: try dimension name suffix match (e.g., "minecraft:overworld" vs "overworld")
		for (var entry : BY_DIMENSION.entrySet()) {
			if (entry.getKey().endsWith(dimensionId) || dimensionId.endsWith(entry.getKey())) {
				return entry.getValue();
			}
		}

		return null;
	}

	private static String resolveBiomeId(ServerLevel level, BlockPos pos) {
		if (pos == null) {
			return "minecraft:plains";
		}

		Holder<Biome> biome = level.getBiome(pos);

		return biome.unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
	}

	@Nullable
	private static IDhApiLevelWrapper resolveLevelWrapper(String dimensionId) {
		var worldProxy = DhBridge.worldProxy();

		if (worldProxy == null) {
			return null;
		}

		try {
			for (var wrapper : worldProxy.getAllLoadedLevelWrappers()) {
				if (dimensionId.equals(wrapper.getDhIdentifier()) || dimensionId.endsWith(wrapper.getDimensionName())) {
					return wrapper;
				}
			}
		} catch (Exception ignored) {
			return null;
		}

		return null;
	}

	private static List<BlockPosState> createSharedChunkList() {
		return new CopyOnWriteArrayList<>();
	}
}

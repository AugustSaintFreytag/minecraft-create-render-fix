package net.saint.createge.client;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderObjectFactory;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.saint.createge.Mod;
import net.saint.createge.ModClient;
import net.saint.createge.dh.DhBridge;
import net.saint.createge.dh.WindmillLODBoxUtil;
import net.saint.createge.dh.WindmillLODEntry;

public final class WindmillLODRenderManager {

	// Configuration

	private static final float LOD_CLIP_DISTANCE_PADDING = 16.0F;
	private static final float LOD_CLIP_DISTANCE_OFFSET = 16.0F;

	private static final float HEIGHT_CLIP_DISTANCE_OVERRIDE = 1.0F;
	private static final int HEIGHT_CLIP_DISTANCE_PADDING = 1_000;

	// State

	private static final Map<UUID, IDhApiRenderableBoxGroup> RENDER_GROUPS = new ConcurrentHashMap<>();
	private static final Map<UUID, Float> LAST_RENDER_ANGLES = new ConcurrentHashMap<>();

	// Ticking

	public static void tick(ClientLevel level, float partialTicks) {
		if (!DhBridge.isReady()) {
			return;
		}

		var dimensionId = getDimensionIdForLevel(level);
		var renderFactory = DhBridge.renderFactory();

		if (renderFactory == null) {
			return;
		}

		var renderRegister = resolveRenderRegister();

		if (renderRegister == null) {
			return;
		}

		var activeIdentifiers = new HashSet<UUID>();

		for (var entry : ModClient.WINDMILL_LOD_MANAGER.entries()) {
			if (!dimensionId.equals(entry.dimensionId)) {
				continue;
			}

			if (!shouldRenderEntryForLevel(level, entry, partialTicks)) {
				continue;
			}

			activeIdentifiers.add(entry.contraptionId);

			var renderAngle = updateRenderAngleForEntry(level, entry, partialTicks);
			var renderGroup = getOrCreateRenderGroup(renderFactory, renderRegister, entry);

			if (renderGroup == null) {
				continue;
			}

			updateRenderGroup(renderGroup, entry, renderAngle);
		}

		removeStaleRenderGroups(activeIdentifiers, renderRegister);
	}

	// Rendering

	@Nullable
	private static IDhApiRenderableBoxGroup getOrCreateRenderGroup(IDhApiCustomRenderObjectFactory renderFactory,
			IDhApiCustomRenderRegister renderRegister, WindmillLODEntry entry) {
		var renderGroup = RENDER_GROUPS.get(entry.contraptionId);

		if (renderGroup != null) {
			entry.renderGroupId = renderGroup.getId();
			return renderGroup;
		}

		var resourceLocation = Mod.MOD_ID + ":windmill/" + entry.contraptionId;
		var originPosition = toOrigin(getRenderAnchorPositionForEntry(entry));
		var crossBoxes = getWindmillCrossBoxesForEntry(entry, entry.rotationAngle);

		try {
			renderGroup = renderFactory.createRelativePositionedGroup(resourceLocation, originPosition, crossBoxes);
			renderGroup.setOriginBlockPos(originPosition);

			renderRegister.add(renderGroup);
			entry.renderGroupId = renderGroup.getId();
			RENDER_GROUPS.put(entry.contraptionId, renderGroup);
		} catch (IllegalArgumentException exception) {
			Mod.LOGGER.warn("Failed to register windmill render group '{}'.", resourceLocation, exception);

			return null;
		}

		return renderGroup;
	}

	private static void updateRenderGroup(IDhApiRenderableBoxGroup renderGroup, WindmillLODEntry entry, float renderAngle) {
		var originPosition = toOrigin(getRenderAnchorPositionForEntry(entry));
		renderGroup.setOriginBlockPos(originPosition);

		var lastAngle = LAST_RENDER_ANGLES.get(entry.contraptionId);

		if (lastAngle != null) {
			var rotationDelta = getRotationDeltaForAngles(lastAngle, renderAngle);
			var rotationUpdateThreshold = Mod.CONFIG.windmillRotationAngleRenderThreshold;

			if (rotationDelta < rotationUpdateThreshold) {
				return;
			}
		}

		updateRenderGroupBoxes(renderGroup, entry, renderAngle);
		LAST_RENDER_ANGLES.put(entry.contraptionId, renderAngle);
	}

	private static void removeStaleRenderGroups(HashSet<UUID> activeIdentifiers, IDhApiCustomRenderRegister renderRegister) {
		var iterator = RENDER_GROUPS.entrySet().iterator();

		while (iterator.hasNext()) {
			var entry = iterator.next();
			var contraptionId = entry.getKey();

			if (activeIdentifiers.contains(contraptionId)) {
				continue;
			}

			var renderGroup = entry.getValue();
			renderGroup.setActive(false);

			try {
				renderRegister.remove(renderGroup.getId());
			} catch (Exception exception) {
				// DH may reject removal while it is still initializing, can be ignored.
			}

			iterator.remove();
			LAST_RENDER_ANGLES.remove(contraptionId);
		}
	}

	// Geometry

	private static List<DhApiRenderableBox> getWindmillCrossBoxesForEntry(WindmillLODEntry entry, float rotationAngle) {
		var bladeColor = getBladeColor();
		var bladeMaterial = getBladeMaterial();

		return WindmillLODBoxUtil.makeWindmillBladeBoxes(entry, bladeColor, bladeMaterial, rotationAngle);
	}

	private static Color getBladeColor() {
		return ModClient.WINDMILL_LOD_MATERIAL_MANAGER.getBladeColor();
	}

	private static EDhApiBlockMaterial getBladeMaterial() {
		return ModClient.WINDMILL_LOD_MATERIAL_MANAGER.getBladeMaterial();
	}

	private static void updateRenderGroupBoxes(IDhApiRenderableBoxGroup renderGroup, WindmillLODEntry entry, float renderAngle) {
		var updatedBoxes = getWindmillCrossBoxesForEntry(entry, renderAngle);

		if (renderGroup.size() != updatedBoxes.size()) {
			renderGroup.clear();
			renderGroup.addAll(updatedBoxes);
			renderGroup.triggerBoxChange();

			return;
		}

		for (var index = 0; index < updatedBoxes.size(); index++) {
			var updatedBox = updatedBoxes.get(index);
			var existingBox = renderGroup.get(index);

			existingBox.minPos = updatedBox.minPos;
			existingBox.maxPos = updatedBox.maxPos;
			existingBox.color = updatedBox.color;
			existingBox.material = updatedBox.material;
		}

		renderGroup.triggerBoxChange();
	}

	// Utility

	private static DhApiVec3d toOrigin(BlockPos anchorPosition) {
		return new DhApiVec3d(anchorPosition.getX() + 0.5, anchorPosition.getY() + 0.5, anchorPosition.getZ() + 0.5);
	}

	private static BlockPos getRenderAnchorPositionForEntry(WindmillLODEntry entry) {
		var bearingDirection = entry.bearingDirection;

		if (bearingDirection == null) {
			return entry.anchorPosition;
		}

		return entry.anchorPosition.relative(bearingDirection);
	}

	private static String getDimensionIdForLevel(ClientLevel level) {
		return level.dimension().location().toString();
	}

	private static boolean shouldRenderEntryForLevel(ClientLevel level, WindmillLODEntry entry, float partialTicks) {
		if (level == null || entry == null) {
			return false;
		}

		var cameraPosition = getCameraPosition();

		if (cameraPosition != null) {
			var originPosition = getRenderAnchorPositionForEntry(entry);
			var distance = getDistanceToCamera(cameraPosition, originPosition);
			var maximumRenderDistance = Mod.CONFIG.windmillMaximumRenderDistance;

			if (distance > maximumRenderDistance) {
				return false;
			}
		}

		var clipDistance = getClipDistanceForLevel(level, partialTicks);

		if (clipDistance > 0.0F) {
			clipDistance += LOD_CLIP_DISTANCE_OFFSET;
		}

		if (clipDistance <= 0.0F) {
			return !isChunkLoadedForEntry(level, entry);
		}

		if (cameraPosition == null) {
			return true;
		}

		var originPosition = getRenderAnchorPositionForEntry(entry);
		var distance = getDistanceToCamera(cameraPosition, originPosition);

		return distance >= clipDistance;
	}

	private static boolean isChunkLoadedForEntry(ClientLevel level, WindmillLODEntry entry) {
		var anchorPosition = entry.anchorPosition;
		var chunkX = anchorPosition.getX() >> 4;
		var chunkZ = anchorPosition.getZ() >> 4;
		var chunkSource = level.getChunkSource();

		if (chunkSource == null) {
			return false;
		}

		return chunkSource.hasChunk(chunkX, chunkZ);
	}

	private static float getClipDistanceForLevel(ClientLevel level, float partialTicks) {
		var renderProxy = DhApi.Delayed.renderProxy;

		if (renderProxy == null) {
			return 0.0F;
		}

		var nearClipDistance = renderProxy.getNearClipPlaneDistanceInBlocks(partialTicks);
		var clipDistance = nearClipDistance + LOD_CLIP_DISTANCE_PADDING;

		if (isHeightClipOverrideActive(level)) {
			return HEIGHT_CLIP_DISTANCE_OVERRIDE;
		}

		return clipDistance;
	}

	private static boolean isHeightClipOverrideActive(ClientLevel level) {
		var minecraft = Minecraft.getInstance();
		var player = minecraft.player;

		if (player == null) {
			return false;
		}

		var playerHeight = player.blockPosition().getY();
		var levelMaxHeight = level.getMaxBuildHeight();

		return playerHeight > levelMaxHeight + HEIGHT_CLIP_DISTANCE_PADDING;
	}

	private static Vec3 getCameraPosition() {
		var minecraft = Minecraft.getInstance();
		var renderer = minecraft.gameRenderer;

		if (renderer == null) {
			return null;
		}

		var camera = renderer.getMainCamera();

		if (camera == null) {
			return null;
		}

		return camera.getPosition();
	}

	private static double getDistanceToCamera(Vec3 cameraPosition, BlockPos originPosition) {
		var originX = originPosition.getX() + 0.5;
		var originY = originPosition.getY() + 0.5;
		var originZ = originPosition.getZ() + 0.5;

		var deltaX = originX - cameraPosition.x;
		var deltaY = originY - cameraPosition.y;
		var deltaZ = originZ - cameraPosition.z;

		return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
	}

	private static float updateRenderAngleForEntry(ClientLevel level, WindmillLODEntry entry, float partialTicks) {
		var currentTick = level.getGameTime();
		var lastSynchronizationTick = entry.lastSynchronizationTick;
		var tickDelta = currentTick - lastSynchronizationTick;
		var rotationSpeed = entry.rotationSpeed;

		if (tickDelta > 0) {
			var updatedAngle = entry.rotationAngle + rotationSpeed * tickDelta;
			entry.rotationAngle = wrapDegrees(updatedAngle);
			entry.lastSynchronizationTick = currentTick;
		}

		var renderAngle = entry.rotationAngle + rotationSpeed * partialTicks;
		renderAngle += Mod.CONFIG.windmillBladeRotationAngleOffset;

		return wrapDegrees(renderAngle);
	}

	private static float wrapDegrees(float angle) {
		var wrapped = angle % 360.0F;

		if (wrapped < 0.0F) {
			wrapped += 360.0F;
		}

		return wrapped;
	}

	private static float getRotationDeltaForAngles(float previousAngle, float nextAngle) {
		var delta = Math.abs(previousAngle - nextAngle) % 360.0F;

		if (delta > 180.0F) {
			delta = 360.0F - delta;
		}

		return delta;
	}

	@Nullable
	private static IDhApiCustomRenderRegister resolveRenderRegister() {
		var worldProxy = DhBridge.worldProxy();

		if (worldProxy == null) {
			return null;
		}

		try {
			var levelWrapper = worldProxy.getSinglePlayerLevel();

			if (levelWrapper == null) {
				return null;
			}

			return levelWrapper.getRenderRegister();
		} catch (Exception ignored) {
			return null;
		}
	}
}

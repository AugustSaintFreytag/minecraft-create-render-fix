package net.saint.createrenderfixer.client;

import java.awt.Color;
import java.util.ArrayList;
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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.dh.DhBridge;
import net.saint.createrenderfixer.dh.WindmillLODEntry;
import net.saint.createrenderfixer.dh.WindmillLODManager;
import net.saint.createrenderfixer.utils.WindmillLODMaterialManager;

public final class WindmillLODRenderManager {

	// Configuration

	private static final float LOD_CLIP_DISTANCE_PADDING = 16.0F;
	private static final float LOD_CLIP_DISTANCE_OFFSET = 16.0F;

	private static final float HEIGHT_CLIP_DISTANCE_OVERRIDE = 1.0F;
	private static final int HEIGHT_CLIP_DISTANCE_PADDING = 1_000;

	// State

	private static final Map<UUID, IDhApiRenderableBoxGroup> RENDER_GROUPS = new ConcurrentHashMap<>();
	private static final Map<UUID, Float> LAST_RENDER_ANGLES = new ConcurrentHashMap<>();

	// Models

	private record BladeLengths(float widthLength, float heightLength) {
	}

	private record BladeSegmentCounts(int widthSegmentCount, int heightSegmentCount) {
	}

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

		for (var entry : WindmillLODManager.entries()) {
			if (!dimensionId.equals(entry.dimensionId)) {
				continue;
			}

			if (!shouldRenderEntryForLevel(level, entry, partialTicks)) {
				continue;
			}

			var renderAngle = getRenderAngleForEntry(level, entry, partialTicks);

			activeIdentifiers.add(entry.contraptionId);

			var renderGroup = ensureRenderGroup(renderFactory, renderRegister, entry);

			if (renderGroup == null) {
				continue;
			}

			updateRenderGroup(renderGroup, entry, renderAngle);
		}

		removeStaleRenderGroups(activeIdentifiers, renderRegister);
	}

	// Rendering

	@Nullable
	private static IDhApiRenderableBoxGroup ensureRenderGroup(IDhApiCustomRenderObjectFactory renderFactory,
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
			var rotationUpdateThreshold = Mod.CONFIG.windmillRotationUpdateThreshold;

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
		var bladeGeometry = entry.bladeGeometry;
		var bladeLengths = new BladeLengths(bladeGeometry.widthLength(), bladeGeometry.heightLength());
		var segmentCounts = new BladeSegmentCounts(bladeGeometry.widthSegmentCount(), bladeGeometry.heightSegmentCount());
		var bladeThickness = getBladeThickness();
		var bladeWidth = bladeGeometry.bladeWidth();
		var thicknessScale = getThicknessScaleForRotationAngle(rotationAngle);
		var bladeColor = getBladeColor();
		var bladeMaterial = getBladeMaterial();
		var baseBoxes = getCrossBoxesForAxis(entry.rotationAxis, bladeLengths, segmentCounts, bladeWidth, bladeThickness, thicknessScale,
				bladeColor, bladeMaterial);

		return rotateBoxesForAxis(baseBoxes, entry.rotationAxis, rotationAngle);
	}

	private static float getBladeThickness() {
		return Mod.CONFIG.windmillBladeThickness;
	}

	private static Color getBladeColor() {
		return WindmillLODMaterialManager.getBladeColor();
	}

	private static EDhApiBlockMaterial getBladeMaterial() {
		return WindmillLODMaterialManager.getBladeMaterial();
	}

	private static List<DhApiRenderableBox> getCrossBoxesForAxis(Direction.Axis rotationAxis, BladeLengths bladeLengths,
			BladeSegmentCounts segmentCounts, float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor,
			EDhApiBlockMaterial bladeMaterial) {
		return switch (rotationAxis) {
		case X -> getCrossBoxesForXAxis(bladeLengths, segmentCounts, bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);
		case Y -> getCrossBoxesForYAxis(bladeLengths, segmentCounts, bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);
		case Z -> getCrossBoxesForZAxis(bladeLengths, segmentCounts, bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);
		};
	}

	private static List<DhApiRenderableBox> getCrossBoxesForXAxis(BladeLengths bladeLengths, BladeSegmentCounts segmentCounts,
			float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var boxes = new ArrayList<DhApiRenderableBox>();
		addBladeSegmentsForAxis(boxes, Direction.Axis.Z, Direction.Axis.X, bladeLengths.widthLength(), segmentCounts.widthSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);
		addBladeSegmentsForAxis(boxes, Direction.Axis.Y, Direction.Axis.X, bladeLengths.heightLength(), segmentCounts.heightSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);

		return boxes;
	}

	private static List<DhApiRenderableBox> getCrossBoxesForYAxis(BladeLengths bladeLengths, BladeSegmentCounts segmentCounts,
			float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var boxes = new ArrayList<DhApiRenderableBox>();
		addBladeSegmentsForAxis(boxes, Direction.Axis.X, Direction.Axis.Y, bladeLengths.widthLength(), segmentCounts.widthSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);
		addBladeSegmentsForAxis(boxes, Direction.Axis.Z, Direction.Axis.Y, bladeLengths.heightLength(), segmentCounts.heightSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);

		return boxes;
	}

	private static List<DhApiRenderableBox> getCrossBoxesForZAxis(BladeLengths bladeLengths, BladeSegmentCounts segmentCounts,
			float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var boxes = new ArrayList<DhApiRenderableBox>();
		addBladeSegmentsForAxis(boxes, Direction.Axis.X, Direction.Axis.Z, bladeLengths.widthLength(), segmentCounts.widthSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);
		addBladeSegmentsForAxis(boxes, Direction.Axis.Y, Direction.Axis.Z, bladeLengths.heightLength(), segmentCounts.heightSegmentCount(),
				bladeWidth, bladeThickness, thicknessScale, bladeColor, bladeMaterial);

		return boxes;
	}

	private static void addBladeSegmentsForAxis(List<DhApiRenderableBox> boxes, Direction.Axis bladeAxis, Direction.Axis rotationAxis,
			float bladeLength, int segmentCount, float bladeWidth, float bladeThickness, float thicknessScale, Color bladeColor,
			EDhApiBlockMaterial bladeMaterial) {
		var segmentLength = bladeLength / segmentCount;
		var halfSegmentLength = segmentLength / 2.0F;
		var startOffset = -bladeLength / 2.0F + halfSegmentLength;
		var halfBladeWidth = bladeWidth / 2.0F;
		var halfBladeThickness = bladeThickness / 2.0F;
		var halfThicknessX = halfBladeThickness;
		var halfThicknessY = halfBladeThickness;
		var halfThicknessZ = halfBladeThickness;

		var widthAxis = getInPlaneWidthAxisForBlade(rotationAxis, bladeAxis);

		switch (widthAxis) {
		case X -> halfThicknessX = halfBladeWidth;
		case Y -> halfThicknessY = halfBladeWidth;
		case Z -> halfThicknessZ = halfBladeWidth;
		}

		switch (widthAxis) {
		case X -> halfThicknessX *= thicknessScale;
		case Y -> halfThicknessY *= thicknessScale;
		case Z -> halfThicknessZ *= thicknessScale;
		}

		for (var index = 0; index < segmentCount; index++) {
			var offset = startOffset + segmentLength * index;
			var minimumX = -halfThicknessX;
			var maximumX = halfThicknessX;
			var minimumY = -halfThicknessY;
			var maximumY = halfThicknessY;
			var minimumZ = -halfThicknessZ;
			var maximumZ = halfThicknessZ;

			switch (bladeAxis) {
			case X -> {
				minimumX = offset - halfSegmentLength;
				maximumX = offset + halfSegmentLength;
			}
			case Y -> {
				minimumY = offset - halfSegmentLength;
				maximumY = offset + halfSegmentLength;
			}
			case Z -> {
				minimumZ = offset - halfSegmentLength;
				maximumZ = offset + halfSegmentLength;
			}
			}

			boxes.add(createBox(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ, bladeColor, bladeMaterial));
		}
	}

	private static Direction.Axis getInPlaneWidthAxisForBlade(Direction.Axis rotationAxis, Direction.Axis bladeAxis) {
		for (var axis : Direction.Axis.values()) {
			if (axis != rotationAxis && axis != bladeAxis) {
				return axis;
			}
		}

		return bladeAxis;
	}

	private static float getThicknessScaleForRotationAngle(float rotationAngle) {
		var radians = Math.toRadians(rotationAngle);
		var weight = (Math.cos(radians * 4.0) + 1.0) / 2.0;
		var maximumThicknessScale = Mod.CONFIG.windmillBladeRotationThicknessScaleMaximum;
		var scale = 1.0 + (maximumThicknessScale - 1.0) * weight;

		return (float) scale;
	}

	private static DhApiRenderableBox createBox(double minimumX, double minimumY, double minimumZ, double maximumX, double maximumY,
			double maximumZ, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var minimumPos = new DhApiVec3d(minimumX, minimumY, minimumZ);
		var maximumPos = new DhApiVec3d(maximumX, maximumY, maximumZ);

		return new DhApiRenderableBox(minimumPos, maximumPos, bladeColor, bladeMaterial);
	}

	private static List<DhApiRenderableBox> rotateBoxesForAxis(List<DhApiRenderableBox> boxes, Direction.Axis rotationAxis,
			float rotationAngle) {
		if (boxes == null || boxes.isEmpty()) {
			return List.of();
		}

		var radians = Math.toRadians(rotationAngle);
		var sin = Math.sin(radians);
		var cos = Math.cos(radians);
		var rotated = new ArrayList<DhApiRenderableBox>(boxes.size());

		for (var box : boxes) {
			rotated.add(getRotatedBoxForAxis(box, rotationAxis, sin, cos));
		}

		return rotated;
	}

	private static DhApiRenderableBox getRotatedBoxForAxis(DhApiRenderableBox box, Direction.Axis rotationAxis, double sin, double cos) {
		var minX = box.minPos.x;
		var minY = box.minPos.y;
		var minZ = box.minPos.z;
		var maxX = box.maxPos.x;
		var maxY = box.maxPos.y;
		var maxZ = box.maxPos.z;

		var rotatedMinX = Double.POSITIVE_INFINITY;
		var rotatedMinY = Double.POSITIVE_INFINITY;
		var rotatedMinZ = Double.POSITIVE_INFINITY;
		var rotatedMaxX = Double.NEGATIVE_INFINITY;
		var rotatedMaxY = Double.NEGATIVE_INFINITY;
		var rotatedMaxZ = Double.NEGATIVE_INFINITY;

		for (var x : new double[] { minX, maxX }) {
			for (var y : new double[] { minY, maxY }) {
				for (var z : new double[] { minZ, maxZ }) {
					var rotatedPoint = rotatePointForAxis(rotationAxis, x, y, z, sin, cos);

					rotatedMinX = Math.min(rotatedMinX, rotatedPoint.x);
					rotatedMinY = Math.min(rotatedMinY, rotatedPoint.y);
					rotatedMinZ = Math.min(rotatedMinZ, rotatedPoint.z);
					rotatedMaxX = Math.max(rotatedMaxX, rotatedPoint.x);
					rotatedMaxY = Math.max(rotatedMaxY, rotatedPoint.y);
					rotatedMaxZ = Math.max(rotatedMaxZ, rotatedPoint.z);
				}
			}
		}

		var minimumPos = new DhApiVec3d(rotatedMinX, rotatedMinY, rotatedMinZ);
		var maximumPos = new DhApiVec3d(rotatedMaxX, rotatedMaxY, rotatedMaxZ);

		var color = box.color;
		var material = EDhApiBlockMaterial.getFromIndex(box.material);

		return new DhApiRenderableBox(minimumPos, maximumPos, color, material);
	}

	private static DhApiVec3d rotatePointForAxis(Direction.Axis rotationAxis, double x, double y, double z, double sin, double cos) {
		return switch (rotationAxis) {
		case X -> new DhApiVec3d(x, y * cos - z * sin, y * sin + z * cos);
		case Y -> new DhApiVec3d(x * cos + z * sin, y, -x * sin + z * cos);
		case Z -> new DhApiVec3d(x * cos - y * sin, x * sin + y * cos, z);
		};
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

	private static float getRenderAngleForEntry(ClientLevel level, WindmillLODEntry entry, float partialTicks) {
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

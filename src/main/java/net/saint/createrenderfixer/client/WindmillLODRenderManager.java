package net.saint.createrenderfixer.client;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderObjectFactory;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderRegister;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderableBoxGroup;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.dh.DhBridge;
import net.saint.createrenderfixer.dh.WindmillLODEntry;
import net.saint.createrenderfixer.dh.WindmillLODManager;

public final class WindmillLODRenderManager {

	// Configuration

	private static final float BLADE_LENGTH_SCALE = 1.0F;
	private static final float BLADE_THICKNESS_SCALE = 0.05F;
	private static final float MIN_BLADE_LENGTH = 1.0F;
	private static final float MIN_BLADE_THICKNESS = 0.2F;
	private static final float ROTATION_UPDATE_THRESHOLD = 0.25F;
	private static final float TARGET_SEGMENT_LENGTH = 1.0F;

	private static final int MIN_SEGMENT_COUNT = 6;
	private static final int MAX_SEGMENT_COUNT = 24;

	private static final boolean USE_ROTATION_TEST_OVERRIDE = false;
	private static final float ROTATION_TEST_OVERRIDE_DEGREES = 30.0F;

	private static final Color WINDMILL_CROSS_COLOR = new Color(149, 129, 95);
	private static final EDhApiBlockMaterial WINDMILL_CROSS_MATERIAL = EDhApiBlockMaterial.WOOD;

	// State

	private static final Map<UUID, IDhApiRenderableBoxGroup> RENDER_GROUPS = new ConcurrentHashMap<>();
	private static final Map<UUID, Float> LAST_RENDER_ANGLES = new ConcurrentHashMap<>();

	// Models

	private record BladeLengths(float widthLength, float heightLength) {
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
			if (!dimensionId.equals(entry.dimensionId())) {
				continue;
			}

			activeIdentifiers.add(entry.contraptionId());

			var renderGroup = ensureRenderGroup(renderFactory, renderRegister, level, entry);
			var renderAngle = getRenderAngleForEntry(level, entry, partialTicks);
			renderAngle = getEffectiveRotationAngle(renderAngle);

			if (renderGroup != null) {
				updateRenderGroup(renderGroup, level, entry, renderAngle);
			}
		}

		removeStaleRenderGroups(activeIdentifiers, renderRegister);
	}

	// Rendering

	@Nullable
	private static IDhApiRenderableBoxGroup ensureRenderGroup(IDhApiCustomRenderObjectFactory renderFactory,
			IDhApiCustomRenderRegister renderRegister, ClientLevel level, WindmillLODEntry entry) {
		var renderGroup = RENDER_GROUPS.get(entry.contraptionId());

		if (renderGroup != null) {
			entry.setRenderGroupId(renderGroup.getId());

			return renderGroup;
		}

		var resourceLocation = Mod.MOD_ID + ":windmill/" + entry.contraptionId();
		var originPosition = toOrigin(getRenderAnchorPositionForEntry(level, entry));
		var crossBoxes = getWindmillCrossBoxesForEntry(entry, getEffectiveRotationAngle(entry.rotationAngle()));

		try {
			renderGroup = renderFactory.createRelativePositionedGroup(resourceLocation, originPosition, crossBoxes);
			renderGroup.setOriginBlockPos(originPosition);

			renderRegister.add(renderGroup);
			entry.setRenderGroupId(renderGroup.getId());
			RENDER_GROUPS.put(entry.contraptionId(), renderGroup);
		} catch (IllegalArgumentException exception) {
			Mod.LOGGER.warn("Failed to register windmill render group '{}'.", resourceLocation, exception);

			return null;
		}

		return renderGroup;
	}

	private static void updateRenderGroup(IDhApiRenderableBoxGroup renderGroup, ClientLevel level, WindmillLODEntry entry,
			float renderAngle) {
		var originPosition = toOrigin(getRenderAnchorPositionForEntry(level, entry));
		renderGroup.setOriginBlockPos(originPosition);

		var lastAngle = LAST_RENDER_ANGLES.get(entry.contraptionId());

		if (lastAngle != null) {
			var rotationDelta = getRotationDeltaForAngles(lastAngle, renderAngle);

			if (rotationDelta < ROTATION_UPDATE_THRESHOLD) {
				return;
			}
		}

		updateRenderGroupBoxes(renderGroup, entry, renderAngle);
		LAST_RENDER_ANGLES.put(entry.contraptionId(), renderAngle);
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
			} catch (Exception ignored) {
				// DH may reject removal while it is still initializing; ignore.
			}

			iterator.remove();
			LAST_RENDER_ANGLES.remove(contraptionId);
		}
	}

	// Geometry

	private static List<DhApiRenderableBox> getWindmillCrossBoxesForEntry(WindmillLODEntry entry, float rotationAngle) {
		var bladeLengths = getBladeLengthsForEntry(entry);
		var bladeThickness = getBladeThicknessForEntry(entry);
		var baseBoxes = getCrossBoxesForAxis(entry.rotationAxis(), bladeLengths, bladeThickness);

		return rotateBoxesForAxis(baseBoxes, entry.rotationAxis(), rotationAngle);
	}

	private static BladeLengths getBladeLengthsForEntry(WindmillLODEntry entry) {
		var widthLength = entry.planeWidth() * BLADE_LENGTH_SCALE;
		var heightLength = entry.planeHeight() * BLADE_LENGTH_SCALE;

		if (widthLength < MIN_BLADE_LENGTH) {
			widthLength = MIN_BLADE_LENGTH;
		}

		if (heightLength < MIN_BLADE_LENGTH) {
			heightLength = MIN_BLADE_LENGTH;
		}

		return new BladeLengths(widthLength, heightLength);
	}

	private static float getBladeThicknessForEntry(WindmillLODEntry entry) {
		var minPlaneSize = Math.min(entry.planeWidth(), entry.planeHeight());
		var bladeThickness = minPlaneSize * BLADE_THICKNESS_SCALE;

		if (bladeThickness < MIN_BLADE_THICKNESS) {
			return MIN_BLADE_THICKNESS;
		}

		return bladeThickness;
	}

	private static List<DhApiRenderableBox> getCrossBoxesForAxis(Direction.Axis rotationAxis, BladeLengths bladeLengths,
			float bladeThickness) {
		return switch (rotationAxis) {
		case X -> getCrossBoxesForXAxis(bladeLengths, bladeThickness);
		case Y -> getCrossBoxesForYAxis(bladeLengths, bladeThickness);
		case Z -> getCrossBoxesForZAxis(bladeLengths, bladeThickness);
		};
	}

	private static List<DhApiRenderableBox> getCrossBoxesForXAxis(BladeLengths bladeLengths, float bladeThickness) {
		var boxes = new ArrayList<DhApiRenderableBox>();
		var halfBladeThickness = bladeThickness / 2.0F;
		addBladeSegmentsForAxis(boxes, Direction.Axis.Z, bladeLengths.widthLength(), halfBladeThickness);
		addBladeSegmentsForAxis(boxes, Direction.Axis.Y, bladeLengths.heightLength(), halfBladeThickness);

		return boxes;
	}

	private static List<DhApiRenderableBox> getCrossBoxesForYAxis(BladeLengths bladeLengths, float bladeThickness) {
		var boxes = new ArrayList<DhApiRenderableBox>();
		var halfBladeThickness = bladeThickness / 2.0F;
		addBladeSegmentsForAxis(boxes, Direction.Axis.X, bladeLengths.widthLength(), halfBladeThickness);
		addBladeSegmentsForAxis(boxes, Direction.Axis.Z, bladeLengths.heightLength(), halfBladeThickness);

		return boxes;
	}

	private static List<DhApiRenderableBox> getCrossBoxesForZAxis(BladeLengths bladeLengths, float bladeThickness) {
		var boxes = new ArrayList<DhApiRenderableBox>();
		var halfBladeThickness = bladeThickness / 2.0F;
		addBladeSegmentsForAxis(boxes, Direction.Axis.X, bladeLengths.widthLength(), halfBladeThickness);
		addBladeSegmentsForAxis(boxes, Direction.Axis.Y, bladeLengths.heightLength(), halfBladeThickness);

		return boxes;
	}

	private static void addBladeSegmentsForAxis(List<DhApiRenderableBox> boxes, Direction.Axis bladeAxis, float bladeLength,
			float halfBladeThickness) {
		var segmentCount = getSegmentCountForBladeLength(bladeLength);
		var segmentLength = bladeLength / segmentCount;
		var halfSegmentLength = segmentLength / 2.0F;
		var startOffset = -bladeLength / 2.0F + halfSegmentLength;

		for (var index = 0; index < segmentCount; index++) {
			var offset = startOffset + segmentLength * index;
			var minimumX = -halfBladeThickness;
			var maximumX = halfBladeThickness;
			var minimumY = -halfBladeThickness;
			var maximumY = halfBladeThickness;
			var minimumZ = -halfBladeThickness;
			var maximumZ = halfBladeThickness;

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

			boxes.add(createBox(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ));
		}
	}

	private static int getSegmentCountForBladeLength(float bladeLength) {
		var segmentCount = Math.round(bladeLength / TARGET_SEGMENT_LENGTH);

		if (segmentCount < MIN_SEGMENT_COUNT) {
			segmentCount = MIN_SEGMENT_COUNT;
		}

		if (segmentCount > MAX_SEGMENT_COUNT) {
			segmentCount = MAX_SEGMENT_COUNT;
		}

		if (segmentCount < 1) {
			segmentCount = 1;
		}

		return segmentCount;
	}

	private static DhApiRenderableBox createBox(double minimumX, double minimumY, double minimumZ, double maximumX, double maximumY,
			double maximumZ) {
		var minimumPos = new DhApiVec3d(minimumX, minimumY, minimumZ);
		var maximumPos = new DhApiVec3d(maximumX, maximumY, maximumZ);

		return new DhApiRenderableBox(minimumPos, maximumPos, WINDMILL_CROSS_COLOR, WINDMILL_CROSS_MATERIAL);
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

		return new DhApiRenderableBox(minimumPos, maximumPos, WINDMILL_CROSS_COLOR, WINDMILL_CROSS_MATERIAL);
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

	private static BlockPos getRenderAnchorPositionForEntry(ClientLevel level, WindmillLODEntry entry) {
		if (level == null) {
			return entry.anchorPosition();
		}

		var bearingDirection = getBearingDirectionForEntry(level, entry);

		if (bearingDirection == null) {
			return entry.anchorPosition();
		}

		return entry.anchorPosition().relative(bearingDirection);
	}

	private static Direction getBearingDirectionForEntry(ClientLevel level, WindmillLODEntry entry) {
		var blockEntity = level.getBlockEntity(entry.anchorPosition());

		if (!(blockEntity instanceof WindmillBearingBlockEntity windmillBearing)) {
			return null;
		}

		var blockState = windmillBearing.getBlockState();

		if (blockState.hasProperty(BlockStateProperties.FACING)) {
			return blockState.getValue(BlockStateProperties.FACING);
		}

		if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
		}

		return null;
	}

	private static String getDimensionIdForLevel(ClientLevel level) {
		return level.dimension().location().toString();
	}

	private static float getRenderAngleForEntry(ClientLevel level, WindmillLODEntry entry, float partialTicks) {
		var currentTick = level.getGameTime();
		var lastSynchronizationTick = entry.lastSynchronizationTick();
		var tickDelta = currentTick - lastSynchronizationTick;

		if (tickDelta > 0) {
			var updatedAngle = entry.rotationAngle() + entry.rotationSpeed() * tickDelta;
			entry.setRotationAngle(wrapDegrees(updatedAngle));
			entry.setLastSynchronizationTick(currentTick);
		}

		var renderAngle = entry.rotationAngle() + entry.rotationSpeed() * partialTicks;

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

	private static float getEffectiveRotationAngle(float rotationAngle) {
		if (!USE_ROTATION_TEST_OVERRIDE) {
			return rotationAngle;
		}

		return ROTATION_TEST_OVERRIDE_DEGREES;
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

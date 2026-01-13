package net.saint.createrenderfixer.dh;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.saint.createrenderfixer.dh.WindmillLODGeometryUtil.BladeGeometry;

public final class WindmillLODEntry {

	// Nbt

	public static final String NBT_CONTRAPTION_ID = "ContraptionId";
	public static final String NBT_DIMENSION_ID = "DimensionId";
	public static final String NBT_ANCHOR_POSITION = "AnchorPosition";
	public static final String NBT_ROTATION_AXIS = "RotationAxis";
	public static final String NBT_BEARING_DIRECTION = "BearingDirection";
	public static final String NBT_PLANE_WIDTH = "PlaneWidth";
	public static final String NBT_PLANE_HEIGHT = "PlaneHeight";
	public static final String NBT_PLANE_DEPTH = "PlaneDepth";
	public static final String NBT_ROTATION_SPEED = "RotationSpeed";
	public static final String NBT_ROTATION_ANGLE = "RotationAngle";
	public static final String NBT_TICK_REGISTERED = "TickRegistered";
	public static final String NBT_LAST_SYNCHRONIZATION_TICK = "LastSynchronizationTick";

	private static final String LEGACY_NBT_IDENTIFIER = "identifier";
	private static final String LEGACY_NBT_ID = "id";
	private static final String LEGACY_NBT_DIMENSION = "dimension";
	private static final String LEGACY_NBT_ANCHOR = "anchor";
	private static final String LEGACY_NBT_AXIS = "axis";
	private static final String LEGACY_NBT_BEARING_DIRECTION = "bearingDirection";
	private static final String LEGACY_NBT_PLANE_WIDTH = "planeWidth";
	private static final String LEGACY_NBT_PLANE_HEIGHT = "planeHeight";
	private static final String LEGACY_NBT_PLANE_DEPTH = "planeDepth";
	private static final String LEGACY_NBT_SPEED = "speed";
	private static final String LEGACY_NBT_ANGLE = "angle";
	private static final String LEGACY_NBT_LAST_SYNCHRONIZATION_TICK = "lastSynchronizationTick";

	// State

	public final UUID contraptionId;
	public final String dimensionId;
	public final BlockPos anchorPosition;
	public final Direction.Axis rotationAxis;
	public final Direction bearingDirection;
	public final float planeWidth;
	public final float planeHeight;
	public final float planeDepth;
	public final BladeGeometry bladeGeometry;
	public final long tickRegistered;

	public volatile float rotationSpeed;
	public volatile float rotationAngle;
	public volatile long lastSynchronizationTick;
	public volatile boolean isStale;
	public volatile long renderGroupId;

	// Init

	public WindmillLODEntry(UUID contraptionId, String dimensionId, BlockPos anchorPosition, Direction.Axis rotationAxis,
			Direction bearingDirection, float planeWidth, float planeHeight, float planeDepth, long tickRegistered, float rotationSpeed,
			float rotationAngle, long lastSynchronizationTick) {
		this.contraptionId = contraptionId;
		this.dimensionId = dimensionId;
		this.anchorPosition = anchorPosition;
		this.rotationAxis = rotationAxis;
		this.bearingDirection = bearingDirection;
		this.planeWidth = planeWidth;
		this.planeHeight = planeHeight;
		this.planeDepth = planeDepth;
		this.bladeGeometry = WindmillLODGeometryUtil.makeBladeGeometry(planeWidth, planeHeight, planeDepth);
		this.tickRegistered = tickRegistered;
		this.rotationSpeed = rotationSpeed;
		this.rotationAngle = rotationAngle;
		this.lastSynchronizationTick = lastSynchronizationTick;
		this.isStale = false;
		this.renderGroupId = -1;
	}

	// Comparison

	public boolean matchesRegistrationData(WindmillLODEntry other) {
		if (other == null) {
			return false;
		}

		if (!dimensionId.equals(other.dimensionId)) {
			return false;
		}

		if (!anchorPosition.equals(other.anchorPosition)) {
			return false;
		}

		if (rotationAxis != other.rotationAxis) {
			return false;
		}

		if (bearingDirection != other.bearingDirection) {
			return false;
		}

		if (planeWidth != other.planeWidth) {
			return false;
		}

		if (planeHeight != other.planeHeight) {
			return false;
		}

		if (planeDepth != other.planeDepth) {
			return false;
		}

		return true;
	}

	// Persistence

	public WindmillLODEntry createPersistenceSnapshot() {
		return new WindmillLODEntry(contraptionId, dimensionId, anchorPosition, rotationAxis, bearingDirection, planeWidth, planeHeight,
				planeDepth, tickRegistered, rotationSpeed, rotationAngle, lastSynchronizationTick);
	}

	// Encoding

	@Nullable
	public CompoundTag toNbt() {
		if (contraptionId == null || dimensionId == null || anchorPosition == null || rotationAxis == null || bearingDirection == null) {
			return null;
		}

		var windmillTag = new CompoundTag();
		windmillTag.putString(NBT_CONTRAPTION_ID, contraptionId.toString());
		windmillTag.putString(NBT_DIMENSION_ID, dimensionId);
		windmillTag.put(NBT_ANCHOR_POSITION, NbtUtils.writeBlockPos(anchorPosition));
		windmillTag.putString(NBT_ROTATION_AXIS, rotationAxis.getName());
		windmillTag.putString(NBT_BEARING_DIRECTION, bearingDirection.getName());
		windmillTag.putFloat(NBT_PLANE_WIDTH, planeWidth);
		windmillTag.putFloat(NBT_PLANE_HEIGHT, planeHeight);
		windmillTag.putFloat(NBT_PLANE_DEPTH, planeDepth);
		windmillTag.putFloat(NBT_ROTATION_SPEED, rotationSpeed);
		windmillTag.putFloat(NBT_ROTATION_ANGLE, rotationAngle);
		windmillTag.putLong(NBT_TICK_REGISTERED, tickRegistered);
		windmillTag.putLong(NBT_LAST_SYNCHRONIZATION_TICK, lastSynchronizationTick);

		return windmillTag;
	}

	@Nullable
	public static WindmillLODEntry fromNbt(@Nullable CompoundTag entryTag) {
		if (entryTag == null) {
			return null;
		}

		var identifierValue = getStringForKeys(entryTag, NBT_CONTRAPTION_ID, LEGACY_NBT_IDENTIFIER, LEGACY_NBT_ID);
		var dimensionIdentifier = getStringForKeys(entryTag, NBT_DIMENSION_ID, LEGACY_NBT_DIMENSION);

		if (identifierValue.isBlank() || dimensionIdentifier.isBlank()) {
			return null;
		}

		var anchorTag = getCompoundTagForKeys(entryTag, NBT_ANCHOR_POSITION, LEGACY_NBT_ANCHOR);

		if (anchorTag == null) {
			return null;
		}

		var anchorPosition = NbtUtils.readBlockPos(anchorTag);
		var axisName = getStringForKeys(entryTag, NBT_ROTATION_AXIS, LEGACY_NBT_AXIS);
		var rotationAxis = getRotationAxisForName(axisName);
		var bearingDirectionName = getStringForKeys(entryTag, NBT_BEARING_DIRECTION, LEGACY_NBT_BEARING_DIRECTION);
		var bearingDirection = getBearingDirectionForName(bearingDirectionName, rotationAxis);
		var planeWidth = getFloatForKeysOrDefault(entryTag, 1.0F, NBT_PLANE_WIDTH, LEGACY_NBT_PLANE_WIDTH);
		var planeHeight = getFloatForKeysOrDefault(entryTag, 1.0F, NBT_PLANE_HEIGHT, LEGACY_NBT_PLANE_HEIGHT);
		var planeDepth = getFloatForKeysOrDefault(entryTag, 1.0F, NBT_PLANE_DEPTH, LEGACY_NBT_PLANE_DEPTH);
		var rotationSpeed = getFloatForKeysOrDefault(entryTag, 0.0F, NBT_ROTATION_SPEED, LEGACY_NBT_SPEED);
		var rotationAngle = getFloatForKeysOrDefault(entryTag, 0.0F, NBT_ROTATION_ANGLE, LEGACY_NBT_ANGLE);
		var lastSynchronizationTick = getLongForKeysOrDefault(entryTag, 0L, NBT_LAST_SYNCHRONIZATION_TICK,
				LEGACY_NBT_LAST_SYNCHRONIZATION_TICK);
		var tickRegistered = getLongForKeysOrDefault(entryTag, lastSynchronizationTick, NBT_TICK_REGISTERED);

		try {
			var contraptionIdentifier = UUID.fromString(identifierValue);

			return new WindmillLODEntry(contraptionIdentifier, dimensionIdentifier, anchorPosition, rotationAxis, bearingDirection,
					planeWidth, planeHeight, planeDepth, tickRegistered, rotationSpeed, rotationAngle, lastSynchronizationTick);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	// Utility

	private static String getStringForKeys(CompoundTag entryTag, String primaryKey, String... fallbackKeys) {
		var value = entryTag.getString(primaryKey);

		if (!value.isBlank()) {
			return value;
		}

		for (var fallbackKey : fallbackKeys) {
			value = entryTag.getString(fallbackKey);

			if (!value.isBlank()) {
				return value;
			}
		}

		return "";
	}

	@Nullable
	private static CompoundTag getCompoundTagForKeys(CompoundTag entryTag, String primaryKey, String... fallbackKeys) {
		if (entryTag.contains(primaryKey, Tag.TAG_COMPOUND)) {
			return entryTag.getCompound(primaryKey);
		}

		for (var fallbackKey : fallbackKeys) {
			if (entryTag.contains(fallbackKey, Tag.TAG_COMPOUND)) {
				return entryTag.getCompound(fallbackKey);
			}
		}

		return null;
	}

	private static float getFloatForKeysOrDefault(CompoundTag entryTag, float defaultValue, String primaryKey, String... fallbackKeys) {
		if (entryTag.contains(primaryKey, Tag.TAG_FLOAT)) {
			return entryTag.getFloat(primaryKey);
		}

		for (var fallbackKey : fallbackKeys) {
			if (entryTag.contains(fallbackKey, Tag.TAG_FLOAT)) {
				return entryTag.getFloat(fallbackKey);
			}
		}

		return defaultValue;
	}

	private static long getLongForKeysOrDefault(CompoundTag entryTag, long defaultValue, String primaryKey, String... fallbackKeys) {
		if (entryTag.contains(primaryKey, Tag.TAG_LONG)) {
			return entryTag.getLong(primaryKey);
		}

		for (var fallbackKey : fallbackKeys) {
			if (entryTag.contains(fallbackKey, Tag.TAG_LONG)) {
				return entryTag.getLong(fallbackKey);
			}
		}

		return defaultValue;
	}

	private static Direction.Axis getRotationAxisForName(String axisName) {
		var axis = Direction.Axis.byName(axisName);

		if (axis != null) {
			return axis;
		}

		return Direction.Axis.Y;
	}

	private static Direction getBearingDirectionForName(String directionName, Direction.Axis rotationAxis) {
		if (!directionName.isBlank()) {
			var direction = Direction.byName(directionName);

			if (direction != null) {
				return direction;
			}
		}

		return Direction.fromAxisAndDirection(rotationAxis, Direction.AxisDirection.POSITIVE);
	}
}

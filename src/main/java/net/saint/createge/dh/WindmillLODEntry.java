package net.saint.createge.dh;

import java.util.UUID;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.saint.createge.library.Size2D;

// TODO: Check if plane size is now obsolete and can be removed.

public final class WindmillLODEntry {

	// Nbt

	public static final String NBT_CONTRAPTION_ID = "ContraptionId";
	public static final String NBT_DIMENSION_ID = "DimensionId";
	public static final String NBT_ANCHOR_POSITION = "AnchorPosition";
	public static final String NBT_ROTATION_AXIS = "RotationAxis";
	public static final String NBT_BEARING_DIRECTION = "BearingDirection";
	public static final String NBT_PLANE_SIZE = "PlaneSize";
	public static final String NBT_BLADE_GEOMETRY = "BladeGeometry";
	public static final String NBT_ROTATION_SPEED = "RotationSpeed";
	public static final String NBT_ROTATION_ANGLE = "RotationAngle";
	public static final String NBT_TICK_REGISTERED = "TickRegistered";
	public static final String NBT_LAST_SYNCHRONIZATION_TICK = "LastSynchronizationTick";

	// State

	public final UUID contraptionId;
	public final String dimensionId;
	public final BlockPos anchorPosition;
	public final Direction.Axis rotationAxis;
	public final Direction bearingDirection;
	public final Size2D planeSize;
	public final WindmillBladeGeometry bladeGeometry;
	public final long tickRegistered;

	public volatile float rotationSpeed;
	public volatile float rotationAngle;
	public volatile long lastSynchronizationTick;
	public volatile boolean isStale;
	public volatile long renderGroupId;

	// Init

	public WindmillLODEntry(UUID contraptionId, String dimensionId, BlockPos anchorPosition, Direction.Axis rotationAxis,
			Direction bearingDirection, Size2D planeSize, WindmillBladeGeometry bladeGeometry, long tickRegistered, float rotationSpeed,
			float rotationAngle, long lastSynchronizationTick) {
		this.contraptionId = contraptionId;
		this.dimensionId = dimensionId;
		this.anchorPosition = anchorPosition;
		this.rotationAxis = rotationAxis;
		this.bearingDirection = bearingDirection;
		this.planeSize = planeSize;
		this.bladeGeometry = bladeGeometry;
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

		if (!planeSize.equals(other.planeSize)) {
			return false;
		}

		if (!bladeGeometry.equals(other.bladeGeometry)) {
			return false;
		}

		return true;
	}

	// Persistence

	public WindmillLODEntry createPersistenceSnapshot() {
		return new WindmillLODEntry(contraptionId, dimensionId, anchorPosition, rotationAxis, bearingDirection, planeSize, bladeGeometry,
				tickRegistered, rotationSpeed, rotationAngle, lastSynchronizationTick);
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
		windmillTag.put(NBT_PLANE_SIZE, planeSize.toNbt());
		windmillTag.put(NBT_BLADE_GEOMETRY, bladeGeometry.toNbt());
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

		var identifierValue = getStringForKeys(entryTag, NBT_CONTRAPTION_ID);
		var dimensionIdentifier = getStringForKeys(entryTag, NBT_DIMENSION_ID);

		if (identifierValue.isBlank() || dimensionIdentifier.isBlank()) {
			return null;
		}

		var anchorTag = getCompoundTagForKey(entryTag, NBT_ANCHOR_POSITION);

		if (anchorTag == null) {
			return null;
		}

		var anchorPosition = NbtUtils.readBlockPos(anchorTag);
		var axisName = getStringForKeys(entryTag, NBT_ROTATION_AXIS);
		var rotationAxis = getRotationAxisForName(axisName);
		var bearingDirectionName = getStringForKeys(entryTag, NBT_BEARING_DIRECTION);
		var bearingDirection = getBearingDirectionForName(bearingDirectionName, rotationAxis);
		var planeSize = getCompoundTagAndDecodeForKeyOrDefault(entryTag, NBT_PLANE_SIZE, Size2D::fromNbt, Size2D.zero());
		var bladeGeometry = getCompoundTagAndDecodeForKeyOrDefault(entryTag, NBT_BLADE_GEOMETRY, WindmillBladeGeometry::fromNbt,
				WindmillBladeGeometry.zero());
		var rotationSpeed = getFloatForKeyOrDefault(entryTag, 0.0F, NBT_ROTATION_SPEED);
		var rotationAngle = getFloatForKeyOrDefault(entryTag, 0.0F, NBT_ROTATION_ANGLE);
		var lastSynchronizationTick = getLongForKeyOrDefault(entryTag, 0L, NBT_LAST_SYNCHRONIZATION_TICK);
		var tickRegistered = getLongForKeyOrDefault(entryTag, lastSynchronizationTick, NBT_TICK_REGISTERED);

		try {
			var contraptionIdentifier = UUID.fromString(identifierValue);

			return new WindmillLODEntry(contraptionIdentifier, dimensionIdentifier, anchorPosition, rotationAxis, bearingDirection,
					planeSize, bladeGeometry, tickRegistered, rotationSpeed, rotationAngle, lastSynchronizationTick);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	// NBT Resolve

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

	private static <T> T getCompoundTagAndDecodeForKeyOrDefault(CompoundTag entryTag, String key, Function<CompoundTag, T> block,
			T defaultValue) {
		var tag = getCompoundTagForKey(entryTag, key);

		if (tag == null) {
			return defaultValue;
		}

		return block.apply(tag);
	}

	private static CompoundTag getCompoundTagForKey(CompoundTag entryTag, String key) {
		if (!entryTag.contains(key, Tag.TAG_COMPOUND)) {
			return null;
		}

		return entryTag.getCompound(key);
	}

	private static float getFloatForKeyOrDefault(CompoundTag entryTag, float defaultValue, String key) {
		if (!entryTag.contains(key, Tag.TAG_FLOAT)) {
			return defaultValue;
		}

		return entryTag.getFloat(key);
	}

	private static long getLongForKeyOrDefault(CompoundTag entryTag, long defaultValue, String key) {
		if (!entryTag.contains(key, Tag.TAG_LONG)) {
			return defaultValue;
		}

		return entryTag.getLong(key);
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

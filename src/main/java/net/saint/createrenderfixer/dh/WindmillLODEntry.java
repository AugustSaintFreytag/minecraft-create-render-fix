package net.saint.createrenderfixer.dh;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class WindmillLODEntry {

	// State

	private final UUID contraptionId;
	private final String dimensionId;
	private final BlockPos anchorPosition;
	private final Direction.Axis rotationAxis;
	private final Direction bearingDirection;
	private final float planeWidth;
	private final float planeHeight;

	private volatile float rotationSpeed;
	private volatile float rotationAngle;
	private volatile long lastSynchronizationTick;
	private volatile boolean stale;
	private volatile long renderGroupId;

	// Init

	public WindmillLODEntry(UUID contraptionId, String dimensionId, BlockPos anchorPosition, Direction.Axis rotationAxis,
			Direction bearingDirection, float planeWidth, float planeHeight, float rotationSpeed, float rotationAngle,
			long lastSynchronizationTick) {
		this.contraptionId = contraptionId;
		this.dimensionId = dimensionId;
		this.anchorPosition = anchorPosition;
		this.rotationAxis = rotationAxis;
		this.bearingDirection = bearingDirection;
		this.planeWidth = planeWidth;
		this.planeHeight = planeHeight;
		this.rotationSpeed = rotationSpeed;
		this.rotationAngle = rotationAngle;
		this.lastSynchronizationTick = lastSynchronizationTick;
		this.stale = false;
		this.renderGroupId = -1;
	}

	// Access

	public UUID contraptionId() {
		return contraptionId;
	}

	public String dimensionId() {
		return dimensionId;
	}

	public BlockPos anchorPosition() {
		return anchorPosition;
	}

	public Direction.Axis rotationAxis() {
		return rotationAxis;
	}

	public Direction bearingDirection() {
		return bearingDirection;
	}

	public float planeWidth() {
		return planeWidth;
	}

	public float planeHeight() {
		return planeHeight;
	}

	public float rotationSpeed() {
		return rotationSpeed;
	}

	public float rotationAngle() {
		return rotationAngle;
	}

	public long lastSynchronizationTick() {
		return lastSynchronizationTick;
	}

	public boolean isStale() {
		return stale;
	}

	public long renderGroupId() {
		return renderGroupId;
	}

	// Mutators

	public void setRotationSpeed(float rotationSpeed) {
		this.rotationSpeed = rotationSpeed;
	}

	public void setRotationAngle(float rotationAngle) {
		this.rotationAngle = rotationAngle;
	}

	public void setStale(boolean stale) {
		this.stale = stale;
	}

	public void setLastSynchronizationTick(long lastSynchronizationTick) {
		this.lastSynchronizationTick = lastSynchronizationTick;
	}

	public void setRenderGroupId(long renderGroupId) {
		this.renderGroupId = renderGroupId;
	}

	// Comparison

	public boolean matchesRegistrationData(WindmillLODEntry other) {
		if (other == null) {
			return false;
		}

		if (!dimensionId.equals(other.dimensionId())) {
			return false;
		}

		if (!anchorPosition.equals(other.anchorPosition())) {
			return false;
		}

		if (rotationAxis != other.rotationAxis()) {
			return false;
		}

		if (bearingDirection != other.bearingDirection()) {
			return false;
		}

		if (planeWidth != other.planeWidth()) {
			return false;
		}

		if (planeHeight != other.planeHeight()) {
			return false;
		}

		return true;
	}

	// Persistence

	public WindmillLODEntry createPersistenceSnapshot() {
		return new WindmillLODEntry(contraptionId, dimensionId, anchorPosition, rotationAxis, bearingDirection, planeWidth, planeHeight,
				rotationSpeed, rotationAngle, lastSynchronizationTick);
	}
}

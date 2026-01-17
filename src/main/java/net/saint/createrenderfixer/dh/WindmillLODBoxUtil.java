package net.saint.createrenderfixer.dh;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.objects.math.DhApiVec3d;
import com.seibel.distanthorizons.api.objects.render.DhApiRenderableBox;

import net.minecraft.core.Direction;
import net.saint.createrenderfixer.Mod;

public final class WindmillLODBoxUtil {

	// API

	public static List<DhApiRenderableBox> makeWindmillBladeBoxes(WindmillLODEntry entry, Color bladeColor,
			EDhApiBlockMaterial bladeMaterial, float rotationAngle) {
		if (entry == null) {
			return List.of();
		}

		var bladeGeometry = entry.bladeGeometry;

		if (bladeGeometry == null) {
			return List.of();
		}

		var rotationAxis = getRotationAxisForEntry(entry);
		var bladeSize = getBladeSizeForGeometry(bladeGeometry);
		var numberOfSegments = getSegmentCountForGeometry(bladeGeometry);

		if (bladeSize.length() <= 0.0F || numberOfSegments <= 0) {
			return List.of();
		}

		var volumeScale = getVolumeScaleForRotationAngle(bladeSize.length(), rotationAngle);
		var baseBoxes = getCrossBoxesForAxis(rotationAxis, bladeSize, numberOfSegments, volumeScale, bladeColor, bladeMaterial);

		return rotateBoxesForAxis(baseBoxes, rotationAxis, rotationAngle);
	}

	// Geometry

	private static WindmillBladeSize getBladeSizeForGeometry(WindmillBladeGeometry bladeGeometry) {
		var bladeLength = bladeGeometry.length();
		var minimumLength = Math.max(0.0F, Mod.CONFIG.windmillBladeMinimumLength);

		if (bladeLength > 0.0F && bladeLength < minimumLength) {
			bladeLength = minimumLength;
		}

		return new WindmillBladeSize(bladeLength, bladeGeometry.width(), bladeGeometry.depth());
	}

	private static int getSegmentCountForGeometry(WindmillBladeGeometry bladeGeometry) {
		var segmentCount = bladeGeometry.numberOfSegments();

		if (segmentCount <= 0) {
			segmentCount = 1;
		}

		return segmentCount;
	}

	private static List<DhApiRenderableBox> getCrossBoxesForAxis(Direction.Axis rotationAxis, WindmillBladeSize bladeSize, int segmentCount,
			float volumeScale, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var boxes = new ArrayList<DhApiRenderableBox>(segmentCount * 2);

		switch (rotationAxis) {
			case X -> {
				addBladeSegmentsForAxis(boxes, Direction.Axis.Y, rotationAxis, bladeSize, segmentCount, volumeScale, bladeColor,
						bladeMaterial);
				addBladeSegmentsForAxis(boxes, Direction.Axis.Z, rotationAxis, bladeSize, segmentCount, volumeScale, bladeColor,
						bladeMaterial);
			}
			case Y -> {
				addBladeSegmentsForAxis(boxes, Direction.Axis.X, rotationAxis, bladeSize, segmentCount, volumeScale, bladeColor,
						bladeMaterial);
				addBladeSegmentsForAxis(boxes, Direction.Axis.Z, rotationAxis, bladeSize, segmentCount, volumeScale, bladeColor,
						bladeMaterial);
			}
			case Z -> {
				addBladeSegmentsForAxis(boxes, Direction.Axis.X, rotationAxis, bladeSize, segmentCount, volumeScale, bladeColor,
						bladeMaterial);
				addBladeSegmentsForAxis(boxes, Direction.Axis.Y, rotationAxis, bladeSize, segmentCount, volumeScale, bladeColor,
						bladeMaterial);
			}
		}

		return boxes;
	}

	private static void addBladeSegmentsForAxis(List<DhApiRenderableBox> boxes, Direction.Axis bladeAxis, Direction.Axis rotationAxis,
			WindmillBladeSize bladeSize, int segmentCount, float volumeScale, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		if (segmentCount <= 0 || bladeSize.length() <= 0.0F) {
			return;
		}

		var segmentLength = bladeSize.length() / segmentCount;
		var halfSegmentLength = segmentLength / 2.0F;
		var startOffset = -bladeSize.length() / 2.0F + halfSegmentLength;
		var halfBladeWidth = bladeSize.width() / 2.0F;
		var halfBladeDepth = bladeSize.depth() / 2.0F;
		var halfDepthX = halfBladeDepth;
		var halfDepthY = halfBladeDepth;
		var halfDepthZ = halfBladeDepth;

		var widthAxis = getInPlaneWidthAxisForBlade(rotationAxis, bladeAxis);

		switch (widthAxis) {
			case X -> halfDepthX = halfBladeWidth;
			case Y -> halfDepthY = halfBladeWidth;
			case Z -> halfDepthZ = halfBladeWidth;
		}

		switch (widthAxis) {
			case X -> halfDepthX *= volumeScale;
			case Y -> halfDepthY *= volumeScale;
			case Z -> halfDepthZ *= volumeScale;
		}

		for (var index = 0; index < segmentCount; index++) {
			var offset = startOffset + segmentLength * index;
			var minimumX = -halfDepthX;
			var maximumX = halfDepthX;
			var minimumY = -halfDepthY;
			var maximumY = halfDepthY;
			var minimumZ = -halfDepthZ;
			var maximumZ = halfDepthZ;

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

	private static Direction.Axis getRotationAxisForEntry(WindmillLODEntry entry) {
		var rotationAxis = entry.rotationAxis;

		if (rotationAxis != null) {
			return rotationAxis;
		}

		return Direction.Axis.Y;
	}

	// Scaling Utility

	private static float getVolumeScaleForRotationAngle(float bladeLength, float rotationAngle) {
		var radians = Math.toRadians(rotationAngle);
		var weight = (Math.cos(radians * 4.0) + 1.0) / 2.0;

		var sizeScale = 1.0f + Mod.CONFIG.windmillBladeRotationVolumeScaleSizeFactor * (bladeLength / 9.0f);
		var maxScale = Mod.CONFIG.windmillBladeRotationVolumeScale * sizeScale;
		var currentScale = 1.0 + (maxScale - 1.0) * weight;

		return (float) currentScale;
	}

	// Rotation

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
		var minimumX = box.minPos.x;
		var minimumY = box.minPos.y;
		var minimumZ = box.minPos.z;
		var maximumX = box.maxPos.x;
		var maximumY = box.maxPos.y;
		var maximumZ = box.maxPos.z;

		var rotatedMinimumX = Double.POSITIVE_INFINITY;
		var rotatedMinimumY = Double.POSITIVE_INFINITY;
		var rotatedMinimumZ = Double.POSITIVE_INFINITY;
		var rotatedMaximumX = Double.NEGATIVE_INFINITY;
		var rotatedMaximumY = Double.NEGATIVE_INFINITY;
		var rotatedMaximumZ = Double.NEGATIVE_INFINITY;

		for (var x : new double[] { minimumX, maximumX }) {
			for (var y : new double[] { minimumY, maximumY }) {
				for (var z : new double[] { minimumZ, maximumZ }) {
					var rotatedPoint = rotatePointForAxis(rotationAxis, x, y, z, sin, cos);

					rotatedMinimumX = Math.min(rotatedMinimumX, rotatedPoint.x);
					rotatedMinimumY = Math.min(rotatedMinimumY, rotatedPoint.y);
					rotatedMinimumZ = Math.min(rotatedMinimumZ, rotatedPoint.z);
					rotatedMaximumX = Math.max(rotatedMaximumX, rotatedPoint.x);
					rotatedMaximumY = Math.max(rotatedMaximumY, rotatedPoint.y);
					rotatedMaximumZ = Math.max(rotatedMaximumZ, rotatedPoint.z);
				}
			}
		}

		var minimumPos = new DhApiVec3d(rotatedMinimumX, rotatedMinimumY, rotatedMinimumZ);
		var maximumPos = new DhApiVec3d(rotatedMaximumX, rotatedMaximumY, rotatedMaximumZ);
		var material = EDhApiBlockMaterial.getFromIndex(box.material);

		return new DhApiRenderableBox(minimumPos, maximumPos, box.color, material);
	}

	private static DhApiVec3d rotatePointForAxis(Direction.Axis rotationAxis, double x, double y, double z, double sin, double cos) {
		return switch (rotationAxis) {
			case X -> new DhApiVec3d(x, y * cos - z * sin, y * sin + z * cos);
			case Y -> new DhApiVec3d(x * cos + z * sin, y, -x * sin + z * cos);
			case Z -> new DhApiVec3d(x * cos - y * sin, x * sin + y * cos, z);
		};
	}

	// Box Form

	private static DhApiRenderableBox createBox(double minimumX, double minimumY, double minimumZ, double maximumX, double maximumY,
			double maximumZ, Color bladeColor, EDhApiBlockMaterial bladeMaterial) {
		var minimumPos = new DhApiVec3d(minimumX, minimumY, minimumZ);
		var maximumPos = new DhApiVec3d(maximumX, maximumY, maximumZ);

		return new DhApiRenderableBox(minimumPos, maximumPos, bladeColor, bladeMaterial);
	}

}

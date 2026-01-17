package net.saint.createrenderfixer.dh;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.library.Size2D;
import net.saint.createrenderfixer.utils.MathUtil;

public final class WindmillLODAnalysisUtil {

	// API

	public static WindmillBladeGeometry getWindmillBladeGeometry(Contraption contraption, Direction.Axis rotationAxis, AABB bounds) {
		var bladeSize = getWindmillBladeSize(contraption, rotationAxis, bounds);
		var numberOfBladeSegments = getNumberOfSegmentsForBladeLength(bladeSize.length());

		return new WindmillBladeGeometry(bladeSize.length(), bladeSize.width(), bladeSize.depth(), numberOfBladeSegments);
	}

	public static WindmillBladeSize getWindmillBladeSize(Contraption contraption, Direction.Axis rotationAxis, AABB bounds) {
		var planeSize = getPlaneSizeForContraptionBounds(rotationAxis, bounds);
		var estimatedPlaneLength = getBladeLengthForContraption(planeSize);
		var estimatedBladeWidth = getBladeWidthForContraption(contraption, planeSize);
		var estimatedBladeDepth = getBladeDepthForWidth(estimatedBladeWidth);

		return new WindmillBladeSize(estimatedPlaneLength, estimatedBladeWidth, estimatedBladeDepth);
	}

	// Utility

	public static Size2D getPlaneSizeForContraptionBounds(Direction.Axis rotationAxis, AABB bounds) {
		if (bounds == null || rotationAxis == null) {
			return new Size2D(0.0f, 0.0f);
		}

		var sizeAlongX = (float) bounds.getXsize();
		var sizeAlongY = (float) bounds.getYsize();
		var sizeAlongZ = (float) bounds.getZsize();

		var lengthAlongX = sizeAlongX;
		var lengthAlongY = sizeAlongY;
		var lengthAlongZ = sizeAlongZ;

		return switch (rotationAxis) {
			case X -> new Size2D(lengthAlongY, lengthAlongZ);
			case Y -> new Size2D(lengthAlongZ, lengthAlongX);
			case Z -> new Size2D(lengthAlongY, lengthAlongX);
		};
	}

	private static float getBladeWidthForContraption(Contraption contraption, Size2D planeSize) {
		var sailCount = getNumberOfSailBlocksInContraption(contraption);

		if (sailCount <= 0) {
			return 0.0F;
		}

		var totalSpan = planeSize.width();

		if (totalSpan <= 0.0F) {
			return 0.0F;
		}

		// Short: l:9.35, w:0.61, d:0.55
		// Long: l:39.95, w:1.33, d:0.91

		var baseWidthFactor = Mod.CONFIG.windmillBladeWidthFactor;
		var compensatoryWidthFactor = getBladeWidthScalingFactor(planeSize.width());
		var bladeWidth = ((float) sailCount * baseWidthFactor * compensatoryWidthFactor) / totalSpan;

		return bladeWidth;
	}

	private static float getBladeWidthScalingFactor(float bladeLength) {
		return Mod.CONFIG.windmillBladeSizeWidthFactor * bladeLength + Mod.CONFIG.windmillBladeSizeWidthTrim;
	}

	private static float getBladeLengthForContraption(Size2D planeSize) {
		return planeSize.width() * Mod.CONFIG.windmillBladeLengthFactor;
	}

	private static float getBladeDepthForWidth(float bladeWidth) {
		return 0.5f + (bladeWidth - 0.5f) * Mod.CONFIG.windmillBladeDepthFactor;
	}

	// Analysis

	private static int getNumberOfSailBlocksInContraption(Contraption contraption) {
		var blocks = contraption.getBlocks();

		if (blocks == null || blocks.isEmpty()) {
			return 0;
		}

		var sailCount = 0;

		for (var entry : blocks.entrySet()) {
			var blockState = entry.getValue().state();

			if (!AllTags.AllBlockTags.WINDMILL_SAILS.matches(blockState)) {
				continue;
			}

			sailCount++;
		}

		return sailCount;
	}

	// Segmentation

	public static int getNumberOfSegmentsForBladeLength(float bladeLength) {
		var targetLength = Math.max(0.1f, Mod.CONFIG.windmillBladeSegmentTargetLength);
		var minimumCount = Math.max(1, Mod.CONFIG.windmillBladeSegmentCountMinimum);
		var maximumCount = Math.max(1, Mod.CONFIG.windmillBladeSegmentCountMaximum);
		var segmentCount = MathUtil.clamp(Math.round(bladeLength / targetLength), minimumCount, maximumCount);

		return segmentCount;
	}
}

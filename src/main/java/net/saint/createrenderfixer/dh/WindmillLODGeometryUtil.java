package net.saint.createrenderfixer.dh;

import net.saint.createrenderfixer.Mod;

public final class WindmillLODGeometryUtil {

	// Models

	public record BladeGeometry(float widthLength, float heightLength, float bladeWidth, int widthSegmentCount, int heightSegmentCount) {
	}

	// API

	public static BladeGeometry makeBladeGeometry(float planeWidth, float planeHeight, float planeDepth) {
		var widthLength = getBladeLengthForPlane(planeWidth);
		var heightLength = getBladeLengthForPlane(planeHeight);
		var bladeWidth = getBladeWidthForPlane(planeDepth);
		var widthSegmentCount = getSegmentCountForBladeLength(widthLength);
		var heightSegmentCount = getSegmentCountForBladeLength(heightLength);

		return new BladeGeometry(widthLength, heightLength, bladeWidth, widthSegmentCount, heightSegmentCount);
	}

	// Utility

	private static float getBladeLengthForPlane(float planeSize) {
		var lengthScale = Mod.CONFIG.windmillBladeLengthScale;
		var lengthTrim = Mod.CONFIG.windmillBladeLengthTrim;
		var minimumLength = Mod.CONFIG.windmillBladeMinimumLength;
		var length = planeSize * lengthScale - lengthTrim;

		if (length < minimumLength) {
			return minimumLength;
		}

		return length;
	}

	private static float getBladeWidthForPlane(float planeDepth) {
		var widthFactor = Mod.CONFIG.windmillBladeWidthFactor;
		var bladeWidth = planeDepth * widthFactor;

		return bladeWidth;
	}

	private static int getSegmentCountForBladeLength(float bladeLength) {
		var targetLength = Mod.CONFIG.windmillBladeSegmentTargetLength;

		if (targetLength <= 0.0F) {
			targetLength = 1.0F;
		}

		var minimumCount = Mod.CONFIG.windmillBladeSegmentCountMinimum;
		var maximumCount = Mod.CONFIG.windmillBladeSegmentCountMaximum;

		// TODO: Use available clamp to limit min and max values.
		var segmentCount = Math.round(bladeLength / targetLength);

		segmentCount = Math.min(segmentCount, maximumCount);
		segmentCount = Math.max(segmentCount, minimumCount);

		return segmentCount;
	}
}

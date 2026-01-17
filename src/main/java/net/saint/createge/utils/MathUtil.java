package net.saint.createge.utils;

public final class MathUtil {

	public static int clamp(int value, int min, int max) {
		return Math.min(max, Math.max(min, value));
	}

	public static float clamp(float value, float min, float max) {
		return Math.min(max, Math.max(min, value));
	}

}

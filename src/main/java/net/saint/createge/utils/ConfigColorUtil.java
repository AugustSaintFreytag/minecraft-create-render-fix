package net.saint.createge.utils;

import java.awt.Color;

public final class ConfigColorUtil {

	// API

	public static Color decodeColor(String value, Color fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}

		var components = value.split(",");

		if (components.length < 3) {
			return fallback;
		}

		var red = parseComponent(components[0]);
		var green = parseComponent(components[1]);
		var blue = parseComponent(components[2]);

		if (red == null || green == null || blue == null) {
			return fallback;
		}

		return new Color(red, green, blue);
	}

	// Utility

	private static Integer parseComponent(String value) {
		if (value == null) {
			return null;
		}

		var trimmed = value.trim();

		if (trimmed.isEmpty()) {
			return null;
		}

		try {
			var parsed = Integer.parseInt(trimmed);

			return clampComponent(parsed);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	private static int clampComponent(int value) {
		if (value < 0) {
			return 0;
		}

		if (value > 255) {
			return 255;
		}

		return value;
	}
}

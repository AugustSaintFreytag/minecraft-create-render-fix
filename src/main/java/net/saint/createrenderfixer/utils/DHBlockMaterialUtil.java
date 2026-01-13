package net.saint.createrenderfixer.utils;

import java.util.Locale;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;

public final class DHBlockMaterialUtil {

	// API

	public static EDhApiBlockMaterial decodeBlockMaterial(String value) {
		if (value == null || value.isBlank()) {
			return EDhApiBlockMaterial.UNKNOWN;
		}

		var normalized = value.trim().toUpperCase(Locale.ROOT);

		if (normalized.isEmpty()) {
			return EDhApiBlockMaterial.UNKNOWN;
		}

		try {
			return EDhApiBlockMaterial.valueOf(normalized);
		} catch (IllegalArgumentException exception) {
			return EDhApiBlockMaterial.UNKNOWN;
		}
	}
}

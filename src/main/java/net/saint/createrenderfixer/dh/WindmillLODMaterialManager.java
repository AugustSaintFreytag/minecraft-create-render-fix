package net.saint.createrenderfixer.dh;

import java.awt.Color;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;

import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.utils.ConfigColorUtil;
import net.saint.createrenderfixer.utils.DHBlockMaterialUtil;

public final class WindmillLODMaterialManager {

	// Properties

	private static EDhApiBlockMaterial bladeMaterial = EDhApiBlockMaterial.UNKNOWN;
	private static Color bladeColor = Color.GREEN;

	// Access

	public static EDhApiBlockMaterial getBladeMaterial() {
		return bladeMaterial;
	}

	public static Color getBladeColor() {
		return bladeColor;
	}

	// Mutation

	public static void reloadFromConfig() {
		bladeMaterial = DHBlockMaterialUtil.decodeBlockMaterial(Mod.CONFIG.windmillBladeMaterial);
		bladeColor = ConfigColorUtil.decodeColor(Mod.CONFIG.windmillBladeColor, Color.GREEN);
	}

}

package net.saint.createrenderfixer.dh;

import java.awt.Color;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;

import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.utils.ConfigColorUtil;
import net.saint.createrenderfixer.utils.DHBlockMaterialUtil;

public final class WindmillLODMaterialManager {

	// Properties

	private EDhApiBlockMaterial bladeMaterial = EDhApiBlockMaterial.UNKNOWN;
	private Color bladeColor = Color.GREEN;

	// Access

	public EDhApiBlockMaterial getBladeMaterial() {
		return bladeMaterial;
	}

	public Color getBladeColor() {
		return bladeColor;
	}

	// Mutation

	public void reloadFromConfig() {
		var config = Mod.CONFIG;

		if (config == null) {
			return;
		}

		bladeMaterial = DHBlockMaterialUtil.decodeBlockMaterial(config.windmillBladeMaterial);
		bladeColor = ConfigColorUtil.decodeColor(config.windmillBladeColor, Color.GREEN);
	}

}

package net.saint.createrenderfixer;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = Mod.MOD_ID)
public class ModConfig implements ConfigData {

	// Logging

	@ConfigEntry.Category("logging")
	@Comment("Enable extra logging for debugging Create Render Fixer. (Default: false)")
	public boolean enableLogging = false;

	// Instances

	@ConfigEntry.Category("instances")
	@Comment("Enable state caching on supported dynamic instances to prevent re-render when state is unmodified. (Default: true)")
	public boolean cacheDynamicInstances = true;

	@ConfigEntry.Category("instances")
	@Comment("Freeze dynamic instances once they're above a certain block distance from the player. (Default: true)")
	public boolean freezeDistantInstances = true;

	@ConfigEntry.Category("instances")
	@Comment("Distance in blocks to freeze dynamic instances. Recommended < 64 to cut in before Create limits tick rate. (Default: 62)")
	public int freezeDistantInstancesRange = 62;

	@ConfigEntry.Category("instances")
	@Comment("Freeze dynamic instances when they're in an occluded chunk. Not effective due to subpar engine occlusion checks. (Default: false)")
	public boolean freezeOccludedInstances = false;

	@ConfigEntry.Category("instances")
	@Comment("Force-disables the tick-based rate limiter on Create dynamic instances. Generally not needed or effective. (Default: false)")
	public boolean forceDisableRateLimiting = false;

	@ConfigEntry.Category("instances")
	@Comment("Blacklist of contraptions to exclude from instance freezing. Comma-separated list of ids.")
	public String freezeInstanceBlacklist = "create:contraption, create:windmill_bearing";

	// LODs

	@ConfigEntry.Category("lods")
	@Comment("Enable injection of Create contraption blocks for LOD building with Distant Horizons. (Default: true)")
	public boolean injectContraptionLODs = true;

	// LODs (Windmill)

	@ConfigEntry.Category("lods-windmill")
	@Comment("Scale factor used to create an LOD representation of windmill blades. (Default: 1.0)")
	public float windmillBladeLengthScale = 1.0f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Thickness in blocks used when constructing windmill blade LOD boxes. (Default: 0.5)")
	public float windmillBladeThickness = 0.5f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Width factor applied to the blade depth when constructing LOD boxes. (Default: 0.5)")
	public float windmillBladeWidthFactor = 0.5f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Minimum blade length used when constructing LOD boxes. (Default: 1.0)")
	public float windmillBladeMinimumLength = 1.0f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Target blade segment length when constructing LOD boxes. (Default: 1.0)")
	public float windmillBladeSegmentTargetLength = 1.0f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Blade length trimmed off each axis before segment generation. (Default: 2.0)")
	public float windmillBladeLengthTrim = 2.0f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Minimum blade segment count used when constructing LOD boxes. (Default: 6)")
	public int windmillBladeSegmentCountMinimum = 6;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Maximum blade segment count used when constructing LOD boxes. (Default: 24)")
	public int windmillBladeSegmentCountMaximum = 24;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Maximum thickness scale multiplier used during blade rotation. (Default: 1.4)")
	public float windmillBladeRotationThicknessScaleMaximum = 1.4f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Blade color as comma-separated red, green, and blue values. (Default: 250, 250, 250)")
	public String windmillBladeColor = "250, 250, 250";

	@ConfigEntry.Category("lods-windmill")
	@Comment("Blade material used by Distant Horizons. Options: UNKNOWN, LEAVES, STONE, WOOD, METAL, DIRT, LAVA, DEEPSLATE, SNOW, SAND, TERRACOTTA, NETHER_STONE, WATER, GRASS, AIR, ILLUMINATED. (Default: WOOD)")
	public String windmillBladeMaterial = "WOOD";

	@ConfigEntry.Category("lods-windmill")
	@Comment("Minimum rotation delta in degrees before LOD boxes update. (Default: 0.5)")
	public float windmillRotationUpdateThreshold = 0.5f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Maximum render distance in blocks for windmill LODs. (Default: 2048)")
	public float windmillMaximumRenderDistance = 2048.0f;

	// Entities

	@ConfigEntry.Category("entities")
	@Comment("Limit entity render distance. (Default: true)")
	public boolean limitEntityRenderDistance = true;

	@ConfigEntry.Category("entities")
	@Comment("Apply entity render distance limit to all entities, not just Create entities. (Default: false)")
	public boolean limitEntityRenderDistanceAppliesToAll = false;

	@ConfigEntry.Category("entities")
	@Comment("Offset added to entity LOD distance thresholds. (Default: 0)")
	public int entityLODDistanceOffset = 0;

	@ConfigEntry.Category("entities")
	@Comment("Limit block entity render distance to respect LOD thresholds. (Default: true)")
	public boolean limitBlockEntityRenderDistance = true;

	@ConfigEntry.Category("entities")
	@Comment("Offset added to block entity LOD distance thresholds. (Default: 0)")
	public int blockEntityLODDistanceOffset = 0;

}

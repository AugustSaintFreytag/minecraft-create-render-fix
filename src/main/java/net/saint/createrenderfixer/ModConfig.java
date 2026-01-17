package net.saint.createrenderfixer;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = Mod.MOD_ID)
public class ModConfig implements ConfigData {

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
	@Comment("Scale factor applied to calculated blade length derived from bounds for windmill LODs. (Default: 0.875)")
	public float windmillBladeLengthFactor = 0.875f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Scale factor applied to estimated blade width derived from bounds to sail block ratio for windmill LODs. (Default: 1.0)")
	public float windmillBladeWidthFactor = 1.0f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Scale factor used to calculate blade width compensation depending on windmill size. (Default: 0.01)")
	public float windmillBladeSizeWidthFactor = 0.01f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Base scale factor trim used to calculate blade width compensation depending on windmill size. (Default: 0.65)")
	public float windmillBladeSizeWidthTrim = 0.65f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Scale factor applied to estimated depth constructing windmill blade LOD boxes. (Default: 0.5)")
	public float windmillBladeDepthFactor = 0.5f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Minimum blade length used when constructing LOD boxes. (Default: 1.0)")
	public float windmillBladeMinimumLength = 1.0f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Target blade segment length when constructing LOD boxes. (Default: 1.0)")
	public float windmillBladeSegmentTargetLength = 1.0f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Minimum blade segment count used when constructing LOD boxes. (Default: 6)")
	public int windmillBladeSegmentCountMinimum = 6;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Maximum blade segment count used when constructing LOD boxes. (Default: 24)")
	public int windmillBladeSegmentCountMaximum = 24;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Factor applied to blade volume to compensate for perceived volume loss during rotation.")
	public float windmillBladeRotationVolumeScale = 1.375f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Factor applied to blade volume based on blade size to compensate for perceived volume loss during rotation.")
	public float windmillBladeRotationVolumeScaleSizeFactor = 0.1f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Blade color as comma-separated red, green, and blue values. (Default: 250, 250, 250)")
	public String windmillBladeColor = "250, 250, 250";

	@ConfigEntry.Category("lods-windmill")
	@Comment("Blade material used by Distant Horizons. Options: UNKNOWN, LEAVES, STONE, WOOD, METAL, DIRT, LAVA, DEEPSLATE, SNOW, SAND, TERRACOTTA, NETHER_STONE, WATER, GRASS, AIR, ILLUMINATED. (Default: WOOD)")
	public String windmillBladeMaterial = "WOOD";

	@ConfigEntry.Category("lods-windmill")
	@Comment("Minimum rotation delta in degrees before LOD boxes update. (Default: 0.5)")
	public float windmillRotationAngleRenderThreshold = 0.5f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Tick interval to synchronize active windmill LODs entries from server to client. (Default: 50)")
	public int windmillSyncTickInterval = 50;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Maximum rotation angle delta between server expectation and client actual to cause override. (Default: 1.0)")
	public float windmillRotationAngleSyncThreshold = 1.0f;

	@ConfigEntry.Category("lods-windmill")
	@Comment("Maximum rotation speed delta between server expectation and client actual to cause override. (Default: 0.05)")
	public float windmillRotationSpeedSyncThreshold = 0.05f;

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

	// Debug

	@ConfigEntry.Category("debug")
	@Comment("Enables logging for lifecycles, registrations, updates, removals. (Default: false)")
	public boolean enableLogging = false;

}

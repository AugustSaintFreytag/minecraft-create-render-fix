package net.saint.createge.dh;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record WindmillBladeGeometry(float length, float width, float depth, int numberOfSegments) {

	public static final String LENGTH_NBT_KEY = "Length";
	public static final String WIDTH_NBT_KEY = "Width";
	public static final String DEPTH_NBT_KEY = "Depth";
	public static final String NUMBER_OF_SEGMENTS_NBT_KEY = "NumberOfSegments";

	// Init

	public static WindmillBladeGeometry zero() {
		return new WindmillBladeGeometry(0.0f, 0.0f, 0.0f, 1);
	}

	// NBT

	public CompoundTag toNbt() {
		var geometryTag = new CompoundTag();
		geometryTag.putFloat(LENGTH_NBT_KEY, length);
		geometryTag.putFloat(WIDTH_NBT_KEY, width);
		geometryTag.putFloat(DEPTH_NBT_KEY, depth);
		geometryTag.putInt(NUMBER_OF_SEGMENTS_NBT_KEY, numberOfSegments);

		return geometryTag;
	}

	@Nullable
	public static WindmillBladeGeometry fromNbt(@Nullable CompoundTag nbt) {
		if (nbt == null) {
			return null;
		}

		if (!nbt.contains(LENGTH_NBT_KEY, Tag.TAG_FLOAT) || !nbt.contains(WIDTH_NBT_KEY, Tag.TAG_FLOAT)
				|| !nbt.contains(DEPTH_NBT_KEY, Tag.TAG_FLOAT) || !nbt.contains(NUMBER_OF_SEGMENTS_NBT_KEY, Tag.TAG_INT)) {
			return null;
		}

		var length = nbt.getFloat(LENGTH_NBT_KEY);
		var width = nbt.getFloat(WIDTH_NBT_KEY);
		var depth = nbt.getFloat(DEPTH_NBT_KEY);
		var numberOfSegments = nbt.getInt(NUMBER_OF_SEGMENTS_NBT_KEY);

		return new WindmillBladeGeometry(length, width, depth, numberOfSegments);
	}
}

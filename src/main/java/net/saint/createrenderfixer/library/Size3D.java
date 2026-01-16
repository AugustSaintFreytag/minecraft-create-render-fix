package net.saint.createrenderfixer.library;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record Size3D(float width, float height, float depth) {

	public static final String WIDTH_NBT_KEY = "Width";
	public static final String HEIGHT_NBT_KEY = "Height";
	public static final String DEPTH_NBT_KEY = "Depth";

	// Init

	public static Size3D zero() {
		return new Size3D(0.0f, 0.0f, 0.0f);
	}

	// NBT

	public CompoundTag toNbt() {
		var sizeTag = new CompoundTag();
		sizeTag.putFloat(WIDTH_NBT_KEY, width);
		sizeTag.putFloat(HEIGHT_NBT_KEY, height);
		sizeTag.putFloat(DEPTH_NBT_KEY, depth);

		return sizeTag;
	}

	@Nullable
	public static Size3D fromNbt(@Nullable CompoundTag sizeTag) {
		if (sizeTag == null) {
			return null;
		}

		if (!sizeTag.contains(WIDTH_NBT_KEY, Tag.TAG_FLOAT) || !sizeTag.contains(HEIGHT_NBT_KEY, Tag.TAG_FLOAT)
				|| !sizeTag.contains(DEPTH_NBT_KEY, Tag.TAG_FLOAT)) {
			return null;
		}

		var width = sizeTag.getFloat(WIDTH_NBT_KEY);
		var height = sizeTag.getFloat(HEIGHT_NBT_KEY);
		var depth = sizeTag.getFloat(DEPTH_NBT_KEY);

		return new Size3D(width, height, depth);
	}

	// Comparison

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof Size3D otherSize)) {
			return false;
		}

		var widthMatches = Float.compare(width, otherSize.width) == 0;
		var heightMatches = Float.compare(height, otherSize.height) == 0;
		var depthMatches = Float.compare(depth, otherSize.depth) == 0;

		return widthMatches && heightMatches && depthMatches;
	}
}

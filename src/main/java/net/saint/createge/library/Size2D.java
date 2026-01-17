package net.saint.createge.library;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record Size2D(float width, float height) {

	public static final String WIDTH_NBT_KEY = "Width";
	public static final String HEIGHT_NBT_KEY = "Height";

	// Init

	public static Size2D zero() {
		return new Size2D(0.0f, 0.0f);
	}

	// NBT

	public CompoundTag toNbt() {
		var sizeTag = new CompoundTag();
		sizeTag.putFloat(WIDTH_NBT_KEY, width);
		sizeTag.putFloat(HEIGHT_NBT_KEY, height);

		return sizeTag;
	}

	@Nullable
	public static Size2D fromNbt(@Nullable CompoundTag sizeTag) {
		if (sizeTag == null) {
			return null;
		}

		if (!sizeTag.contains(WIDTH_NBT_KEY, Tag.TAG_FLOAT) || !sizeTag.contains(HEIGHT_NBT_KEY, Tag.TAG_FLOAT)) {
			return null;
		}

		var width = sizeTag.getFloat(WIDTH_NBT_KEY);
		var height = sizeTag.getFloat(HEIGHT_NBT_KEY);

		return new Size2D(width, height);
	}

	// Comparison

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof Size2D otherSize)) {
			return false;
		}

		var widthMatches = Float.compare(width, otherSize.width) == 0;
		var heightMatches = Float.compare(height, otherSize.height) == 0;

		return widthMatches && heightMatches;
	}
}

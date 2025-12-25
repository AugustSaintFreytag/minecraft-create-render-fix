package net.saint.createrenderfixer.mixin.dh;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.seibel.distanthorizons.core.level.AbstractDhLevel;
import com.seibel.distanthorizons.core.pos.DhChunkPos;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;

/**
 * Ensures DH treats contraption-bearing chunks as changed by eagerly populating the
 * updatedChunkPosSetBySectionPos for the target chunk before DH builds its data. This forces DH to
 * consider the chunk fully touched.
 */
@Mixin(value = AbstractDhLevel.class, remap = false)
public abstract class AbstractDhLevelMixin {

	@Shadow
	protected ConcurrentHashMap<Long, Set<DhChunkPos>> updatedChunkPosSetBySectionPos;

	@Inject(method = "updateChunkAsync", at = @At("HEAD"))
	private void crf$forceChunkTouched(IChunkWrapper chunkWrapper, int hash, CallbackInfo callbackInfo) {
		if (chunkWrapper == null) {
			return;
		}

		var chunkPosition = chunkWrapper.getChunkPos();
		if (chunkPosition == null || updatedChunkPosSetBySectionPos == null) {
			return;
		}

		// Derive the section key exactly as DH does (covers a 4x4 chunk area).
		var sectionPosition = DhSectionPos.encodeContaining((byte) 6, chunkPosition);

		updatedChunkPosSetBySectionPos.compute(sectionPosition, (key, existing) -> {
			var set = existing != null ? existing : new HashSet<DhChunkPos>();
			set.add(chunkPosition);

			return set;
		});
	}
}

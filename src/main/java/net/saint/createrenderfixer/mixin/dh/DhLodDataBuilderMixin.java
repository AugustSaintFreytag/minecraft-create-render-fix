package net.saint.createrenderfixer.mixin.dh;

import java.util.concurrent.atomic.AtomicInteger;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.seibel.distanthorizons.core.dataObjects.transformers.LodDataBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IMutableBlockPosWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;

import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.dh.ContraptionBlockRegistry;
import net.saint.createrenderfixer.dh.DhBridge;

/**
 * Redirects block lookups during DH chunk baking to inject contraption states directly.
 */
@Mixin(value = LodDataBuilder.class, remap = false)
public abstract class DhLodDataBuilderMixin {

	// State

	private static final AtomicInteger CALL_LOG_TALLY = new AtomicInteger();
	private static final AtomicInteger INJECT_LOG_TALLY = new AtomicInteger();

	// Injections

	@Redirect(method = "createFromChunk", at = @At(value = "INVOKE", target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/chunk/IChunkWrapper;getLightBlockingHeightMapValue(II)I"))
	private static int crf$boostLightBlockingHeightMap(IChunkWrapper chunkWrapper, int relativeX, int relativeZ, ILevelWrapper levelWrapper,
			IChunkWrapper originalChunkWrapper) {
		var dimensionId = levelWrapper.getDhIdentifier();
		var chunkPosition = chunkWrapper.getChunkPos();
		var worldX = (chunkPosition.getX() << 4) + relativeX;
		var worldZ = (chunkPosition.getZ() << 4) + relativeZ;
		var originalValue = chunkWrapper.getLightBlockingHeightMapValue(relativeX, relativeZ);

		return ContraptionBlockRegistry.highestYInColumn(dimensionId, worldX, worldZ, originalValue);
	}

	@Redirect(method = "createFromChunk", at = @At(value = "INVOKE", target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/chunk/IChunkWrapper;getSolidHeightMapValue(II)I"))
	private static int crf$boostSolidHeightMap(IChunkWrapper chunkWrapper, int relativeX, int relativeZ, ILevelWrapper levelWrapper,
			IChunkWrapper originalChunkWrapper) {
		var dimensionId = levelWrapper.getDhIdentifier();
		var chunkPosition = chunkWrapper.getChunkPos();
		var worldX = (chunkPosition.getX() << 4) + relativeX;
		var worldZ = (chunkPosition.getZ() << 4) + relativeZ;
		var originalValue = chunkWrapper.getSolidHeightMapValue(relativeX, relativeZ);

		return ContraptionBlockRegistry.highestYInColumn(dimensionId, worldX, worldZ, originalValue);
	}

	@Redirect(method = "createFromChunk", at = @At(value = "INVOKE", target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/chunk/IChunkWrapper;getBlockState(IIILcom/seibel/distanthorizons/core/wrapperInterfaces/misc/IMutableBlockPosWrapper;Lcom/seibel/distanthorizons/core/wrapperInterfaces/block/IBlockStateWrapper;)Lcom/seibel/distanthorizons/core/wrapperInterfaces/block/IBlockStateWrapper;"))
	private static IBlockStateWrapper crf$injectContraptionBlocks(IChunkWrapper chunkWrapper, int relX, int relY, int relZ,
			IMutableBlockPosWrapper mutablePos, IBlockStateWrapper cachedState, ILevelWrapper levelWrapper,
			IChunkWrapper originalChunkWrapper) {

		var wrapperFactory = DhBridge.wrapperFactory();

		if (wrapperFactory != null) {
			var dimensionId = levelWrapper.getDhIdentifier();

			if (CALL_LOG_TALLY.getAndIncrement() == 0) {
				Mod.LOGGER.info("DH contraption chunk bake redirect active for {}.", dimensionId);
			}

			var chunkPosition = chunkWrapper.getChunkPos();
			var worldX = (chunkPosition.getX() << 4) + relX;
			var worldZ = (chunkPosition.getZ() << 4) + relZ;

			var overrideData = ContraptionBlockRegistry.find(dimensionId, worldX, relY, worldZ);

			if (overrideData != null) {
				try {
					var blockState = overrideData.state();
					var stateWrapper = wrapperFactory.getBlockStateWrapper(new Object[] { blockState }, levelWrapper);

					if (INJECT_LOG_TALLY.getAndIncrement() < 20) {
						Mod.LOGGER.info("Injected contraption state at {}/{}/{} in {} ({}).", worldX, relY, worldZ, dimensionId,
								blockState.getBlock().getDescriptionId());
					}

					return (IBlockStateWrapper) stateWrapper;
				} catch (Exception exception) {
					Mod.LOGGER.debug("DH contraption inject failed at {}/{}/{} in {}", worldX, relY, worldZ, dimensionId, exception);
				}
			}
		}

		return chunkWrapper.getBlockState(relX, relY, relZ, mutablePos, cachedState);
	}
}

package net.saint.createge.dh;

import java.util.concurrent.atomic.AtomicInteger;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkProcessingEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;

import net.saint.createge.Mod;

/**
 * Injects contraption blocks into DH's chunk processing pipeline.
 */
public final class DhChunkProcessingHandler extends DhApiChunkProcessingEvent {

	private static final AtomicInteger OVERRIDE_TALLY = new AtomicInteger();
	private static final AtomicInteger EVENT_TALLY = new AtomicInteger();
	private static final AtomicInteger MISS_TALLY = new AtomicInteger();

	public static void init() {
		try {
			DhApi.events.bind(DhApiChunkProcessingEvent.class, new DhChunkProcessingHandler());
			Mod.LOGGER.info("Attached DH chunk processing handler for contraptions.");
		} catch (Throwable throwable) {
			Mod.LOGGER.info("Could not bind DH chunk processing handler for contraptions.", throwable);
		}
	}

	@Override
	public void blockOrBiomeChangedDuringChunkProcessing(DhApiEventParam<EventParam> eventParam) {
		if (!DhBridge.isReady()) {
			return;
		}

		if (!Mod.CONFIG.injectContraptionLODs) {
			return;
		}

		if (EVENT_TALLY.getAndIncrement() == 0) {
			Mod.LOGGER.info("Handling first call to DH chunk processing event.");
		}

		var wrapperFactory = DhBridge.wrapperFactory();

		if (wrapperFactory == null) {
			return;
		}

		var event = eventParam.value;
		var levelWrapper = event.levelWrapper;
		var dimensionId = levelWrapper.getDhIdentifier();

		var worldX = (event.chunkX << 4) + event.relativeBlockPosX;
		var worldY = event.blockPosY;
		var worldZ = (event.chunkZ << 4) + event.relativeBlockPosZ;

		var override = ContraptionBlockRegistry.find(dimensionId, worldX, worldY, worldZ);

		if (override == null) {
			return;
		}

		try {
			var stateWrapper = wrapperFactory.getBlockStateWrapper(new Object[] { override.state() }, levelWrapper);
			event.setBlockOverride(stateWrapper);

			if (override.biomeId() != null) {
				var biomeWrapper = wrapperFactory.getBiomeWrapper(override.biomeId(), levelWrapper);
				event.setBiomeOverride(biomeWrapper);
			}

			if (OVERRIDE_TALLY.getAndIncrement() < 20) {
				Mod.LOGGER.info("Applied DH contraption override at x{}, y{}, z{} in {}: state={}, biome={}.", worldX, worldY, worldZ,
						dimensionId, override.state().getBlock().getDescriptionId(), override.biomeId());
			}
		} catch (Exception exception) {
			Mod.LOGGER.info("Failed to wrap contraption state for DH override at x{}, y{}, z{}.", worldX, worldY, worldZ, exception);
		}
	}
}

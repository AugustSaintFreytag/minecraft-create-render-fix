package net.saint.createge.dh;

import org.jetbrains.annotations.Nullable;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.factories.IDhApiWrapperFactory;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiCustomRenderObjectFactory;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiWorldProxy;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;

import net.saint.createge.Mod;

/**
 * Thin bootstrap around the DH API to capture delayed singletons once DH is ready.
 */
public final class DhBridge {

	// References

	private static IDhApiWorldProxy worldProxy;
	private static IDhApiCustomRenderObjectFactory renderFactory;
	private static IDhApiWrapperFactory wrapperFactory;

	// State

	private static volatile boolean isInitialized;

	// Init

	public static void init() {
		try {
			DhApi.events.bind(DhApiAfterDhInitEvent.class, new DhApiAfterDhInitEvent() {
				@Override
				public void afterDistantHorizonsInit(DhApiEventParam<Void> input) {
					worldProxy = DhApi.Delayed.worldProxy;
					renderFactory = DhApi.Delayed.customRenderObjectFactory;
					wrapperFactory = DhApi.Delayed.wrapperFactory;
					isInitialized = worldProxy != null && renderFactory != null && wrapperFactory != null;

					Mod.LOGGER.info("Distant Horizons API initialized: {}", isInitialized ? "ready" : "incomplete");
				}
			});
		} catch (Throwable throwable) {
			Mod.LOGGER.info("Failed to bind Distant Horizons init event; DH likely absent.", throwable);
			isInitialized = false;
		}
	}

	public static boolean isReady() {
		return isInitialized && worldProxy != null && renderFactory != null;
	}

	@Nullable
	public static IDhApiWorldProxy worldProxy() {
		return worldProxy;
	}

	@Nullable
	public static IDhApiCustomRenderObjectFactory renderFactory() {
		return renderFactory;
	}

	@Nullable
	public static IDhApiWrapperFactory wrapperFactory() {
		return wrapperFactory;
	}
}

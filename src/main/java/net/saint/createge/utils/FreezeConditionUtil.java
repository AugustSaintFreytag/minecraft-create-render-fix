package net.saint.createge.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import net.saint.createge.Mod;
import net.saint.createge.mixin.LevelRendererAccessor;
import net.saint.createge.mixin.RenderChunkInfoAccessor;

public final class FreezeConditionUtil {

	public static boolean shouldFreezeAtPosition(BlockPos worldPosition, int cameraX, int cameraY, int cameraZ) {
		if (!Mod.CONFIG.freezeDistantInstances) {
			return false;
		}

		var limit = Mod.CONFIG.freezeDistantInstancesRange;

		if (limit <= 0) {
			return false;
		}

		if (Mod.CONFIG.freezeOccludedInstances && isPositionOccluded(worldPosition)) {
			return true;
		}

		var dx = worldPosition.getX() - cameraX;
		var dy = worldPosition.getY() - cameraY;
		var dz = worldPosition.getZ() - cameraZ;
		var limitSq = (long) limit * (long) limit;

		return (long) dx * dx + (long) dy * dy + (long) dz * dz > limitSq;
	}

	public static boolean shouldFreezePosition(BlockPos worldPosition) {
		var cameraPosition = getCameraPosition();

		if (cameraPosition == null) {
			return false;
		}

		return shouldFreezeAtPosition(worldPosition, (int) cameraPosition.x, (int) cameraPosition.y, (int) cameraPosition.z);
	}

	public static boolean isPositionOccluded(BlockPos position) {
		var client = Minecraft.getInstance();
		var levelRenderer = client.levelRenderer;

		if (!(levelRenderer instanceof LevelRendererAccessor accessor)) {
			return false;
		}

		var chunksInFrustum = accessor.getRenderChunksInFrustum();

		if (position == null || chunksInFrustum == null || chunksInFrustum.isEmpty()) {
			return false;
		}

		var section = SectionPos.of(position);
		var sectionX = section.getX();
		var sectionY = section.getY();
		var sectionZ = section.getZ();

		for (var chunkInfo : chunksInFrustum) {
			var renderChunk = ((RenderChunkInfoAccessor) chunkInfo).getChunk();

			if (renderChunk == null) {
				continue;
			}

			var origin = renderChunk.getOrigin();
			if (origin == null) {
				continue;
			}

			var renderSection = SectionPos.of(origin);
			if (renderSection.getX() == sectionX && renderSection.getY() == sectionY && renderSection.getZ() == sectionZ) {
				return false;
			}
		}

		return true;
	}

	private static Vec3 getCameraPosition() {
		var client = Minecraft.getInstance();

		if (client == null || client.gameRenderer == null) {
			return null;
		}

		var camera = client.gameRenderer.getMainCamera();

		if (camera == null) {
			return null;
		}

		return camera.getPosition();
	}
}

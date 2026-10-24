package dev.piotrulla.waypoint.internal.render;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import dev.piotrulla.waypoint.internal.Position3d;

import java.util.Optional;
import java.util.UUID;

abstract class AbstractWaypointRenderer implements WaypointRenderer {

    protected static final float NO_ROTATION = 0.0F;
    private static final int NO_DATA = 0;
    private static final boolean NOT_ON_GROUND = false;

    @Override
    public PacketWrapper<?> teleport(int entityId, Position3d position) {
        return new WrapperPlayServerEntityTeleport(entityId, this.toVector3d(position), NO_ROTATION, NO_ROTATION, NOT_ON_GROUND);
    }

    @Override
    public PacketWrapper<?> destroy(int entityId) {
        return new WrapperPlayServerDestroyEntities(entityId);
    }

    protected PacketWrapper<?> spawnEntity(int entityId, UUID entityUuid, EntityType entityType, Position3d position) {
        return new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(entityUuid),
                entityType,
                this.toVector3d(position),
                NO_ROTATION,
                NO_ROTATION,
                NO_ROTATION,
                NO_DATA,
                Optional.empty()
        );
    }

    protected Vector3d toVector3d(Position3d position) {
        return new Vector3d(position.x(), position.y(), position.z());
    }
}

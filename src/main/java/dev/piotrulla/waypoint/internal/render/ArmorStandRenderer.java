package dev.piotrulla.waypoint.internal.render;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import dev.piotrulla.waypoint.Waypoint;
import dev.piotrulla.waypoint.internal.WaypointTextComposer;
import net.kyori.adventure.text.Component;
import dev.piotrulla.waypoint.internal.Position3d;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fallback for servers/clients without display entities (1.16 - 1.19.3).
 * Single-line name tag, waypoint scale is ignored.
 */
public final class ArmorStandRenderer extends AbstractWaypointRenderer {

    private static final ServerVersion FROZEN_TICKS_SINCE = ServerVersion.V_1_17;
    private static final ServerVersion UNIFIED_SPAWN_SINCE = ServerVersion.V_1_19;

    private static final int INDEX_ENTITY_FLAGS = 0;
    private static final int INDEX_CUSTOM_NAME = 2;
    private static final int INDEX_CUSTOM_NAME_VISIBLE = 3;
    private static final int INDEX_NO_GRAVITY = 5;
    private static final int ARMOR_STAND_FLAGS_INDEX_LEGACY = 14;
    private static final int ARMOR_STAND_FLAGS_INDEX_MODERN = 15;

    private static final byte ENTITY_FLAG_INVISIBLE = 0x20;
    private static final byte ARMOR_STAND_FLAG_SMALL = 0x01;
    private static final byte ARMOR_STAND_FLAG_NO_BASEPLATE = 0x08;
    private static final byte ARMOR_STAND_FLAG_MARKER = 0x10;
    private static final byte ARMOR_STAND_FLAGS = (byte) (ARMOR_STAND_FLAG_SMALL | ARMOR_STAND_FLAG_NO_BASEPLATE | ARMOR_STAND_FLAG_MARKER);

    private static final Vector3d ZERO_VELOCITY = new Vector3d(0.0, 0.0, 0.0);

    private final int armorStandFlagsIndex;
    private final boolean legacyLivingSpawn;
    private final WaypointTextComposer textComposer;

    private ArmorStandRenderer(int armorStandFlagsIndex, boolean legacyLivingSpawn, WaypointTextComposer textComposer) {
        this.armorStandFlagsIndex = armorStandFlagsIndex;
        this.legacyLivingSpawn = legacyLivingSpawn;
        this.textComposer = textComposer;
    }

    public static ArmorStandRenderer forServer(ServerVersion serverVersion, WaypointTextComposer textComposer) {
        int flagsIndex = serverVersion.isNewerThanOrEquals(FROZEN_TICKS_SINCE)
                ? ARMOR_STAND_FLAGS_INDEX_MODERN
                : ARMOR_STAND_FLAGS_INDEX_LEGACY;
        boolean legacyLivingSpawn = serverVersion.isOlderThan(UNIFIED_SPAWN_SINCE);

        return new ArmorStandRenderer(flagsIndex, legacyLivingSpawn, textComposer);
    }

    @Override
    public List<PacketWrapper<?>> spawn(int entityId, UUID entityUuid, Position3d position, Waypoint waypoint, String distanceLabel) {
        return List.of(
                this.spawnArmorStand(entityId, entityUuid, position),
                new WrapperPlayServerEntityMetadata(entityId, this.fullMetadata(waypoint, distanceLabel))
        );
    }

    @Override
    public PacketWrapper<?> updateText(int entityId, Waypoint waypoint, String distanceLabel) {
        List<EntityData> metadata = List.of(this.customNameEntry(waypoint, distanceLabel));
        return new WrapperPlayServerEntityMetadata(entityId, metadata);
    }

    private PacketWrapper<?> spawnArmorStand(int entityId, UUID entityUuid, Position3d position) {
        if (!this.legacyLivingSpawn) {
            return this.spawnEntity(entityId, entityUuid, EntityTypes.ARMOR_STAND, position);
        }
        return new WrapperPlayServerSpawnLivingEntity(
                entityId,
                entityUuid,
                EntityTypes.ARMOR_STAND,
                this.toVector3d(position),
                NO_ROTATION,
                NO_ROTATION,
                NO_ROTATION,
                ZERO_VELOCITY,
                Collections.emptyList()
        );
    }

    private List<EntityData> fullMetadata(Waypoint waypoint, String distanceLabel) {
        return List.of(
                new EntityData(INDEX_ENTITY_FLAGS, EntityDataTypes.BYTE, ENTITY_FLAG_INVISIBLE),
                this.customNameEntry(waypoint, distanceLabel),
                new EntityData(INDEX_CUSTOM_NAME_VISIBLE, EntityDataTypes.BOOLEAN, true),
                new EntityData(INDEX_NO_GRAVITY, EntityDataTypes.BOOLEAN, true),
                new EntityData(this.armorStandFlagsIndex, EntityDataTypes.BYTE, ARMOR_STAND_FLAGS)
        );
    }

    private EntityData customNameEntry(Waypoint waypoint, String distanceLabel) {
        Component text = this.textComposer.composeSingleLine(waypoint, distanceLabel);
        return new EntityData(INDEX_CUSTOM_NAME, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(text));
    }
}
package dev.piotrulla.waypoint.internal.render;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import dev.piotrulla.waypoint.Waypoint;
import dev.piotrulla.waypoint.internal.Position3d;

import java.util.List;
import java.util.UUID;

public interface WaypointRenderer {

    List<PacketWrapper<?>> spawn(int entityId, UUID entityUuid, Position3d position, Waypoint waypoint, String distanceLabel);

    PacketWrapper<?> teleport(int entityId, Position3d position);

    PacketWrapper<?> updateText(int entityId, Waypoint waypoint, String distanceLabel);

    PacketWrapper<?> destroy(int entityId);
}

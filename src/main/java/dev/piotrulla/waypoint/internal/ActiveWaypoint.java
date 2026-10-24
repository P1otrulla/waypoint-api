package dev.piotrulla.waypoint.internal;

import dev.piotrulla.waypoint.Waypoint;
import dev.piotrulla.waypoint.internal.render.WaypointRenderer;

import java.util.UUID;

public final class ActiveWaypoint {

    private static final long NO_DISTANCE_KEY = Long.MIN_VALUE;

    private final Waypoint waypoint;
    private final int entityId;
    private final UUID entityUuid;

    private WaypointRenderer renderer;
    private Position3d lastPosition;
    private long lastDistanceKey = NO_DISTANCE_KEY;

    public ActiveWaypoint(Waypoint waypoint, int entityId, UUID entityUuid) {
        this.waypoint = waypoint;
        this.entityId = entityId;
        this.entityUuid = entityUuid;
    }

    public Waypoint waypoint() {
        return this.waypoint;
    }

    public String id() {
        return this.waypoint.id();
    }

    public int entityId() {
        return this.entityId;
    }

    public UUID entityUuid() {
        return this.entityUuid;
    }

    public boolean isSpawned() {
        return this.renderer != null;
    }

    public WaypointRenderer renderer() {
        if (this.renderer == null) {
            throw new IllegalStateException("Waypoint '" + this.waypoint.id() + "' is not spawned");
        }
        return this.renderer;
    }

    public void markSpawned(WaypointRenderer renderer, Position3d position, long distanceKey) {
        this.renderer = renderer;
        this.lastPosition = position;
        this.lastDistanceKey = distanceKey;
    }

    public void markDespawned() {
        this.renderer = null;
        this.lastPosition = null;
        this.lastDistanceKey = NO_DISTANCE_KEY;
    }

    public boolean hasMoved(Position3d position, double epsilonSquared) {
        return this.lastPosition == null || this.lastPosition.distanceSquared(position) > epsilonSquared;
    }

    public void updatePosition(Position3d position) {
        this.lastPosition = position;
    }

    public boolean hasDistanceKeyChanged(long distanceKey) {
        return this.lastDistanceKey != distanceKey;
    }

    public void updateDistanceKey(long distanceKey) {
        this.lastDistanceKey = distanceKey;
    }
}

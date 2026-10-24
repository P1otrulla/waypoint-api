package dev.piotrulla.waypoint.internal;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerWaypoints {

    private static final double EYE_MOVEMENT_EPSILON = 0.001;
    private static final double EYE_MOVEMENT_EPSILON_SQUARED = EYE_MOVEMENT_EPSILON * EYE_MOVEMENT_EPSILON;

    private final Player player;
    private final Map<String, ActiveWaypoint> waypoints = new LinkedHashMap<>();
    private final Collection<ActiveWaypoint> waypointsView = Collections.unmodifiableCollection(this.waypoints.values());

    private User user;
    private Position3d lastEyePosition;
    private boolean dirty = true;

    public PlayerWaypoints(Player player) {
        this.player = player;
    }

    public Player player() {
        return this.player;
    }

    public User resolveUser() {
        if (this.user == null) {
            this.user = PacketEvents.getAPI().getPlayerManager().getUser(this.player);
        }
        return this.user;
    }

    public ActiveWaypoint put(ActiveWaypoint activeWaypoint) {
        this.markDirty();
        return this.waypoints.put(activeWaypoint.id(), activeWaypoint);
    }

    public ActiveWaypoint get(String waypointId) {
        return this.waypoints.get(waypointId);
    }

    public ActiveWaypoint remove(String waypointId) {
        return this.waypoints.remove(waypointId);
    }

    public boolean removeIfSame(ActiveWaypoint activeWaypoint) {
        return this.waypoints.remove(activeWaypoint.id(), activeWaypoint);
    }

    public boolean contains(ActiveWaypoint activeWaypoint) {
        return this.waypoints.get(activeWaypoint.id()) == activeWaypoint;
    }

    public Collection<ActiveWaypoint> values() {
        return this.waypointsView;
    }

    public boolean isEmpty() {
        return this.waypoints.isEmpty();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean updateEyePosition(Position3d eyePosition) {
        boolean moved = this.lastEyePosition == null
                || this.lastEyePosition.distanceSquared(eyePosition) > EYE_MOVEMENT_EPSILON_SQUARED;
        if (!moved && !this.dirty) {
            return false;
        }
        this.lastEyePosition = eyePosition;
        this.dirty = false;
        return true;
    }
}

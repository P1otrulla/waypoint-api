package dev.piotrulla.waypoint;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * All methods must be called from the primary server thread.
 */
public interface WaypointService {

    /**
     * Shows waypoint to player. Replaces existing waypoint with the same id.
     */
    void show(Player player, Waypoint waypoint);

    boolean hide(Player player, String waypointId);

    void hideAll(Player player);

    Optional<Waypoint> findWaypoint(Player player, String waypointId);

    List<Waypoint> getWaypoints(Player player);
}

package dev.piotrulla.waypoint;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired (after the update tick) when player is inside waypoint reach radius.
 * Not cancelled = waypoint gets removed.
 * Cancelled = waypoint stays and the event fires again when the player moves while still in radius.
 */
public final class WaypointReachEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Waypoint waypoint;
    private boolean cancelled;

    public WaypointReachEvent(Player player, Waypoint waypoint) {
        super(player);
        this.waypoint = waypoint;
    }

    public Waypoint getWaypoint() {
        return this.waypoint;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

package dev.piotrulla.waypoint;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import dev.piotrulla.waypoint.internal.ActiveWaypoint;
import dev.piotrulla.waypoint.internal.EntityIdAllocator;
import dev.piotrulla.waypoint.internal.PlayerWaypoints;
import dev.piotrulla.waypoint.internal.Position3d;
import dev.piotrulla.waypoint.internal.ResolvedWaypointPosition;
import dev.piotrulla.waypoint.internal.WaypointPositionResolver;
import dev.piotrulla.waypoint.internal.WaypointTextComposer;
import dev.piotrulla.waypoint.internal.render.WaypointRenderer;
import dev.piotrulla.waypoint.internal.render.WaypointRendererSelector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Hot path notes:
 * - no snapshot copies: reach events are queued and fired after the loop, so nothing external mutates state mid-iteration
 * - players whose eye position didn't change are skipped entirely
 * - packets for one player are written and flushed once per tick
 */
public final class PacketWaypointService implements WaypointService, Listener {

    private static final int DEFAULT_UPDATE_INTERVAL_TICKS = 2;
    private static final int MIN_UPDATE_INTERVAL_TICKS = 1;
    private static final long INITIAL_DELAY_TICKS = 1L;
    private static final double POSITION_EPSILON = 0.01;
    private static final double POSITION_EPSILON_SQUARED = POSITION_EPSILON * POSITION_EPSILON;
    private static final double RESPAWN_TELEPORT_DISTANCE = 32.0;
    private static final double RESPAWN_TELEPORT_DISTANCE_SQUARED = RESPAWN_TELEPORT_DISTANCE * RESPAWN_TELEPORT_DISTANCE;

    private final Plugin plugin;
    private final int updateIntervalTicks;
    private final WaypointRendererSelector rendererSelector;
    private final WaypointPositionResolver positionResolver;
    private final WaypointTextComposer textComposer;
    private final EntityIdAllocator entityIdAllocator;
    private final Map<UUID, PlayerWaypoints> players = new HashMap<>();
    private final List<PendingReach> pendingReaches = new ArrayList<>();
    private final Location locationBuffer = new Location(null, 0.0, 0.0, 0.0);

    private BukkitTask updateTask;

    public PacketWaypointService(Plugin plugin) {
        this(plugin, DEFAULT_UPDATE_INTERVAL_TICKS);
    }

    public PacketWaypointService(Plugin plugin, int updateIntervalTicks) {
        if (updateIntervalTicks < MIN_UPDATE_INTERVAL_TICKS) {
            throw new IllegalArgumentException("updateIntervalTicks must be >= " + MIN_UPDATE_INTERVAL_TICKS + ", got " + updateIntervalTicks);
        }
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.updateIntervalTicks = updateIntervalTicks;
        this.textComposer = new WaypointTextComposer();
        this.rendererSelector = WaypointRendererSelector.create(this.textComposer, updateIntervalTicks);
        this.positionResolver = new WaypointPositionResolver();
        this.entityIdAllocator = new EntityIdAllocator();
    }

    public void start() {
        if (this.updateTask != null) {
            throw new IllegalStateException("Waypoint service is already started");
        }
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        this.updateTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, INITIAL_DELAY_TICKS, this.updateIntervalTicks);
    }

    public void shutdown() {
        if (this.updateTask == null) {
            return;
        }
        this.updateTask.cancel();
        this.updateTask = null;
        HandlerList.unregisterAll(this);

        for (PlayerWaypoints playerWaypoints : List.copyOf(this.players.values())) {
            this.hideAll(playerWaypoints.player());
        }
        this.players.clear();
        this.pendingReaches.clear();
    }

    @Override
    public void show(Player player, Waypoint waypoint) {
        this.ensurePrimaryThread();
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(waypoint, "waypoint");

        ActiveWaypoint active = new ActiveWaypoint(waypoint, this.entityIdAllocator.next(), UUID.randomUUID());
        ActiveWaypoint previous = this.players
                .computeIfAbsent(player.getUniqueId(), playerId -> new PlayerWaypoints(player))
                .put(active);

        if (previous != null) {
            this.despawn(previous, this.directSink(player));
        }
    }

    @Override
    public boolean hide(Player player, String waypointId) {
        this.ensurePrimaryThread();
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(waypointId, "waypointId");

        PlayerWaypoints playerWaypoints = this.players.get(player.getUniqueId());
        if (playerWaypoints == null) {
            return false;
        }

        ActiveWaypoint removed = playerWaypoints.remove(waypointId);
        if (removed == null) {
            return false;
        }

        this.despawn(removed, this.directSink(player));
        this.cleanupIfEmpty(playerWaypoints);
        return true;
    }

    @Override
    public void hideAll(Player player) {
        this.ensurePrimaryThread();
        Objects.requireNonNull(player, "player");

        PlayerWaypoints removed = this.players.remove(player.getUniqueId());
        if (removed == null) {
            return;
        }
        Consumer<PacketWrapper<?>> sink = this.directSink(player);
        removed.values().forEach(active -> this.despawn(active, sink));
    }

    @Override
    public Optional<Waypoint> findWaypoint(Player player, String waypointId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(waypointId, "waypointId");

        PlayerWaypoints playerWaypoints = this.players.get(player.getUniqueId());
        if (playerWaypoints == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playerWaypoints.get(waypointId)).map(ActiveWaypoint::waypoint);
    }

    @Override
    public List<Waypoint> getWaypoints(Player player) {
        Objects.requireNonNull(player, "player");

        PlayerWaypoints playerWaypoints = this.players.get(player.getUniqueId());
        if (playerWaypoints == null) {
            return Collections.emptyList();
        }
        return playerWaypoints.values().stream()
                .map(ActiveWaypoint::waypoint)
                .toList();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.players.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        this.markAllDespawned(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        this.markAllDespawned(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (this.isLongDistanceTeleport(event.getFrom(), event.getTo())) {
            this.despawnAll(event.getPlayer());
        }
    }

    private void tick() {
        for (PlayerWaypoints playerWaypoints : this.players.values()) {
            this.updatePlayer(playerWaypoints);
        }
        this.firePendingReaches();
    }

    private void updatePlayer(PlayerWaypoints playerWaypoints) {
        Player player = playerWaypoints.player();
        Location location = player.getLocation(this.locationBuffer);
        Position3d eye = new Position3d(location.getX(), location.getY() + player.getEyeHeight(), location.getZ());

        if (!playerWaypoints.updateEyePosition(eye)) {
            return;
        }

        User user = playerWaypoints.resolveUser();
        if (user == null) {
            playerWaypoints.markDirty(); // not injected yet, retry next tick
            return;
        }

        World world = location.getWorld();
        boolean packetsWritten = false;
        for (ActiveWaypoint active : playerWaypoints.values()) {
            packetsWritten |= this.updateWaypoint(playerWaypoints, user, world, eye, active);
        }

        if (packetsWritten) {
            user.flushPackets();
        }
    }

    /**
     * @return true if any packet was written for this waypoint
     */
    private boolean updateWaypoint(PlayerWaypoints owner, User user, World world, Position3d eye, ActiveWaypoint active) {
        Waypoint waypoint = active.waypoint();
        if (!waypoint.isInWorld(world)) {
            return this.despawn(active, user::writePacket);
        }

        ResolvedWaypointPosition resolved = this.positionResolver.resolve(eye, waypoint);
        if (waypoint.isReachedAt(resolved.distance())) {
            this.pendingReaches.add(new PendingReach(owner, active));
        }

        return this.render(owner.player(), user, active, resolved);
    }

    /**
     * @return true if any packet was written
     */
    private boolean render(Player player, User user, ActiveWaypoint active, ResolvedWaypointPosition resolved) {
        Waypoint waypoint = active.waypoint();
        Position3d position = resolved.position();
        long distanceKey = this.textComposer.distanceKey(resolved.distance());

        if (!active.isSpawned()) {
            WaypointRenderer renderer = this.rendererSelector.select(player);
            String distanceLabel = this.textComposer.formatDistanceKey(distanceKey);
            for (PacketWrapper<?> packet : renderer.spawn(active.entityId(), active.entityUuid(), position, waypoint, distanceLabel)) {
                user.writePacket(packet);
            }
            active.markSpawned(renderer, position, distanceKey);
            return true;
        }

        WaypointRenderer renderer = active.renderer();
        boolean packetsWritten = false;

        if (active.hasMoved(position, POSITION_EPSILON_SQUARED)) {
            user.writePacket(renderer.teleport(active.entityId(), position));
            active.updatePosition(position);
            packetsWritten = true;
        }

        if (waypoint.showDistance() && active.hasDistanceKeyChanged(distanceKey)) {
            String distanceLabel = this.textComposer.formatDistanceKey(distanceKey);
            user.writePacket(renderer.updateText(active.entityId(), waypoint, distanceLabel));
            active.updateDistanceKey(distanceKey);
            packetsWritten = true;
        }

        return packetsWritten;
    }

    private void firePendingReaches() {
        if (this.pendingReaches.isEmpty()) {
            return;
        }
        try {
            for (PendingReach pendingReach : this.pendingReaches) {
                this.handleReached(pendingReach);
            }
        } finally {
            this.pendingReaches.clear();
        }
    }

    private void handleReached(PendingReach pendingReach) {
        PlayerWaypoints owner = pendingReach.owner();
        ActiveWaypoint active = pendingReach.active();
        if (!this.isStillActive(owner, active)) {
            return; // quit, hidden or replaced by an earlier listener in this batch
        }

        Player player = owner.player();
        WaypointReachEvent event = new WaypointReachEvent(player, active.waypoint());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || !this.isStillActive(owner, active)) {
            return;
        }

        owner.removeIfSame(active);
        this.despawn(active, this.directSink(player));
        this.cleanupIfEmpty(owner);
    }

    /**
     * @return true if a destroy packet was written
     */
    private boolean despawn(ActiveWaypoint active, Consumer<PacketWrapper<?>> sink) {
        if (!active.isSpawned()) {
            return false;
        }
        sink.accept(active.renderer().destroy(active.entityId()));
        active.markDespawned();
        return true;
    }

    private void despawnAll(Player player) {
        PlayerWaypoints playerWaypoints = this.players.get(player.getUniqueId());
        if (playerWaypoints == null) {
            return;
        }
        Consumer<PacketWrapper<?>> sink = this.directSink(player);
        playerWaypoints.values().forEach(active -> this.despawn(active, sink));
        playerWaypoints.markDirty();
    }

    private void markAllDespawned(Player player) {
        PlayerWaypoints playerWaypoints = this.players.get(player.getUniqueId());
        if (playerWaypoints == null) {
            return;
        }
        playerWaypoints.values().forEach(ActiveWaypoint::markDespawned);
        playerWaypoints.markDirty();
    }

    private boolean isLongDistanceTeleport(Location from, Location to) {
        if (to == null || from.getWorld() != to.getWorld()) {
            return false; // world change is handled by PlayerChangedWorldEvent
        }
        return from.distanceSquared(to) > RESPAWN_TELEPORT_DISTANCE_SQUARED;
    }

    private boolean isStillActive(PlayerWaypoints owner, ActiveWaypoint active) {
        return this.players.get(owner.player().getUniqueId()) == owner && owner.contains(active);
    }

    private void cleanupIfEmpty(PlayerWaypoints playerWaypoints) {
        if (playerWaypoints.isEmpty()) {
            this.players.remove(playerWaypoints.player().getUniqueId(), playerWaypoints);
        }
    }

    /**
     * Sends and flushes immediately - for one-off actions outside the tick (show/hide/events).
     */
    private Consumer<PacketWrapper<?>> directSink(Player player) {
        return packet -> PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private void ensurePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("WaypointService must be used from the primary server thread");
        }
    }

    private record PendingReach(PlayerWaypoints owner, ActiveWaypoint active) {
    }
}

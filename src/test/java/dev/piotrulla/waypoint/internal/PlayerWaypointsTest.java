package dev.piotrulla.waypoint.internal;

import dev.piotrulla.waypoint.TestWorlds;
import dev.piotrulla.waypoint.Waypoint;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PlayerWaypointsTest {

    private static final String WAYPOINT_ID = "home";
    private static final Position3d EYE = new Position3d(0.0, 65.62, 0.0);
    private static final Position3d MOVED_EYE = new Position3d(0.5, 65.62, 0.0);
    private static final Position3d JITTER_EYE = new Position3d(0.0001, 65.62, 0.0);

    private PlayerWaypoints playerWaypoints;
    private Location location;

    @BeforeEach
    void setUp() {
        this.playerWaypoints = new PlayerWaypoints(mock(Player.class));
        this.location = new Location(TestWorlds.defaultWorld(), 10.0, 64.0, 10.0);
    }

    @Test
    void shouldRejectNullPlayer() {
        assertThrows(NullPointerException.class, () -> new PlayerWaypoints(null));
    }

    @Test
    void shouldRequireUpdateOnFirstTick() {
        assertTrue(this.playerWaypoints.updateEyePosition(EYE));
    }

    @Test
    void shouldSkipUpdateWhenPlayerStandsStill() {
        this.playerWaypoints.updateEyePosition(EYE);

        assertFalse(this.playerWaypoints.updateEyePosition(EYE));
        assertFalse(this.playerWaypoints.updateEyePosition(JITTER_EYE));
    }

    @Test
    void shouldUpdateWhenPlayerMoves() {
        this.playerWaypoints.updateEyePosition(EYE);

        assertTrue(this.playerWaypoints.updateEyePosition(MOVED_EYE));
        assertFalse(this.playerWaypoints.updateEyePosition(MOVED_EYE));
    }

    @Test
    void shouldForceUpdateWhenDirty() {
        this.playerWaypoints.updateEyePosition(EYE);

        this.playerWaypoints.markDirty();

        assertTrue(this.playerWaypoints.updateEyePosition(EYE));
        assertFalse(this.playerWaypoints.updateEyePosition(EYE));
    }

    @Test
    void putShouldMarkDirtyAndReturnPrevious() {
        this.playerWaypoints.updateEyePosition(EYE);
        ActiveWaypoint first = this.activeWaypoint();
        ActiveWaypoint second = this.activeWaypoint();

        assertNull(this.playerWaypoints.put(first));
        assertTrue(this.playerWaypoints.updateEyePosition(EYE));

        assertSame(first, this.playerWaypoints.put(second));
        assertSame(second, this.playerWaypoints.get(WAYPOINT_ID));
    }

    @Test
    void removeIfSameShouldIgnoreReplacedInstance() {
        ActiveWaypoint replaced = this.activeWaypoint();
        ActiveWaypoint current = this.activeWaypoint();
        this.playerWaypoints.put(replaced);
        this.playerWaypoints.put(current);

        assertFalse(this.playerWaypoints.contains(replaced));
        assertFalse(this.playerWaypoints.removeIfSame(replaced));
        assertTrue(this.playerWaypoints.contains(current));

        assertTrue(this.playerWaypoints.removeIfSame(current));
        assertTrue(this.playerWaypoints.isEmpty());
    }

    @Test
    void valuesShouldBeReadOnly() {
        this.playerWaypoints.put(this.activeWaypoint());

        assertThrows(UnsupportedOperationException.class, () -> this.playerWaypoints.values().clear());
    }

    private ActiveWaypoint activeWaypoint() {
        Waypoint waypoint = Waypoint.builder(WAYPOINT_ID, this.location).build();
        return new ActiveWaypoint(waypoint, 1, UUID.randomUUID());
    }
}

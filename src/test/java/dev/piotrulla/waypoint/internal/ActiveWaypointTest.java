package dev.piotrulla.waypoint.internal;

import dev.piotrulla.waypoint.TestWorlds;
import dev.piotrulla.waypoint.Waypoint;
import dev.piotrulla.waypoint.internal.render.WaypointRenderer;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ActiveWaypointTest {

    private static final String WAYPOINT_ID = "test";
    private static final int ENTITY_ID = 1337;
    private static final double EPSILON = 0.01;
    private static final double EPSILON_SQUARED = EPSILON * EPSILON;
    private static final long DISTANCE_KEY = 10L;
    private static final long OTHER_DISTANCE_KEY = 11L;
    private static final Position3d POSITION = new Position3d(1.0, 2.0, 3.0);

    private ActiveWaypoint activeWaypoint;
    private WaypointRenderer renderer;

    @BeforeEach
    void setUp() {
        Waypoint waypoint = Waypoint.builder(WAYPOINT_ID, new Location(TestWorlds.defaultWorld(), 0.0, 0.0, 0.0)).build();
        this.activeWaypoint = new ActiveWaypoint(waypoint, ENTITY_ID, UUID.randomUUID());
        this.renderer = mock(WaypointRenderer.class);
    }

    @Test
    void shouldExposeWaypointId() {
        assertEquals(WAYPOINT_ID, this.activeWaypoint.id());
    }

    @Test
    void shouldStartDespawned() {
        assertFalse(this.activeWaypoint.isSpawned());
        assertThrows(IllegalStateException.class, () -> this.activeWaypoint.renderer());
    }

    @Test
    void shouldRejectNullsOnSpawn() {
        assertThrows(NullPointerException.class, () -> this.activeWaypoint.markSpawned(null, POSITION, DISTANCE_KEY));
        assertThrows(NullPointerException.class, () -> this.activeWaypoint.markSpawned(this.renderer, null, DISTANCE_KEY));
    }

    @Test
    void shouldTrackSpawnState() {
        this.activeWaypoint.markSpawned(this.renderer, POSITION, DISTANCE_KEY);

        assertTrue(this.activeWaypoint.isSpawned());
        assertSame(this.renderer, this.activeWaypoint.renderer());

        this.activeWaypoint.markDespawned();

        assertFalse(this.activeWaypoint.isSpawned());
        assertTrue(this.activeWaypoint.hasMoved(POSITION, EPSILON_SQUARED));
        assertTrue(this.activeWaypoint.hasDistanceKeyChanged(DISTANCE_KEY));
    }

    @Test
    void shouldDetectMovementAboveEpsilonOnly() {
        this.activeWaypoint.markSpawned(this.renderer, POSITION, DISTANCE_KEY);

        assertFalse(this.activeWaypoint.hasMoved(POSITION, EPSILON_SQUARED));
        assertFalse(this.activeWaypoint.hasMoved(this.offsetX(EPSILON / 2.0), EPSILON_SQUARED));
        assertTrue(this.activeWaypoint.hasMoved(this.offsetX(EPSILON * 2.0), EPSILON_SQUARED));
    }

    @Test
    void shouldUpdatePosition() {
        Position3d moved = new Position3d(5.0, 5.0, 5.0);
        this.activeWaypoint.markSpawned(this.renderer, POSITION, DISTANCE_KEY);

        this.activeWaypoint.updatePosition(moved);

        assertFalse(this.activeWaypoint.hasMoved(moved, EPSILON_SQUARED));
        assertTrue(this.activeWaypoint.hasMoved(POSITION, EPSILON_SQUARED));
    }

    @Test
    void shouldTrackDistanceKey() {
        this.activeWaypoint.markSpawned(this.renderer, POSITION, DISTANCE_KEY);

        assertFalse(this.activeWaypoint.hasDistanceKeyChanged(DISTANCE_KEY));
        assertTrue(this.activeWaypoint.hasDistanceKeyChanged(OTHER_DISTANCE_KEY));

        this.activeWaypoint.updateDistanceKey(OTHER_DISTANCE_KEY);

        assertFalse(this.activeWaypoint.hasDistanceKeyChanged(OTHER_DISTANCE_KEY));
    }

    private Position3d offsetX(double offset) {
        return new Position3d(POSITION.x() + offset, POSITION.y(), POSITION.z());
    }
}

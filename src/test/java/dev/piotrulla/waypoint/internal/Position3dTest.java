package dev.piotrulla.waypoint.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Position3dTest {

    private static final double PRECISION = 1.0E-9;

    @Test
    void shouldComputeDistance() {
        Position3d origin = new Position3d(0.0, 0.0, 0.0);
        Position3d target = new Position3d(3.0, 4.0, 12.0);

        assertEquals(169.0, origin.distanceSquared(target), PRECISION);
        assertEquals(13.0, origin.distance(target), PRECISION);
        assertEquals(origin.distance(target), target.distance(origin), PRECISION);
    }

    @Test
    void shouldBeValueObject() {
        assertEquals(new Position3d(1.0, 2.0, 3.0), new Position3d(1.0, 2.0, 3.0));
    }

    @Test
    void resolvedPositionShouldRejectNullPosition() {
        assertThrows(NullPointerException.class, () -> new ResolvedWaypointPosition(null, 1.0));
    }
}

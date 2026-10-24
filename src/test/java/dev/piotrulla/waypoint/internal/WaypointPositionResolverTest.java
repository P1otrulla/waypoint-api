package dev.piotrulla.waypoint.internal;

import dev.piotrulla.waypoint.TestWorlds;
import dev.piotrulla.waypoint.Waypoint;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaypointPositionResolverTest {

    private static final double PRECISION = 1.0E-9;
    private static final double EYE_Y = 64.0;
    private static final double DISPLAY_DISTANCE = 12.0;
    private static final String WAYPOINT_ID = "target";
    private static final Position3d EYE = new Position3d(0.0, EYE_Y, 0.0);

    private final WaypointPositionResolver resolver = new WaypointPositionResolver();

    private World world;

    @BeforeEach
    void setUp() {
        this.world = TestWorlds.defaultWorld();
    }

    @Test
    void shouldPlaceDisplayOnTargetWhenCloserThanDisplayDistance() {
        ResolvedWaypointPosition resolved = this.resolver.resolve(EYE, this.waypointAt(5.0, EYE_Y, 0.0));

        this.assertPosition(new Position3d(5.0, EYE_Y, 0.0), resolved.position());
        assertEquals(5.0, resolved.distance(), PRECISION);
    }

    @Test
    void shouldPlaceDisplayOnTargetWhenExactlyAtDisplayDistance() {
        ResolvedWaypointPosition resolved = this.resolver.resolve(EYE, this.waypointAt(DISPLAY_DISTANCE, EYE_Y, 0.0));

        this.assertPosition(new Position3d(DISPLAY_DISTANCE, EYE_Y, 0.0), resolved.position());
        assertEquals(DISPLAY_DISTANCE, resolved.distance(), PRECISION);
    }

    @Test
    void shouldProjectFarTargetOntoDisplayDistance() {
        ResolvedWaypointPosition resolved = this.resolver.resolve(EYE, this.waypointAt(100.0, EYE_Y, 0.0));

        this.assertPosition(new Position3d(DISPLAY_DISTANCE, EYE_Y, 0.0), resolved.position());
        assertEquals(100.0, resolved.distance(), PRECISION);
    }

    @Test
    void shouldKeepDirectionForDiagonalTarget() {
        // 30-40-50 triangle -> projected point is (30, 40) * 12 / 50
        ResolvedWaypointPosition resolved = this.resolver.resolve(EYE, this.waypointAt(30.0, EYE_Y + 40.0, 0.0));

        this.assertPosition(new Position3d(7.2, EYE_Y + 9.6, 0.0), resolved.position());
        assertEquals(50.0, resolved.distance(), PRECISION);
        assertEquals(DISPLAY_DISTANCE, resolved.position().distance(EYE), PRECISION);
    }

    private Waypoint waypointAt(double x, double y, double z) {
        return Waypoint.builder(WAYPOINT_ID, new Location(this.world, x, y, z))
                .displayDistance(DISPLAY_DISTANCE)
                .build();
    }

    private void assertPosition(Position3d expected, Position3d actual) {
        assertEquals(expected.x(), actual.x(), PRECISION, "x");
        assertEquals(expected.y(), actual.y(), PRECISION, "y");
        assertEquals(expected.z(), actual.z(), PRECISION, "z");
    }
}

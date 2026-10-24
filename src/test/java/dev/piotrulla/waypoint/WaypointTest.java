package dev.piotrulla.waypoint;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointTest {

    private static final String WAYPOINT_ID = "spawn";
    private static final String OTHER_WORLD_NAME = "world_nether";
    private static final double X = 10.5;
    private static final double Y = 64.0;
    private static final double Z = -20.5;
    private static final double DEFAULT_DISPLAY_DISTANCE = 12.0;
    private static final float DEFAULT_SCALE = 1.0F;
    private static final double REACH_RADIUS = 3.0;

    private World world;
    private Location location;

    @BeforeEach
    void setUp() {
        this.world = TestWorlds.defaultWorld();
        this.location = new Location(this.world, X, Y, Z);
    }

    @Test
    void shouldBuildWithDefaults() {
        Waypoint waypoint = Waypoint.builder(WAYPOINT_ID, this.location).build();

        assertEquals(WAYPOINT_ID, waypoint.id());
        assertEquals(TestWorlds.DEFAULT_WORLD_NAME, waypoint.worldName());
        assertEquals(new Vector(X, Y, Z), waypoint.toVector());
        assertEquals(X, waypoint.x());
        assertEquals(Y, waypoint.y());
        assertEquals(Z, waypoint.z());
        assertEquals(Component.text(WAYPOINT_ID), waypoint.label());
        assertEquals(DEFAULT_DISPLAY_DISTANCE, waypoint.displayDistance());
        assertEquals(DEFAULT_SCALE, waypoint.scale());
        assertEquals(0.0, waypoint.reachRadius());
        assertTrue(waypoint.showDistance());
    }

    @Test
    void shouldReturnDefensiveVectorCopy() {
        Waypoint waypoint = Waypoint.builder(WAYPOINT_ID, this.location).build();

        waypoint.toVector().setX(999.0);

        assertEquals(X, waypoint.toVector().getX());
    }

    @Test
    void shouldNotBeAffectedByLocationMutation() {
        Waypoint.Builder builder = Waypoint.builder(WAYPOINT_ID, this.location);

        this.location.setX(500.0);

        assertEquals(X, builder.build().toVector().getX());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRejectBlankId(String id) {
        assertThrows(IllegalArgumentException.class, () -> Waypoint.builder(id, this.location));
    }

    @Test
    void shouldRejectNullLocation() {
        assertThrows(NullPointerException.class, () -> Waypoint.builder(WAYPOINT_ID, null));
    }

    @Test
    void shouldRejectLocationWithoutWorld() {
        Location worldless = new Location(null, X, Y, Z);

        assertThrows(NullPointerException.class, () -> Waypoint.builder(WAYPOINT_ID, worldless));
    }

    @Test
    void shouldRejectNullLabel() {
        Waypoint.Builder builder = Waypoint.builder(WAYPOINT_ID, this.location);

        assertThrows(NullPointerException.class, () -> builder.label(null));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void shouldRejectNonFiniteCoordinates(double coordinate) {
        Location invalid = new Location(this.world, coordinate, Y, Z);
        Waypoint.Builder builder = Waypoint.builder(WAYPOINT_ID, invalid);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 1.99, 48.01, Double.NaN})
    void shouldRejectDisplayDistanceOutOfRange(double displayDistance) {
        Waypoint.Builder builder = Waypoint.builder(WAYPOINT_ID, this.location).displayDistance(displayDistance);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @ParameterizedTest
    @ValueSource(doubles = {2.0, 12.0, 48.0})
    void shouldAcceptDisplayDistanceOnBounds(double displayDistance) {
        Waypoint waypoint = Waypoint.builder(WAYPOINT_ID, this.location).displayDistance(displayDistance).build();

        assertEquals(displayDistance, waypoint.displayDistance());
    }

    @ParameterizedTest
    @ValueSource(floats = {0.0F, -1.0F, Float.NaN, Float.POSITIVE_INFINITY})
    void shouldRejectInvalidScale(float scale) {
        Waypoint.Builder builder = Waypoint.builder(WAYPOINT_ID, this.location).scale(scale);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, Double.NaN, Double.POSITIVE_INFINITY})
    void shouldRejectInvalidReachRadius(double reachRadius) {
        Waypoint.Builder builder = Waypoint.builder(WAYPOINT_ID, this.location).reachRadius(reachRadius);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void shouldMatchWorldByName() {
        Waypoint waypoint = Waypoint.builder(WAYPOINT_ID, this.location).build();

        assertTrue(waypoint.isInWorld(TestWorlds.mockWorld(TestWorlds.DEFAULT_WORLD_NAME)));
        assertFalse(waypoint.isInWorld(TestWorlds.mockWorld(OTHER_WORLD_NAME)));
        assertFalse(waypoint.isInWorld(null));
    }

    @Test
    void shouldNeverBeReachedWhenReachDisabled() {
        Waypoint waypoint = Waypoint.builder(WAYPOINT_ID, this.location).build();

        assertFalse(waypoint.isReachedAt(0.0));
    }

    @Test
    void shouldBeReachedInsideRadiusInclusive() {
        Waypoint waypoint = Waypoint.builder(WAYPOINT_ID, this.location).reachRadius(REACH_RADIUS).build();

        assertTrue(waypoint.isReachedAt(REACH_RADIUS - 0.1));
        assertTrue(waypoint.isReachedAt(REACH_RADIUS));
        assertFalse(waypoint.isReachedAt(REACH_RADIUS + 0.1));
    }

    @Test
    void toBuilderShouldCopyEverythingAndKeepOriginalImmutable() {
        Component label = Component.text("Quest", NamedTextColor.GOLD);
        Waypoint original = Waypoint.builder(WAYPOINT_ID, this.location)
                .label(label)
                .displayDistance(20.0)
                .scale(2.0F)
                .reachRadius(REACH_RADIUS)
                .showDistance(false)
                .build();

        Waypoint copy = original.toBuilder().scale(3.0F).build();

        assertEquals(original.id(), copy.id());
        assertEquals(original.worldName(), copy.worldName());
        assertEquals(original.toVector(), copy.toVector());
        assertEquals(label, copy.label());
        assertEquals(20.0, copy.displayDistance());
        assertEquals(REACH_RADIUS, copy.reachRadius());
        assertFalse(copy.showDistance());
        assertEquals(3.0F, copy.scale());
        assertEquals(2.0F, original.scale());
    }
}

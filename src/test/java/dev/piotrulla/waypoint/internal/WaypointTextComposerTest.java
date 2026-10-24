package dev.piotrulla.waypoint.internal;

import dev.piotrulla.waypoint.TestWorlds;
import dev.piotrulla.waypoint.Waypoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class WaypointTextComposerTest {

    private static final String DISTANCE_LABEL = "42m";
    private static final Component LABEL = Component.text("Quest", NamedTextColor.GOLD);
    private static final Component DISTANCE = Component.text(DISTANCE_LABEL, NamedTextColor.GRAY);

    private final WaypointTextComposer composer = new WaypointTextComposer();

    private Waypoint.Builder waypointBuilder;

    @BeforeEach
    void setUp() {
        Location location = new Location(TestWorlds.defaultWorld(), 0.0, 0.0, 0.0);
        this.waypointBuilder = Waypoint.builder("quest", location).label(LABEL);
    }

    @ParameterizedTest
    @CsvSource({
            "0.0, 0m",
            "12.4, 12m",
            "999.4, 999m",
            "999.6, 1.0km",
            "1000.0, 1.0km",
            "2500.0, 2.5km",
            "12345.0, 12.3km"
    })
    void shouldFormatDistance(double distance, String expected) {
        assertEquals(expected, this.composer.formatDistance(distance));
    }

    @Test
    void shouldGiveSameKeyForValuesDisplayedTheSame() {
        assertEquals(this.composer.distanceKey(12.1), this.composer.distanceKey(12.4));
        assertEquals(this.composer.distanceKey(2510.0), this.composer.distanceKey(2540.0));
    }

    @Test
    void shouldGiveDifferentKeyWhenDisplayedValueChanges() {
        assertNotEquals(this.composer.distanceKey(12.4), this.composer.distanceKey(12.6));
        assertNotEquals(this.composer.distanceKey(2540.0), this.composer.distanceKey(2560.0));
        assertNotEquals(this.composer.distanceKey(999.0), this.composer.distanceKey(1000.0));
    }

    @Test
    void shouldFormatWithDotRegardlessOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("pl-PL"));
            assertEquals("2.5km", this.composer.formatDistance(2500.0));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void shouldComposeMultiLine() {
        Component expected = LABEL.append(Component.newline()).append(DISTANCE);

        assertEquals(expected, this.composer.composeMultiLine(this.waypointBuilder.build(), DISTANCE_LABEL));
    }

    @Test
    void shouldComposeSingleLine() {
        Component expected = LABEL.append(Component.text(" ")).append(DISTANCE);

        assertEquals(expected, this.composer.composeSingleLine(this.waypointBuilder.build(), DISTANCE_LABEL));
    }

    @Test
    void shouldReturnBareLabelWhenDistanceHidden() {
        Waypoint waypoint = this.waypointBuilder.showDistance(false).build();

        assertSame(LABEL, this.composer.composeMultiLine(waypoint, DISTANCE_LABEL));
        assertSame(LABEL, this.composer.composeSingleLine(waypoint, DISTANCE_LABEL));
    }
}

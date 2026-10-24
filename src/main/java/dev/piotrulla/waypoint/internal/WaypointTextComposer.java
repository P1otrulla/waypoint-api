package dev.piotrulla.waypoint.internal;

import dev.piotrulla.waypoint.Waypoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public final class WaypointTextComposer {

    private static final long METERS_PER_KILOMETER = 1000L;
    private static final double METERS_PER_KILOMETER_TENTH = 100.0;
    private static final long TENTHS_PER_KILOMETER = 10L;
    private static final long KILOMETER_KEY_OFFSET = 1L << 40;

    private static final String METERS_SUFFIX = "m";
    private static final String KILOMETERS_SUFFIX = "km";
    private static final String DECIMAL_SEPARATOR = ".";

    private static final TextColor DISTANCE_COLOR = NamedTextColor.GRAY;
    private static final Component SINGLE_LINE_SEPARATOR = Component.text(" ");

    public long distanceKey(double distance) {
        long meters = Math.round(distance);
        if (meters < METERS_PER_KILOMETER) {
            return meters;
        }
        return KILOMETER_KEY_OFFSET + Math.round(distance / METERS_PER_KILOMETER_TENTH);
    }

    public String formatDistanceKey(long distanceKey) {
        if (distanceKey < KILOMETER_KEY_OFFSET) {
            return distanceKey + METERS_SUFFIX;
        }
        long tenths = distanceKey - KILOMETER_KEY_OFFSET;
        return (tenths / TENTHS_PER_KILOMETER) + DECIMAL_SEPARATOR + (tenths % TENTHS_PER_KILOMETER) + KILOMETERS_SUFFIX;
    }

    public String formatDistance(double distance) {
        return this.formatDistanceKey(this.distanceKey(distance));
    }

    public Component composeMultiLine(Waypoint waypoint, String distanceLabel) {
        return this.compose(waypoint, distanceLabel, Component.newline());
    }

    public Component composeSingleLine(Waypoint waypoint, String distanceLabel) {
        return this.compose(waypoint, distanceLabel, SINGLE_LINE_SEPARATOR);
    }

    private Component compose(Waypoint waypoint, String distanceLabel, Component separator) {
        if (!waypoint.showDistance()) {
            return waypoint.label();
        }
        return waypoint.label()
                .append(separator)
                .append(Component.text(distanceLabel, DISTANCE_COLOR));
    }
}

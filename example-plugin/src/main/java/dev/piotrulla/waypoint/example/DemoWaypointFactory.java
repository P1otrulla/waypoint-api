package dev.piotrulla.waypoint.example;

import dev.piotrulla.waypoint.Waypoint;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;

import java.util.List;
import java.util.Objects;

/**
 * Four reachable waypoints around the player + one far away to show km formatting and scale.
 */
final class DemoWaypointFactory {

    private static final double CARDINAL_DISTANCE = 50.0;
    private static final double FAR_DISTANCE = 2500.0;
    private static final double NO_OFFSET = 0.0;
    private static final double REACH_RADIUS = 3.0;
    private static final float FAR_SCALE = 1.5F;

    private static final String NORTH_ID = "demo-north";
    private static final String EAST_ID = "demo-east";
    private static final String SOUTH_ID = "demo-south";
    private static final String WEST_ID = "demo-west";
    private static final String FAR_ID = "demo-far";

    List<Waypoint> create(Location origin) {
        Objects.requireNonNull(origin, "origin");

        return List.of(
                this.cardinal(NORTH_ID, origin, NO_OFFSET, -CARDINAL_DISTANCE, "Północ", NamedTextColor.AQUA),
                this.cardinal(EAST_ID, origin, CARDINAL_DISTANCE, NO_OFFSET, "Wschód", NamedTextColor.GREEN),
                this.cardinal(SOUTH_ID, origin, NO_OFFSET, CARDINAL_DISTANCE, "Południe", NamedTextColor.YELLOW),
                this.cardinal(WEST_ID, origin, -CARDINAL_DISTANCE, NO_OFFSET, "Zachód", NamedTextColor.RED),
                this.far(origin)
        );
    }

    private Waypoint cardinal(String id, Location origin, double offsetX, double offsetZ, String name, TextColor color) {
        return Waypoint.builder(id, origin.clone().add(offsetX, NO_OFFSET, offsetZ))
                .label(Component.text(name, color))
                .reachRadius(REACH_RADIUS)
                .build();
    }

    private Waypoint far(Location origin) {
        return Waypoint.builder(FAR_ID, origin.clone().add(FAR_DISTANCE, NO_OFFSET, NO_OFFSET))
                .label(Component.text("Daleko", NamedTextColor.LIGHT_PURPLE))
                .scale(FAR_SCALE)
                .build();
    }
}

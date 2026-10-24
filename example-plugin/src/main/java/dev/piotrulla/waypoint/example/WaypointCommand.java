package dev.piotrulla.waypoint.example;

import dev.piotrulla.waypoint.Waypoint;
import dev.piotrulla.waypoint.WaypointService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

final class WaypointCommand implements CommandExecutor, TabCompleter {

    private static final String SUB_ADD = "add";
    private static final String SUB_AT = "at";
    private static final String SUB_REMOVE = "remove";
    private static final String SUB_CLEAR = "clear";
    private static final String SUB_LIST = "list";
    private static final String SUB_DEMO = "demo";
    private static final List<String> SUBCOMMANDS = List.of(SUB_ADD, SUB_AT, SUB_REMOVE, SUB_CLEAR, SUB_LIST, SUB_DEMO);

    private static final int SUBCOMMAND_INDEX = 0;
    private static final int ID_INDEX = 1;
    private static final int X_INDEX = 2;
    private static final int Y_INDEX = 3;
    private static final int Z_INDEX = 4;
    private static final int ADD_LABEL_START = 2;
    private static final int AT_LABEL_START = 5;
    private static final int ADD_MIN_ARGS = 2;
    private static final int AT_MIN_ARGS = 5;
    private static final int REMOVE_ARGS = 2;
    private static final int SUBCOMMAND_COMPLETION_ARGS = 1;
    private static final int ID_COMPLETION_ARGS = 2;

    private static final double BLOCK_CENTER_OFFSET = 0.5;
    private static final double LABEL_HEIGHT_OFFSET = 1.5;
    private static final String LABEL_SEPARATOR = " ";
    private static final String VECTOR_PATTERN = "%.1f %.1f %.1f";
    private static final LegacyComponentSerializer LABEL_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private static final Component PREFIX = Component.text("[Waypoint] ", NamedTextColor.GOLD);
    private static final List<String> USAGE_LINES = List.of(
            "/wp add <id> [label...]  - waypoint na Twojej pozycji",
            "/wp at <id> <x> <y> <z> [label...]",
            "/wp remove <id>",
            "/wp clear",
            "/wp list",
            "/wp demo  - 4 strony świata + jeden daleki"
    );

    private final WaypointService waypointService;
    private final DemoWaypointFactory demoWaypointFactory;

    WaypointCommand(WaypointService waypointService, DemoWaypointFactory demoWaypointFactory) {
        this.waypointService = Objects.requireNonNull(waypointService, "waypointService");
        this.demoWaypointFactory = Objects.requireNonNull(demoWaypointFactory, "demoWaypointFactory");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX.append(Component.text("Tylko gracz może używać tej komendy.", NamedTextColor.RED)));
            return true;
        }
        if (args.length == 0) {
            this.sendUsage(player);
            return true;
        }

        switch (args[SUBCOMMAND_INDEX].toLowerCase(Locale.ROOT)) {
            case SUB_ADD -> this.handleAdd(player, args);
            case SUB_AT -> this.handleAt(player, args);
            case SUB_REMOVE -> this.handleRemove(player, args);
            case SUB_CLEAR -> this.handleClear(player);
            case SUB_LIST -> this.handleList(player);
            case SUB_DEMO -> this.handleDemo(player);
            default -> this.sendUsage(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        if (args.length == SUBCOMMAND_COMPLETION_ARGS) {
            return this.filterByPrefix(SUBCOMMANDS, args[SUBCOMMAND_INDEX]);
        }
        if (args.length == ID_COMPLETION_ARGS && SUB_REMOVE.equalsIgnoreCase(args[SUBCOMMAND_INDEX])) {
            List<String> ids = this.waypointService.getWaypoints(player).stream()
                    .map(Waypoint::id)
                    .toList();
            return this.filterByPrefix(ids, args[ID_INDEX]);
        }
        return Collections.emptyList();
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < ADD_MIN_ARGS) {
            this.sendUsage(player);
            return;
        }
        String id = args[ID_INDEX];
        Location location = this.aboveBlockCenter(player.getLocation());
        this.tryShow(player, id, location, this.parseLabel(args, ADD_LABEL_START, id));
    }

    private void handleAt(Player player, String[] args) {
        if (args.length < AT_MIN_ARGS) {
            this.sendUsage(player);
            return;
        }
        OptionalDouble x = this.parseCoordinate(args[X_INDEX]);
        OptionalDouble y = this.parseCoordinate(args[Y_INDEX]);
        OptionalDouble z = this.parseCoordinate(args[Z_INDEX]);
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            this.send(player, "Niepoprawne koordynaty.", NamedTextColor.RED);
            return;
        }

        String id = args[ID_INDEX];
        Location location = new Location(player.getWorld(), x.getAsDouble(), y.getAsDouble(), z.getAsDouble());
        this.tryShow(player, id, location, this.parseLabel(args, AT_LABEL_START, id));
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length != REMOVE_ARGS) {
            this.sendUsage(player);
            return;
        }
        String id = args[ID_INDEX];
        if (this.waypointService.hide(player, id)) {
            this.send(player, "Usunięto waypoint '" + id + "'.", NamedTextColor.GREEN);
            return;
        }
        this.send(player, "Nie masz waypointu '" + id + "'.", NamedTextColor.RED);
    }

    private void handleClear(Player player) {
        this.waypointService.hideAll(player);
        this.send(player, "Usunięto wszystkie waypointy.", NamedTextColor.GREEN);
    }

    private void handleList(Player player) {
        List<Waypoint> waypoints = this.waypointService.getWaypoints(player);
        if (waypoints.isEmpty()) {
            this.send(player, "Nie masz żadnych waypointów.", NamedTextColor.GRAY);
            return;
        }

        this.send(player, "Twoje waypointy (" + waypoints.size() + "):", NamedTextColor.YELLOW);
        for (Waypoint waypoint : waypoints) {
            player.sendMessage(Component.text(" - " + waypoint.id() + " ", NamedTextColor.GRAY)
                    .append(waypoint.label())
                    .append(Component.text(" @ " + waypoint.worldName() + " " + this.formatVector(waypoint.toVector()), NamedTextColor.DARK_GRAY)));
        }
    }

    private void handleDemo(Player player) {
        List<Waypoint> demoWaypoints = this.demoWaypointFactory.create(this.aboveBlockCenter(player.getLocation()));
        demoWaypoints.forEach(waypoint -> this.waypointService.show(player, waypoint));
        this.send(player, "Dodano " + demoWaypoints.size() + " demo waypointów. Dojdź do któregoś (3 kratki).", NamedTextColor.GREEN);
    }

    private void tryShow(Player player, String id, Location location, Component label) {
        try {
            Waypoint waypoint = Waypoint.builder(id, location)
                    .label(label)
                    .build();
            this.waypointService.show(player, waypoint);
            this.send(player, "Dodano waypoint '" + id + "'.", NamedTextColor.GREEN);
        } catch (IllegalArgumentException exception) {
            this.send(player, "Niepoprawny waypoint: " + exception.getMessage(), NamedTextColor.RED);
        }
    }

    private Component parseLabel(String[] args, int labelStart, String fallbackId) {
        if (args.length <= labelStart) {
            return Component.text(fallbackId, NamedTextColor.WHITE);
        }
        String rawLabel = String.join(LABEL_SEPARATOR, Arrays.copyOfRange(args, labelStart, args.length));
        return LABEL_SERIALIZER.deserialize(rawLabel);
    }

    private OptionalDouble parseCoordinate(String raw) {
        try {
            return OptionalDouble.of(Double.parseDouble(raw));
        } catch (NumberFormatException exception) {
            return OptionalDouble.empty();
        }
    }

    private Location aboveBlockCenter(Location location) {
        return new Location(
                location.getWorld(),
                location.getBlockX() + BLOCK_CENTER_OFFSET,
                location.getBlockY() + LABEL_HEIGHT_OFFSET,
                location.getBlockZ() + BLOCK_CENTER_OFFSET
        );
    }

    private String formatVector(Vector vector) {
        return String.format(Locale.ROOT, VECTOR_PATTERN, vector.getX(), vector.getY(), vector.getZ());
    }

    private List<String> filterByPrefix(List<String> candidates, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }

    private void sendUsage(Player player) {
        this.send(player, "Użycie:", NamedTextColor.YELLOW);
        USAGE_LINES.forEach(line -> player.sendMessage(Component.text(line, NamedTextColor.GRAY)));
    }

    private void send(Player player, String message, TextColor color) {
        player.sendMessage(PREFIX.append(Component.text(message, color)));
    }
}

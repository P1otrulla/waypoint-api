package dev.piotrulla.waypoint.example;

import dev.piotrulla.waypoint.PacketWaypointService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class WaypointExamplePlugin extends JavaPlugin {

    private static final String COMMAND_NAME = "waypoint";

    private PacketWaypointService waypointService;

    @Override
    public void onEnable() {
        this.waypointService = new PacketWaypointService(this);
        this.waypointService.start();

        WaypointCommand waypointCommand = new WaypointCommand(this.waypointService, new DemoWaypointFactory());
        PluginCommand pluginCommand = Objects.requireNonNull(
                this.getCommand(COMMAND_NAME),
                "Command '" + COMMAND_NAME + "' is missing in plugin.yml"
        );
        pluginCommand.setExecutor(waypointCommand);
        pluginCommand.setTabCompleter(waypointCommand);

        this.getServer().getPluginManager().registerEvents(new WaypointReachListener(), this);
    }

    @Override
    public void onDisable() {
        if (this.waypointService != null) {
            this.waypointService.shutdown();
        }
    }
}

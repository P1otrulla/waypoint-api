package dev.piotrulla.waypoint.example;

import dev.piotrulla.waypoint.WaypointReachEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

final class WaypointReachListener implements Listener {

    private static final String REACH_SOUND = "entity.player.levelup";
    private static final float SOUND_VOLUME = 1.0F;
    private static final float SOUND_PITCH = 1.2F;
    private static final Component REACH_PREFIX = Component.text("Dotarłeś do: ", NamedTextColor.GREEN);

    @EventHandler
    public void onReach(WaypointReachEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(REACH_PREFIX.append(event.getWaypoint().label()));
        player.playSound(player.getLocation(), REACH_SOUND, SOUND_VOLUME, SOUND_PITCH);
    }
}

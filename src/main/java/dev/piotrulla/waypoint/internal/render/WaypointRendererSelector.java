package dev.piotrulla.waypoint.internal.render;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import dev.piotrulla.waypoint.internal.WaypointTextComposer;
import org.bukkit.entity.Player;

public final class WaypointRendererSelector {

    private static final ServerVersion DISPLAY_MIN_SERVER = ServerVersion.V_1_19_4;
    private static final ClientVersion DISPLAY_MIN_CLIENT = ClientVersion.V_1_19_4;

    private final WaypointRenderer displayRenderer;
    private final WaypointRenderer fallbackRenderer;

    private WaypointRendererSelector(WaypointRenderer displayRenderer, WaypointRenderer fallbackRenderer) {
        this.displayRenderer = displayRenderer;
        this.fallbackRenderer = fallbackRenderer;
    }

    public static WaypointRendererSelector create(WaypointTextComposer textComposer, int interpolationTicks) {
        ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();

        WaypointRenderer displayRenderer = serverVersion.isNewerThanOrEquals(DISPLAY_MIN_SERVER)
                ? TextDisplayRenderer.forServer(serverVersion, textComposer, interpolationTicks)
                : null;

        return new WaypointRendererSelector(displayRenderer, ArmorStandRenderer.forServer(serverVersion, textComposer));
    }

    public WaypointRenderer select(Player player) {
        if (this.displayRenderer == null || !this.supportsDisplays(player)) {
            return this.fallbackRenderer;
        }

        return this.displayRenderer;
    }

    private boolean supportsDisplays(Player player) {
        ClientVersion clientVersion = PacketEvents.getAPI().getPlayerManager().getClientVersion(player);
        return clientVersion != null
                && clientVersion != ClientVersion.UNKNOWN
                && clientVersion.isNewerThanOrEquals(DISPLAY_MIN_CLIENT);
    }
}

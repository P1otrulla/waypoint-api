package dev.piotrulla.waypoint.internal.render;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import dev.piotrulla.waypoint.Waypoint;
import dev.piotrulla.waypoint.internal.WaypointTextComposer;
import net.kyori.adventure.text.Component;
import dev.piotrulla.waypoint.internal.Position3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TextDisplayRenderer extends AbstractWaypointRenderer {

    private static final byte BILLBOARD_CENTER = 3;
    private static final int MAX_LIGHT_LEVEL = 15;
    private static final int BLOCK_LIGHT_SHIFT = 4;
    private static final int SKY_LIGHT_SHIFT = 20;
    private static final int FULL_BRIGHTNESS = (MAX_LIGHT_LEVEL << BLOCK_LIGHT_SHIFT) | (MAX_LIGHT_LEVEL << SKY_LIGHT_SHIFT);
    private static final float VIEW_RANGE = 4.0F;
    private static final int BACKGROUND_COLOR = 0x40000000;
    private static final byte FLAG_SHADOW = 0x01;
    private static final byte FLAG_SEE_THROUGH = 0x02;
    private static final byte TEXT_FLAGS = (byte) (FLAG_SHADOW | FLAG_SEE_THROUGH);

    private final DisplayMetadataLayout layout;
    private final WaypointTextComposer textComposer;
    private final int interpolationTicks;

    private TextDisplayRenderer(DisplayMetadataLayout layout, WaypointTextComposer textComposer, int interpolationTicks) {
        this.layout = layout;
        this.textComposer = textComposer;
        this.interpolationTicks = interpolationTicks;
    }

    public static TextDisplayRenderer forServer(ServerVersion serverVersion, WaypointTextComposer textComposer, int interpolationTicks) {
        if (interpolationTicks < 0) {
            throw new IllegalArgumentException("interpolationTicks must be >= 0, got " + interpolationTicks);
        }
        return new TextDisplayRenderer(DisplayMetadataLayout.forServer(serverVersion), textComposer, interpolationTicks);
    }

    @Override
    public List<PacketWrapper<?>> spawn(int entityId, UUID entityUuid, Position3d position, Waypoint waypoint, String distanceLabel) {
        return List.of(
                this.spawnEntity(entityId, entityUuid, EntityTypes.TEXT_DISPLAY, position),
                new WrapperPlayServerEntityMetadata(entityId, this.fullMetadata(waypoint, distanceLabel))
        );
    }

    @Override
    public PacketWrapper<?> updateText(int entityId, Waypoint waypoint, String distanceLabel) {
        List<EntityData> metadata = List.of(this.textEntry(waypoint, distanceLabel));
        return new WrapperPlayServerEntityMetadata(entityId, metadata);
    }

    private List<EntityData> fullMetadata(Waypoint waypoint, String distanceLabel) {
        float scale = waypoint.scale();
        List<EntityData> metadata = new ArrayList<>();

        this.layout.teleportDurationIndex().ifPresent(index ->
                metadata.add(new EntityData(index, EntityDataTypes.INT, this.interpolationTicks)));

        metadata.add(new EntityData(this.layout.scaleIndex(), EntityDataTypes.VECTOR3F, new Vector3f(scale, scale, scale)));
        metadata.add(new EntityData(this.layout.billboardIndex(), EntityDataTypes.BYTE, BILLBOARD_CENTER));
        metadata.add(new EntityData(this.layout.brightnessIndex(), EntityDataTypes.INT, FULL_BRIGHTNESS));
        metadata.add(new EntityData(this.layout.viewRangeIndex(), EntityDataTypes.FLOAT, VIEW_RANGE));
        metadata.add(this.textEntry(waypoint, distanceLabel));
        metadata.add(new EntityData(this.layout.backgroundColorIndex(), EntityDataTypes.INT, BACKGROUND_COLOR));
        metadata.add(new EntityData(this.layout.textFlagsIndex(), EntityDataTypes.BYTE, TEXT_FLAGS));
        return metadata;
    }

    private EntityData textEntry(Waypoint waypoint, String distanceLabel) {
        Component text = this.textComposer.composeMultiLine(waypoint, distanceLabel);
        return new EntityData(this.layout.textIndex(), EntityDataTypes.ADV_COMPONENT, text);
    }
}
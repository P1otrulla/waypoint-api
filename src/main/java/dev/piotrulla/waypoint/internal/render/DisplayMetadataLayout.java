package dev.piotrulla.waypoint.internal.render;

import com.github.retrooper.packetevents.manager.server.ServerVersion;

import java.util.OptionalInt;

record DisplayMetadataLayout(
        OptionalInt teleportDurationIndex,
        int scaleIndex,
        int billboardIndex,
        int brightnessIndex,
        int viewRangeIndex,
        int textIndex,
        int backgroundColorIndex,
        int textFlagsIndex
) {

    private static final ServerVersion TELEPORT_DURATION_SINCE = ServerVersion.V_1_20_2;

    private static final int LEGACY_SCALE_INDEX = 11;
    private static final int LEGACY_BILLBOARD_INDEX = 14;
    private static final int LEGACY_BRIGHTNESS_INDEX = 15;
    private static final int LEGACY_VIEW_RANGE_INDEX = 16;
    private static final int LEGACY_TEXT_INDEX = 22;
    private static final int LEGACY_BACKGROUND_COLOR_INDEX = 24;
    private static final int LEGACY_TEXT_FLAGS_INDEX = 26;

    private static final int TELEPORT_DURATION_INDEX = 10;
    private static final int NO_SHIFT = 0;
    private static final int TELEPORT_DURATION_SHIFT = 1;

    static DisplayMetadataLayout forServer(ServerVersion serverVersion) {
        if (serverVersion.isNewerThanOrEquals(TELEPORT_DURATION_SINCE)) {
            return shifted(TELEPORT_DURATION_SHIFT, OptionalInt.of(TELEPORT_DURATION_INDEX));
        }
        return shifted(NO_SHIFT, OptionalInt.empty());
    }

    private static DisplayMetadataLayout shifted(int shift, OptionalInt teleportDurationIndex) {
        return new DisplayMetadataLayout(
                teleportDurationIndex,
                LEGACY_SCALE_INDEX + shift,
                LEGACY_BILLBOARD_INDEX + shift,
                LEGACY_BRIGHTNESS_INDEX + shift,
                LEGACY_VIEW_RANGE_INDEX + shift,
                LEGACY_TEXT_INDEX + shift,
                LEGACY_BACKGROUND_COLOR_INDEX + shift,
                LEGACY_TEXT_FLAGS_INDEX + shift
        );
    }
}

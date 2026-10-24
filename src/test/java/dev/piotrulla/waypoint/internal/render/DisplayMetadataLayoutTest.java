package dev.piotrulla.waypoint.internal.render;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayMetadataLayoutTest {

    private static final DisplayMetadataLayout EXPECTED_LEGACY = new DisplayMetadataLayout(
            OptionalInt.empty(), 11, 14, 15, 16, 22, 24, 26
    );

    private static final DisplayMetadataLayout EXPECTED_MODERN = new DisplayMetadataLayout(
            OptionalInt.of(10), 12, 15, 16, 17, 23, 25, 27
    );

    @ParameterizedTest
    @EnumSource(value = ServerVersion.class, names = {"V_1_19_4", "V_1_20", "V_1_20_1"})
    void shouldUseLegacyLayoutBefore1_20_2(ServerVersion serverVersion) {
        assertEquals(EXPECTED_LEGACY, DisplayMetadataLayout.forServer(serverVersion));
    }

    @ParameterizedTest
    @EnumSource(value = ServerVersion.class, names = {"V_1_20_2", "V_1_20_4", "V_1_21"})
    void shouldUseShiftedLayoutWithTeleportDurationSince1_20_2(ServerVersion serverVersion) {
        assertEquals(EXPECTED_MODERN, DisplayMetadataLayout.forServer(serverVersion));
    }
}

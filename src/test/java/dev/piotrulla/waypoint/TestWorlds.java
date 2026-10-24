package dev.piotrulla.waypoint;

import org.bukkit.World;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TestWorlds {

    public static final String DEFAULT_WORLD_NAME = "world";

    private TestWorlds() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static World mockWorld(String name) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        return world;
    }

    public static World defaultWorld() {
        return mockWorld(DEFAULT_WORLD_NAME);
    }
}

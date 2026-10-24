package dev.piotrulla.waypoint.internal;

import java.util.concurrent.atomic.AtomicInteger;

public final class EntityIdAllocator {

    private static final int FIRST_ID = Integer.MAX_VALUE;

    private final AtomicInteger nextId = new AtomicInteger(FIRST_ID);

    public int next() {
        return this.nextId.getAndDecrement();
    }
}

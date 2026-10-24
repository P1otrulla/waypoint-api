package dev.piotrulla.waypoint.internal;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityIdAllocatorTest {

    private static final int THREADS = 8;
    private static final int IDS_PER_THREAD = 1_000;
    private static final long TIMEOUT_SECONDS = 10L;

    @Test
    void shouldAllocateDescendingFromTopOfIntRange() {
        EntityIdAllocator allocator = new EntityIdAllocator();

        assertEquals(Integer.MAX_VALUE, allocator.next());
        assertEquals(Integer.MAX_VALUE - 1, allocator.next());
        assertEquals(Integer.MAX_VALUE - 2, allocator.next());
    }

    @Test
    void shouldAllocateUniqueIdsConcurrently() throws InterruptedException {
        EntityIdAllocator allocator = new EntityIdAllocator();
        Set<Integer> ids = ConcurrentHashMap.newKeySet();
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        try {
            for (int thread = 0; thread < THREADS; thread++) {
                executor.execute(() -> {
                    this.awaitQuietly(startSignal);
                    for (int index = 0; index < IDS_PER_THREAD; index++) {
                        ids.add(allocator.next());
                    }
                });
            }
            startSignal.countDown();
        } finally {
            executor.shutdown();
        }

        assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(THREADS * IDS_PER_THREAD, ids.size());
    }

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}

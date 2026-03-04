package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class Int2IntOpenHashMapAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		map.put(1, 11);
		map.put(2, 22);

		int sum = 0;
		try (AllocFreeEntryIteratorInt2Int it = map.poolAllocFreeIterator()) {
			for (Int2IntMap.Entry entry : it) {
				sum += entry.getIntKey();
				sum += entry.getIntValue();
			}
		}
		assertEquals(36, sum);
	}

	@Test
	public void testEntryObjectIsReused() {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		map.put(1, 11);
		map.put(2, 22);

		try (AllocFreeEntryIteratorInt2Int it = map.poolAllocFreeIterator()) {
			final Int2IntMap.Entry first = it.next();
			final Int2IntMap.Entry second = it.next();
			assertSame(first, second);
		}
	}

	@Test
	public void testStructuralModificationGuard() {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		map.put(1, 11);
		map.put(2, 22);
		try (AllocFreeEntryIteratorInt2Int it = map.poolAllocFreeIterator()) {
			map.put(3, 33);
			try {
				it.hasNext();
				fail("Expected IllegalStateException");
			} catch (final IllegalStateException expected) {
				// Expected.
			}
		}
	}

	@Test
	public void testConcurrentBorrowAcrossThreads() throws Exception {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		for (int i = 0; i < 256; i++) {
			map.put(i, i * 2);
		}
		final long expected = iterateAll(map);
		final long[] sums = new long[2];
		final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
		final CountDownLatch opened = new CountDownLatch(2);
		final CountDownLatch go = new CountDownLatch(1);
		final CountDownLatch done = new CountDownLatch(2);

		final Runnable worker0 = new Runnable() {
			@Override
			public void run() {
				try (AllocFreeEntryIteratorInt2Int it = map.poolAllocFreeIterator()) {
					opened.countDown();
					go.await();
					sums[0] = sum(it);
				} catch (final Throwable t) {
					failure.compareAndSet(null, t);
				} finally {
					done.countDown();
				}
			}
		};
		final Runnable worker1 = new Runnable() {
			@Override
			public void run() {
				try (AllocFreeEntryIteratorInt2Int it = map.poolAllocFreeIterator()) {
					opened.countDown();
					go.await();
					sums[1] = sum(it);
				} catch (final Throwable t) {
					failure.compareAndSet(null, t);
				} finally {
					done.countDown();
				}
			}
		};

		new Thread(worker0, "alloc-free-map-worker-0").start();
		new Thread(worker1, "alloc-free-map-worker-1").start();

		assertTrue("Workers did not open iterators in time", opened.await(5, TimeUnit.SECONDS));
		go.countDown();
		assertTrue("Workers did not finish in time", done.await(5, TimeUnit.SECONDS));
		if (failure.get() != null) throw new AssertionError(failure.get());
		assertEquals(expected, sums[0]);
		assertEquals(expected, sums[1]);
	}

	@Test
	public void testZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final Int2IntOpenHashMap map = new Int2IntOpenHashMap(256);
		for (int i = 0; i < 256; i++) {
			map.put(i, i * 2);
		}

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += iterateAll(map);
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += iterateAll(map);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in hot iteration path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	private static long iterateAll(final Int2IntOpenHashMap map) {
		try (AllocFreeEntryIteratorInt2Int it = map.poolAllocFreeIterator()) {
			return sum(it);
		}
	}

	private static long sum(final AllocFreeEntryIteratorInt2Int it) {
		long sum = 0;
		while (it.hasNext()) {
			final Int2IntMap.Entry entry = it.next();
			sum += entry.getIntKey();
			sum += entry.getIntValue();
		}
		return sum;
	}

	private static ThreadMXBean allocationThreadMxBean() {
		final java.lang.management.ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		if (!(threadMXBean instanceof ThreadMXBean)) return null;
		final ThreadMXBean bean = (ThreadMXBean)threadMXBean;
		if (!bean.isThreadAllocatedMemorySupported()) return null;
		if (!bean.isThreadAllocatedMemoryEnabled()) {
			bean.setThreadAllocatedMemoryEnabled(true);
		}
		return bean;
	}
}

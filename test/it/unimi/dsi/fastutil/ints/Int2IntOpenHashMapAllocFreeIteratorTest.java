package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class Int2IntOpenHashMapAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		map.put(1, 11);
		map.put(2, 22);

		final AllocFreeEntryIteratorInt2Int allocFreeIterator = map.createAllocFreeIterator();
		int sum = 0;
		try (AllocFreeEntryIteratorInt2Int it = map.iterateEntries(allocFreeIterator)) {
			for (Int2IntMap.Entry entry : it) {
				sum += entry.getIntKey();
				sum += entry.getIntValue();
			}
		}
		assertEquals(36, sum);
	}

	@Test
	public void testIteratorReuseAndInUseGuard() {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		map.put(1, 11);
		map.put(2, 22);

		final AllocFreeEntryIteratorInt2Int reusable = map.createAllocFreeIterator();
		try (AllocFreeEntryIteratorInt2Int it = map.iterateEntries(reusable)) {
			try {
				map.iterateEntries(reusable);
				fail("Expected IllegalStateException");
			} catch (final IllegalStateException expected) {
				// Expected.
			}
			final Int2IntMap.Entry first = it.next();
			final Int2IntMap.Entry second = it.next();
			assertSame(first, second);
		}
		try (AllocFreeEntryIteratorInt2Int it = map.iterateEntries(reusable)) {
			int seen = 0;
			while (it.hasNext()) {
				it.next();
				seen++;
			}
			assertEquals(2, seen);
		}
	}

	@Test
	public void testIteratorOwnerGuard() {
		final Int2IntOpenHashMap map1 = new Int2IntOpenHashMap();
		final Int2IntOpenHashMap map2 = new Int2IntOpenHashMap();
		final AllocFreeEntryIteratorInt2Int iterator = map1.createAllocFreeIterator();
		try {
			map2.iterateEntries(iterator);
			fail("Expected IllegalArgumentException");
		} catch (final IllegalArgumentException expected) {
			// Expected.
		}
	}

	@Test
	public void testCloseWithoutOpenGuard() {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		final AllocFreeEntryIteratorInt2Int iterator = map.createAllocFreeIterator();
		try {
			iterator.close();
			fail("Expected IllegalStateException");
		} catch (final IllegalStateException expected) {
			// Expected.
		}
	}

	@Test
	public void testStructuralModificationGuard() {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		map.put(1, 11);
		map.put(2, 22);
		final AllocFreeEntryIteratorInt2Int iterator = map.createAllocFreeIterator();
		try (AllocFreeEntryIteratorInt2Int it = map.iterateEntries(iterator)) {
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
	public void testZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final Int2IntOpenHashMap map = new Int2IntOpenHashMap(256);
		for (int i = 0; i < 256; i++) {
			map.put(i, i * 2);
		}
		final AllocFreeEntryIteratorInt2Int reusable = map.createAllocFreeIterator();

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += iterateAll(map, reusable);
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += iterateAll(map, reusable);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in hot iteration path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	private static long iterateAll(final Int2IntOpenHashMap map, final AllocFreeEntryIteratorInt2Int reusable) {
		long sum = 0;
		try (AllocFreeEntryIteratorInt2Int it = map.iterateEntries(reusable)) {
			while (it.hasNext()) {
				final Int2IntMap.Entry entry = it.next();
				sum += entry.getIntKey();
				sum += entry.getIntValue();
			}
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

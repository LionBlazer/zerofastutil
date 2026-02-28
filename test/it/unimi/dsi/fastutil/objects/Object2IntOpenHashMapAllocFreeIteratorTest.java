package it.unimi.dsi.fastutil.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class Object2IntOpenHashMapAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final Object2IntOpenHashMap<String> map = new Object2IntOpenHashMap<String>();
		map.put("one", 1);
		map.put("two", 2);

		final AllocFreeEntryIteratorObject2Int<String> iterator = map.createAllocFreeIterator();
		int seen = 0;
		try (AllocFreeEntryIteratorObject2Int<String> it = map.iterateEntries(iterator)) {
			for (Object2IntMap.Entry<String> entry : it) {
				seen += entry.getKey().length();
				seen += entry.getIntValue();
			}
		}
		assertEquals(9, seen);
	}

	@Test
	public void testEntryObjectIsReused() {
		final Object2IntOpenHashMap<String> map = new Object2IntOpenHashMap<String>();
		map.put("one", 1);
		map.put("two", 2);
		final AllocFreeEntryIteratorObject2Int<String> iterator = map.createAllocFreeIterator();

		try (AllocFreeEntryIteratorObject2Int<String> it = map.iterateEntries(iterator)) {
			final Object2IntMap.Entry<String> first = it.next();
			final Object2IntMap.Entry<String> second = it.next();
			assertSame(first, second);
		}
	}

	@Test
	public void testZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final Object2IntOpenHashMap<String> map = new Object2IntOpenHashMap<String>(256);
		for (int i = 0; i < 256; i++) {
			map.put(Integer.toString(i), i);
		}
		final AllocFreeEntryIteratorObject2Int<String> reusable = map.createAllocFreeIterator();

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

	private static long iterateAll(final Object2IntOpenHashMap<String> map, final AllocFreeEntryIteratorObject2Int<String> reusable) {
		long sum = 0;
		try (AllocFreeEntryIteratorObject2Int<String> it = map.iterateEntries(reusable)) {
			while (it.hasNext()) {
				final Object2IntMap.Entry<String> entry = it.next();
				sum += entry.getKey().length();
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

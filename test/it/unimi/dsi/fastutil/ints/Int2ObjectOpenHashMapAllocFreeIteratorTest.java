package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class Int2ObjectOpenHashMapAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final Int2ObjectOpenHashMap<String> map = new Int2ObjectOpenHashMap<String>();
		map.put(1, "one");
		map.put(2, "two");

		final AllocFreeEntryIteratorInt2Object<String> iterator = map.createAllocFreeIterator();
		int seen = 0;
		try (AllocFreeEntryIteratorInt2Object<String> it = map.iterateEntries(iterator)) {
			for (Int2ObjectMap.Entry<String> entry : it) {
				seen += entry.getIntKey();
				seen += entry.getValue().length();
			}
		}
		assertEquals(9, seen);
	}

	@Test
	public void testEntryObjectIsReused() {
		final Int2ObjectOpenHashMap<String> map = new Int2ObjectOpenHashMap<String>();
		map.put(1, "one");
		map.put(2, "two");
		final AllocFreeEntryIteratorInt2Object<String> iterator = map.createAllocFreeIterator();

		try (AllocFreeEntryIteratorInt2Object<String> it = map.iterateEntries(iterator)) {
			final Int2ObjectMap.Entry<String> first = it.next();
			final Int2ObjectMap.Entry<String> second = it.next();
			assertSame(first, second);
		}
	}

	@Test
	public void testZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final Int2ObjectOpenHashMap<String> map = new Int2ObjectOpenHashMap<String>(256);
		for (int i = 0; i < 256; i++) {
			map.put(i, Integer.toString(i));
		}
		final AllocFreeEntryIteratorInt2Object<String> reusable = map.createAllocFreeIterator();

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

	private static long iterateAll(final Int2ObjectOpenHashMap<String> map, final AllocFreeEntryIteratorInt2Object<String> reusable) {
		long sum = 0;
		try (AllocFreeEntryIteratorInt2Object<String> it = map.iterateEntries(reusable)) {
			while (it.hasNext()) {
				final Int2ObjectMap.Entry<String> entry = it.next();
				sum += entry.getIntKey();
				sum += entry.getValue().length();
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

package it.unimi.dsi.fastutil.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

import it.unimi.dsi.fastutil.ZeroAllocIterable;
import it.unimi.dsi.fastutil.ZeroAllocIterator;

public class Object2IntOpenHashMapZeroAllocIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final Object2IntOpenHashMap<String> map = new Object2IntOpenHashMap<String>();
		map.put("one", 1);
		map.put("two", 2);

		int seen = 0;
		try (ZeroAllocEntryIteratorObject2Int<String> it = map.poolZeroAllocIterator()) {
			for (Object2IntMap.Entry<String> entry : it) {
				seen += entry.getKey().length();
				seen += entry.getIntValue();
			}
		}
		assertEquals(9, seen);
	}

	@Test
	public void testMapImplementsZeroAllocIterableOfEntries() {
		final Object2IntMap<String> map = new Object2IntOpenHashMap<String>();
		map.put("aa", 10);
		map.put("bbbb", 20);

		assertEquals(36, sumEntries(map));
	}

	@Test
	public void testEntryObjectIsReused() {
		final Object2IntOpenHashMap<String> map = new Object2IntOpenHashMap<String>();
		map.put("one", 1);
		map.put("two", 2);

		try (ZeroAllocEntryIteratorObject2Int<String> it = map.poolZeroAllocIterator()) {
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

	private static long iterateAll(final Object2IntOpenHashMap<String> map) {
		long sum = 0;
		try (ZeroAllocEntryIteratorObject2Int<String> it = map.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Object2IntMap.Entry<String> entry = it.next();
				sum += entry.getKey().length();
				sum += entry.getIntValue();
			}
		}
		return sum;
	}

	private static int sumEntries(final ZeroAllocIterable<Object2IntMap.Entry<String>> iterable) {
		int sum = 0;
		try (ZeroAllocIterator<Object2IntMap.Entry<String>> it = iterable.poolZeroAllocIterator()) {
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

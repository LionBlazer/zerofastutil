package it.unimi.dsi.fastutil.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

import it.unimi.dsi.fastutil.ZeroAllocIterable;
import it.unimi.dsi.fastutil.ZeroAllocIterator;

public class Object2ObjectOpenHashMapZeroAllocIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<String, String>();
		map.put("a", "1");
		map.put("b", "2");

		int seen = 0;
		try (ZeroAllocEntryIterator<String, String> it = map.poolZeroAllocIterator()) {
			for (Object2ObjectMap.Entry<String, String> entry : it) {
				assertNotNull(entry.getKey());
				assertNotNull(entry.getValue());
				seen++;
			}
		}
		assertEquals(2, seen);
	}

	@Test
	public void testMapImplementsZeroAllocIterableOfEntries() {
		final Object2ObjectMap<String, String> map = new Object2ObjectOpenHashMap<String, String>();
		map.put("a", "11");
		map.put("bbb", "2");

		assertEquals(7, sumEntries(map));
	}

	@Test
	public void testEntryObjectIsReused() {
		final Object2ObjectOpenHashMap<Integer, Integer> map = new Object2ObjectOpenHashMap<Integer, Integer>();
		map.put(Integer.valueOf(1), Integer.valueOf(11));
		map.put(Integer.valueOf(2), Integer.valueOf(22));

		try (ZeroAllocEntryIterator<Integer, Integer> it = map.poolZeroAllocIterator()) {
			final Object2ObjectMap.Entry<Integer, Integer> first = it.next();
			final Object2ObjectMap.Entry<Integer, Integer> second = it.next();
			assertSame(first, second);
		}
	}

	@Test
	public void testStructuralModificationGuard() {
		final Object2ObjectOpenHashMap<Integer, Integer> map = new Object2ObjectOpenHashMap<Integer, Integer>();
		map.put(Integer.valueOf(1), Integer.valueOf(11));
		map.put(Integer.valueOf(2), Integer.valueOf(22));
		try (ZeroAllocEntryIterator<Integer, Integer> it = map.poolZeroAllocIterator()) {
			map.put(Integer.valueOf(3), Integer.valueOf(33));
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

		final Object2ObjectOpenHashMap<Integer, Integer> map = new Object2ObjectOpenHashMap<Integer, Integer>(256);
		for (int i = 0; i < 256; i++) {
			map.put(Integer.valueOf(i), Integer.valueOf(i * 2));
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

	private static long iterateAll(final Object2ObjectOpenHashMap<Integer, Integer> map) {
		long sum = 0;
		try (ZeroAllocEntryIterator<Integer, Integer> it = map.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Object2ObjectMap.Entry<Integer, Integer> entry = it.next();
				sum += entry.getKey().intValue();
				sum += entry.getValue().intValue();
			}
		}
		return sum;
	}

	private static int sumEntries(final ZeroAllocIterable<Object2ObjectMap.Entry<String, String>> iterable) {
		int sum = 0;
		try (ZeroAllocIterator<Object2ObjectMap.Entry<String, String>> it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Object2ObjectMap.Entry<String, String> entry = it.next();
				sum += entry.getKey().length();
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

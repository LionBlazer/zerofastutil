package it.unimi.dsi.fastutil.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class Object2ObjectOpenHashMapAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<String, String>();
		map.put("a", "1");
		map.put("b", "2");

		final AllocFreeEntryIterator<String, String> allocFreeIterator = map.createAllocFreeIterator();
		int seen = 0;
		try (AllocFreeEntryIterator<String, String> it = map.iterateEntries(allocFreeIterator)) {
			for (Object2ObjectMap.Entry<String, String> entry : it) {
				assertNotNull(entry.getKey());
				assertNotNull(entry.getValue());
				seen++;
			}
		}
		assertEquals(2, seen);
	}

	@Test
	public void testIteratorReuseAndInUseGuard() {
		final Object2ObjectOpenHashMap<Integer, Integer> map = new Object2ObjectOpenHashMap<Integer, Integer>();
		map.put(Integer.valueOf(1), Integer.valueOf(11));
		map.put(Integer.valueOf(2), Integer.valueOf(22));

		final AllocFreeEntryIterator<Integer, Integer> reusable = map.createAllocFreeIterator();
		try (AllocFreeEntryIterator<Integer, Integer> it = map.iterateEntries(reusable)) {
			try {
				map.iterateEntries(reusable);
				fail("Expected IllegalStateException");
			} catch (final IllegalStateException expected) {
				// Expected.
			}
			final Object2ObjectMap.Entry<Integer, Integer> first = it.next();
			final Object2ObjectMap.Entry<Integer, Integer> second = it.next();
			assertSame(first, second);
		}
		try (AllocFreeEntryIterator<Integer, Integer> it = map.iterateEntries(reusable)) {
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
		final Object2ObjectOpenHashMap<Integer, Integer> map1 = new Object2ObjectOpenHashMap<Integer, Integer>();
		final Object2ObjectOpenHashMap<Integer, Integer> map2 = new Object2ObjectOpenHashMap<Integer, Integer>();
		final AllocFreeEntryIterator<Integer, Integer> iterator = map1.createAllocFreeIterator();
		try {
			map2.iterateEntries(iterator);
			fail("Expected IllegalArgumentException");
		} catch (final IllegalArgumentException expected) {
			// Expected.
		}
	}

	@Test
	public void testCloseWithoutOpenGuard() {
		final Object2ObjectOpenHashMap<Integer, Integer> map = new Object2ObjectOpenHashMap<Integer, Integer>();
		final AllocFreeEntryIterator<Integer, Integer> iterator = map.createAllocFreeIterator();
		try {
			iterator.close();
			fail("Expected IllegalStateException");
		} catch (final IllegalStateException expected) {
			// Expected.
		}
	}

	@Test
	public void testStructuralModificationGuard() {
		final Object2ObjectOpenHashMap<Integer, Integer> map = new Object2ObjectOpenHashMap<Integer, Integer>();
		map.put(Integer.valueOf(1), Integer.valueOf(11));
		map.put(Integer.valueOf(2), Integer.valueOf(22));
		final AllocFreeEntryIterator<Integer, Integer> iterator = map.createAllocFreeIterator();
		try (AllocFreeEntryIterator<Integer, Integer> it = map.iterateEntries(iterator)) {
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
		final AllocFreeEntryIterator<Integer, Integer> reusable = map.createAllocFreeIterator();

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

	private static long iterateAll(
			final Object2ObjectOpenHashMap<Integer, Integer> map,
			final AllocFreeEntryIterator<Integer, Integer> reusable) {
		long sum = 0;
		try (AllocFreeEntryIterator<Integer, Integer> it = map.iterateEntries(reusable)) {
			while (it.hasNext()) {
				final Object2ObjectMap.Entry<Integer, Integer> entry = it.next();
				sum += entry.getKey().intValue();
				sum += entry.getValue().intValue();
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

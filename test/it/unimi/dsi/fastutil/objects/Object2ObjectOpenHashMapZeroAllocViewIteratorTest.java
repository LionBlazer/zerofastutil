package it.unimi.dsi.fastutil.objects;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

import it.unimi.dsi.fastutil.ZeroAllocIterable;
import it.unimi.dsi.fastutil.ZeroAllocIterator;

public class Object2ObjectOpenHashMapZeroAllocViewIteratorTest {

	@Test
	public void testKeySetIsUsableAsZeroAllocIterable() {
		final Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<String, String>();
		map.put("a", "xx");
		map.put("bbbb", "y");

		assertEquals(5, sumLengths(map.keySet()));
	}

	@Test
	public void testValuesAreUsableAsZeroAllocIterable() {
		final Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<String, String>();
		map.put("a", "xx");
		map.put("bbbb", "yyy");

		assertEquals(5, sumLengths(map.values()));
	}

	@Test
	public void testKeySetZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<String, String>(256);
		for (int i = 0; i < 256; i++) {
			map.put(Integer.toString(i), Integer.toString(i * 2));
		}

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += sumLengths(map.keySet());
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += sumLengths(map.keySet());
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in Object2ObjectOpenHashMap.keySet() hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	@Test
	public void testValuesZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<String, String>(256);
		for (int i = 0; i < 256; i++) {
			map.put(Integer.toString(i), Integer.toString(i * 2));
		}

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += sumLengths(map.values());
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += sumLengths(map.values());
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in Object2ObjectOpenHashMap.values() hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	private static long sumLengths(final ZeroAllocIterable<String> iterable) {
		long sum = 0;
		try (ZeroAllocIterator<String> it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.next().length();
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

package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class Int2IntOpenHashMapZeroAllocViewIteratorTest {

	@Test
	public void testKeySetIsUsableAsIntZeroAllocIterable() {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		map.put(1, 11);
		map.put(2, 22);

		assertEquals(3, sum(map.keySet()));
	}

	@Test
	public void testValuesAreUsableAsIntZeroAllocIterable() {
		final Int2IntOpenHashMap map = new Int2IntOpenHashMap();
		map.put(1, 11);
		map.put(2, 22);

		assertEquals(33, sum(map.values()));
	}

	@Test
	public void testKeySetZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final Int2IntOpenHashMap map = new Int2IntOpenHashMap(256);
		for (int i = 0; i < 256; i++) {
			map.put(i, i * 2);
		}

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += sum(map.keySet());
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += sum(map.keySet());
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in Int2IntOpenHashMap.keySet() hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	@Test
	public void testValuesZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final Int2IntOpenHashMap map = new Int2IntOpenHashMap(256);
		for (int i = 0; i < 256; i++) {
			map.put(i, i * 2);
		}

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += sum(map.values());
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += sum(map.values());
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in Int2IntOpenHashMap.values() hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	private static long sum(final IntZeroAllocIterable iterable) {
		long sum = 0;
		try (IntZeroAllocIterator it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.nextInt();
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

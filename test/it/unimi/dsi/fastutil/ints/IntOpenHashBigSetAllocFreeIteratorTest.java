package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class IntOpenHashBigSetAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final IntOpenHashBigSet set = new IntOpenHashBigSet();
		set.add(4);
		set.add(8);

		int sum = 0;
		try (AllocFreeIteratorIntBig it = set.poolAllocFreeIterator()) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		assertEquals(12, sum);
	}

	@Test
	public void testZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final IntOpenHashBigSet set = new IntOpenHashBigSet(512);
		for (int i = 0; i < 512; i++) {
			set.add(1000 + i);
		}

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += iterateAll(set);
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += iterateAll(set);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in hot iteration path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	private static long iterateAll(final IntOpenHashBigSet set) {
		long sum = 0;
		try (AllocFreeIteratorIntBig it = set.poolAllocFreeIterator()) {
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

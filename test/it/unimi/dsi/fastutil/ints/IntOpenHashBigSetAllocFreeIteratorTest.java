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

		final AllocFreeIteratorIntBig allocFreeIterator = set.createAllocFreeIterator();
		int sum = 0;
		try (AllocFreeIteratorIntBig it = set.iterateElements(allocFreeIterator)) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		assertEquals(12, sum);
	}

	@Test
	public void testIteratorReuseAndInUseGuard() {
		final IntOpenHashBigSet set = new IntOpenHashBigSet();
		set.add(1);
		set.add(2);

		final AllocFreeIteratorIntBig reusable = set.createAllocFreeIterator();
		try (AllocFreeIteratorIntBig it = set.iterateElements(reusable)) {
			try {
				set.iterateElements(reusable);
				fail("Expected IllegalStateException");
			} catch (final IllegalStateException expected) {
				// Expected.
			}
			assertEquals(2, count(it));
		}
		try (AllocFreeIteratorIntBig it = set.iterateElements(reusable)) {
			assertEquals(2, count(it));
		}
	}

	@Test
	public void testZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final IntOpenHashBigSet set = new IntOpenHashBigSet(512);
		for (int i = 0; i < 512; i++) {
			set.add(1000 + i);
		}
		final AllocFreeIteratorIntBig reusable = set.createAllocFreeIterator();

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += iterateAll(set, reusable);
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += iterateAll(set, reusable);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in hot iteration path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	private static int count(final AllocFreeIteratorIntBig it) {
		int seen = 0;
		while (it.hasNext()) {
			it.nextInt();
			seen++;
		}
		return seen;
	}

	private static long iterateAll(final IntOpenHashBigSet set, final AllocFreeIteratorIntBig reusable) {
		long sum = 0;
		try (AllocFreeIteratorIntBig it = set.iterateElements(reusable)) {
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

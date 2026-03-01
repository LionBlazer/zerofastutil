package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class IntOpenHashSetAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final IntOpenHashSet set = new IntOpenHashSet();
		set.add(10);
		set.add(20);

		final AllocFreeIteratorInt allocFreeIterator = set.createAllocFreeIterator();
		int sum = 0;
		try (AllocFreeIteratorInt it = set.iterateElements(allocFreeIterator)) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		assertEquals(30, sum);
	}

	@Test
	public void testIteratorReuseAndInUseGuard() {
		final IntOpenHashSet set = new IntOpenHashSet();
		set.add(1);
		set.add(2);

		final AllocFreeIteratorInt reusable = set.createAllocFreeIterator();
		try (AllocFreeIteratorInt it = set.iterateElements(reusable)) {
			try {
				set.iterateElements(reusable);
				fail("Expected IllegalStateException");
			} catch (final IllegalStateException expected) {
				// Expected.
			}
			assertEquals(2, count(it));
		}
		try (AllocFreeIteratorInt it = set.iterateElements(reusable)) {
			assertEquals(2, count(it));
		}
	}

	@Test
	public void testIteratorOwnerGuard() {
		final IntOpenHashSet set1 = new IntOpenHashSet();
		final IntOpenHashSet set2 = new IntOpenHashSet();
		final AllocFreeIteratorInt iterator = set1.createAllocFreeIterator();
		try {
			set2.iterateElements(iterator);
			fail("Expected IllegalArgumentException");
		} catch (final IllegalArgumentException expected) {
			// Expected.
		}
	}

	@Test
	public void testStructuralModificationGuard() {
		final IntOpenHashSet set = new IntOpenHashSet();
		set.add(1);
		set.add(2);
		final AllocFreeIteratorInt iterator = set.createAllocFreeIterator();
		try (AllocFreeIteratorInt it = set.iterateElements(iterator)) {
			set.add(3);
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

		final IntOpenHashSet set = new IntOpenHashSet(512);
		for (int i = 0; i < 512; i++) {
			set.add(1000 + i);
		}
		final AllocFreeIteratorInt reusable = set.createAllocFreeIterator();

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

	private static int count(final AllocFreeIteratorInt it) {
		int seen = 0;
		while (it.hasNext()) {
			it.nextInt();
			seen++;
		}
		return seen;
	}

	private static long iterateAll(final IntOpenHashSet set, final AllocFreeIteratorInt reusable) {
		long sum = 0;
		try (AllocFreeIteratorInt it = set.iterateElements(reusable)) {
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

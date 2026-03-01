package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class IntOpenCustomHashSetAllocFreeIteratorTest {

	private static final IntHash.Strategy STRATEGY = new IntHash.Strategy() {
		private static final long serialVersionUID = 1L;

		@Override
		public int hashCode(final int e) {
			return e;
		}

		@Override
		public boolean equals(final int a, final int b) {
			return a == b;
		}
	};

	@Test
	public void testSimpleUsageExample() {
		final IntOpenCustomHashSet set = new IntOpenCustomHashSet(STRATEGY);
		set.add(5);
		set.add(7);

		final AllocFreeIteratorIntCustom allocFreeIterator = set.createAllocFreeIterator();
		int sum = 0;
		try (AllocFreeIteratorIntCustom it = set.iterateElements(allocFreeIterator)) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		assertEquals(12, sum);
	}

	@Test
	public void testIteratorReuseAndInUseGuard() {
		final IntOpenCustomHashSet set = new IntOpenCustomHashSet(STRATEGY);
		set.add(1);
		set.add(2);

		final AllocFreeIteratorIntCustom reusable = set.createAllocFreeIterator();
		try (AllocFreeIteratorIntCustom it = set.iterateElements(reusable)) {
			try {
				set.iterateElements(reusable);
				fail("Expected IllegalStateException");
			} catch (final IllegalStateException expected) {
				// Expected.
			}
			assertEquals(2, count(it));
		}
		try (AllocFreeIteratorIntCustom it = set.iterateElements(reusable)) {
			assertEquals(2, count(it));
		}
	}

	@Test
	public void testStructuralModificationGuard() {
		final IntOpenCustomHashSet set = new IntOpenCustomHashSet(STRATEGY);
		set.add(1);
		set.add(2);
		final AllocFreeIteratorIntCustom iterator = set.createAllocFreeIterator();
		try (AllocFreeIteratorIntCustom it = set.iterateElements(iterator)) {
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

		final IntOpenCustomHashSet set = new IntOpenCustomHashSet(512, STRATEGY);
		for (int i = 0; i < 512; i++) {
			set.add(1000 + i);
		}
		final AllocFreeIteratorIntCustom reusable = set.createAllocFreeIterator();

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

	private static int count(final AllocFreeIteratorIntCustom it) {
		int seen = 0;
		while (it.hasNext()) {
			it.nextInt();
			seen++;
		}
		return seen;
	}

	private static long iterateAll(final IntOpenCustomHashSet set, final AllocFreeIteratorIntCustom reusable) {
		long sum = 0;
		try (AllocFreeIteratorIntCustom it = set.iterateElements(reusable)) {
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

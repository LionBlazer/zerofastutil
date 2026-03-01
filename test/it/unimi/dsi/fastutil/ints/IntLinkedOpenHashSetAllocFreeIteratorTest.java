package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class IntLinkedOpenHashSetAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final IntLinkedOpenHashSet set = new IntLinkedOpenHashSet();
		set.add(3);
		set.add(1);
		set.add(2);

		final AllocFreeIteratorIntLinked allocFreeIterator = set.createAllocFreeIterator();
		try (AllocFreeIteratorIntLinked it = set.iterateElements(allocFreeIterator)) {
			assertEquals(3, it.nextInt());
			assertEquals(1, it.nextInt());
			assertEquals(2, it.nextInt());
			assertFalse(it.hasNext());
		}
	}

	@Test
	public void testIteratorReuseAndInUseGuard() {
		final IntLinkedOpenHashSet set = new IntLinkedOpenHashSet();
		set.add(1);
		set.add(2);

		final AllocFreeIteratorIntLinked reusable = set.createAllocFreeIterator();
		try (AllocFreeIteratorIntLinked it = set.iterateElements(reusable)) {
			try {
				set.iterateElements(reusable);
				fail("Expected IllegalStateException");
			} catch (final IllegalStateException expected) {
				// Expected.
			}
			assertEquals(2, count(it));
		}
		try (AllocFreeIteratorIntLinked it = set.iterateElements(reusable)) {
			assertEquals(2, count(it));
		}
	}

	@Test
	public void testStructuralModificationGuard() {
		final IntLinkedOpenHashSet set = new IntLinkedOpenHashSet();
		set.add(1);
		set.add(2);
		final AllocFreeIteratorIntLinked iterator = set.createAllocFreeIterator();
		try (AllocFreeIteratorIntLinked it = set.iterateElements(iterator)) {
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

		final IntLinkedOpenHashSet set = new IntLinkedOpenHashSet(512);
		for (int i = 0; i < 512; i++) {
			set.add(1000 + i);
		}
		final AllocFreeIteratorIntLinked reusable = set.createAllocFreeIterator();

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

	private static int count(final AllocFreeIteratorIntLinked it) {
		int seen = 0;
		while (it.hasNext()) {
			it.nextInt();
			seen++;
		}
		return seen;
	}

	private static long iterateAll(final IntLinkedOpenHashSet set, final AllocFreeIteratorIntLinked reusable) {
		long sum = 0;
		try (AllocFreeIteratorIntLinked it = set.iterateElements(reusable)) {
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

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

		try (AllocFreeIteratorIntLinked it = set.poolAllocFreeIterator()) {
			assertEquals(3, it.nextInt());
			assertEquals(1, it.nextInt());
			assertEquals(2, it.nextInt());
			assertFalse(it.hasNext());
		}
	}

	@Test
	public void testStructuralModificationGuard() {
		final IntLinkedOpenHashSet set = new IntLinkedOpenHashSet();
		set.add(1);
		set.add(2);
		try (AllocFreeIteratorIntLinked it = set.poolAllocFreeIterator()) {
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

	private static long iterateAll(final IntLinkedOpenHashSet set) {
		long sum = 0;
		try (AllocFreeIteratorIntLinked it = set.poolAllocFreeIterator()) {
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

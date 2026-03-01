package it.unimi.dsi.fastutil.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class ObjectOpenHashSetAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final ObjectOpenHashSet<String> set = new ObjectOpenHashSet<String>();
		set.add("a");
		set.add("bb");

		final AllocFreeIterator<String> allocFreeIterator = set.createAllocFreeIterator();
		int totalLength = 0;
		try (AllocFreeIterator<String> it = set.iterateElements(allocFreeIterator)) {
			for (String value : it) {
				totalLength += value.length();
			}
		}
		assertEquals(3, totalLength);
	}

	@Test
	public void testIteratorReuseAndInUseGuard() {
		final ObjectOpenHashSet<String> set = new ObjectOpenHashSet<String>();
		set.add("one");
		set.add("two");

		final AllocFreeIterator<String> reusable = set.createAllocFreeIterator();
		try (AllocFreeIterator<String> it = set.iterateElements(reusable)) {
			try {
				set.iterateElements(reusable);
				fail("Expected IllegalStateException");
			} catch (final IllegalStateException expected) {
				// Expected.
			}
			assertEquals(2, count(it));
		}
		try (AllocFreeIterator<String> it = set.iterateElements(reusable)) {
			assertEquals(2, count(it));
		}
	}

	@Test
	public void testIteratorOwnerGuard() {
		final ObjectOpenHashSet<String> set1 = new ObjectOpenHashSet<String>();
		final ObjectOpenHashSet<String> set2 = new ObjectOpenHashSet<String>();
		final AllocFreeIterator<String> iterator = set1.createAllocFreeIterator();
		try {
			set2.iterateElements(iterator);
			fail("Expected IllegalArgumentException");
		} catch (final IllegalArgumentException expected) {
			// Expected.
		}
	}

	@Test
	public void testStructuralModificationGuard() {
		final ObjectOpenHashSet<String> set = new ObjectOpenHashSet<String>();
		set.add("one");
		set.add("two");
		final AllocFreeIterator<String> iterator = set.createAllocFreeIterator();
		try (AllocFreeIterator<String> it = set.iterateElements(iterator)) {
			set.add("three");
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

		final ObjectOpenHashSet<String> set = new ObjectOpenHashSet<String>(512);
		for (int i = 0; i < 512; i++) {
			set.add(Integer.toString(i));
		}
		final AllocFreeIterator<String> reusable = set.createAllocFreeIterator();

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

	private static int count(final AllocFreeIterator<String> it) {
		int seen = 0;
		while (it.hasNext()) {
			it.next();
			seen++;
		}
		return seen;
	}

	private static long iterateAll(final ObjectOpenHashSet<String> set, final AllocFreeIterator<String> reusable) {
		long sum = 0;
		try (AllocFreeIterator<String> it = set.iterateElements(reusable)) {
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

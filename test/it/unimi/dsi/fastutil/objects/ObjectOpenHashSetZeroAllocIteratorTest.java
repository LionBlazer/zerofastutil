package it.unimi.dsi.fastutil.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class ObjectOpenHashSetZeroAllocIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final ObjectOpenHashSet<String> set = new ObjectOpenHashSet<String>();
		set.add("a");
		set.add("bb");

		int totalLength = 0;
		try (it.unimi.dsi.fastutil.ZeroAllocIterator<String> it = set.poolZeroAllocIterator()) {
			for (String value : it) {
				totalLength += value.length();
			}
		}
		assertEquals(3, totalLength);
	}

	@Test
	public void testStructuralModificationGuard() {
		final ObjectOpenHashSet<String> set = new ObjectOpenHashSet<String>();
		set.add("one");
		set.add("two");
		try (it.unimi.dsi.fastutil.ZeroAllocIterator<String> it = set.poolZeroAllocIterator()) {
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

	@Test
	public void testZeroAllocationViaPublicZeroAllocIteratorEnhancedForLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final ObjectOpenHashSet<String> set = new ObjectOpenHashSet<String>(512);
		for (int i = 0; i < 512; i++) {
			set.add(Integer.toString(i));
		}

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += iterateAllViaPublicApiEnhancedFor(set);
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += iterateAllViaPublicApiEnhancedFor(set);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations through public ZeroAllocIterator API, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	private static long iterateAll(final ObjectOpenHashSet<String> set) {
		long sum = 0;
		try (it.unimi.dsi.fastutil.ZeroAllocIterator<String> it = set.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.next().length();
			}
		}
		return sum;
	}

	private static long iterateAllViaPublicApiEnhancedFor(final ObjectOpenHashSet<String> set) {
		long sum = 0;
		try (it.unimi.dsi.fastutil.ZeroAllocIterator<String> it = set.poolZeroAllocIterator()) {
			for (final String value : it) {
				sum += value.length();
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

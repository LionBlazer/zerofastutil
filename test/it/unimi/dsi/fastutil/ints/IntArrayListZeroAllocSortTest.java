package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class IntArrayListZeroAllocSortTest {
	private static final IntComparator DESCENDING = new IntComparator() {
		@Override
		public int compare(final int left, final int right) {
			return Integer.compare(right, left);
		}
	};

	@Test
	public void testSortWithExternalBufferGrowsAndDoesNotShrink() {
		final IntArrays.StableSortBuffer buffer = new IntArrays.StableSortBuffer(1);
		final IntArrayList list = IntArrayList.of(5, 4, 3, 2, 1);

		list.sort((IntComparator)null, buffer);
		assertEquals(IntArrayList.of(1, 2, 3, 4, 5), list);
		final int grownCapacity = buffer.capacity();
		assertTrue(grownCapacity >= 5);

		list.clear();
		list.add(2);
		list.add(1);
		list.sort((IntComparator)null, buffer);

		assertEquals(IntArrayList.of(1, 2), list);
		assertEquals(grownCapacity, buffer.capacity());
	}

	@Test
	public void testSortWithExternalBufferZeroAllocationAndCorrectnessNaturalHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final IntArrayList list = createMutableIntList(128);
		final IntArrays.StableSortBuffer buffer = new IntArrays.StableSortBuffer(0);
		for (int i = 0; i < 20000; i++) {
			fillPattern(list, i);
			list.sort((IntComparator)null, buffer);
		}

		final long threadId = Thread.currentThread().getId();
		long sink = 0;
		int sortErrors = 0;
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			fillPattern(list, i);
			final long beforeFingerprint = multisetFingerprint(list);
			list.sort((IntComparator)null, buffer);
			if (!isNonDecreasing(list)) sortErrors++;
			if (beforeFingerprint != multisetFingerprint(list)) sortErrors++;
			sink += list.getInt(0);
			sink += list.getInt(list.size() - 1);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in hot natural sort-with-buffer path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals("Natural sort correctness failures in hot loop", 0, sortErrors);
		assertEquals(0L, sink & 1L);
	}

	@Test
	public void testSortWithExternalBufferZeroAllocationAndCorrectnessComparatorHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final IntArrayList list = createMutableIntList(128);
		final IntArrays.StableSortBuffer buffer = new IntArrays.StableSortBuffer(0);
		for (int i = 0; i < 20000; i++) {
			fillPattern(list, i);
			list.sort(DESCENDING, buffer);
		}

		final long threadId = Thread.currentThread().getId();
		long sink = 0;
		int sortErrors = 0;
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			fillPattern(list, i);
			final long beforeFingerprint = multisetFingerprint(list);
			list.sort(DESCENDING, buffer);
			if (!isNonIncreasing(list)) sortErrors++;
			if (beforeFingerprint != multisetFingerprint(list)) sortErrors++;
			sink += list.getInt(0);
			sink += list.getInt(list.size() - 1);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in hot comparator sort-with-buffer path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals("Comparator sort correctness failures in hot loop", 0, sortErrors);
		assertEquals(0L, sink & 1L);
	}

	@Test
	public void testSortWithExternalBufferUsesSameBufferObject() {
		final IntArrayList list = createMutableIntList(16);
		final IntArrays.StableSortBuffer buffer = new IntArrays.StableSortBuffer(16);
		final IntArrays.StableSortBuffer sameRef = buffer;

		fillPattern(list, 1);
		list.sort((IntComparator)null, buffer);
		assertSame(sameRef, buffer);
	}

	private static IntArrayList createMutableIntList(final int size) {
		final IntArrayList list = new IntArrayList(size);
		for (int i = 0; i < size; i++) {
			list.add(0);
		}
		return list;
	}

	private static void fillPattern(final IntArrayList list, final int iteration) {
		for (int i = 0; i < list.size(); i++) {
			final int value = ((i * 73 + iteration * 19) & 63) - 32;
			list.set(i, value);
		}
	}

	private static long multisetFingerprint(final IntArrayList list) {
		long sum = 0;
		long sumSquares = 0;
		for (int i = 0; i < list.size(); i++) {
			final long value = list.getInt(i);
			sum += value;
			sumSquares += value * value;
		}
		return (sum * 0x9E3779B97F4A7C15L) ^ (sumSquares * 0xC2B2AE3D27D4EB4FL);
	}

	private static boolean isNonDecreasing(final IntArrayList list) {
		for (int i = 1; i < list.size(); i++) {
			if (list.getInt(i - 1) > list.getInt(i)) return false;
		}
		return true;
	}

	private static boolean isNonIncreasing(final IntArrayList list) {
		for (int i = 1; i < list.size(); i++) {
			if (list.getInt(i - 1) < list.getInt(i)) return false;
		}
		return true;
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

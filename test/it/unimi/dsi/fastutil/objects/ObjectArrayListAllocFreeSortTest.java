package it.unimi.dsi.fastutil.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.management.ManagementFactory;
import java.util.Comparator;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class ObjectArrayListAllocFreeSortTest {
	private static final Comparator<String> DESCENDING = new Comparator<String>() {
		@Override
		public int compare(final String left, final String right) {
			return right.compareTo(left);
		}
	};

	@Test
	public void testSortWithExternalBufferZeroAllocationAndCorrectnessNaturalHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final ObjectArrayList<String> list = createMutableObjectList(128);
		final String[] pool = createValuePool(256);
		final ObjectArrays.StableSortBuffer<String> buffer = new ObjectArrays.StableSortBuffer<>(0);
		for (int i = 0; i < 20000; i++) {
			fillPattern(list, pool, i);
			list.sort(null, buffer);
		}

		final long threadId = Thread.currentThread().getId();
		long sink = 0;
		int sortErrors = 0;
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			fillPattern(list, pool, i);
			final long beforeFingerprint = multisetFingerprint(list);
			list.sort(null, buffer);
			if (!isNonDecreasing(list)) sortErrors++;
			if (beforeFingerprint != multisetFingerprint(list)) sortErrors++;
			sink += list.get(0).length();
			sink += list.get(list.size() - 1).length();
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in ObjectArrayList natural sort-with-buffer hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals("ObjectArrayList natural sort correctness failures in hot loop", 0, sortErrors);
		assertEquals(0L, sink & 1L);
	}

	@Test
	public void testSortWithExternalBufferZeroAllocationAndCorrectnessComparatorHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final ObjectArrayList<String> list = createMutableObjectList(128);
		final String[] pool = createValuePool(256);
		final ObjectArrays.StableSortBuffer<String> buffer = new ObjectArrays.StableSortBuffer<String>(0);
		for (int i = 0; i < 20000; i++) {
			fillPattern(list, pool, i);
			list.sort(DESCENDING, buffer);
		}

		final long threadId = Thread.currentThread().getId();
		long sink = 0;
		int sortErrors = 0;
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			fillPattern(list, pool, i);
			final long beforeFingerprint = multisetFingerprint(list);
			list.sort(DESCENDING, buffer);
			if (!isNonIncreasing(list)) sortErrors++;
			if (beforeFingerprint != multisetFingerprint(list)) sortErrors++;
			sink += list.get(0).length();
			sink += list.get(list.size() - 1).length();
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in ObjectArrayList comparator sort-with-buffer hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals("ObjectArrayList comparator sort correctness failures in hot loop", 0, sortErrors);
		assertEquals(0L, sink & 1L);
	}

	@Test
	public void testSortWithExternalBufferUsesSameBufferObject() {
		final ObjectArrayList<String> list = createMutableObjectList(16);
		final String[] pool = createValuePool(32);
		final ObjectArrays.StableSortBuffer<String> buffer = new ObjectArrays.StableSortBuffer<String>(16);
		final ObjectArrays.StableSortBuffer<String> sameRef = buffer;

		fillPattern(list, pool, 1);
		list.sort(null, buffer);
		assertSame(sameRef, buffer);
	}

	private static ObjectArrayList<String> createMutableObjectList(final int size) {
		final ObjectArrayList<String> list = new ObjectArrayList<String>(size);
		for (int i = 0; i < size; i++) {
			list.add("");
		}
		return list;
	}

	private static String[] createValuePool(final int size) {
		final String[] pool = new String[size];
		for (int i = 0; i < size; i++) {
			pool[i] = padded(i);
		}
		return pool;
	}

	private static void fillPattern(final ObjectArrayList<String> list, final String[] pool, final int iteration) {
		for (int i = 0; i < list.size(); i++) {
			final int index = (i * 73 + iteration * 19) % pool.length;
			list.set(i, pool[index]);
		}
	}

	private static long multisetFingerprint(final ObjectArrayList<String> list) {
		long sumIdentity = 0;
		long sumIdentitySquares = 0;
		long sumLength = 0;
		for (int i = 0; i < list.size(); i++) {
			final String value = list.get(i);
			final int identity = System.identityHashCode(value);
			sumIdentity += identity;
			sumIdentitySquares += (long)identity * (long)identity;
			sumLength += value.length();
		}
		return (sumIdentity * 0x9E3779B97F4A7C15L) ^ (sumIdentitySquares * 0xC2B2AE3D27D4EB4FL) ^ sumLength;
	}

	private static boolean isNonDecreasing(final ObjectArrayList<String> list) {
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i - 1).compareTo(list.get(i)) > 0) return false;
		}
		return true;
	}

	private static boolean isNonIncreasing(final ObjectArrayList<String> list) {
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i - 1).compareTo(list.get(i)) < 0) return false;
		}
		return true;
	}

	private static String padded(final int value) {
		if (value < 10) return "000" + value;
		if (value < 100) return "00" + value;
		if (value < 1000) return "0" + value;
		return Integer.toString(value);
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

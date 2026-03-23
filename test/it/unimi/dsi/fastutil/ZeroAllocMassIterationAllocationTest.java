package it.unimi.dsi.fastutil;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class ZeroAllocMassIterationAllocationTest {
	private static final int WARMUP_ROUNDS = 20_000;
	private static final int MEASURE_ROUNDS = 50_000;

	@Test
	public void testMainObjectZeroAllocCollectionsStayAllocationFreeInHotLoops() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final ObjectArrayList<String> list = new ObjectArrayList<String>();
		list.add("aa");
		list.add("bbbb");
		list.add("cccccc");

		final ObjectOpenHashSet<String> set = new ObjectOpenHashSet<String>();
		set.add("aa");
		set.add("bbbb");
		set.add("cccccc");

		final Object2ObjectOpenHashMap<String, String> map = new Object2ObjectOpenHashMap<String, String>();
		map.put("aa", "cccc");
		map.put("bbbb", "dddddd");

		assertZeroAllocObjectArrayList(threadMxBean, list);
		assertZeroAllocObjectOpenHashSet(threadMxBean, set);
		assertZeroAllocObjectMapEntries(threadMxBean, map);
		assertZeroAllocObjectMapKeys(threadMxBean, map);
		assertZeroAllocObjectMapValues(threadMxBean, map);
	}

	private static void assertZeroAllocObjectArrayList(final ThreadMXBean threadMxBean, final ObjectArrayList<String> list) {
		final long expectedPerIteration = iterateObjectArrayList(list);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateObjectArrayList(list);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateObjectArrayList(list);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in ObjectArrayList hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static void assertZeroAllocObjectOpenHashSet(final ThreadMXBean threadMxBean, final ObjectOpenHashSet<String> set) {
		final long expectedPerIteration = iterateObjectOpenHashSet(set);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateObjectOpenHashSet(set);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateObjectOpenHashSet(set);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in ObjectOpenHashSet hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static void assertZeroAllocObjectMapEntries(final ThreadMXBean threadMxBean, final Object2ObjectOpenHashMap<String, String> map) {
		final long expectedPerIteration = iterateObjectMapEntries(map);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateObjectMapEntries(map);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateObjectMapEntries(map);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in Object2ObjectOpenHashMap entry hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static void assertZeroAllocObjectMapKeys(final ThreadMXBean threadMxBean, final Object2ObjectOpenHashMap<String, String> map) {
		final long expectedPerIteration = iterateObjectMapKeys(map);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateObjectMapKeys(map);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateObjectMapKeys(map);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in Object2ObjectOpenHashMap.keySet() hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static void assertZeroAllocObjectMapValues(final ThreadMXBean threadMxBean, final Object2ObjectOpenHashMap<String, String> map) {
		final long expectedPerIteration = iterateObjectMapValues(map);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateObjectMapValues(map);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateObjectMapValues(map);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in Object2ObjectOpenHashMap.values() hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static long iterateObjectArrayList(final ObjectArrayList<String> list) {
		long sum = 0;
		try (ZeroAllocIterator<String> it = list.poolZeroAllocIterator()) {
			for (final String value : it) {
				sum += value.length();
			}
		}
		return sum;
	}

	private static long iterateObjectOpenHashSet(final ObjectOpenHashSet<String> set) {
		long sum = 0;
		try (ZeroAllocIterator<String> it = set.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.next().length();
			}
		}
		return sum;
	}

	private static long iterateObjectMapEntries(final Object2ObjectOpenHashMap<String, String> map) {
		long sum = 0;
		try (ZeroAllocIterator<Object2ObjectMap.Entry<String, String>> it = map.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Object2ObjectMap.Entry<String, String> entry = it.next();
				sum += entry.getKey().length();
				sum += entry.getValue().length();
			}
		}
		return sum;
	}

	private static long iterateObjectMapKeys(final Object2ObjectOpenHashMap<String, String> map) {
		long sum = 0;
		try (ZeroAllocIterator<String> it = map.keySet().poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.next().length();
			}
		}
		return sum;
	}

	private static long iterateObjectMapValues(final Object2ObjectOpenHashMap<String, String> map) {
		long sum = 0;
		try (ZeroAllocIterator<String> it = map.values().poolZeroAllocIterator()) {
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

package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

import it.unimi.dsi.fastutil.ZeroAllocIterator;

public class IntZeroAllocMassIterationAllocationTest {
	private static final int WARMUP_ROUNDS = 20_000;
	private static final int MEASURE_ROUNDS = 50_000;

	@Test
	public void testMainIntZeroAllocCollectionsStayAllocationFreeInHotLoops() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final IntArrayList list = new IntArrayList();
		list.add(2);
		list.add(4);
		list.add(6);

		final IntOpenHashSet set = new IntOpenHashSet();
		set.add(2);
		set.add(4);
		set.add(6);

		final Int2IntOpenHashMap intMap = new Int2IntOpenHashMap();
		intMap.put(2, 20);
		intMap.put(4, 40);

		final Int2ObjectOpenHashMap<String> objectMap = new Int2ObjectOpenHashMap<String>();
		objectMap.put(2, "aa");
		objectMap.put(4, "bbbb");

		assertZeroAllocIntArrayList(threadMxBean, list);
		assertZeroAllocIntOpenHashSet(threadMxBean, set);
		assertZeroAllocIntMapEntries(threadMxBean, intMap);
		assertZeroAllocIntMapKeys(threadMxBean, intMap);
		assertZeroAllocIntMapValues(threadMxBean, intMap);
		assertZeroAllocIntToObjectMapValues(threadMxBean, objectMap);
	}

	private static void assertZeroAllocIntArrayList(final ThreadMXBean threadMxBean, final IntArrayList list) {
		final long expectedPerIteration = iterateIntArrayList(list);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateIntArrayList(list);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateIntArrayList(list);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in IntArrayList hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static void assertZeroAllocIntOpenHashSet(final ThreadMXBean threadMxBean, final IntOpenHashSet set) {
		final long expectedPerIteration = iterateIntOpenHashSet(set);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateIntOpenHashSet(set);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateIntOpenHashSet(set);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in IntOpenHashSet hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static void assertZeroAllocIntMapEntries(final ThreadMXBean threadMxBean, final Int2IntOpenHashMap map) {
		final long expectedPerIteration = iterateIntMapEntries(map);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateIntMapEntries(map);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateIntMapEntries(map);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in Int2IntOpenHashMap entry hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static void assertZeroAllocIntMapKeys(final ThreadMXBean threadMxBean, final Int2IntOpenHashMap map) {
		final long expectedPerIteration = iterateIntMapKeys(map);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateIntMapKeys(map);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateIntMapKeys(map);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in Int2IntOpenHashMap.keySet() hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static void assertZeroAllocIntMapValues(final ThreadMXBean threadMxBean, final Int2IntOpenHashMap map) {
		final long expectedPerIteration = iterateIntMapValues(map);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateIntMapValues(map);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateIntMapValues(map);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in Int2IntOpenHashMap.values() hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static void assertZeroAllocIntToObjectMapValues(final ThreadMXBean threadMxBean, final Int2ObjectOpenHashMap<String> map) {
		final long expectedPerIteration = iterateIntToObjectMapValues(map);
		long sink = 0;
		for (int i = 0; i < WARMUP_ROUNDS; i++) {
			sink += iterateIntToObjectMapValues(map);
		}
		final long before = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId());
		for (int i = 0; i < MEASURE_ROUNDS; i++) {
			sink += iterateIntToObjectMapValues(map);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(Thread.currentThread().getId()) - before;
		assertEquals("Expected zero allocations in Int2ObjectOpenHashMap.values() hot path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(expectedPerIteration * (WARMUP_ROUNDS + MEASURE_ROUNDS), sink);
	}

	private static long iterateIntArrayList(final IntArrayList list) {
		long sum = 0;
		try (IntZeroAllocIterator it = list.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		return sum;
	}

	private static long iterateIntOpenHashSet(final IntOpenHashSet set) {
		long sum = 0;
		try (IntZeroAllocIterator it = set.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		return sum;
	}

	private static long iterateIntMapEntries(final Int2IntOpenHashMap map) {
		long sum = 0;
		try (ZeroAllocEntryIteratorInt2Int it = map.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Int2IntMap.Entry entry = it.next();
				sum += entry.getIntKey();
				sum += entry.getIntValue();
			}
		}
		return sum;
	}

	private static long iterateIntMapKeys(final Int2IntOpenHashMap map) {
		long sum = 0;
		try (IntZeroAllocIterator it = map.keySet().poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		return sum;
	}

	private static long iterateIntMapValues(final Int2IntOpenHashMap map) {
		long sum = 0;
		try (IntZeroAllocIterator it = map.values().poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		return sum;
	}

	private static long iterateIntToObjectMapValues(final Int2ObjectOpenHashMap<String> map) {
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

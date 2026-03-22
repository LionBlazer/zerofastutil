package it.unimi.dsi.fastutil.longs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import it.unimi.dsi.fastutil.ZeroAllocIterable;
import it.unimi.dsi.fastutil.ZeroAllocIterator;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public class LongZeroAllocIterableCompatibilityTest {

	@Test
	public void testLongCollectionsListsAndMapsImplementZeroAllocIterableHierarchy() {
		final LongCollection set = new LongOpenHashSet();
		set.add(10L);
		set.add(40L);

		final LongList list = new LongArrayList();
		list.add(20L);
		list.add(80L);

		final Long2ObjectMap<String> map = new Long2ObjectOpenHashMap<String>();
		map.put(30L, "ab");
		map.put(70L, "cde");

		final Object2LongMap<String> object2Long = new Object2LongOpenHashMap<String>();
		object2Long.put("aa", 10L);
		object2Long.put("bbbb", 20L);

		assertEquals(50L, sumLongs(set));
		assertEquals(100L, sumLongs(list));
		assertEquals(105L, sumLongObjectEntries(map));
		assertEquals(100L, sumLongs(map.keySet()));
		assertEquals(5, sumStringLengths(map.values()));
		assertEquals(36L, sumObjectLongEntries(object2Long));
		assertEquals(30L, sumLongs(object2Long.values()));
	}

	private static long sumLongs(final LongZeroAllocIterable iterable) {
		long sum = 0;
		try (LongZeroAllocIterator it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.nextLong();
			}
		}
		return sum;
	}

	private static long sumLongObjectEntries(final ZeroAllocIterable<Long2ObjectMap.Entry<String>> iterable) {
		long sum = 0;
		try (ZeroAllocIterator<Long2ObjectMap.Entry<String>> it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Long2ObjectMap.Entry<String> entry = it.next();
				sum += entry.getLongKey();
				sum += entry.getValue().length();
			}
		}
		return sum;
	}

	private static long sumObjectLongEntries(final ZeroAllocIterable<Object2LongMap.Entry<String>> iterable) {
		long sum = 0;
		try (ZeroAllocIterator<Object2LongMap.Entry<String>> it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Object2LongMap.Entry<String> entry = it.next();
				sum += entry.getKey().length();
				sum += entry.getLongValue();
			}
		}
		return sum;
	}

	private static int sumStringLengths(final ZeroAllocIterable<String> iterable) {
		int sum = 0;
		try (ZeroAllocIterator<String> it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.next().length();
			}
		}
		return sum;
	}
}

package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import it.unimi.dsi.fastutil.ZeroAllocIterable;
import it.unimi.dsi.fastutil.ZeroAllocIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class IntZeroAllocIterableCompatibilityTest {

	@Test
	public void testIntCollectionsListsAndMapsImplementZeroAllocIterableHierarchy() {
		final IntCollection set = new IntOpenHashSet();
		set.add(1);
		set.add(4);

		final IntList list = new IntArrayList();
		list.add(2);
		list.add(8);

		final Int2IntMap map = new Int2IntOpenHashMap();
		map.put(3, 30);
		map.put(7, 70);

		final Int2ObjectMap<String> int2Object = new Int2ObjectOpenHashMap<String>();
		int2Object.put(5, "xx");
		int2Object.put(9, "yyy");

		final Object2IntMap<String> object2Int = new Object2IntOpenHashMap<String>();
		object2Int.put("aa", 10);
		object2Int.put("bbbb", 20);

		assertEquals(5, sumInts(set));
		assertEquals(10, sumInts(list));
		assertEquals(110, sumIntEntries(map));
		assertEquals(10, sumInts(map.keySet()));
		assertEquals(100, sumInts(map.values()));
		assertEquals(19, sumIntObjectEntries(int2Object));
		assertEquals(14, sumInts(int2Object.keySet()));
		assertEquals(5, sumStringLengths(int2Object.values()));
		assertEquals(36, sumObjectIntEntries(object2Int));
		assertEquals(30, sumInts(object2Int.values()));
	}

	private static int sumInts(final IntZeroAllocIterable iterable) {
		int sum = 0;
		try (IntZeroAllocIterator it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		return sum;
	}

	private static int sumIntEntries(final ZeroAllocIterable<Int2IntMap.Entry> iterable) {
		int sum = 0;
		try (ZeroAllocIterator<Int2IntMap.Entry> it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Int2IntMap.Entry entry = it.next();
				sum += entry.getIntKey();
				sum += entry.getIntValue();
			}
		}
		return sum;
	}

	private static int sumIntObjectEntries(final ZeroAllocIterable<Int2ObjectMap.Entry<String>> iterable) {
		int sum = 0;
		try (ZeroAllocIterator<Int2ObjectMap.Entry<String>> it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Int2ObjectMap.Entry<String> entry = it.next();
				sum += entry.getIntKey();
				sum += entry.getValue().length();
			}
		}
		return sum;
	}

	private static int sumObjectIntEntries(final ZeroAllocIterable<Object2IntMap.Entry<String>> iterable) {
		int sum = 0;
		try (ZeroAllocIterator<Object2IntMap.Entry<String>> it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Object2IntMap.Entry<String> entry = it.next();
				sum += entry.getKey().length();
				sum += entry.getIntValue();
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

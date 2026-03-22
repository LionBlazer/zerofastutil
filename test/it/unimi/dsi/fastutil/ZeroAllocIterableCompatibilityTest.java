package it.unimi.dsi.fastutil;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class ZeroAllocIterableCompatibilityTest {

	@Test
	public void testObjectCollectionsAndListsImplementZeroAllocIterableHierarchy() {
		final ObjectCollection<String> set = new ObjectOpenHashSet<String>();
		set.add("a");
		set.add("bbb");

		final ObjectList<String> list = new ObjectArrayList<String>();
		list.add("cccc");
		list.add("dd");

		assertEquals(4, sumStringLengths(set));
		assertEquals(6, sumStringLengths(list));
	}

	@Test
	public void testObjectMapsAndViewsImplementZeroAllocIterableHierarchy() {
		final Object2ObjectMap<String, String> map = new Object2ObjectOpenHashMap<String, String>();
		map.put("k1", "vv");
		map.put("key2", "x");

		assertEquals(9, sumObjectEntries(map));
		assertEquals(6, sumStringLengths(map.keySet()));
		assertEquals(3, sumStringLengths(map.values()));
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

	private static int sumObjectEntries(final ZeroAllocIterable<Object2ObjectMap.Entry<String, String>> iterable) {
		int sum = 0;
		try (ZeroAllocIterator<Object2ObjectMap.Entry<String, String>> it = iterable.poolZeroAllocIterator()) {
			while (it.hasNext()) {
				final Object2ObjectMap.Entry<String, String> entry = it.next();
				sum += entry.getKey().length();
				sum += entry.getValue().length();
			}
		}
		return sum;
	}
}

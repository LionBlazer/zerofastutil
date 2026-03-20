package it.unimi.dsi.fastutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import org.junit.Test;

public class ListZeroAllocSortApiCoverageTest {

	@Test
	public void testZeroAllocSortApiOnListInterfaces() throws Exception {
		assertListApi("it.unimi.dsi.fastutil.booleans.BooleanList", "it.unimi.dsi.fastutil.booleans.BooleanComparator", "it.unimi.dsi.fastutil.booleans.BooleanArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.bytes.ByteList", "it.unimi.dsi.fastutil.bytes.ByteComparator", "it.unimi.dsi.fastutil.bytes.ByteArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.chars.CharList", "it.unimi.dsi.fastutil.chars.CharComparator", "it.unimi.dsi.fastutil.chars.CharArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.doubles.DoubleList", "it.unimi.dsi.fastutil.doubles.DoubleComparator", "it.unimi.dsi.fastutil.doubles.DoubleArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.floats.FloatList", "it.unimi.dsi.fastutil.floats.FloatComparator", "it.unimi.dsi.fastutil.floats.FloatArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.ints.IntList", "it.unimi.dsi.fastutil.ints.IntComparator", "it.unimi.dsi.fastutil.ints.IntArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.longs.LongList", "it.unimi.dsi.fastutil.longs.LongComparator", "it.unimi.dsi.fastutil.longs.LongArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.shorts.ShortList", "it.unimi.dsi.fastutil.shorts.ShortComparator", "it.unimi.dsi.fastutil.shorts.ShortArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.objects.ObjectList", "java.util.Comparator", "it.unimi.dsi.fastutil.objects.ObjectArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.objects.ReferenceList", "java.util.Comparator", "it.unimi.dsi.fastutil.objects.ObjectArrays$StableSortBuffer");
	}

	@Test
	public void testZeroAllocSortApiOnArrayLists() throws Exception {
		assertListApi("it.unimi.dsi.fastutil.booleans.BooleanArrayList", "it.unimi.dsi.fastutil.booleans.BooleanComparator", "it.unimi.dsi.fastutil.booleans.BooleanArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.bytes.ByteArrayList", "it.unimi.dsi.fastutil.bytes.ByteComparator", "it.unimi.dsi.fastutil.bytes.ByteArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.chars.CharArrayList", "it.unimi.dsi.fastutil.chars.CharComparator", "it.unimi.dsi.fastutil.chars.CharArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.doubles.DoubleArrayList", "it.unimi.dsi.fastutil.doubles.DoubleComparator", "it.unimi.dsi.fastutil.doubles.DoubleArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.floats.FloatArrayList", "it.unimi.dsi.fastutil.floats.FloatComparator", "it.unimi.dsi.fastutil.floats.FloatArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.ints.IntArrayList", "it.unimi.dsi.fastutil.ints.IntComparator", "it.unimi.dsi.fastutil.ints.IntArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.longs.LongArrayList", "it.unimi.dsi.fastutil.longs.LongComparator", "it.unimi.dsi.fastutil.longs.LongArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.shorts.ShortArrayList", "it.unimi.dsi.fastutil.shorts.ShortComparator", "it.unimi.dsi.fastutil.shorts.ShortArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.objects.ObjectArrayList", "java.util.Comparator", "it.unimi.dsi.fastutil.objects.ObjectArrays$StableSortBuffer");
		assertListApi("it.unimi.dsi.fastutil.objects.ReferenceArrayList", "java.util.Comparator", "it.unimi.dsi.fastutil.objects.ObjectArrays$StableSortBuffer");
	}

	private static void assertListApi(final String className, final String comparatorTypeName, final String bufferTypeName) throws Exception {
		final Class<?> listType = Class.forName(className);
		final Class<?> comparatorType = Class.forName(comparatorTypeName);
		final Class<?> bufferType = Class.forName(bufferTypeName);

		final Method sortWithBuffer = listType.getMethod("sortWithBuffer", bufferType);
		assertNotNull(sortWithBuffer);
		assertEquals(void.class, sortWithBuffer.getReturnType());

		final Method sortWithComparatorAndBuffer = listType.getMethod("sort", comparatorType, bufferType);
		assertNotNull(sortWithComparatorAndBuffer);
		assertEquals(void.class, sortWithComparatorAndBuffer.getReturnType());

		try {
			listType.getMethod("sortPooled");
			org.junit.Assert.fail("sortPooled() must not be present on " + className);
		} catch (final NoSuchMethodException expected) {
			// Expected.
		}

		try {
			listType.getMethod("sortPooled", comparatorType);
			org.junit.Assert.fail("sortPooled(comparator) must not be present on " + className);
		} catch (final NoSuchMethodException expected) {
			// Expected.
		}
	}
}

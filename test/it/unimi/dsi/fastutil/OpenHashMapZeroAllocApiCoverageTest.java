package it.unimi.dsi.fastutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

import it.unimi.dsi.fastutil.bytes.Byte2CharOpenHashMap;
import it.unimi.dsi.fastutil.chars.Char2ByteOpenHashMap;
import it.unimi.dsi.fastutil.doubles.Double2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

public class OpenHashMapZeroAllocApiCoverageTest {

	@Test
	public void testZeroAllocApiOnGeneratedOpenHashMapFamilies() throws Exception {
		assertZeroAllocApi(Object2ObjectOpenHashMap.class);
		assertZeroAllocApi(Object2ObjectOpenCustomHashMap.class);
		assertZeroAllocApi(Object2ObjectLinkedOpenHashMap.class);
		assertZeroAllocApi(Object2ObjectLinkedOpenCustomHashMap.class);

		assertZeroAllocApi(Int2IntOpenHashMap.class);
		assertZeroAllocApi(Int2IntOpenCustomHashMap.class);
		assertZeroAllocApi(Int2IntLinkedOpenHashMap.class);
		assertZeroAllocApi(Int2ObjectOpenHashMap.class);

		assertZeroAllocApi(Reference2ReferenceOpenHashMap.class);
		assertZeroAllocApi(Reference2ReferenceOpenCustomHashMap.class);
		assertZeroAllocApi(Reference2ReferenceLinkedOpenHashMap.class);

		assertZeroAllocApi(Byte2CharOpenHashMap.class);
		assertZeroAllocApi(Char2ByteOpenHashMap.class);
		assertZeroAllocApi(Double2FloatOpenHashMap.class);
		assertZeroAllocApi(Long2ObjectOpenHashMap.class);
		assertZeroAllocApi(Object2IntOpenHashMap.class);
		assertZeroAllocApi(Object2LongOpenHashMap.class);
	}

	private static void assertZeroAllocApi(final Class<?> mapClass) throws Exception {
		final Method pool = mapClass.getMethod("poolZeroAllocIterator");
		assertNotNull(pool);
		final Class<?> iteratorType = pool.getReturnType();
		assertTrue("Iterator type should be generated as top-level class", iteratorType.getEnclosingClass() == null);
		assertTrue("Iterator name should start with ZeroAllocEntryIterator", iteratorType.getSimpleName().startsWith("ZeroAllocEntryIterator"));
		assertFalse("Iterator name should stay concise and not contain 'Map'", iteratorType.getSimpleName().contains("Map"));
		assertEquals(iteratorType, pool.getReturnType());
	}
}

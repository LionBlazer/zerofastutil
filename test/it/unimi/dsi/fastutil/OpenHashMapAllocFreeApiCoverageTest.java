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

public class OpenHashMapAllocFreeApiCoverageTest {

	@Test
	public void testAllocFreeApiOnGeneratedOpenHashMapFamilies() throws Exception {
		assertAllocFreeApi(Object2ObjectOpenHashMap.class);
		assertAllocFreeApi(Object2ObjectOpenCustomHashMap.class);
		assertAllocFreeApi(Object2ObjectLinkedOpenHashMap.class);
		assertAllocFreeApi(Object2ObjectLinkedOpenCustomHashMap.class);

		assertAllocFreeApi(Int2IntOpenHashMap.class);
		assertAllocFreeApi(Int2IntOpenCustomHashMap.class);
		assertAllocFreeApi(Int2IntLinkedOpenHashMap.class);
		assertAllocFreeApi(Int2ObjectOpenHashMap.class);

		assertAllocFreeApi(Reference2ReferenceOpenHashMap.class);
		assertAllocFreeApi(Reference2ReferenceOpenCustomHashMap.class);
		assertAllocFreeApi(Reference2ReferenceLinkedOpenHashMap.class);

		assertAllocFreeApi(Byte2CharOpenHashMap.class);
		assertAllocFreeApi(Char2ByteOpenHashMap.class);
		assertAllocFreeApi(Double2FloatOpenHashMap.class);
		assertAllocFreeApi(Long2ObjectOpenHashMap.class);
		assertAllocFreeApi(Object2IntOpenHashMap.class);
		assertAllocFreeApi(Object2LongOpenHashMap.class);
	}

	private static void assertAllocFreeApi(final Class<?> mapClass) throws Exception {
		final Method create = mapClass.getMethod("createAllocFreeIterator");
		assertNotNull(create);
		final Class<?> iteratorType = create.getReturnType();
		assertTrue("Iterator type should be generated as top-level class", iteratorType.getEnclosingClass() == null);
		assertTrue("Iterator name should start with AllocFreeEntryIterator", iteratorType.getSimpleName().startsWith("AllocFreeEntryIterator"));
		assertFalse("Iterator name should stay concise and not contain 'Map'", iteratorType.getSimpleName().contains("Map"));

		final Method iterate = mapClass.getMethod("iterateEntries", iteratorType);
		assertNotNull(iterate);
		assertEquals(iteratorType, iterate.getReturnType());
	}
}

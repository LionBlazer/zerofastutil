package it.unimi.dsi.fastutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntOpenCustomHashSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashBigSet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashBigSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class OpenHashSetZeroAllocApiCoverageTest {

	@Test
	public void testZeroAllocApiOnGeneratedOpenHashSetFamilies() throws Exception {
		assertZeroAllocApi(ObjectOpenHashSet.class);
		assertZeroAllocApi(ObjectOpenCustomHashSet.class);
		assertZeroAllocApi(ObjectLinkedOpenHashSet.class);
		assertZeroAllocApi(ObjectLinkedOpenCustomHashSet.class);
		assertZeroAllocApi(ObjectOpenHashBigSet.class);

		assertZeroAllocApi(IntOpenHashSet.class);
		assertZeroAllocApi(IntOpenCustomHashSet.class);
		assertZeroAllocApi(IntLinkedOpenHashSet.class);
		assertZeroAllocApi(IntLinkedOpenCustomHashSet.class);
		assertZeroAllocApi(IntOpenHashBigSet.class);
	}

	private static void assertZeroAllocApi(final Class<?> setClass) throws Exception {
		final Method pool = setClass.getMethod("poolZeroAllocIterator");
		assertNotNull(pool);
		final Class<?> iteratorType = pool.getReturnType();
		assertTrue("Iterator type should be generated as top-level class", iteratorType.getEnclosingClass() == null);
		assertTrue("Iterator name should start with ZeroAllocIterator", iteratorType.getSimpleName().startsWith("ZeroAllocIterator"));
		assertFalse("Iterator name should stay concise and not contain 'Set'", iteratorType.getSimpleName().contains("Set"));
		assertEquals(iteratorType, pool.getReturnType());
	}
}

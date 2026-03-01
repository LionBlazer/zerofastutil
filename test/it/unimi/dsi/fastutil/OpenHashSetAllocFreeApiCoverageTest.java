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

public class OpenHashSetAllocFreeApiCoverageTest {

	@Test
	public void testAllocFreeApiOnGeneratedOpenHashSetFamilies() throws Exception {
		assertAllocFreeApi(ObjectOpenHashSet.class);
		assertAllocFreeApi(ObjectOpenCustomHashSet.class);
		assertAllocFreeApi(ObjectLinkedOpenHashSet.class);
		assertAllocFreeApi(ObjectLinkedOpenCustomHashSet.class);
		assertAllocFreeApi(ObjectOpenHashBigSet.class);

		assertAllocFreeApi(IntOpenHashSet.class);
		assertAllocFreeApi(IntOpenCustomHashSet.class);
		assertAllocFreeApi(IntLinkedOpenHashSet.class);
		assertAllocFreeApi(IntLinkedOpenCustomHashSet.class);
		assertAllocFreeApi(IntOpenHashBigSet.class);
	}

	private static void assertAllocFreeApi(final Class<?> setClass) throws Exception {
		final Method create = setClass.getMethod("createAllocFreeIterator");
		assertNotNull(create);
		final Class<?> iteratorType = create.getReturnType();
		assertTrue("Iterator type should be generated as top-level class", iteratorType.getEnclosingClass() == null);
		assertTrue("Iterator name should start with AllocFreeIterator", iteratorType.getSimpleName().startsWith("AllocFreeIterator"));
		assertFalse("Iterator name should stay concise and not contain 'Set'", iteratorType.getSimpleName().contains("Set"));

		final Method iterate = setClass.getMethod("iterateElements", iteratorType);
		assertNotNull(iterate);
		assertEquals(iteratorType, iterate.getReturnType());
	}
}

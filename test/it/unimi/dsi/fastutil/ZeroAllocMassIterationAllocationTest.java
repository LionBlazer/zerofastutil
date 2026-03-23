package it.unimi.dsi.fastutil;

import static org.junit.Assert.assertEquals;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleToLongFunction;
import java.util.function.IntToLongFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ToLongFunction;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

import it.unimi.dsi.fastutil.bytes.Byte2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.bytes.Byte2CharOpenHashMap;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.chars.Char2ByteOpenHashMap;
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.doubles.Double2FloatOpenHashMap;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class ZeroAllocMassIterationAllocationTest {
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
	private static final int WARMUP_ROUNDS = 20_000;
	private static final int MEASURE_ROUNDS = 50_000;

	@Test
	public void testAllZeroAllocCollectionsStayAllocationFreeInHotLoops() throws Throwable {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final List<IterationCase> cases = buildCases();
		final long threadId = Thread.currentThread().getId();

		long sink = 0;
		for (final IterationCase iterationCase : cases) {
			for (int i = 0; i < WARMUP_ROUNDS; i++) {
				sink += iterationCase.iterateOnce();
			}

			final long before = threadMxBean.getThreadAllocatedBytes(threadId);
			for (int i = 0; i < MEASURE_ROUNDS; i++) {
				sink += iterationCase.iterateOnce();
			}
			final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
			assertEquals("Expected zero allocations in " + iterationCase.name + ", but got " + allocated + " bytes", 0L, allocated);
		}

		assertEquals(0L, sink & 1L);
	}

	private static List<IterationCase> buildCases() throws Throwable {
		final List<IterationCase> cases = new ArrayList<IterationCase>();

		final ObjectArrayList<String> objectList = new ObjectArrayList<String>();
		objectList.add("aa");
		objectList.add("bbbb");
		objectList.add("cccccc");
		cases.add(objectCase("ObjectArrayList", objectList, new ToLongFunction<Object>() {
			@Override
			public long applyAsLong(final Object value) {
				return ((String)value).length();
			}
		}));

		final ObjectOpenHashSet<String> objectSet = new ObjectOpenHashSet<String>();
		objectSet.add("aa");
		objectSet.add("bbbb");
		objectSet.add("cccccc");
		cases.add(objectCase("ObjectOpenHashSet", objectSet, new ToLongFunction<Object>() {
			@Override
			public long applyAsLong(final Object value) {
				return ((String)value).length();
			}
		}));

		final Object2ObjectOpenHashMap<String, String> objectMap = new Object2ObjectOpenHashMap<String, String>();
		objectMap.put("aa", "cccc");
		objectMap.put("bbbb", "dddddd");
		cases.add(objectCase("Object2ObjectOpenHashMap.entries", objectMap, new ToLongFunction<Object>() {
			@Override
			public long applyAsLong(final Object value) {
				final Object2ObjectMap.Entry<String, String> entry = (Object2ObjectMap.Entry<String, String>)value;
				return entry.getKey().length() + entry.getValue().length();
			}
		}));
		cases.add(objectCase("Object2ObjectOpenHashMap.keySet", objectMap.keySet(), new ToLongFunction<Object>() {
			@Override
			public long applyAsLong(final Object value) {
				return ((String)value).length();
			}
		}));
		cases.add(objectCase("Object2ObjectOpenHashMap.values", objectMap.values(), new ToLongFunction<Object>() {
			@Override
			public long applyAsLong(final Object value) {
				return ((String)value).length();
			}
		}));

		final it.unimi.dsi.fastutil.booleans.BooleanArrayList booleanList = new it.unimi.dsi.fastutil.booleans.BooleanArrayList();
		booleanList.add(true);
		booleanList.add(false);
		booleanList.add(true);
		booleanList.add(false);
		cases.add(booleanCase("BooleanArrayList", booleanList, new BooleanToLongFunction() {
			@Override
			public long applyAsLong(final boolean value) {
				return value ? 1L : 0L;
			}
		}));

		final ByteArrayList byteList = new ByteArrayList();
		byteList.add((byte)2);
		byteList.add((byte)4);
		byteList.add((byte)6);
		cases.add(intBackedPrimitiveCase("ByteArrayList", byteList, "nextByte", Kind.BYTE, new IntToLongFunction() {
			@Override
			public long applyAsLong(final int value) {
				return value;
			}
		}));

		final CharArrayList charList = new CharArrayList();
		charList.add('b');
		charList.add('d');
		charList.add('f');
		cases.add(intBackedPrimitiveCase("CharArrayList", charList, "nextChar", Kind.CHAR, new IntToLongFunction() {
			@Override
			public long applyAsLong(final int value) {
				return value;
			}
		}));

		final ShortArrayList shortList = new ShortArrayList();
		shortList.add((short)2);
		shortList.add((short)4);
		shortList.add((short)6);
		cases.add(intBackedPrimitiveCase("ShortArrayList", shortList, "nextShort", Kind.SHORT, new IntToLongFunction() {
			@Override
			public long applyAsLong(final int value) {
				return value;
			}
		}));

		final IntArrayList intList = new IntArrayList();
		intList.add(2);
		intList.add(4);
		intList.add(6);
		cases.add(intBackedPrimitiveCase("IntArrayList", intList, "nextInt", Kind.INT, new IntToLongFunction() {
			@Override
			public long applyAsLong(final int value) {
				return value;
			}
		}));

		final LongArrayList longList = new LongArrayList();
		longList.add(2L);
		longList.add(4L);
		longList.add(6L);
		cases.add(longCase("LongArrayList", longList, new LongUnaryOperator() {
			@Override
			public long applyAsLong(final long operand) {
				return operand;
			}
		}));

		final FloatArrayList floatList = new FloatArrayList();
		floatList.add(2.0f);
		floatList.add(4.0f);
		floatList.add(6.0f);
		cases.add(floatCase("FloatArrayList", floatList, new FloatToLongFunction() {
			@Override
			public long applyAsLong(final float value) {
				return (long)value;
			}
		}));

		final DoubleArrayList doubleList = new DoubleArrayList();
		doubleList.add(2.0d);
		doubleList.add(4.0d);
		doubleList.add(6.0d);
		cases.add(doubleCase("DoubleArrayList", doubleList, new DoubleToLongFunction() {
			@Override
			public long applyAsLong(final double value) {
				return (long)value;
			}
		}));

		final IntOpenHashSet intSet = new IntOpenHashSet();
		intSet.add(2);
		intSet.add(4);
		intSet.add(6);
		cases.add(intBackedPrimitiveCase("IntOpenHashSet", intSet, "nextInt", Kind.INT, new IntToLongFunction() {
			@Override
			public long applyAsLong(final int value) {
				return value;
			}
		}));

		final Int2IntOpenHashMap intMap = new Int2IntOpenHashMap();
		intMap.put(2, 20);
		intMap.put(4, 40);
		cases.add(objectCase("Int2IntOpenHashMap.entries", intMap, new ToLongFunction<Object>() {
			@Override
			public long applyAsLong(final Object value) {
				final Int2IntMap.Entry entry = (Int2IntMap.Entry)value;
				return entry.getIntKey() + entry.getIntValue();
			}
		}));
		cases.add(intBackedPrimitiveCase("Int2IntOpenHashMap.keySet", intMap.keySet(), "nextInt", Kind.INT, new IntToLongFunction() {
			@Override
			public long applyAsLong(final int value) {
				return value;
			}
		}));
		cases.add(intBackedPrimitiveCase("Int2IntOpenHashMap.values", intMap.values(), "nextInt", Kind.INT, new IntToLongFunction() {
			@Override
			public long applyAsLong(final int value) {
				return value;
			}
		}));

		final Byte2BooleanOpenHashMap byteToBooleanMap = new Byte2BooleanOpenHashMap();
		byteToBooleanMap.put((byte)2, true);
		byteToBooleanMap.put((byte)4, false);
		byteToBooleanMap.put((byte)6, true);
		byteToBooleanMap.put((byte)8, false);
		cases.add(booleanCase("Byte2BooleanOpenHashMap.values", byteToBooleanMap.values(), new BooleanToLongFunction() {
			@Override
			public long applyAsLong(final boolean value) {
				return value ? 1L : 0L;
			}
		}));

		final Char2ByteOpenHashMap charToByteMap = new Char2ByteOpenHashMap();
		charToByteMap.put('a', (byte)2);
		charToByteMap.put('b', (byte)4);
		charToByteMap.put('c', (byte)6);
		cases.add(intBackedPrimitiveCase("Char2ByteOpenHashMap.values", charToByteMap.values(), "nextByte", Kind.BYTE, new IntToLongFunction() {
			@Override
			public long applyAsLong(final int value) {
				return value;
			}
		}));

		final Byte2CharOpenHashMap byteToCharMap = new Byte2CharOpenHashMap();
		byteToCharMap.put((byte)2, 'b');
		byteToCharMap.put((byte)4, 'd');
		byteToCharMap.put((byte)6, 'f');
		cases.add(intBackedPrimitiveCase("Byte2CharOpenHashMap.values", byteToCharMap.values(), "nextChar", Kind.CHAR, new IntToLongFunction() {
			@Override
			public long applyAsLong(final int value) {
				return value;
			}
		}));

		final Double2FloatOpenHashMap doubleToFloatMap = new Double2FloatOpenHashMap();
		doubleToFloatMap.put(2.0d, 2.0f);
		doubleToFloatMap.put(4.0d, 4.0f);
		cases.add(doubleCase("Double2FloatOpenHashMap.keySet", doubleToFloatMap.keySet(), new DoubleToLongFunction() {
			@Override
			public long applyAsLong(final double value) {
				return (long)value;
			}
		}));
		cases.add(floatCase("Double2FloatOpenHashMap.values", doubleToFloatMap.values(), new FloatToLongFunction() {
			@Override
			public long applyAsLong(final float value) {
				return (long)value;
			}
		}));

		final Object2LongOpenHashMap<String> objectToLongMap = new Object2LongOpenHashMap<String>();
		objectToLongMap.put("aa", 2L);
		objectToLongMap.put("bbbb", 4L);
		objectToLongMap.put("cccccc", 6L);
		cases.add(longCase("Object2LongOpenHashMap.values", objectToLongMap.values(), new LongUnaryOperator() {
			@Override
			public long applyAsLong(final long operand) {
				return operand;
			}
		}));

		return cases;
	}

	private static IterationCase objectCase(final String name, final Object owner, final ToLongFunction<Object> valueFn) throws Throwable {
		return new IterationCase(name, owner, "next", Kind.OBJECT, Object.class, valueFn, null, null, null, null);
	}

	private static IterationCase booleanCase(final String name, final Object owner, final BooleanToLongFunction valueFn) throws Throwable {
		return new IterationCase(name, owner, "nextBoolean", Kind.BOOLEAN, boolean.class, null, valueFn, null, null, null);
	}

	private static IterationCase intBackedPrimitiveCase(final String name, final Object owner, final String nextMethod, final Kind kind, final IntToLongFunction valueFn) throws Throwable {
		return new IterationCase(name, owner, nextMethod, kind, kind == Kind.INT ? int.class : (kind == Kind.CHAR ? char.class : (kind == Kind.SHORT ? short.class : byte.class)), null, null, valueFn, null, null);
	}

	private static IterationCase longCase(final String name, final Object owner, final LongUnaryOperator valueFn) throws Throwable {
		return new IterationCase(name, owner, "nextLong", Kind.LONG, long.class, null, null, null, valueFn, null);
	}

	private static IterationCase floatCase(final String name, final Object owner, final FloatToLongFunction valueFn) throws Throwable {
		return new IterationCase(name, owner, "nextFloat", Kind.FLOAT, float.class, null, null, null, null, valueFn);
	}

	private static IterationCase doubleCase(final String name, final Object owner, final DoubleToLongFunction valueFn) throws Throwable {
		return new IterationCase(name, owner, "nextDouble", Kind.DOUBLE, double.class, null, null, null, null, null, valueFn);
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

	private enum Kind {
		OBJECT,
		BOOLEAN,
		BYTE,
		SHORT,
		CHAR,
		INT,
		LONG,
		FLOAT,
		DOUBLE
	}

	private interface BooleanToLongFunction {
		long applyAsLong(boolean value);
	}

	private interface FloatToLongFunction {
		long applyAsLong(float value);
	}

	private static final class IterationCase {
		private final String name;
		private final Object owner;
		private final Kind kind;
		private final MethodHandle borrow;
		private final MethodHandle hasNext;
		private final MethodHandle close;
		private final MethodHandle next;
		private final ToLongFunction<Object> objectValueFn;
		private final BooleanToLongFunction booleanValueFn;
		private final IntToLongFunction intValueFn;
		private final LongUnaryOperator longValueFn;
		private final FloatToLongFunction floatValueFn;
		private final DoubleToLongFunction doubleValueFn;

		private IterationCase(
				final String name,
				final Object owner,
				final String nextMethod,
				final Kind kind,
				final Class<?> nextReturnType,
				final ToLongFunction<Object> objectValueFn,
				final BooleanToLongFunction booleanValueFn,
				final IntToLongFunction intValueFn,
				final LongUnaryOperator longValueFn,
				final FloatToLongFunction floatValueFn) throws Throwable {
			this(name, owner, nextMethod, kind, nextReturnType, objectValueFn, booleanValueFn, intValueFn, longValueFn, floatValueFn, null);
		}

		private IterationCase(
				final String name,
				final Object owner,
				final String nextMethod,
				final Kind kind,
				final Class<?> nextReturnType,
				final ToLongFunction<Object> objectValueFn,
				final BooleanToLongFunction booleanValueFn,
				final IntToLongFunction intValueFn,
				final LongUnaryOperator longValueFn,
				final FloatToLongFunction floatValueFn,
				final DoubleToLongFunction doubleValueFn) throws Throwable {
			this.name = name;
			this.owner = owner;
			this.kind = kind;
			this.objectValueFn = objectValueFn;
			this.booleanValueFn = booleanValueFn;
			this.intValueFn = intValueFn;
			this.longValueFn = longValueFn;
			this.floatValueFn = floatValueFn;
			this.doubleValueFn = doubleValueFn;

			this.borrow = LOOKUP
					.unreflect(owner.getClass().getMethod("poolZeroAllocIterator"))
					.asType(MethodType.methodType(Object.class, Object.class));

			final Object iterator = borrowIterator();
			try {
				final Class<?> iteratorClass = iterator.getClass();
				this.hasNext = LOOKUP.unreflect(iteratorClass.getMethod("hasNext")).asType(MethodType.methodType(boolean.class, Object.class));
				this.close = LOOKUP.unreflect(iteratorClass.getMethod("close")).asType(MethodType.methodType(void.class, Object.class));
				this.next = LOOKUP.unreflect(iteratorClass.getMethod(nextMethod)).asType(MethodType.methodType(nextReturnType, Object.class));
			} finally {
				closeIterator(iterator);
			}
		}

		private Object borrowIterator() throws Throwable {
			return (Object)borrow.invokeExact(owner);
		}

		private void closeIterator(final Object iterator) throws Throwable {
			close.invokeExact(iterator);
		}

		private long iterateOnce() throws Throwable {
			final Object iterator = borrowIterator();
			try {
				long sum = 0;
				while ((boolean)hasNext.invokeExact(iterator)) {
					sum += nextValue(iterator);
				}
				return sum;
			} finally {
				closeIterator(iterator);
			}
		}

		private long nextValue(final Object iterator) throws Throwable {
			switch (kind) {
			case OBJECT:
				return objectValueFn.applyAsLong((Object)next.invokeExact(iterator));
			case BOOLEAN:
				return booleanValueFn.applyAsLong((boolean)next.invokeExact(iterator));
			case BYTE:
				return intValueFn.applyAsLong((byte)next.invokeExact(iterator));
			case SHORT:
				return intValueFn.applyAsLong((short)next.invokeExact(iterator));
			case CHAR:
				return intValueFn.applyAsLong((char)next.invokeExact(iterator));
			case INT:
				return intValueFn.applyAsLong((int)next.invokeExact(iterator));
			case LONG:
				return longValueFn.applyAsLong((long)next.invokeExact(iterator));
			case FLOAT:
				return floatValueFn.applyAsLong((float)next.invokeExact(iterator));
			case DOUBLE:
				return doubleValueFn.applyAsLong((double)next.invokeExact(iterator));
			default:
				throw new AssertionError("Unsupported iteration kind: " + kind);
			}
		}
	}
}

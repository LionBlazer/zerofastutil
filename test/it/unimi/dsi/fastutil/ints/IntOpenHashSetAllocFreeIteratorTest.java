package it.unimi.dsi.fastutil.ints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assume;
import org.junit.Test;

import com.sun.management.ThreadMXBean;

public class IntOpenHashSetAllocFreeIteratorTest {

	@Test
	public void testSimpleUsageExample() {
		final IntOpenHashSet set = new IntOpenHashSet();
		set.add(10);
		set.add(20);

		int sum = 0;
		try (AllocFreeIteratorInt it = set.poolAllocFreeIterator()) {
			while (it.hasNext()) {
				sum += it.nextInt();
			}
		}
		assertEquals(30, sum);
	}

	@Test
	public void testStructuralModificationGuard() {
		final IntOpenHashSet set = new IntOpenHashSet();
		set.add(1);
		set.add(2);
		try (AllocFreeIteratorInt it = set.poolAllocFreeIterator()) {
			set.add(3);
			try {
				it.hasNext();
				fail("Expected IllegalStateException");
			} catch (final IllegalStateException expected) {
				// Expected.
			}
		}
	}

	@Test
	public void testConcurrentBorrowAcrossThreads() throws Exception {
		final IntOpenHashSet set = new IntOpenHashSet();
		for (int i = 0; i < 512; i++) {
			set.add(1000 + i);
		}
		final long expected = iterateAll(set);
		final long[] sums = new long[2];
		final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
		final CountDownLatch opened = new CountDownLatch(2);
		final CountDownLatch go = new CountDownLatch(1);
		final CountDownLatch done = new CountDownLatch(2);

		final Runnable worker0 = new Runnable() {
			@Override
			public void run() {
				try (AllocFreeIteratorInt it = set.poolAllocFreeIterator()) {
					opened.countDown();
					go.await();
					sums[0] = sum(it);
				} catch (final Throwable t) {
					failure.compareAndSet(null, t);
				} finally {
					done.countDown();
				}
			}
		};
		final Runnable worker1 = new Runnable() {
			@Override
			public void run() {
				try (AllocFreeIteratorInt it = set.poolAllocFreeIterator()) {
					opened.countDown();
					go.await();
					sums[1] = sum(it);
				} catch (final Throwable t) {
					failure.compareAndSet(null, t);
				} finally {
					done.countDown();
				}
			}
		};

		new Thread(worker0, "alloc-free-set-worker-0").start();
		new Thread(worker1, "alloc-free-set-worker-1").start();

		assertTrue("Workers did not open iterators in time", opened.await(5, TimeUnit.SECONDS));
		go.countDown();
		assertTrue("Workers did not finish in time", done.await(5, TimeUnit.SECONDS));
		if (failure.get() != null) throw new AssertionError(failure.get());
		assertEquals(expected, sums[0]);
		assertEquals(expected, sums[1]);
	}

	@Test
	public void testZeroAllocationInHotLoop() {
		final ThreadMXBean threadMxBean = allocationThreadMxBean();
		Assume.assumeTrue(threadMxBean != null);

		final IntOpenHashSet set = new IntOpenHashSet(512);
		for (int i = 0; i < 512; i++) {
			set.add(1000 + i);
		}

		long sink = 0;
		for (int i = 0; i < 20000; i++) {
			sink += iterateAll(set);
		}

		final long threadId = Thread.currentThread().getId();
		final long before = threadMxBean.getThreadAllocatedBytes(threadId);
		for (int i = 0; i < 50000; i++) {
			sink += iterateAll(set);
		}
		final long allocated = threadMxBean.getThreadAllocatedBytes(threadId) - before;
		assertEquals("Expected zero allocations in hot iteration path, but got " + allocated + " bytes", 0L, allocated);
		assertEquals(0L, sink & 1L);
	}

	private static long iterateAll(final IntOpenHashSet set) {
		try (AllocFreeIteratorInt it = set.poolAllocFreeIterator()) {
			return sum(it);
		}
	}

	private static long sum(final AllocFreeIteratorInt it) {
		long sum = 0;
		while (it.hasNext()) {
			sum += it.nextInt();
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

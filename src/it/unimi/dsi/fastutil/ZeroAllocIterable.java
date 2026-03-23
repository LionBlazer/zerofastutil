package it.unimi.dsi.fastutil;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;

public interface ZeroAllocIterable<K> {
	ZeroAllocIterator<K> poolZeroAllocIterator();

	static <K> ZeroAllocIterator<K> wrap(final Iterable<? extends K> iterable) {
		return BorrowedZeroAllocIterator.acquire(iterable);
	}

	final class BorrowedZeroAllocIterator<K> implements ZeroAllocIterator<K> {
		private static final ThreadLocal<ArrayDeque<BorrowedZeroAllocIterator<?>>> POOL =
				ThreadLocal.withInitial(ArrayDeque::new);

		private Iterator<? extends K> delegate;
		private boolean inUse;

		private BorrowedZeroAllocIterator() {}

		@SuppressWarnings("unchecked")
		static <K> BorrowedZeroAllocIterator<K> acquire(final Iterable<? extends K> iterable) {
			final ArrayDeque<BorrowedZeroAllocIterator<?>> pool = POOL.get();
			BorrowedZeroAllocIterator<K> iterator = (BorrowedZeroAllocIterator<K>)pool.pollFirst();
			if (iterator == null) iterator = new BorrowedZeroAllocIterator<K>();
			iterator.open(iterable);
			return iterator;
		}

		private void open(final Iterable<? extends K> iterable) {
			if (inUse) throw new IllegalStateException("Iterator already in use.");
			delegate = Objects.requireNonNull(iterable, "iterable").iterator();
			inUse = true;
		}

		private void ensureOpen() {
			if (!inUse) throw new IllegalStateException("Iterator is not in use.");
		}

		@Override
		public boolean hasNext() {
			ensureOpen();
			return delegate.hasNext();
		}

		@Override
		public K next() {
			ensureOpen();
			return delegate.next();
		}

		@Override
		public void remove() {
			ensureOpen();
			delegate.remove();
		}

		@Override
		public void close() {
			if (!inUse) throw new IllegalStateException("Iterator is not in use.");
			inUse = false;
			delegate = null;
			POOL.get().offerFirst(this);
		}
	}
}

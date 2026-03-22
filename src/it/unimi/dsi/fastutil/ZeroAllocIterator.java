package it.unimi.dsi.fastutil;

import java.util.Iterator;

public interface ZeroAllocIterator<K> extends Iterator<K>, Iterable<K>, AutoCloseable {
	@Override
	default Iterator<K> iterator() {
		return this;
	}

	@Override
	void close();
}

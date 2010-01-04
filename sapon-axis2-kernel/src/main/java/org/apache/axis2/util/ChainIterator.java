package org.apache.axis2.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class ChainIterator<E>
	implements Iterator<E>
{
	private final Deque<Iterator<E>> iterators
		= new ArrayDeque<Iterator<E>>();

	private Iterator<E> currIter;

	public ChainIterator(Iterator<E> first, Iterator<E> second) {
		iterators.add(first);
		iterators.add(second);
	}

	public ChainIterator(List<Iterator<E>> iters) {
		iterators.addAll(iters);
	}

	public void addFirst(Iterator<E> iter) {
		iterators.addFirst(currIter);
		currIter = iter;
	}

	public void addLast(Iterator<E> iter) {
		iterators.addLast(iter);
	}

	@Override
	public boolean hasNext() {
		while(currIter == null || !currIter.hasNext()) {
			if(iterators.isEmpty()) {
				return false;
			}
			currIter = iterators.remove();
		}

		return true;
	}

	@Override
	public E next() {
		if(!hasNext()) {
			throw new NoSuchElementException();
		}

		return currIter.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}

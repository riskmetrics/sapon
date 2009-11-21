package org.apache.axis2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A not-thread-safe copy-on-write List implementation, that only makes a copy
 * of the backing list the first time the list is modified.  The copied list
 * is an ArrayList by default; you can extend this class and just override the
 * protected doCopy() method if you need another implementation.
 *
 * @author jfager
 *
 * @param <T>
 */
public class CopyOnWriteOnceList<T> implements List<T> {

	private List<T> backingList;
	private boolean copied = false;

	public CopyOnWriteOnceList(List<T> backingList) {
		this.backingList = backingList;
	}

	private void ensureCopy() {
		if(copied) {
			return;
		}
		backingList = doCopy();
		copied = true;
	}

	protected List<T> doCopy() {
		return new ArrayList<T>(backingList);
	}

	@Override
	public boolean add(T e) {
		ensureCopy();
		return backingList.add(e);
	}

	@Override
	public void add(int index, T element) {
		ensureCopy();
		backingList.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		ensureCopy();
		return backingList.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		ensureCopy();
		return backingList.addAll(index, c);
	}

	@Override
	public void clear() {
		ensureCopy();
		backingList.clear();
	}

	@Override
	public boolean contains(Object o) {
		return backingList.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return backingList.contains(c);
	}

	@Override
	public T get(int index) {
		return backingList.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return backingList.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return backingList.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return backingList.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return backingList.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		return backingList.listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		return backingList.listIterator(index);
	}

	@Override
	public boolean remove(Object o) {
		ensureCopy();
		return backingList.remove(o);
	}

	@Override
	public T remove(int index) {
		ensureCopy();
		return backingList.remove(index);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		ensureCopy();
		return backingList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		ensureCopy();
		return backingList.retainAll(c);
	}

	@Override
	public T set(int index, T element) {
		ensureCopy();
		return backingList.set(index, element);
	}

	@Override
	public int size() {
		return backingList.size();
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return backingList.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return backingList.toArray();
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		return backingList.toArray(a);
	}
}

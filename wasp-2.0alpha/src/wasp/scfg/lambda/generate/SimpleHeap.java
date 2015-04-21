/*
 * Copyright 2006, 2007 Yuk Wah Wong.
 * 
 * This file is part of the WASP distribution.
 *
 * WASP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * WASP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with WASP; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package wasp.scfg.lambda.generate;

import java.lang.reflect.Array;
import java.util.NoSuchElementException;

import wasp.util.Arrays;

/**
 * A more compact version of <code>wasp.util.Heap</code>.
 * 
 * @see wasp.util.Heap
 * @author Dan Klein
 * @author Christopher Manning
 * @author ywwong
 * 
 */
public class SimpleHeap {

	private static int INC = 100;
	
	private Comparable[] objs;
	private int nobjs;

	public SimpleHeap(int capacity) {
		objs = new Comparable[capacity];
		nobjs = 0;
	}

	private SimpleHeap(Comparable[] objs, int nobjs) {
		this.objs = objs;
		this.nobjs = nobjs;
	}

	public Object clone() {
		return new SimpleHeap((Comparable[]) objs.clone(), nobjs);
	}

	// Primitive Heap Operations

	private int parent(int index) {
		return (index - 1) / 2;
	}

	private int leftChild(int index) {
		return index * 2 + 1;
	}

	private int rightChild(int index) {
		return index * 2 + 2;
	}

	/**
	 * On the assumption that leftChild(entry) and rightChild(entry) satisfy the
	 * heap property, make sure that the heap at entry satisfies this property
	 * by possibly percolating the element o downwards. I've replaced the
	 * obvious recursive formulation with an iterative one to gain (marginal)
	 * speed.
	 */
	private void heapify(int index) {
		while (true) {
			int min = index;

			int left = leftChild(index);
			if (left < nobjs && objs[min].compareTo(objs[left]) > 0)
				min = left;

			int right = rightChild(index);
			if (right < nobjs && objs[min].compareTo(objs[right]) > 0)
				min = right;

			if (min == index)
				break;
			else {
				// Swap min and index
				Comparable o = objs[min];
				objs[min] = objs[index];
				objs[index] = o;
				index = min;
			}
		}
	}

	/**
	 * Finds the object with the minimum key, removes it from the heap, and
	 * returns it.
	 * 
	 * @return the object with minimum key
	 */
	public Object extractMin() {
		if (nobjs == 0)
			throw new NoSuchElementException();

		Object minObj = objs[0];
		objs[0] = null;
		--nobjs;
		if (nobjs > 0) {
			objs[0] = objs[nobjs];
			objs[nobjs] = null;
			heapify(0);
		}
		return minObj;
	}

	/**
	 * Finds the object with the minimum key and returns it, without modifying
	 * the heap.
	 * 
	 * @return the object with minimum key
	 */
	public Object min() {
		if (nobjs == 0)
			throw new NoSuchElementException();
		return objs[0];
	}

	public boolean contains(Comparable o) {
		return contains(0, o);
	}
	
	private boolean contains(int index, Comparable o) {
		if (index >= nobjs)
			return false;
		int c = objs[index].compareTo(o);
		if (c > 0)
			return false;
		if (c == 0 && objs[index].equals(o))
			return true;
		return contains(leftChild(index), o) || contains(rightChild(index), o);
	}
	
	/**
	 * Adds an object to the heap.
	 * 
	 * @param o
	 *            a <code>Comparable</code> value
	 */
	public boolean add(Comparable o) {
		if (nobjs == objs.length)
			objs = (Comparable[]) Arrays.resize(objs, objs.length+INC);
		int index = nobjs++;
		int parent = parent(index);
		while (index > 0 && o.compareTo(objs[parent]) < 0) {
			objs[index] = objs[parent];
			index = parent;
			parent = parent(index);
		}
		objs[index] = o;
		return true;
	}

	/**
	 * Checks if the heap is empty.
	 */
	public boolean isEmpty() {
		return nobjs == 0;
	}

	/**
	 * Get the number of elements in the heap.
	 */
	public int size() {
		return nobjs;
	}

	public void clear() {
		for (int i = 0; i < nobjs; ++i)
			objs[i] = null;
		nobjs = 0;
	}

	/**
	 * Returns an array containing all of the elements in this heap in the
	 * sorted order; the runtime type of the returned array is that of the
	 * specified array. If the heap fits in the specified array, it is returned
	 * therein. Otherwise, a new array is allocated with the runtime type of the
	 * specified array and the size of this heap.
	 * 
	 * If the heap fits in the specified array with room to spare (i.e., the
	 * array has more elements than the heap), the element in the array
	 * immediately following the end of the collection is set to
	 * <code>null</code>.
	 * 
	 * This operation is <b>destructive</b>; elements are removed from this
	 * heap before they are stored in the returned array.
	 * 
	 * @param a
	 *            the array into which the elements of the heap are to be
	 *            stored, if it is big enough; otherwise, a new array of the
	 *            same runtime type is allocated for this purpose.
	 * @return an array containing the elements of the heap.
	 * @throws ArrayStoreException
	 *             if the runtime type of a is not a supertype of the runtime
	 *             type of every element in this heap.
	 */
	public Object[] toArray(Object[] a) {
		if (a.length < nobjs)
			a = (Object[]) Array.newInstance(a.getClass().getComponentType(),
					nobjs);
		else if (a.length > nobjs)
			a[nobjs] = null;
		for (int i = 0; !isEmpty();)
			a[i++] = extractMin();
		return a;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for (int i = 0; i < nobjs; ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(objs[i]);
		}
		sb.append(']');
		return sb.toString();
	}

}

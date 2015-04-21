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
package wasp.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringTokenizer;

import wasp.math.Math;

/**
 * Common array operations.
 * 
 * @author ywwong
 *
 */
public class Arrays {

	private Arrays() {}

	public static void addAll(Collection c, Object[] array) {
		for (int i = 0; i < array.length; ++i)
			c.add(array[i]);
	}
	
	public static byte[] append(byte[] array, byte val) {
		return insert(array, array.length, val);
	}
	
	public static int[] append(int[] array, int val) {
		return insert(array, array.length, val);
	}
	
	public static short[] append(short[] array, short val) {
		return insert(array, array.length, val);
	}
	
	public static Object[] append(Object[] array, Object obj) {
		return insert(array, array.length, obj);
	}
	
	public static Object[] append(Class type, Object[] array, Object obj) {
		return insert(type, array, array.length, obj);
	}
	
	public static int argmax(short[] array) {
		int argmax = -1;
		int max = Int.MIN_VALUE;
		for (int i = 0; i < array.length; ++i)
			if (max < array[i]) {
				argmax = i;
				max = array[i];
			}
		return argmax;
	}
	
	public static int binarySearch(int[] array, int val) {
		return java.util.Arrays.binarySearch(array, val);
	}
	
	public static int binarySearch(short[] array, short val) {
		return java.util.Arrays.binarySearch(array, val);
	}
	
	public static int binarySearch(Object[] array, Object obj) {
		return java.util.Arrays.binarySearch(array, obj);
	}
	
	public static int binarySearch(Object[] array, Object obj, Comparator comp) {
		return java.util.Arrays.binarySearch(array, obj, comp);
	}
	
	public static void clear(Object[] array) {
		fill(array, null);
	}
	
	public static int compare(byte[] array1, byte[] array2) {
		for (int i = 0; i < array1.length; ++i)
			if (array1[i] < array2[i])
				return -1;
			else if (array1[i] > array2[i])
				return 1;
		return 0;
	}
	
	public static String concat(String[] tokens) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < tokens.length; ++i) {
			if (i > 0)
				sb.append(' ');
			sb.append(tokens[i]);
		}
		return sb.toString();
	}
	
	public static double[] concat(double[] array1, double[] array2) {
		double[] a = new double[array1.length+array2.length];
		for (int i = 0; i < array1.length; ++i)
			a[i] = array1[i];
		for (int i = 0; i < array2.length; ++i)
			a[i+array1.length] = array2[i];
		return a;
	}
	
	public static int[] concat(int[] array1, int[] array2) {
		int[] a = new int[array1.length+array2.length];
		for (int i = 0; i < array1.length; ++i)
			a[i] = array1[i];
		for (int i = 0; i < array2.length; ++i)
			a[i+array1.length] = array2[i];
		return a;
	}
	
	public static short[] concat(short[] array1, short[] array2) {
		short[] a = new short[array1.length+array2.length];
		for (int i = 0; i < array1.length; ++i)
			a[i] = array1[i];
		for (int i = 0; i < array2.length; ++i)
			a[i+array1.length] = array2[i];
		return a;
	}
	
	public static Object[] concat(Object[] array1, Object[] array2) {
		Class type = array1.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array1.length+array2.length);
		for (int i = 0; i < array1.length; ++i)
			a[i] = array1[i];
		for (int i = 0; i < array2.length; ++i)
			a[i+array1.length] = array2[i];
		return a;
	}
	
	public static Object[] concat(Class type, Object[] array1, Object[] array2) {
		Object[] a = (Object[]) Array.newInstance(type, array1.length+array2.length);
		for (int i = 0; i < array1.length; ++i)
			a[i] = array1[i];
		for (int i = 0; i < array2.length; ++i)
			a[i+array1.length] = array2[i];
		return a;
	}
	
	public static boolean contains(boolean[] array, boolean val) {
		for (int i = 0; i < array.length; ++i)
			if (array[i] == val)
				return true;
		return false;
	}
	
	public static boolean contains(int[] array, int val) {
		for (int i = 0; i < array.length; ++i)
			if (array[i] == val)
				return true;
		return false;
	}
	
	public static boolean contains(short[] array, int length, short val) {
		for (int i = 0; i < length; ++i)
			if (array[i] == val)
				return true;
		return false;
	}
	
	public static boolean contains(Object[] array, Object obj) {
		for (int i = 0; i < array.length; ++i)
			if (array[i] == null) {
				if (obj == null)
					return true;
			} else {
				if (array[i].equals(obj))
					return true;
			}
		return false;
	}
	
	public static boolean containsType(Object[] array, Class type) {
		for (int i = 0; i < array.length; ++i)
			if (array[i] != null && type.isInstance(array[i]))
				return true;
		return false;
	}
	
	/**
	 * Creates and returns a <i>deep</i> copy of the specified array, where copies of the array elements
	 * are made using the <code>Copyable.copy()</code> method.
	 * 
	 * @param array an array of <code>Copyable</code> objects.
	 * @return a deep copy of the <code>array</code> argument.
	 */
	public static Object[] copy(Copyable[] array) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array.length);
		for (int i = 0; i < array.length; ++i)
			a[i] = array[i].copy();
		return a;
	}
	
	public static int count(boolean[] array) {
		int sum = 0;
		for (int i = 0; i < array.length; ++i)
			if (array[i])
				++sum;
		return sum;
	}
	
	public static int countInstances(Object[] array, Class type) {
		return countInstances(array, 0, array.length, type);
	}
	
	public static int countInstances(Object[] array, int from, int to, Class type) {
		int count = 0;
		for (int i = from; i < to; ++i)
			if (type.isInstance(array[i]))
				++count;
		return count;
	}
	
	public static boolean equal(boolean[] array1, boolean[] array2) {
		if (array1 != null && array2 != null && array1.length == array2.length) {
			for (int i = 0; i < array1.length; ++i)
				if (array1[i] != array2[i])
					return false;
			return true;
		}
		return false;
	}
	
	public static boolean equal(byte[] array1, byte[] array2) {
		if (array1 != null && array2 != null && array1.length == array2.length) {
			for (int i = 0; i < array1.length; ++i)
				if (array1[i] != array2[i])
					return false;
			return true;
		}
		return false;
	}
	
	public static boolean equal(double[] array1, double[] array2) {
		if (array1 != null && array2 != null && array1.length == array2.length) {
			for (int i = 0; i < array1.length; ++i)
				if (array1[i] != array2[i])
					return false;
			return true;
		}
		return false;
	}
	
	public static boolean equal(float[] array1, float[] array2) {
		if (array1 != null && array2 != null && array1.length == array2.length) {
			for (int i = 0; i < array1.length; ++i)
				if (array1[i] != array2[i])
					return false;
			return true;
		}
		return false;
	}
	
	public static boolean equal(int[] array1, int[] array2) {
		if (array1 != null && array2 != null && array1.length == array2.length) {
			for (int i = 0; i < array1.length; ++i)
				if (array1[i] != array2[i])
					return false;
			return true;
		}
		return false;
	}
	
	public static boolean equal(short[] array1, short[] array2) {
		if (array1 != null && array2 != null && array1.length == array2.length) {
			for (int i = 0; i < array1.length; ++i)
				if (array1[i] != array2[i])
					return false;
			return true;
		}
		return false;
	}
	
	public static boolean equal(Object[] array1, Object[] array2) {
		if (array1 != null && array2 != null && array1.length == array2.length) {
			for (int i = 0; i < array1.length; ++i)
				if (array1[i] == null) {
					if (array2[i] != null)
						return false;
				} else {
					if (!array1[i].equals(array2[i]))
						return false;
				}
			return true;
		}
		return false;
	}
	
	public static void fill(boolean[] array, int from, int to, boolean val) {
		for (int i = from; i < to; ++i)
			array[i] = val;
	}
	
	public static void fill(double[] array, double val) {
		for (int i = 0; i < array.length; ++i)
			array[i] = val;
	}
	
	public static void fill(int[] array, int val) {
		for (int i = 0; i < array.length; ++i)
			array[i] = val;
	}
	
	public static void fill(short[] array, short val) {
		for (int i = 0; i < array.length; ++i)
			array[i] = val;
	}
	
	public static void fill(Object[] array, Object val) {
		for (int i = 0; i < array.length; ++i)
			array[i] = val;
	}
	
	public static int findInstance(Object[] array, Class type, int nth) {
		for (int i = 0, j = 0; i < array.length; ++i)
			if (type.isInstance(array[i])) {
				if (j == nth)
					return i;
				++j;
			}
		return -1;
	}
	
	public static int findLastInstance(Object[] array, Class type, int nth) {
		for (int i = array.length-1, j = 0; i >= 0; --i)
			if (type.isInstance(array[i])) {
				if (j == nth)
					return i;
				++j;
			}
		return -1;
	}
	
	public static int hashCode(boolean[] array) {
		int hash = 1;
		for (int i = 0; i < array.length; ++i)
			hash = 31*hash + ((array[i]) ? 1231 : 1237);
		return hash;
	}
	
	public static int hashCode(byte[] array) {
		int hash = 1;
		for (int i = 0; i < array.length; ++i)
			hash = 31*hash + array[i];
		return hash;
	}
	
	public static int hashCode(int[] array) {
		int hash = 1;
		for (int i = 0; i < array.length; ++i)
			hash = 31*hash + array[i];
		return hash;
	}
	
	public static int hashCode(short[] array) {
		int hash = 1;
		for (int i = 0; i < array.length; ++i)
			hash = 31*hash + array[i];
		return hash;
	}
	
	public static int hashCode(Object[] array) {
		int hash = 1;
		for (int i = 0; i < array.length; ++i)
			hash = 31*hash + ((array[i]==null) ? 0 : array[i].hashCode());
		return hash;
	}
	
	public static int indexOf(boolean[] array, boolean val, int from, int to) {
		for (int i = from; i < to; ++i)
			if (array[i] == val)
				return i;
		return -1;
	}
	
	public static int indexOf(boolean[] array, boolean val, int from) {
		for (int i = from; i < array.length; ++i)
			if (array[i] == val)
				return i;
		return -1;
	}
	
	public static int indexOf(boolean[] array, boolean val) {
		for (int i = 0; i < array.length; ++i)
			if (array[i] == val)
				return i;
		return -1;
	}
	
	public static int indexOf(int[] array, int val) {
		for (int i = 0; i < array.length; ++i)
			if (array[i] == val)
				return i;
		return -1;
	}
	
	public static int indexOf(short[] array, short val) {
		for (int i = 0; i < array.length; ++i)
			if (array[i] == val)
				return i;
		return -1;
	}
	
	public static int indexOf(Object[] array, Object obj, int from, int to) {
		for (int i = from; i < to; ++i)
			if (obj == null) {
				if (array[i] == null)
					return i;
			} else {
				if (array[i] != null && array[i].equals(obj))
					return i;
			}
		return -1;
	}
	
	public static int indexOf(Object[] array, Object obj) {
		for (int i = 0; i < array.length; ++i)
			if (obj == null) {
				if (array[i] == null)
					return i;
			} else {
				if (array[i] != null && array[i].equals(obj))
					return i;
			}
		return -1;
	}
	
	public static byte[] insert(byte[] array, int index, byte val) {
		byte[] a = new byte[array.length+1];
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		a[index] = val;
		for (int i = index; i < array.length; ++i)
			a[i+1] = array[i];
		return a;
	}
	
	public static double[] insert(double[] array, int index, double val) {
		double[] a = new double[array.length+1];
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		a[index] = val;
		for (int i = index; i < array.length; ++i)
			a[i+1] = array[i];
		return a;
	}
	
	public static int[] insert(int[] array, int index, int val) {
		int[] a = new int[array.length+1];
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		a[index] = val;
		for (int i = index; i < array.length; ++i)
			a[i+1] = array[i];
		return a;
	}
	
	public static short[] insert(short[] array, int index, short val) {
		short[] a = new short[array.length+1];
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		a[index] = val;
		for (int i = index; i < array.length; ++i)
			a[i+1] = array[i];
		return a;
	}
	
	public static Object[] insert(Object[] array, int index, Object obj) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array.length+1);
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		a[index] = obj;
		for (int i = index; i < array.length; ++i)
			a[i+1] = array[i];
		return a;
	}
	
	public static Object[] insert(Class type, Object[] array, int index, Object obj) {
		Object[] a = (Object[]) Array.newInstance(type, array.length+1);
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		a[index] = obj;
		for (int i = index; i < array.length; ++i)
			a[i+1] = array[i];
		return a;
	}
	
	public static Object[] insertAll(Object[] array, int index, Object[] objs) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array.length+objs.length);
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		for (int i = 0; i < objs.length; ++i)
			a[index+i] = objs[i];
		for (int i = index; i < array.length; ++i)
			a[i+objs.length] = array[i];
		return a;
	}
	
	public static Object[] intersect(Object[] array1, Object[] array2) {
		Class type1 = array1.getClass().getComponentType();
		Class type2 = array2.getClass().getComponentType();
		if (!type1.equals(type2))
			return null;
		ArrayList list = new ArrayList();
		for (int i = 0; i < array1.length; ++i)
			for (int j = 0; j < array2.length; ++j)
				if (array1[i].equals(array2[j])) {
					list.add(array1[i]);
					break;
				}
		return list.toArray((Object[]) Array.newInstance(type1, 0));
	}
	
	public static String join(String[] array, int from, int to, String sep) {
		StringBuffer sb = new StringBuffer();
		if (to-from > 0) {
			sb.append(array[from]);
			for (int i = from+1; i < to; ++i) {
				sb.append(sep);
				sb.append(array[i]);
			}
		}
		return sb.toString();
	}
	
	public static int lastIndexOf(boolean[] array, boolean val, int from) {
		for (int i = from; i >= 0; --i)
			if (array[i] == val)
				return i;
		return -1;
	}
	
	public static int lastIndexOf(boolean[] array, boolean val) {
		for (int i = array.length-1; i >= 0; --i)
			if (array[i] == val)
				return i;
		return -1;
	}
	
	private static class RandomIndex implements Comparable {
		public int index;
		private double rand;
		public RandomIndex(int index) {
			this.index = index;
			rand = Math.random();
		}
		public int compareTo(Object o) {
			if (rand < ((RandomIndex) o).rand)
				return -1;
			else if (rand > ((RandomIndex) o).rand)
				return 1;
			else
				return 0;
		}
	}
	public static Object[] randomSort(Object[] array) {
		RandomIndex[] indices = new RandomIndex[array.length];
		for (int i = 0; i < array.length; ++i)
			indices[i] = new RandomIndex(i);
		sort(indices);
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array.length);
		for (int i = 0; i < array.length; ++i)
			a[i] = array[indices[i].index];
		return a;
	}
	
	public static double[] remove(double[] array, int index) {
		double[] a = new double[array.length-1];
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		for (int i = index+1; i < array.length; ++i)
			a[i-1] = array[i];
		return a;
	}
	
	public static int[] remove(int[] array, int index) {
		int[] a = new int[array.length-1];
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		for (int i = index+1; i < array.length; ++i)
			a[i-1] = array[i];
		return a;
	}
	
	public static short[] remove(short[] array, int index) {
		short[] a = new short[array.length-1];
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		for (int i = index+1; i < array.length; ++i)
			a[i-1] = array[i];
		return a;
	}
	
	public static Object[] remove(Object[] array, int index) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array.length-1);
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		for (int i = index+1; i < array.length; ++i)
			a[i-1] = array[i];
		return a;
	}
	
	public static Object[] remove(Object[] array, int from, int to) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array.length-(to-from));
		for (int i = 0; i < from; ++i)
			a[i] = array[i];
		for (int i = to; i < array.length; ++i)
			a[i-(to-from)] = array[i];
		return a;
	}
	
	public static Object[] remove(Object[] array, Object obj) {
		int i = indexOf(array, obj);
		if (i >= 0)
			return remove(array, i);
		else
			return array;
	}
	
	public static void reorder(Object[] array, int[] index) {
		Object[] a = (Object[]) array.clone();
		for (int i = 0; i < array.length; ++i)
			array[i] = a[index[i]];
	}
	
	public static double[] replace(double[] array, int from, int to, double replacement) {
		double[] a = new double[array.length-(to-from)+1];
		for (int i = 0; i < from; ++i)
			a[i] = array[i];
		a[from] = replacement;
		for (int i = to; i < array.length; ++i)
			a[i-(to-from)+1] = array[i];
		return a;
	}
	
	public static Object[] replace(Object[] array, int from, int to, Object replacement) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array.length-(to-from)+1);
		for (int i = 0; i < from; ++i)
			a[i] = array[i];
		a[from] = replacement;
		for (int i = to; i < array.length; ++i)
			a[i-(to-from)+1] = array[i];
		return a;
	}
	
	public static Object[] replace(Object[] array, int index, Object[] replacement) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array.length+replacement.length-1);
		for (int i = 0; i < index; ++i)
			a[i] = array[i];
		for (int i = 0; i < replacement.length; ++i)
			a[i+index] = replacement[i];
		for (int i = index+1; i < array.length; ++i)
			a[i+replacement.length-1] = array[i];
		return a;
	}
	
	public static double[] resize(double[] array, int length) {
		double[] a = new double[length];
		for (int i = 0; i < array.length && i < length; ++i)
			a[i] = array[i];
		return a;
	}
	
	public static double[] resize(double[] array, int length, double fill) {
		double[] a = new double[length];
		for (int i = 0; i < array.length && i < length; ++i)
			a[i] = array[i];
		for (int i = array.length; i < length; ++i)
			a[i] = fill;
		return a;
	}
	
	public static int[] resize(int[] array, int length) {
		int[] a = new int[length];
		for (int i = 0; i < array.length && i < length; ++i)
			a[i] = array[i];
		return a;
	}
	
	public static short[] resize(short[] array, int length) {
		short[] a = new short[length];
		for (int i = 0; i < array.length && i < length; ++i)
			a[i] = array[i];
		return a;
	}
	
	public static Object[] resize(Object[] array, int length) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, length);
		for (int i = 0; i < array.length && i < length; ++i)
			a[i] = array[i];
		return a;
	}

	public static Object[] resize(Class type, Object[] array, int length) {
		Object[] a = (Object[]) Array.newInstance(type, length);
		for (int i = 0; i < array.length && i < length; ++i)
			a[i] = array[i];
		return a;
	}

	public static void reverse(short[] array) {
		for (int i = 0, j = array.length-1; i < array.length/2; ++i, --j) {
			short val = array[i];
			array[i] = array[j];
			array[j] = val;
		}
	}
	
	public static void set(int[] array, int[] val) {
		for (int i = 0; i < array.length; ++i)
			array[i] = val[i];
	}
	
	public static void shiftLeft(Object[] array) {
		for (int i = 0; i < array.length-1; ++i)
			array[i] = array[i+1];
		array[array.length-1] = null;
	}
	
	public static void sort(int[] array, Comparator comp) {
		Int[] a = new Int[array.length];
		for (int i = 0; i < array.length; ++i)
			a[i] = new Int(array[i]);
		java.util.Arrays.sort(a, comp);  // comp must compare Ints
		for (int i = 0; i < array.length; ++i)
			array[i] = a[i].val;
	}
	
	public static void sort(short[] array) {
		java.util.Arrays.sort(array);
	}
	
	public static void sort(Object[] array) {
		java.util.Arrays.sort(array);
	}
	
	public static void sort(Object[] array, Comparator comp) {
		java.util.Arrays.sort(array, comp);
	}
	
	public static double[] subarray(double[] array, int from, int to) {
		double[] a = new double[to-from];
		for (int i = 0; i < to-from; ++i)
			a[i] = array[i+from];
		return a;
	}
	
	public static short[] subarray(short[] array, int from) {
		short[] a = new short[array.length-from];
		for (int i = 0; i < array.length-from; ++i)
			a[i] = array[i+from];
		return a;
	}
	
	public static Object[] subarray(Object[] array, int from, int to) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, to-from);
		for (int i = 0; i < to-from; ++i)
			a[i] = array[i+from];
		return a;
	}
	
	public static Object[] subarray(Object[] array, int from) {
		Class type = array.getClass().getComponentType();
		Object[] a = (Object[]) Array.newInstance(type, array.length-from);
		for (int i = 0; i < array.length-from; ++i)
			a[i] = array[i+from];
		return a;
	}
	
	public static Object[] subseq(Object[] array, Class type) {
		int n = 0;
		for (int i = 0; i < array.length; ++i)
			if (type.isInstance(array[i]))
				++n;
		Object[] a = (Object[]) Array.newInstance(type, n);
		for (int i = 0, j = 0; i < array.length; ++i)
			if (type.isInstance(array[i]))
				a[j++] = array[i];
		return a;
	}
	
	public static short sum(short[] array) {
		short sum = 0;
		for (int i = 0; i < array.length; ++i)
			sum += array[i];
		return sum;
	}
	
	public static int[] toIntArray(Int[] array) {
		int[] a = new int[array.length];
		for (int i = 0; i < array.length; ++i)
			a[i] = ((Int) array[i]).val;
		return a;
	}
	
	public static int[] toIntArray(Collection c) {
		int size = c.size();
		int[] a = new int[size];
		Iterator it = c.iterator();
		for (int i = 0; i < size; ++i)
			a[i] = ((Int) it.next()).val;
		return a;
	}
	
	public static short[] toShortArray(Collection c) {
		int size = c.size();
		short[] a = new short[size];
		Iterator it = c.iterator();
		for (int i = 0; i < size; ++i)
			a[i] = ((Short) it.next()).val;
		return a;
	}
	
	public static String toString(double[] array) {
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for (int i = 0; i < array.length; ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(array[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(float[] array) {
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for (int i = 0; i < array.length; ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(array[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(int[] array) {
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for (int i = 0; i < array.length; ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(array[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(short[] array) {
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for (int i = 0; i < array.length; ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(array[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String toString(Object[] array) {
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		for (int i = 0; i < array.length; ++i) {
			if (i > 0)
				sb.append(", ");
			sb.append(array[i]);
		}
		sb.append(']');
		return sb.toString();
	}
	
	public static String[] tokenize(String str) {
		ArrayList list = new ArrayList();
		StringTokenizer tokenizer = new StringTokenizer(str);
		while (tokenizer.hasMoreTokens())
			list.add(tokenizer.nextToken());
		return (String[]) list.toArray(new String[0]);
	}
	
}

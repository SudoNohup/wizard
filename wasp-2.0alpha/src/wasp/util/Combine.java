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

/**
 * Given an <i>m</i>-sized array, enumerates all possible <i>n-</i>-combinations,
 * i.e.&nbsp;<sub>m</sub>C<sub>n</sub>.
 * 
 * @author ywwong
 *
 */
public class Combine {

	private short[] array;
	private short m, n;
	private short[] idx;
	private short[] p;
	
	public Combine(short[] array, short n) {
		this.array = array;
		m = (short) array.length;
		this.n = n;
		idx = new short[n];
		for (short i = 0; i < n; ++i)
			idx[i] = i;
		p = new short[n];
	}

	public Combine(short m, short n) {
		this(array(m), n);
	}
	
	private static short[] array(short m) {
		short[] a = new short[m];
		for (short i = 0; i < m; ++i)
			a[i] = i;
		return a;
	}
	
	public short[] nextCombine() {
		if (idx == null)
			return null;
		for (short i = 0; i < n; ++i)
			p[i] = array[idx[i]];
		// find next combination
		short i;
		for (i = (short) (n-1); i >= 0; --i) {
			++idx[i];
			if (idx[i] <= m-(n-i))
				break;
		}
		if (i < 0)
			idx = null;
		else
			for (++i; i < n; ++i)
				idx[i] = (short) (idx[i-1]+1);
		return p;
	}
	
}

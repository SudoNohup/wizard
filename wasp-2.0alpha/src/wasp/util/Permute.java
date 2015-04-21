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
 * Given an <i>m</i>-sized array, enumerates all possible permutations of its <i>n-</i>-sized subsets,
 * i.e.&nbsp;it handles the case of <sub>m</sub>P<sub>n</sub>.  This code is more general than
 * <code>FullPermute</code> but is less efficient.
 * 
 * @author ywwong
 *
 */
public class Permute {

	private short[] array;
	private short m, n;
	private Object[][] mask;
	private short[] idx;
	private boolean[] visited;
	private short[] p;
	
	public Permute(short[] array, short n, Object[][] mask) {
		this.array = array;
		m = (short) array.length;
		this.n = n;
		this.mask = mask;
		idx = new short[n];
		visited = new boolean[m];
		fill((short) 0);
		p = new short[n];
	}
	
	public Permute(short m, short n, Object[][] mask) {
		this(array(m), n, mask);
	}
	
	public Permute(short m, short n) {
		this(array(m), n, null);
	}
	
	private static short[] array(short m) {
		short[] a = new short[m];
		for (short i = 0; i < m; ++i)
			a[i] = i;
		return a;
	}
	
	public short[] nextPermute() {
		if (idx == null)
			return null;
		for (short i = 0; i < n; ++i)
			p[i] = array[idx[i]];
		// find next permutation
		short i;
		OUTER: for (i = (short) (n-1); i >= 0; --i) {
			visited[idx[i]] = false;
			for (short j = (short) (idx[i]+1); j < m; ++j)
				if (!visited[j] && (mask == null || mask[i][j] != null)) {
					idx[i] = j;
					visited[j] = true;
					break OUTER;
				}
		}
		if (i < 0) {
			idx = null;
			return p;
		}
		fill((short) (i+1));
		return p;
	}
	
	private void fill(short start) {
		short prev = -1;
		for (short i = start; i < n; ++i) {
			idx[i] = -1;
			for (short j = (short) (prev+1); j < m; ++j)
				if (!visited[j] && (mask == null || mask[i][j] != null)) {
					idx[i] = j;
					visited[j] = true;
					break;
				}
			if (idx[i] < 0) {
				idx = null;
				return;
			}
			prev = idx[i];
		}
	}
	
}

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
 * The disjoint set data structure.
 * 
 * @author ywwong
 *
 */
public class DisjointSets {

	private int[] p;
	private int[] rank;
	
	public DisjointSets(int nobjs) {
		p = new int[nobjs];
		for (int i = 0; i < nobjs; ++i)
			p[i] = i;
		rank = new int[nobjs];
	}
	
	public void union(int i, int j) {
		link(findSet(i), findSet(j));
	}
	
	private void link(int i, int j) {
		if (rank[i] > rank[j])
			p[j] = i;
		else {
			p[i] = j;
			if (rank[i] == rank[j])
				++rank[j];
		}
	}
	
	public int findSet(int i) {
		if (i != p[i])
			p[i] = findSet(p[i]);
		return p[i];
	}
	
	public boolean isSameComponent(int i, int j) {
		return findSet(i) == findSet(j);
	}
	
}

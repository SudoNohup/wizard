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
 * Pairs of comparable objects.
 * 
 * @author ywwong
 *
 */
public class ComparablePair extends Pair implements Comparable {

	public ComparablePair(Comparable first, Comparable second) {
		super(first, second);
	}

	public boolean equals(Object o) {
		return o instanceof ComparablePair && super.equals(o);
	}
	
	public int compareTo(Object o) {
		Comparable t1 = (Comparable) first;
		Comparable t2 = (Comparable) second;
		Comparable o1 = (Comparable) ((ComparablePair) o).first;
		Comparable o2 = (Comparable) ((ComparablePair) o).second;
		if (t1 == null && o1 != null)
			return -1;
		else if (t1 != null && o1 == null)
			return 1;
		else if (t1 != null && o1 != null) {
			int c1 = t1.compareTo(o1);
			if (c1 < 0)
				return -1;
			else if (c1 > 0)
				return 1;
		}
		if (t2 == null && o2 != null)
			return -1;
		else if (t2 != null && o2 == null)
			return 1;
		else if (t2 != null && o2 != null) {
			int c2 = t2.compareTo(o2);
			if (c2 < 0)
				return -1;
			else if (c2 > 0)
				return 1;
		}
		return 0;
	}
	
}

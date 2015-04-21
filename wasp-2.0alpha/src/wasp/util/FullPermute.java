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
 * Enumerates all possible permutations of a given array.  This code only handles the case of
 * <sub>m</sub>P<sub>m</sub>.  It is based on the algorithm presented in
 * <a href="http://www.geocities.com/permute_it/01example.html"
 * target="_new">http://www.geocities.com/permute_it/01example.html</a>.
 * 
 * @author ywwong
 *
 */
public class FullPermute {

	private short[] array;
	private short[] p;
	private short i;

	public FullPermute(short[] array) {
		this.array = array;
		p = new short[array.length+1];
		for (short j = 0; j <= array.length; ++j)
			p[j] = j;
		i = 1;
	}
	
	public FullPermute(short m) {
		this(array(m));
	}
	
	private static short[] array(short m) {
		short[] a = new short[m];
		for (short i = 0; i < m; ++i)
			a[i] = i;
		return a;
	}
	
	public short[] nextPermute() {
		while (i < array.length) {
			--p[i];
			short j = (short) ((i%2) * p[i]);
			short tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
			i = 1;
			while (p[i] == 0) {
				p[i] = i;
				++i;
			}
			return array;
		}
		return null;
	}
	
}
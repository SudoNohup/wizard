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
 * A simplified version of <code>java.lang.Float</code> where the stored value is mutable.
 * 
 * @author ywwong
 * 
 */
public class Float {

	public static final float POSITIVE_INFINITY = java.lang.Float.POSITIVE_INFINITY;
	public static final float NEGATIVE_INFINITY = java.lang.Float.NEGATIVE_INFINITY;
	public static final float NaN = java.lang.Float.NaN;
	
	public float val;

	public Float(float val) {
		this.val = val;
	}

	public Float() {
		this.val = 0;
	}

	public boolean equals(Object o) {
		return o instanceof Float && val == ((Float) o).val;
	}

	public int hashCode() {
		return java.lang.Float.floatToIntBits(val);
	}

	public int compareTo(Object o) {
		if (val < ((Float) o).val)
			return -1;
		else if (val > ((Float) o).val)
			return 1;
		else
			return 0;
	}

	public static boolean isInfinite(float val) {
		return java.lang.Float.isInfinite(val);
	}
	
	public static boolean isNaN(float val) {
		return java.lang.Float.isNaN(val);
	}
	
	public static float parseFloat(String s) {
		return java.lang.Float.parseFloat(s);
	}
	
	public static String toString(float val) {
		return java.lang.Float.toString(val);
	}
	
	public String toString() {
		return java.lang.Float.toString(val);
	}

}

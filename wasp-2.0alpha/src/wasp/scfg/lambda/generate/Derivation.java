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

import wasp.main.generate.LogLinearModel;

/**
 * The derivation data structure used in <i>k</i>-best generation.
 * 
 * @author ywwong
 *
 */
public class Derivation implements Comparable {

	public Hyperarc edge;
	public short ptr1;
	public short ptr2;

	public LogLinearModel.Scores scores;
	public double weight;
	
	public Derivation(Hyperarc edge, short ptr1, short ptr2) {
		this.edge = edge;
		this.ptr1 = ptr1;
		this.ptr2 = ptr2;
	}
	
	public Derivation(Hyperarc edge) {
		this.edge = edge;
		ptr1 = ptr2 = 0;
	}
	
	public boolean equals(Object o) {
		if (o instanceof Derivation) {
			Derivation d = (Derivation) o;
			return edge == d.edge && ptr1 == d.ptr1 && ptr2 == d.ptr2;
		}
		return false;
	}
	
	public int hashCode() {
		int hash = 1;
		hash = 31*hash + edge.hashCode();
		hash = 31*hash + ptr1;
		hash = 31*hash + ptr2;
		return hash;
	}
	
	public int compareTo(Object o) {
		Derivation d = (Derivation) o;
		if (weight > d.weight)
			return -1;
		else if (weight < d.weight)
			return 1;
		else
			return 0;
	}
	
}

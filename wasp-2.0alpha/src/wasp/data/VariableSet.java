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
package wasp.data;

import java.util.BitSet;

/**
 * The class for sets of logical variables.
 * 
 * @author ywwong
 *
 */
public class VariableSet {

	private BitSet set;
	
	public VariableSet() {
		set = null;
	}
	
	private VariableSet(BitSet set) {
		this.set = set;
	}
	
	public boolean equals(Object o) {
		return o instanceof VariableSet
		&& ((set==null) ? (((VariableSet) o).set==null) : set.equals(((VariableSet) o).set));
	}
	
	public boolean isEmpty() {
		return set == null;
	}
	
	public boolean contains(Variable var) {
		return (set==null) ? false : set.get(var.getVarId());
	}
	
	public boolean intersects(VariableSet vars) {
		if (set == null || vars.set == null)
			return false;
		return set.intersects(vars.set);
	}
	
	public void add(Variable var) {
		if (set == null)
			set = new BitSet();
		set.set(var.getVarId());
	}
	
	public VariableSet union(VariableSet vars) {
		if (vars.set == null)
			return this;
		if (set == null)
			return vars;
		VariableSet s = new VariableSet((BitSet) set.clone());
		s.set.or(vars.set);
		return s;
	}
	
	public void remove(Variable var) {
		if (set != null) {
			set.set(var.getVarId(), false);
			if (set.isEmpty())
				set = null;
		}
	}
	
	public String toString() {
		return (set==null) ? "{}" : set.toString();
	}
	
}

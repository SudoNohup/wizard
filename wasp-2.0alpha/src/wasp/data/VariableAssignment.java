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

import wasp.math.Math;
import wasp.util.Arrays;

/**
 * The class for keeping track of mappings between logical variables.  It uses a simple array
 * representation which assumes that all variable names are <b>small</b>, non-negative integers. 
 * 
 * @author ywwong
 *
 */
public class VariableAssignment {

	private Variable[] assign;
	
	public VariableAssignment() {
		assign = null;
	}

	private VariableAssignment(Variable[] assign) {
		this.assign = assign;
	}
	
	public boolean equals(Object o) {
		return o instanceof VariableAssignment
		&& ((assign==null) ? (((VariableAssignment) o).assign==null)
				: Arrays.equal(assign, ((VariableAssignment) o).assign));
	}
	
	public int hashCode() {
		return (assign==null) ? 0 : Arrays.hashCode(assign);
	}
	
	public boolean isEmpty() {
		return assign == null;
	}
	
	public Variable get(Variable slot) {
		return (assign==null || assign.length<=slot.getVarId()) ? null : assign[slot.getVarId()];
	}
	
	/**
	 * Assigns a variable to the given slot.
	 * 
	 * @param slot the slot index.
	 * @param var the variable being assigned.
	 * @return <code>false</code> if another variable has been assigned to the slot; <code>true</code>
	 * otherwise.
	 */
	public boolean put(Variable slot, Variable var) {
		short s = slot.getVarId();
		if (assign == null)
			assign = new Variable[s+1];
		else if (s >= assign.length)
			assign = (Variable[]) Arrays.resize(Variable.class, assign, s+1);
		if (assign[s] != null && !assign[s].equals(var))
			return false;
		assign[s] = var;
		return true;
	}
	
	public VariableAssignment union(VariableAssignment a) {
		// assume the content of the output assignment is not going to change
		if (a.assign == null)
			return this;
		if (assign == null)
			return a;
		Variable[] v = new Variable[Math.max(assign.length, a.assign.length)];
		for (short i = 0; i < assign.length; ++i)
			v[i] = assign[i];
		for (short i = 0; i < a.assign.length; ++i)
			if (a.assign[i] != null) {
				if (v[i] != null && !v[i].equals(a.assign[i]))
					return null;
				v[i] = a.assign[i];
			}
		return new VariableAssignment(v);
	}
	
	public VariableAssignment downwardMap(Nonterminal slot, Nonterminal lhs) {
		VariableAssignment subset = new VariableAssignment();
		for (short i = 0; i < slot.countArgs(); ++i)
			subset.put(lhs.getArg(i), get(slot.getArg(i)));
		return subset;
	}
	
	public VariableAssignment upwardMap(Nonterminal slot, Nonterminal lhs) {
		VariableAssignment subset = new VariableAssignment();
		for (short i = 0; i < lhs.countArgs(); ++i)
			subset.put(slot.getArg(i), get(lhs.getArg(i)));
		return subset;
	}
	
	public VariableSet free(Nonterminal lhs) {
		VariableSet subset = new VariableSet();
		if (assign != null) {
			for (short i = 0; i < assign.length; ++i)
				if (assign[i] != null)
					subset.add(assign[i]);
			for (short i = 0; i < lhs.countArgs(); ++i) {
				short v = lhs.getArg(i).getVarId();
				if (assign != null && v < assign.length && assign[v] != null)
					subset.remove(assign[v]);
			}
		}
		return subset;
	}
	
	public boolean containsSome(VariableSet set) {
		if (assign != null)
			for (short i = 0; i < assign.length; ++i)
				if (assign[i] != null && set.contains(assign[i]))
					return true;
		return false;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		if (assign != null) {
			boolean first = true;
			for (short i = 0; i < assign.length; ++i)
				if (assign[i] != null) {
					if (first)
						first = false;
					else
						sb.append(", ");
					sb.append(i);
					sb.append("->");
					sb.append(assign[i].getVarId());
				}
		}
		sb.append("}");
		return sb.toString();
	}
	
}

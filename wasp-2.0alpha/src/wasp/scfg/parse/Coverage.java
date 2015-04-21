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
package wasp.scfg.parse;

import wasp.data.VariableAssignment;
import wasp.data.VariableSet;
import wasp.util.Arrays;
import wasp.util.BitSet;

/**
 * This class keeps track of MR parse tree nodes that have been covered in a partial derivation.
 *  
 * @author ywwong
 *
 */
public class Coverage {

	private BitSet[] sets;
	private short[] roots;
	private VariableAssignment[] vars;
	private VariableSet[] fvars;
	private int hash;
	
	public Coverage() {
		sets = null;
		roots = null;
		vars = null;
		fvars = null;
		hash = 0;
	}
	
	public Coverage(short length, short root, VariableAssignment vars, VariableSet fvars) {
		sets = new BitSet[1];
		sets[0] = new BitSet(length);
		roots = new short[1];
		roots[0] = root;
		this.vars = new VariableAssignment[1];
		this.vars[0] = vars;
		this.fvars = new VariableSet[1];
		this.fvars[0] = fvars;
		hash = sets[0].hashCode();
	}
	
	public Coverage(short length, short i, short root, VariableAssignment vars, VariableSet fvars) {
		sets = new BitSet[1];
		sets[0] = new BitSet(length);
		sets[0].set(i, true);
		roots = new short[1];
		roots[0] = root;
		this.vars = new VariableAssignment[1];
		this.vars[0] = vars;
		this.fvars = new VariableSet[1];
		this.fvars[0] = fvars;
		hash = sets[0].hashCode();
	}
	
	public boolean equals(Object o) {
		if (o instanceof Coverage) {
			Coverage c = (Coverage) o;
			short s = size();
			if (s != c.size())
				return false;
			if (hash != c.hash)
				return false;
			for (short i = 0; i < s; ++i)
				if (roots[i] != c.roots[i] || !sets[i].equals(c.sets[i]) || !vars[i].equals(c.vars[i])
						|| !fvars[i].equals(c.fvars[i]))
					return false;
			return true;
		}
		return false;
	}
	
	public int hashCode() {
		return hash;
	}
	
	public boolean isEmpty() {
		return sets == null;
	}
	
	public boolean isFull() {
		for (short i = 0; i < size(); ++i)
			if (sets[i].isFull())
				return true;
		return false;
	}
	
	public short size() {
		return (sets==null) ? 0 : (short) sets.length;
	}
	
	public BitSet getSet(short index) {
		return sets[index];
	}
	
	public short getRoot(short index) {
		return roots[index];
	}
	
	public VariableAssignment getVars(short index) {
		return vars[index];
	}
	
	public VariableSet getFreeVars(short index) {
		return fvars[index];
	}
	
	public void setFreeVars(short index, VariableSet fvars) {
		this.fvars[index] = fvars;
	}
	
	public Coverage intersect(Coverage cov) {
		Coverage c = new Coverage();
		for (short i = 0; i < size(); ++i)
			for (short j = 0; j < cov.size(); ++j)
				if (roots[i] == cov.roots[j] && sets[i].equals(cov.sets[j])
						&& vars[i].equals(cov.vars[j]) && fvars[i].equals(cov.fvars[j]))
					c.add(sets[i], roots[i], vars[i], fvars[i]);
		return c;
	}

	public Coverage product(Coverage cov) {
		Coverage c = new Coverage();
		for (short i = 0; i < size(); ++i)
			for (short j = 0; j < cov.size(); ++j) {
				VariableAssignment v = vars[i].union(cov.vars[j]);
				VariableSet fv = fvars[i].union(cov.fvars[j]);
				if (v != null)
					c.add(sets[i].union(cov.sets[j]), roots[i], v, fv);
			}
		return c;
	}
	
	public void add(BitSet set, short root, VariableAssignment vars, VariableSet fvars) {
		if (sets == null) {
			sets = new BitSet[1];
			sets[0] = set;
			roots = new short[1];
			roots[0] = root;
			this.vars = new VariableAssignment[1];
			this.vars[0] = vars;
			this.fvars = new VariableSet[1];
			this.fvars[0] = fvars;
			hash = set.hashCode();
		} else {
			sets = (BitSet[]) Arrays.append(BitSet.class, sets, set);
			roots = Arrays.append(roots, root);
			this.vars = (VariableAssignment[]) Arrays.append(VariableAssignment.class, this.vars, vars);
			this.fvars = (VariableSet[]) Arrays.append(VariableSet.class, this.fvars, fvars);
			hash += set.hashCode();
		}
	}
	
	public void add(short length, short i, short root, VariableAssignment vars, VariableSet fvars) {
		BitSet s = new BitSet(length);
		s.set(i, true);
		add(s, root, vars, fvars);
	}
	
	public void addAll(Coverage cov) {
		if (cov.isEmpty())
			return;
		// only shallow copying is performed
		if (sets == null) {
			sets = (BitSet[]) cov.sets.clone();
			roots = (short[]) cov.roots.clone();
			vars = (VariableAssignment[]) cov.vars.clone();
			fvars = (VariableSet[]) cov.fvars.clone();
			hash = cov.hash;
		} else {
			sets = (BitSet[]) Arrays.concat(BitSet.class, sets, cov.sets);
			roots = Arrays.concat(roots, cov.roots);
			vars = (VariableAssignment[]) Arrays.concat(VariableAssignment.class, vars, cov.vars);
			fvars = (VariableSet[]) Arrays.concat(VariableSet.class, fvars, cov.fvars);
			hash += cov.hash;
		}
	}
	
	public String toString() {
		if (sets == null)
			return "{}";
		else {
			StringBuffer sb = new StringBuffer();
			sb.append("{");
			for (short i = 0; i < sets.length; ++i) {
				if (i > 0)
					sb.append(", ");
				sb.append("(");
				sb.append(sets[i]);
				sb.append(", ");
				sb.append(roots[i]);
				sb.append(", ");
				sb.append(vars[i]);
				sb.append(", ");
				sb.append(fvars[i]);
				sb.append(")");
			}
			sb.append("}");
			return sb.toString();
		}
	}
	
}

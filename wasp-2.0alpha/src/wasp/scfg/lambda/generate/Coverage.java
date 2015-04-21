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

import wasp.data.Nonterminal;
import wasp.data.VariableAssignment;
import wasp.util.Arrays;
import wasp.util.BitSet;

/**
 * This class keeps track of MR parse tree nodes that have been covered in a partial derivation.
 *  
 * @author ywwong
 *
 */
public class Coverage {

	private short[] roots;
	private BitSet[] sets;
	private VariableAssignment[] vars;
	private Nonterminal[][] args;
	private short[][] pos;
	
	public Coverage() {
		roots = null;
		sets = null;
		vars = null;
		args = null;
		pos = null;
	}
	
	public Coverage(short length, short root, VariableAssignment vars) {
		roots = new short[1];
		roots[0] = root;
		sets = new BitSet[1];
		sets[0] = new BitSet(length);
		this.vars = new VariableAssignment[1];
		this.vars[0] = vars;
		args = new Nonterminal[1][0];
		pos = new short[1][0];
	}
	
	public Coverage(short length, short root, short i, VariableAssignment vars) {
		roots = new short[1];
		roots[0] = root;
		sets = new BitSet[1];
		sets[0] = new BitSet(length);
		sets[0].set(i, true);
		this.vars = new VariableAssignment[1];
		this.vars[0] = vars;
		args = new Nonterminal[1][0];
		pos = new short[1][0];
	}
	
	public Coverage(short length, Nonterminal arg, short pos) {
		roots = new short[1];
		roots[0] = pos;
		sets = new BitSet[1];
		sets[0] = new BitSet(length);
		vars = new VariableAssignment[1];
		vars[0] = new VariableAssignment();
		args = new Nonterminal[1][1];
		args[0][0] = arg;
		this.pos = new short[1][1];
		this.pos[0][0] = pos;
	}
	
	public boolean isEmpty() {
		return roots == null;
	}
	
	public short size() {
		return (roots==null) ? 0 : (short) roots.length;
	}
	
	public short getRoot(short index) {
		return roots[index];
	}
	
	public BitSet getSet(short index) {
		return sets[index];
	}
	
	public VariableAssignment getVars(short index) {
		return vars[index];
	}
	
	public Nonterminal[] getArgs(short index) {
		return args[index];
	}
	
	public short[] getPos(short index) {
		return pos[index];
	}
	
	public Coverage product(Coverage cov) {
		Coverage c = new Coverage();
		for (short i = 0; i < size(); ++i)
			for (short j = 0; j < cov.size(); ++j) {
				VariableAssignment vars = this.vars[i].union(cov.vars[j]);
				if (vars == null)
					continue;
				BitSet set = sets[i].union(cov.sets[j]);
				short root = roots[i];
				// merge sort
				short ni = (short) this.args[i].length;
				short nj = (short) cov.args[j].length;
				Nonterminal[] args = new Nonterminal[ni+nj];
				short[] pos = new short[ni+nj];
				for (short k = 0, l = 0, m = 0; m < ni+nj; ++m)
					if (k < ni && (l == nj || this.pos[i][k] >= cov.pos[j][l])) {
						args[m] = this.args[i][k];
						pos[m] = this.pos[i][k];
						++k;
					} else {
						args[m] = cov.args[j][l];
						pos[m] = cov.pos[j][l];
						++l;
					}
				c.add(set, root, vars, args, pos);
			}
		return c;
	}
	
	private void add(BitSet set, short root, VariableAssignment vars, Nonterminal[] args, short[] pos) {
		if (roots == null) {
			roots = new short[1];
			roots[0] = root;
			sets = new BitSet[1];
			sets[0] = set;
			this.vars = new VariableAssignment[1];
			this.vars[0] = vars;
			this.args = new Nonterminal[1][];
			this.args[0] = args;
			this.pos = new short[1][];
			this.pos[0] = pos;
		} else {
			roots = Arrays.append(roots, root);
			sets = (BitSet[]) Arrays.append(sets, set);
			this.vars = (VariableAssignment[]) Arrays.append(this.vars, vars);
			this.args = (Nonterminal[][]) Arrays.append(this.args, args);
			this.pos = (short[][]) Arrays.append(this.pos, pos);
		}
	}
	
	public void addAll(Coverage cov) {
		if (cov.isEmpty())
			return;
		// only shallow copying is performed
		if (roots == null) {
			roots = (short[]) cov.roots.clone();
			sets = (BitSet[]) cov.sets.clone();
			vars = (VariableAssignment[]) cov.vars.clone();
			args = (Nonterminal[][]) cov.args.clone();
			pos = (short[][]) cov.pos.clone();
		} else {
			roots = Arrays.concat(roots, cov.roots);
			sets = (BitSet[]) Arrays.concat(sets, cov.sets);
			vars = (VariableAssignment[]) Arrays.concat(vars, cov.vars);
			args = (Nonterminal[][]) Arrays.concat(args, cov.args);
			pos = (short[][]) Arrays.concat(pos, cov.pos);
		}
	}
	
}

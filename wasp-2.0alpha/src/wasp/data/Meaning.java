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

import wasp.main.Config;
import wasp.mrl.MRLParser;
import wasp.mrl.Production;
import wasp.mrl.ProductionSymbol;
import wasp.util.Arrays;

/**
 * Meaning representations and related data structures.
 * 
 * @author ywwong
 *
 */
public class Meaning {

	/** The string representation of this meaning representation. */
	public String str;
	/** The symbols that make up this meaning representation. */
	public Symbol[] syms;
	/** The parse tree of this meaning representation. */
	public Node parse;
	/** The linearized parse of this meaning representation. */
	public Node[] linear;
	/** The MRL productions used in the linearized parse of this meaning representation. */
	public Production[] lprods;
	/** The index of the parent of each node in the MR parse tree (<code>-1</code> if there is no
	 * parent). */
	public short[] parent;
	/** The index of children of each node in the MR parse tree. */
	public short[][] child;
	/** The index of the last descendant of each node in the MR parse tree. */
	public short[] lastd;
	
	public Meaning(String str) {
		this.str = str;
		syms = Config.getMRLGrammar().tokenize(str);
		parse();
		if (parse != null)
			init();
	}
	
	protected Meaning() {}
	
	private void parse() {
		parse = new MRLParser(Config.getMRLGrammar()).parse(syms);
	}
	
	private void init() {
		linear = parse.getDescends();
		lprods = new Production[linear.length];
		parent = new short[linear.length];
		child = new short[linear.length][];
		lastd = new short[linear.length];
		for (short i = 0; i < linear.length; ++i) {
			lprods[i] = ((ProductionSymbol) linear[i].getSymbol()).getProduction();
			Node p = linear[i].getParent();
			parent[i] = (p==null) ? -1 : (short) Arrays.indexOf(linear, p);
			child[i] = new short[linear[i].countChildren()];
			for (short j = 0; j < child[i].length; ++j)
				child[i][j] = (short) Arrays.indexOf(linear, linear[i].getChild(j));
			Node[] d = linear[i].getDescends();
			lastd[i] = (short) Arrays.indexOf(linear, d[d.length-1]);
		}
	}
	
	/**
	 * Replaces the specified node in the MR parse tree with another.  Note that this operation does not
	 * affect the <code>str</code> field.
	 * 
	 * @param i the index of the node to replace.
	 * @param replacement the replacement node.
	 * @return a copy of this MR with the <code>i</code>-th node replaced.
	 */
	public Meaning replace(short i, Node replacement) {
		Meaning m = new Meaning(str);
		if (i == 0)
			m.parse = replacement;
		else
			m.linear[i].getParent().replaceChild(m.linear[i], replacement);
		m.syms = toSyms(m.parse);
		m.init();
		return m;
	}

	private static Symbol[] toSyms(Node parse) {
		Production prod = ((ProductionSymbol) parse.getSymbol()).getProduction();
		Symbol[] syms = (Symbol[]) Arrays.copy(prod.getRhs());
		short nc = parse.countChildren();
		for (short i = (short) (nc-1); i >= 0; --i)
			syms = apply(syms, i, toSyms(parse.getChild(i)));
		return syms;
	}
	
	private static Symbol[] apply(Symbol[] syms, short index, Symbol[] replacement) {
		short idx = 0;
		for (short i = 0; i < syms.length; ++i)
			if (syms[i] instanceof Nonterminal) {
				if (idx == index)
					return (Symbol[]) Arrays.replace(syms, i, replacement);
				++idx;
			}
		return syms;
	}
	
}

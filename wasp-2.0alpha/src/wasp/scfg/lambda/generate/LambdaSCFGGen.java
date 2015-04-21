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

import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.data.Node;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.main.generate.LogLinearGen;
import wasp.nl.NL;
import wasp.scfg.Rule;
import wasp.scfg.RuleSymbol;
import wasp.util.Arrays;

/**
 * Parse trees produced by the lambda-SCFG-based chart generator.
 * 
 * @author ywwong
 *
 */
public class LambdaSCFGGen extends LogLinearGen {

	private static Logger logger = Logger.getLogger(LambdaSCFGGen.class.getName());
	static {
		logger.setLevel(Level.FINEST);
	}
	
	private Derivation d;
	
	public LambdaSCFGGen(Derivation d) {
		super(d.weight, d.scores);
		this.d = d.edge.tail1.D[d.ptr1];
	}
	
	public Node toTree() {
		return toTree(d);
	}
	
	private Node toTree(Derivation d) {
		Rule rule = d.edge.head.rule;
		Node n = new Node(new RuleSymbol(rule));
		Node[] c = new Node[rule.countArgs()];
		while (d.edge.arity() == 2) {
			Derivation back = d.edge.tail1.D[d.ptr1];
			Derivation comp = d.edge.tail2.D[d.ptr2];
			short index = back.edge.head.args[back.edge.head.dot].getIndex();
			c[index-1] = toTree(comp);
			d = back;
		}
		for (short i = 0; i < c.length; ++i)
			n.addChild(c[i]);
		return n;
	}

	/**
	 * Returns the NL sentence being generated.
	 */
	public String toStr() {
		if (str != null)
			return str;
		return new NL().combine(toTermArray(toSyms(d)));
	}
	
	private Symbol[] toSyms(Derivation d) {
		Rule rule = d.edge.head.rule;
		Symbol[] syms = (Symbol[]) Arrays.copy(rule.getE());
		while (d.edge.arity() == 2) {
			Derivation back = d.edge.tail1.D[d.ptr1];
			Derivation comp = d.edge.tail2.D[d.ptr2];
			short index = back.edge.head.args[back.edge.head.dot].getIndex();
			short i = findIndex(syms, index);
			syms = (Symbol[]) Arrays.replace(syms, i, toSyms(comp));
			d = back;
		}
		return syms;
	}
	
	private short findIndex(Symbol[] syms, short index) {
		for (short i = 0; i < syms.length; ++i)
			if (syms[i].getIndex() == index)
				return i;
		return -1;
	}
	
	private Terminal[] toTermArray(Symbol[] array) {
		Terminal[] a = new Terminal[array.length];
		for (short i = 0; i < array.length; ++i)
			a[i] = (Terminal) array[i];
		return a;
	}
	
	public Terminal[] toTerms() {
		return toTermArray(toSyms(d));
	}
	
}

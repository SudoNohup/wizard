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

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.data.Anaphor;
import wasp.data.Node;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Variable;
import wasp.data.VariableAssignment;
import wasp.main.Config;
import wasp.main.Parse;
import wasp.mrl.MRLGrammar;
import wasp.mrl.MRLParser;
import wasp.scfg.Rule;
import wasp.scfg.RuleSymbol;
import wasp.util.Arrays;
import wasp.util.Pair;
import wasp.util.Short;

/**
 * Parse trees produced by the SCFG- or lambda-SCFG-based semantic parser.
 * 
 * @author ywwong
 *
 */
public class SCFGParse extends Parse {

	private static Logger logger = Logger.getLogger(SCFGParse.class.getName());
	static {
		logger.setLevel(Level.FINEST);
	}
	
	public Item item;
	
	public SCFGParse(Item item) {
		super(item.inner);
		this.item = item;
	}
	
	public Node toTree() {
		return (item==null) ? null : toTree(item);
	}
	
	private Node toTree(Item item) {
		Node n = new Node(new RuleSymbol(item.rule));
		Node[] c = new Node[item.rule.countArgs()];
		while (item != null) {
			if (item.getBackComplete(0) != null) {
				short index = item.rule.getE(item.getBack(0).dot).getIndex();
				c[index-1] = toTree(item.getBackComplete(0));
			}
			item = item.getBack(0);
		}
		for (short i = 0; i < c.length; ++i)
			n.addChild(c[i]);
		return n;
	}

	/**
	 * Returns the meaning representation given by this parse.
	 */
	public String toStr() {
		if (str != null)
			return str;
		if (item == null)
			return null;
		// resolve anaphora
		HashMap items = new HashMap();
		addItems(item, items);
		Node tree = toResolvedTree(item, items);
		if (tree == null)
			return null;
		// get MR yield
		Symbol[] syms = toSyms(tree, new VariableAssignment(), new Short(1));
		// combine AC operators
		MRLGrammar gram = Config.getMRLGrammar();
		MRLParser parser = new MRLParser(gram);
		tree = parser.parse(syms);
		tree = parser.compact(tree);
		syms = parser.yield(tree);
		return gram.combine(syms);
	}
	
	private void addItems(Item item, HashMap items) {
		items.put(key(item.start, item.rule.getLhs()), item);
		while (item != null) {
			if (item.getBackComplete(0) != null)
				addItems(item.getBackComplete(0), items);
			item = item.getBack(0);
		}
	}
	
	private Pair key(short start, Nonterminal type) {
		return new Pair(new Short(start), type);
	}
	
	private Node toResolvedTree(Item item, HashMap items) {
		if (item.rule.getProduction().isAnaphor()) {
			Item a;
			Nonterminal type = ((Anaphor) item.rule.getF((short) 0)).getType();
			for (short i = (short) (item.start-1); i >= 0; --i)
				if ((a = (Item) items.get(key(i, type))) != null)
					return toResolvedTree(a, items);
			return null;
		}
		Node n = new Node(new RuleSymbol(item.rule));
		Node[] c = new Node[item.rule.countArgs()];
		while (item != null) {
			if (item.getBackComplete(0) != null) {
				short index = item.rule.getE(item.getBack(0).dot).getIndex();
				c[index-1] = toResolvedTree(item.getBackComplete(0), items);
			}
			item = item.getBack(0);
		}
		for (short i = 0; i < c.length; ++i)
			n.addChild(c[i]);
		return n;
	}

	private Symbol[] toSyms(Node node, VariableAssignment vars, Short nextVar) {
		Rule rule = ((RuleSymbol) node.getSymbol()).getRule();
		Symbol[] syms = (Symbol[]) Arrays.copy(rule.getF());
		for (short i = 0; i < syms.length; ++i)
			if (syms[i] instanceof Variable) {
				Variable v1 = (Variable) syms[i];
				Variable v2 = vars.get(v1);
				if (v2 == null) {
					v2 = new Variable(nextVar.val++);
					vars.put(v1, v2);
				}
				syms[i] = v2;
			} else if (syms[i] instanceof Nonterminal) {
				Nonterminal n = (Nonterminal) syms[i];
				for (short j = 0; j < n.countArgs(); ++j) {
					Variable v1 = n.getArg(j);
					Variable v2 = vars.get(v1);
					if (v2 == null) {
						v2 = new Variable(nextVar.val++);
						vars.put(v1, v2);
					}
				}
			}
		for (short i = 0; i < node.countChildren(); ++i) {
			short j = findIndex(syms, (short) (i+1));
			Nonterminal n1 = (Nonterminal) syms[j];
			Nonterminal n2 = ((RuleSymbol) node.getChild(i).getSymbol()).getRule().getLhs();
			VariableAssignment subset = vars.downwardMap(n1, n2);
			syms = (Symbol[]) Arrays.replace(syms, j, toSyms(node.getChild(i), subset, nextVar));
		}
		return syms;
	}
	
	private short findIndex(Symbol[] syms, short index) {
		for (short i = 0; i < syms.length; ++i)
			if (syms[i].getIndex() == index)
				return i;
		return -1;
	}
	
}

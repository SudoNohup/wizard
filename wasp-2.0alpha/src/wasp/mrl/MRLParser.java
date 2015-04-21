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
package wasp.mrl;

import java.util.ArrayList;
import java.util.Iterator;

import wasp.data.Node;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.util.Arrays;

/**
 * A simple Earley chart parser for MRLs.
 * 
 * @author ywwong
 *
 */
public class MRLParser {

	private MRLGrammar gram;
	
	/**
	 * Creates a parser based on the specified MRL grammar.
	 * 
	 * @param gram the MRL grammar.
	 */
	public MRLParser(MRLGrammar gram) {
		this.gram = gram;
	}
	
	public Node parse(Symbol[] F) {
		Chart c = new Chart(F);
		c.addItem(new Item(new Production(gram.getStart()), (short) 0));
		for (short i = 0; i <= c.maxPos; ++i) {
			if (i > 0)
				complete(F, c, i);
			if (i < c.maxPos)
				predictAndScan(F, c, i);
		}
		Item parse = findParse(c);
		return (parse==null) ? null : toTree(parse);
	}
	
	private void complete(Symbol[] F, Chart c, short current) {
		while (!c.comps[current].isEmpty()) {
			Item comp = (Item) c.comps[current].extractMin();
			if (comp.prod.isDummy())
				continue;
			ArrayList items = (ArrayList) c.toComps[comp.start].get(comp.prod.getLhs().getId());
			if (items == null)
				continue;
			for (Iterator it = items.iterator(); it.hasNext();) {
				Item item = (Item) it.next();
				if (item.allowsRepeat()) {
					/*
					System.err.println(item.prod+" "+item.dot+" "+item.start+" "+item.current);
					Item i1 = new Item(item, comp, true);
					System.err.println(" -> "+i1.dot+" "+i1.start+" "+i1.current);
					Item i2 = new Item(item, comp, false);
					System.err.println(" -> "+i2.dot+" "+i2.start+" "+i2.current);
					c.addItem(i1);
					c.addItem(i2);
					*/
					c.addItem(new Item(item, comp, true));
					c.addItem(new Item(item, comp, false));
				} else {
					/*
					System.err.println(item.prod+" "+item.dot+" "+item.start+" "+item.current);
					Item i1 = new Item(item, comp);
					System.err.println(" -> "+i1.dot+" "+i1.start+" "+i1.current);
					c.addItem(i1);
					*/
					c.addItem(new Item(item, comp));
				}
			}
		}
	}
	
	private void predictAndScan(Symbol[] F, Chart c, short current) {
		ArrayList set = c.sets[current];
		for (int i = 0 ; i < set.size(); ++i) {
			Item item = (Item) set.get(i);
			if (item.dot == item.prod.length())
				continue;
			Symbol sym = item.prod.getRhs(item.dot);
			if (sym instanceof Nonterminal) {
				// predict
				for (int j = 0; j < gram.countNonterms(); ++j)
					if (gram.isLeftCorner(sym.getId(), j) && !c.isPredicted(current, j)) {
						c.predict(current, j);
						Production[] prods = gram.getProductions(j);
						for (int k = 0; k < prods.length; ++k)
							if (prods[k].isOrig() && prods[k].possibleMatch(F[current])) {
								//System.err.println(current+": "+prods[k]);
								c.addItem(new Item(prods[k], current));
							}
					}
			} else {
				// scan
				if (sym.matches(F[current])) {
					/*
					System.err.println(item.prod+" "+item.dot+" "+item.start+" "+item.current);
					Item i1 = new Item(item, F[current]);
					System.err.println(" -> "+i1.dot+" "+i1.start+" "+i1.current);
					c.addItem(i1);
					*/
					c.addItem(new Item(item, F[current]));
				}
			}
		}
	}
	
	private Item findParse(Chart c) {
		Nonterminal start = gram.getStart();
		for (Iterator it = c.sets[c.maxPos].iterator(); it.hasNext();) {
			Item item = (Item) it.next();
			if (item.start == 0 && item.prod.getLhs().equals(start))
				return item;
		}
		return null;
	}

	private Node toTree(Item item) {
		Node n = new Node(new ProductionSymbol(item.prod));
		while (item != null) {
			if (item.backComp != null)
				n.addChildToFront(toTree(item.backComp));
			item = item.back;
		}
		return n;
	}
	
	/**
	 * Returns a version of the given MR parse tree with all redundant associative-commutative (AC)
	 * operators removed.
	 * 
	 * @param node an MR parse tree.
	 * @return the given MR parse tree with all redundant AC operators removed.
	 */
	public Node compact(Node node) {
		Production prod = ((ProductionSymbol) node.getSymbol()).getProduction();
		Node n = node.shallowCopy();
		for (short i = 0; i < node.countChildren(); ++i) {
			Node c = compact(node.getChild(i));
			if (prod.isAC() && prod.equals(((ProductionSymbol) c.getSymbol()).getProduction()))
				for (short j = 0; j < c.countChildren(); ++j)
					n.addChild(c.getChild(j));
			else
				n.addChild(c);
		}
		return n;
	}
	
	/**
	 * Returns the yield of the given MR parse tree as an array of symbols.  This is the inverse of the
	 * <code>parse</code> method.
	 * 
	 * @param node an MR parse tree.
	 * @return the yield of the given MR parse tree as an array of symbols.
	 */
	public Symbol[] yield(Node node) {
		Production prod = ((ProductionSymbol) node.getSymbol()).getProduction();
		Symbol[] syms = prod.repeatRhs(node.countChildren());
		short i = node.countChildren();
		for (short j = (short) (syms.length-1); j >= 0; --j)
			if (syms[j] instanceof Nonterminal)
				syms = (Symbol[]) Arrays.replace(syms, j, yield(node.getChild(--i)));
		return syms;
	}
	
}

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
package wasp.scfg.generate;

import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.data.Node;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.main.generate.LogLinearGen;
import wasp.nl.NL;
import wasp.scfg.RuleSymbol;
import wasp.util.Arrays;

/**
 * Parse trees produced by the SCFG-based chart generator.
 * 
 * @author ywwong
 *
 */
public class SCFGGen extends LogLinearGen {

	private static Logger logger = Logger.getLogger(SCFGGen.class.getName());
	static {
		logger.setLevel(Level.FINEST);
	}
	
	public Item item;
	
	public SCFGGen(Item item) {
		super(item.inner, item.scores);
		this.item = item;
	}
	
	public Node toTree() {
		return (item==null) ? null : toTree(item);
	}
	
	private Node toTree(Item item) {
		Node n = new Node(new RuleSymbol(item.rule));
		Node[] c = new Node[item.rule.countArgs()];
		while (item != null) {
			if (item.backComp instanceof Item) {
				short index = item.rule.getF(item.back.dotF).getIndex();
				c[index-1] = toTree((Item) item.backComp);
			}
			item = item.back;
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
		if (item == null)
			return null;
		return new NL().combine(toTermArray(toSyms(item)));
	}
	
	private Symbol[] toSyms(Item item) {
		Symbol[] syms = (Symbol[]) Arrays.copy(item.rule.getE());
		while (item.dotE > 0) {
			if (item.backComp instanceof GapItem)
				syms = (Symbol[]) Arrays.insertAll(syms, item.dotE, toSyms((GapItem) item.backComp));
			item = item.back;
		}
		while (item != null) {
			if (item.backComp instanceof Item) {
				short index = item.rule.getF(item.back.dotF).getIndex();
				short i = findIndex(syms, index);
				syms = (Symbol[]) Arrays.replace(syms, i, toSyms((Item) item.backComp));
			}
			item = item.back;
		}
		return syms;
	}
	
	private Symbol[] toSyms(GapItem item) {
		Symbol[] syms = new Symbol[item.dot];
		for (short i = (short) (item.dot-1); i >= 0; --i) {
			item = item.back;
			syms[i] = item.backWord;
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
		if (item == null)
			return null;
		return toTermArray(toSyms(item));
	}
	
}

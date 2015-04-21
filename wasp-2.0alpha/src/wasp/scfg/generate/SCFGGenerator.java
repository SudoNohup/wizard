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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import wasp.data.Meaning;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.main.Config;
import wasp.main.Generator;
import wasp.main.generate.LogLinearModel;
import wasp.math.Math;
import wasp.nl.BasicGapModel;
import wasp.nl.NgramModel;
import wasp.scfg.SCFG;
import wasp.scfg.Rule;
import wasp.scfg.SCFGModel;
import wasp.scfg.parse.SCFGParse;
import wasp.scfg.parse.SCFGParser;
import wasp.util.Int;
import wasp.util.SortIterator;

/**
 * The SCFG-based chart generator.
 * 
 * @author ywwong
 *
 */
public class SCFGGenerator extends Generator {

	private SCFG gram;
	private NgramModel lm;
	private LogLinearModel llm;
	private int kbest;
	
	private boolean tmNorm;
	private int tmKBest;
	
	private GapGenerator gg;
	private SCFGParser parser;
	
	/**
	 * Creates an SCFG generator based on the specified SCFG and n-gram language model.
	 * 
	 * @param tm the SCFG translation model.
	 * @param lm the n-gram language model for the target NL.
	 * @param llm the log-linear generation model.
	 * @param kbest the number of top-scoring generated sentences to return.
	 */
	public SCFGGenerator(SCFGModel tm, NgramModel lm, LogLinearModel llm, int kbest) {
		short N = Short.parseShort(Config.get(Config.NGRAM_N));
		Item.setN(N);
		GapItem.setN(N);
		gram = tm.gram;
		this.lm = lm;
		this.llm = llm;
		this.kbest = kbest;
		String prob = Config.get(Config.TRANSLATION_PROB_MODEL);
		tmNorm = prob.equals("maxent") || prob.equals("pscfg");
		tmKBest = Int.parseInt(Config.get(Config.TRANSLATION_KBEST));
		tmKBest = (tmNorm) ? Math.max(tmKBest, kbest) : kbest;
		gg = new GapGenerator((BasicGapModel) tm.gm, lm, llm);
		parser = new SCFGParser(tm);
	}
	
	public SCFGGenerator(SCFGModel tm, NgramModel lm, LogLinearModel llm) {
		this(tm, lm, llm, Config.getKBest());
	}
	
	public boolean batch() {
		return false;
	}
	
	public Iterator generate(Meaning F) {
		Chart c = new Chart(gram, F.syms, tmKBest);
		Item item = new Item(new Rule(gram.getStart()), (short) 0, lm, llm);
		c.addItem(item);
		for (short i = 0; i <= c.maxPos; ++i) {
			if (i > 0)
				complete(F.syms, c, i);
			if (i < c.maxPos)
				predictAndScan(F.syms, c, i);
		}
		// extra reranking step for normalizing translation probability
		if (tmNorm)
			for (Iterator it = new GenIterator(c); it.hasNext();) {
				SCFGGen gen = (SCFGGen) it.next();
				Terminal[] E = gen.toTerms();
				double z_E = Double.NEGATIVE_INFINITY;
				for (Iterator jt = parser.parse(E); jt.hasNext();) {
					SCFGParse parse = (SCFGParse) jt.next();
					z_E = Math.logAdd(z_E, parse.score);
				}
				gen.item.scores.tm -= z_E;
				gen.item.inner -= llm.wTM*z_E;
			}
		return new SortIterator(new GenIterator(c), kbest);
	}
	
	private void complete(Symbol[] F, Chart c, short current) {
		while (!c.comps[current].isEmpty()) {
			Item comp = (Item) c.comps[current].extractMin();
			if (comp.rule.isDummy())
				continue;
			ArrayList items = c.toComps[comp.start][comp.rule.getLhs().getId()];
			if (items == null)
				continue;
			for (Iterator it = items.iterator(); it.hasNext();) {
				Item item = (Item) it.next();
				addItem(c, new Item(item, comp, lm, llm));
			}
		}
	}
	
	private void addItem(Chart c, Item item) {
		if (!item.isCompleteF())
			c.addItem(item);
		else {
			// fill in the word gaps only after rewriting all non-terminals in the rule
			Item[] items = fillGaps(item);
			for (int i = 0; i < items.length; ++i)
				c.addItem(items[i]);
		}
	}
	
	/**
	 * Returns a list of complete items obtained by filling in all word gaps in the given item.
	 * 
	 * @param item the item to begin with, with none of its word gaps filled.
	 * @return a list of complete items obtained by filling in all word gaps in the given item.
	 */
	private Item[] fillGaps(Item item) {
		Item[] array = new Item[1];
		array[0] = item;
		for (short i = 0; i < item.rule.lengthE(); ++i) {
			short gap = item.rule.getGap((short) (i+1));
			if (gap == 0)
				for (int j = 0; j < array.length; ++j)
					array[j] = new Item(array[j]);
			else {
				Symbol prev = item.rule.getE(i);
				Symbol next = item.rule.getE((short) (i+1));
				ArrayList list = new ArrayList();
				for (int j = 0; j < array.length; ++j) {
					GapItem[] comps = gg.generate(array[j].context, gap, prev, next);
					for (int k = 0; k < comps.length; ++k)
						list.add(new Item(array[j], comps[k]));
				}
				array = (Item[]) list.toArray(new Item[0]);
			}
		}
		return array;
	}
	
	private void predictAndScan(Symbol[] F, Chart c, short current) {
		ArrayList set = c.sets[current];
		for (int i = 0; i < set.size(); ++i) {
			Item item = (Item) set.get(i);
			if (item.isCompleteF())
				continue;
			Symbol sym = item.rule.getF(item.dotF);
			if (sym instanceof Nonterminal) {
				// predict
				for (int j = 0; j < gram.countNonterms(); ++j)
					if (gram.isLeftCornerForF(sym.getId(), j) && !c.isPredicted(current, j)) {
						c.predict(current, j);
						Rule[] rules = gram.getRules(j);
						for (int k = 0; k < rules.length; ++k)
							if (rules[k].isActive() && rules[k].possibleMatchF(F[current]))
								c.addItem(new Item(rules[k], current, lm, llm));
					}
			} else {
				// scan
				if (sym.matches(F[current]))
					addItem(c, new Item(item, F[current]));
			}
		}
	}
	
	private class GenIterator implements Iterator {
		private Chart c;
		private Iterator it;
		private Item next;
		public GenIterator(Chart c) {
			this.c = c;
			it = c.sets[c.maxPos].iterator();
			findNext();
		}
		private void findNext() {
			next = null;
			int start = gram.getStart().getId();
			while (it.hasNext()) {
				Item item = (Item) it.next();
				if (item.start == 0 && item.isCompleteF() && item.isCompleteE()
						&& item.rule.getLhsId() == start) {
					next = item;
					if (c.needAdjust)
						next.adjustInner(lm, llm);
					break;
				}
			}
			c.needAdjust = false;
		}
		public boolean hasNext() {
			return next != null;
		}
		public Object next() {
			if (this.next == null)
				throw new NoSuchElementException();
			Item next = this.next;
			findNext();
			return new SCFGGen(next);
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
}

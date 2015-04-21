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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.data.Meaning;
import wasp.data.Node;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.data.VariableAssignment;
import wasp.data.VariableSet;
import wasp.main.Config;
import wasp.main.Parser;
import wasp.math.Math;
import wasp.mrl.Denotation;
import wasp.mrl.Production;
import wasp.mrl.ProductionSymbol;
import wasp.scfg.SCFG;
import wasp.scfg.Rule;
import wasp.scfg.SCFGModel;
import wasp.scfg.parse.features.ParseFeatures;
import wasp.util.Arrays;
import wasp.util.BitSet;
import wasp.util.Heap;
import wasp.util.Permute;
import wasp.util.SortIterator;

/**
 * An Earley chart parser based on synchronous context-free grammars (SCFGs) or lambda-SCFGs.
 * 
 * @author ywwong
 *
 */
public class SCFGParser extends Parser {
	
	private static final Logger logger = Logger.getLogger(SCFGParser.class.getName());
	static {
		logger.setLevel(Level.INFO);
	}

	private SCFGModel model;
	private int kbest;
	private boolean checkCov;
	
	/** The chart currently in use.  Each call to the <code>parse</code> method creates a new chart
	 * based on the input sentence.  This chart is re-used by the outside algorithm during parameter
	 * estimation. */
	private Chart c;
	/** The input sentence currently being considered. */
	private Terminal[] E;
	private VariableAssignment vaEmpty;
	private VariableSet vsEmpty;
	private Coverage cEmpty;
	private Coverage cDummy;
	// Symbol -> Coverage
	private HashMap cWilds;
	// Production -> Coverage
	private HashMap cProds;
	
	/**
	 * Creates a parser based on the specified SCFG for parameter estimation.
	 * 
	 * @param model an SCFG model.
	 */
	public SCFGParser(SCFGModel model, boolean bogus) {
		this.model = model;
		kbest = 0;
		checkCov = false;
		c = null;
		E = null;
	}
	
	/**
	 * Creates a parser based on the specified SCFG for Viterbi approximation.
	 * 
	 * @param model an SCFG model.
	 * @param kbest the maximum number of top-scoring theories to keep for each cell.
	 */
	public SCFGParser(SCFGModel model, int kbest) {
		this.model = model;
		this.kbest = kbest;
		checkCov = true;
		c = null;
		E = null;
	}
	
	/**
	 * Creates an SCFG parser based on the specified SCFG translation model.
	 */
	public SCFGParser(SCFGModel model) {
		this.model = model;
		kbest = Config.getKBest();
		checkCov = true;
		c = null;
		E = null;
	}
	
	public boolean batch() {
		return false;
	}
	
	public Iterator parse(Terminal[] E, Meaning F) {
		this.E = E;
		if (F != null)  // training
			initc(F);
		c = new Chart(model.gram, this.E, kbest, checkCov);
		Item item = new Item(new Rule(model.gram.getStart()), (short) 0);
		item.inner = 0;
		if (ParseFeatures.USE_VAR_TYPES)
			item.varTypes = item.rule.getVarTypes();
		if (F != null)  // training
			item.cov = cDummy;  // dummy rule
		c.addItem(item);
		for (short i = 0; i <= c.maxPos; ++i) {
			if (i > 0)
				complete(this.E, F, c, i);
			if (i < c.maxPos)
				predictAndScan(this.E, F, c, i);
		}
		Iterator parseIt = new ParseIterator(model.gram, c);
		return (kbest==0) ? parseIt : new SortIterator(parseIt, kbest);
	}
	
	public Iterator parse(Terminal[] E) {
		return parse(E, null);
	}
	
	private void initc(Meaning F) {
		short length = (short) F.linear.length;
		vaEmpty = new VariableAssignment();
		vsEmpty = new VariableSet();
		cEmpty = new Coverage();
		cDummy = new Coverage(length, (short) 0, vaEmpty, vsEmpty);
		cWilds = new HashMap();
		Symbol[] wilds = new Symbol[length];
		for (short i = 0; i < length; ++i)
			if (F.lprods[i].isWildcardMatch())
				wilds[i] = F.lprods[i].getRhs((short) 0);
		boolean[] checked = new boolean[length];
		for (short i = 0; i < length; ++i)
			if (!checked[i] && wilds[i] != null) {
				checked[i] = true;
				Coverage cov = new Coverage(length, i, i, vaEmpty, vsEmpty);
				for (short j = (short) (i+1); j < length; ++j)
					if (wilds[j] != null && wilds[j].equals(wilds[i])) {
						checked[j] = true;
						cov.add(length, j, j, vaEmpty, vsEmpty);
					}
				cWilds.put(wilds[i], cov);
			}
		cProds = new HashMap();
		Rule[] rules = model.gram.getRules();
		for (int i = 0; i < rules.length; ++i) {
			if (!rules[i].isActive())
				continue;
			Production prod = rules[i].getProduction();
			if (cProds.containsKey(prod))
				continue;
			Coverage cov = new Coverage();
			for (short j = 0; j < length; ++j) {
				Coverage c = match(prod.getParse(), F, j);
				if (c != null)
					cov.addAll(c);
			}
			if (cov.isEmpty())
				continue;
			// find free variables
			for (short j = 0; j < cov.size(); ++j) {
				VariableSet fvars = cov.getVars(j).free(prod.getLhs());
				if (fvars.isEmpty())
					fvars = vsEmpty;
				cov.setFreeVars(j, fvars);
			}
			cProds.put(prod, cov);
		}
		//logger.finest(cProds.toString());
	}
	
	private Coverage match(Node parse, Meaning F, short index) {
		Production prod = ((ProductionSymbol) parse.getSymbol()).getProduction();
		Production Fprod = F.lprods[index];
		// see if the symbols match
		if (!prod.matches(Fprod))
			return null;
		// see if the variables match
		VariableAssignment vars = prod.matchesVar(Fprod);
		if (vars == null)
			return null;
		if (vars.isEmpty())
			vars = vaEmpty;
		// see if the child nodes match
		short length = (short) F.linear.length;
		Coverage c1 = new Coverage(length, index, index, vars, vsEmpty);
		if (prod.isAC()) {
			// quit if there are not enough child nodes
			if (parse.countChildren() > F.child[index].length)
				return null;
			// memoize to save time
			short m = (short) F.child[index].length;
			ArrayList list = new ArrayList();
			for (short i = 0; i < parse.countChildren(); ++i) {
				Node child = parse.getChild(i);
				if (child.getSymbol() instanceof ProductionSymbol) {
					Coverage[] a = new Coverage[m];
					for (short j = 0; j < m; ++j)
						a[j] = match(child, F, F.child[index][j]);
					list.add(a);
				}
			}
			Coverage[][] c = (Coverage[][]) list.toArray(new Coverage[0][]);
			short n = (short) c.length;
			if (n == 0)
				return c1;
			// try all possible matchings
			Coverage c2 = new Coverage();
			Permute s = new Permute(m, n);
			short[] idx;
			PERMUTE: while ((idx = s.nextPermute()) != null) {
				Coverage c3 = c1;
				for (short i = 0; i < n; ++i) {
					if (c[i][idx[i]] == null)
						continue PERMUTE;
					c3 = c3.product(c[i][idx[i]]);
					if (c3.isEmpty())
						continue PERMUTE;
				}
				c2.addAll(c3);
			}
			return (c2.isEmpty()) ? null : c2;
		} else {
			// there is only one possible matching
			for (short i = 0; i < parse.countChildren(); ++i) {
				Node child = parse.getChild(i);
				if (child.getSymbol() instanceof ProductionSymbol) {
					Coverage c = match(child, F, F.child[index][i]);
					if (c == null)
						return null;
					c1 = c1.product(c);
					if (c1.isEmpty())
						return null;
				}
			}
			return c1;
		}
	}
	
	private void complete(Terminal[] E, Meaning F, Chart c, short current) {
		while (!c.comps[current].isEmpty()) {
			Item comp = (Item) c.comps[current].extractMin();
			if (comp.rule.isDummy())
				continue;
			ArrayList items = c.toComps[comp.start][comp.rule.getLhs().getId()];
			if (items == null)
				continue;
			for (Iterator it = items.iterator(); it.hasNext();) {
				Item item = (Item) it.next();
				Nonterminal in = (Nonterminal) item.rule.getE(item.dot);
				Nonterminal cn = comp.rule.getLhs();
				if (in.countArgs() != cn.countArgs())
					continue;
				Item next = new Item(item, comp);
				if (model.pf.useRuleBigrams)
					next.lastRule = comp.rule.partialRuleId;
				if (model.pf.useFreeVars)
					next.nfvars = model.pf.binFreeVars((short) (item.nfvars+comp.nfvars));
				if (ParseFeatures.USE_VAR_TYPES) {
					Denotation cv = comp.varTypes.uncut(in.getArgs(), item.rule.getMaxVarId());
					next.varTypes = item.varTypes.intersect(cv);
					//logger.finest("i: "+item.start+","+item.current+" "+item.rule);
					//logger.finest("c: "+comp.start+","+comp.current+" "+comp.rule);
					//logger.finest(item.varTypes.toString());
					//logger.finest(comp.varTypes.toString());
					//logger.finest("  "+next.varTypes);
					if (next.varTypes.isEmpty())
						continue;
					Nonterminal lhs = item.rule.getLhs();
					if (lhs != null && item.rule.isLastArg(item.dot))
						next.varTypes = next.varTypes.cut(lhs.getArgs());
				}
				if (F != null)  // training
					next.cov = cov(F, item, comp);
				next.inner = item.inner+comp.inner;
				for (int i = 0; i < model.pf.complete.length; ++i)
					next.inner += model.pf.complete[i].weight(E, item, comp, next);
				c.addItem(next);
				skipWords(E, c, next);
			}
		}
	}
	
	private Coverage cov(Meaning F, Item item, Item comp) {
		Coverage cov = new Coverage();
		Nonterminal in = (Nonterminal) item.rule.getE(item.dot);
		Node inode = item.rule.getArgNode(in.getIndex());
		Production ip = ((ProductionSymbol) inode.getParent().getSymbol()).getProduction();
		// for each subtree in COMP...
		COMP: for (short i = 0; i < comp.cov.size(); ++i) {
			Nonterminal cn = comp.rule.getLhs();
			BitSet cs = comp.cov.getSet(i);
			short cr = comp.cov.getRoot(i);
			VariableAssignment cv = comp.cov.getVars(i);
			VariableSet cfv = comp.cov.getFreeVars(i);
			Production cp = F.lprods[cr];
			boolean ignoreRoot = ip.isAC() && cp.isAC() && ip.matches(cp);
			// check completeness of the COMP subtree
			if (ignoreRoot)
				for (short j = 0; j < F.child[cr].length; ++j) {
					short c = F.child[cr][j];
					if (cs.get(c) && !cs.and(c, F.lastd[c]))
						continue COMP;
				}
			else
				if (!cs.and(cr, F.lastd[cr]))
					continue COMP;
			// find possible matching subtrees for the COMP subtree
			short r = -1;  // avoid compiler warning
			if (ignoreRoot)
				for (short j = 0; j < F.child[cr].length; ++j) {
					short c = F.child[cr][j];
					if (cs.get(c)) {
						r = findRoot(F, c, inode);
						break;
					}
				}
			else
				r = findRoot(F, cr, inode);
			if (r < 0)
				continue COMP;
			// find each matching subtree in ITEM...
			ITEM: for (short j = 0; j < item.cov.size(); ++j) {
				short ir = item.cov.getRoot(j);
				if (ir != r)
					continue ITEM;
				// see if the variable assignments are ok
				VariableAssignment v = item.cov.getVars(j);
				if (v.containsSome(cfv))
					continue ITEM;
				v = v.union(cv.upwardMap(in, cn));
				if (v == null)
					continue ITEM;
				VariableSet fv = item.cov.getFreeVars(j).union(cfv);
				// combine the COMP and the ITEM subtrees
				BitSet s = item.cov.getSet(j);
				if (ignoreRoot) {
					if (!s.get(cr))
						continue ITEM;
					s = (BitSet) s.copy();
					for (short k = 0; k < F.child[cr].length; ++k) {
						short c = F.child[cr][k];
						if (cs.get(c)) {
							if (s.or(c, F.lastd[c]))
								continue ITEM;
							s.setAll(c, F.lastd[c], true);
						}
					}
				} else {
					if (F.parent[cr] >= 0 && !s.get(F.parent[cr]))
						continue ITEM;
					if (s.or(cr, F.lastd[cr]))
						continue ITEM;
					s = s.union(cs);
				}
				// add the combined COMP+ITEM subtree
				cov.add(s, r, v, fv);
			}
		}
		//logger.finest("i: "+item.rule.getProduction());
		//logger.finest("c: "+comp.rule.getProduction());
		//logger.finest(item.cov.toString());
		//logger.finest(comp.cov.toString());
		//logger.finest("  "+cov);
		return (cov.isEmpty()) ? cEmpty : cov;
	}
	
	private short findRoot(Meaning F, short r, Node n) {
		while (n.hasParent()) {
			Production argp = ((ProductionSymbol) n.getParent().getSymbol()).getProduction();
			if (argp.isDummy())
				n = n.getParent();
			else if (F.parent[r] < 0)
				break;
			else if (argp.isAC()) {
				n = n.getParent();
				r = F.parent[r];
			} else if (n.getParentIndex() == Arrays.indexOf(F.child[F.parent[r]], r)) {
				n = n.getParent();
				r = F.parent[r];
			} else
				break;
		}
		return (n.hasParent()) ? -1 : r;
	}
	
	private void skipWords(Terminal[] E, Chart c, Item item) {
		short gap = item.rule.getGap(item.dot);
		for (short i = 0; i < gap && item.current < E.length; ++i) {
			Item next = new Item(item);
			next.varTypes = item.varTypes;
			next.cov = item.cov;
			next.inner = item.inner;
			for (int j = 0; j < model.pf.scan.length; ++j)
				next.inner += model.pf.scan[j].weight(E, item, next);
			c.addItem(next);
			item = next;
		}
	}
	
	private void predictAndScan(Terminal[] E, Meaning F, Chart c, short current) {
		ArrayList set = c.sets[current];
		for (int i = 0; i < set.size(); ++i) {
			Item item = (Item) set.get(i);
			if (item.dot == item.rule.lengthE())
				continue;
			Symbol sym = item.rule.getE(item.dot);
			if (sym instanceof Nonterminal) {
				// predict
				short nslots = ((Nonterminal) sym).countArgs();
				for (int j = 0; j < model.gram.countNonterms(); ++j)
					if (model.gram.isLeftCornerForE(sym.getId(), j) && !c.isPredicted(current, j, nslots)) {
						c.predict(current, j, nslots);
						Rule[] rules = model.gram.getRules(j);
						for (int k = 0; k < rules.length; ++k) {
							if (!rules[k].isActive())
								continue;
							if (!rules[k].possibleMatchE(E[current]))
								continue;
							if (rules[k].getLhs().countArgs() != nslots)
								continue;
							Item next = new Item(rules[k], current);
							if (model.pf.useFreeVars)
								next.nfvars = model.pf.binFreeVars(rules[k].countFreeVars());
							if (ParseFeatures.USE_VAR_TYPES)
								next.varTypes = rules[k].getVarTypes();
							if (F != null)  // training
								next.cov = cov(rules[k]);
							next.inner = 0;
							for (int l = 0; l < model.pf.predict.length; ++l)
								next.inner += model.pf.predict[l].weight(E, next);
							c.addItem(next);
						}
					}
			} else {
				// scan
				if (sym.matches(E[current])) {
					Item next = new Item(item, E[current]);
					next.varTypes = item.varTypes;
					if (F != null) // training
						next.cov = cov(item, E[current]);
					next.inner = item.inner;
					for (int j = 0; j < model.pf.scan.length; ++j)
						next.inner += model.pf.scan[j].weight(E, item, next);
					c.addItem(next);
					skipWords(E, c, next);
				}
			}
		}
	}
	
	private Coverage cov(Rule rule) {
		Coverage cov = (Coverage) cProds.get(rule.getProduction());
		return (cov==null) ? cEmpty : cov;
	}
	
	private Coverage cov(Item item, Terminal word) {
		if (item.rule.isWildcard()) {
			Coverage cov = (Coverage) cWilds.get(word);
			return (cov==null) ? cEmpty : cov.intersect(item.cov);
		} else
			return item.cov;
	}
	
	private class ParseIterator implements Iterator {
		private SCFG gram;
		private Iterator it;
		private Item next;
		public ParseIterator(SCFG gram, Chart c) {
			this.gram = gram;
			it = c.sets[c.maxPos].iterator();
			findNext();
		}
		private void findNext() {
			next = null;
			Nonterminal start = gram.getStart();
			while (it.hasNext()) {
				Item item = (Item) it.next();
				if (item.start != 0 || item.dot != item.rule.lengthE() || !start.equals(item.rule.getLhs()))
					continue;
				if (checkCov && item.cov != null && !item.cov.isFull())
					continue;
				next = item;
				break;
			}
		}
		public boolean hasNext() {
			return next != null;
		}
		public Object next() {
			if (this.next == null)
				throw new NoSuchElementException();
			Item next = this.next;
			findNext();
			return new SCFGParse(next);
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	///
	/// Parameter estimation
	///
	
	/**
	 * The outside algorithm for calcaluting the outer scores of chart items.  The previous call to the
	 * <code>parse</code> method supplies the chart and input sentence required by this algorithm.  If
	 * no such call has been made, then a <code>NullPointerException</code> is thrown.
	 * 
	 * @param checkCov indicates if items with incomplete coverage are ignored during the outside
	 * algorithm.
	 * @throws NullPointerException if the <code>parse</code> method has not been called.
	 */
	public void outside(boolean checkCov) {
		for (int i = 0; i < model.pf.all.length; ++i)
			model.pf.all[i].resetOuterScores();
		c.resetOuterScores();
		initOuterScores(c, checkCov);
		for (short i = c.maxPos; i > 0; --i) {
			reverseComplete(E, c, i);
			reverseScan(E, c, i);
		}
		for (short i = 0; i < c.maxPos; ++i)
			reversePredict(E, c, i);
	}
	
	private void initOuterScores(Chart c, boolean checkCov) {
		for (Iterator it = new ParseIterator(model.gram, c); it.hasNext();) {
			SCFGParse parse = (SCFGParse) it.next();
			if (checkCov && !parse.item.cov.isFull())
				continue;
			parse.item.outer = 0;
		}
	}
	
	private void reverseComplete(Terminal[] E, Chart c, short current) {
		Heap heap = c.createReverseHeap();
		for (Iterator it = c.sets[current].iterator(); it.hasNext();) {
			Item item = (Item) it.next();
			if (item.dot == 0)
				continue;
			if (item.isCompleted())
				heap.add(item);
		}
		double[] w = new double[model.pf.complete.length];
		while (!heap.isEmpty()) {
			Item item = (Item) heap.extractMin();
			int nback = item.countBack();
			for (int i = 0; i < nback; ++i) {
				Item back = item.getBack(i);
				Item comp = item.getBackComplete(i);
				double z = item.outer+back.inner+comp.inner;
				for (int j = 0; j < model.pf.complete.length; ++j)
					z += w[j] = model.pf.complete[j].weight(E, back, comp, item);
				back.outer = Math.logAdd(back.outer, z-back.inner);
				comp.outer = Math.logAdd(comp.outer, z-comp.inner);
				for (int j = 0; j < model.pf.complete.length; ++j)
					model.pf.complete[j].addOuterScore(E, back, comp, item, z-w[j]);
			}
		}
	}
	
	private void reverseScan(Terminal[] E, Chart c, short current) {
		double[] w = new double[model.pf.scan.length];
		for (Iterator it = c.sets[current].iterator(); it.hasNext();) {
			Item item = (Item) it.next();
			if (item.isScan()) {
				int nback = item.countBack();
				for (int j = 0; j < nback; ++j) {
					Item back = item.getBack(j);
					double z = item.outer+back.inner;
					for (int k = 0; k < model.pf.scan.length; ++k)
						z += w[k] = model.pf.scan[k].weight(E, back, item);
					back.outer = Math.logAdd(back.outer, z-back.inner);
					for (int k = 0; k < model.pf.scan.length; ++k)
						model.pf.scan[k].addOuterScore(E, back, item, z-w[k]);
				}
			}
		}
	}
	
	private void reversePredict(Terminal[] E, Chart c, short current) {
		double[] w = new double[model.pf.predict.length];
		for (Iterator it = c.sets[current].iterator(); it.hasNext();) {
			Item item = (Item) it.next();
			if (item.isPredict() && !item.rule.isDummy()) {
				double z = item.outer;
				for (int j = 0; j < model.pf.predict.length; ++j)
					z += w[j] = model.pf.predict[j].weight(E, item);
				for (int j = 0; j < model.pf.predict.length; ++j)
					model.pf.predict[j].addOuterScore(E, item, z-w[j]);
			}
		}
	}
	
}

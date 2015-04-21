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

import java.util.ArrayList;
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
import wasp.main.Generator;
import wasp.main.generate.LogLinearModel;
import wasp.math.Math;
import wasp.mrl.Production;
import wasp.mrl.ProductionSymbol;
import wasp.nl.NgramModel;
import wasp.scfg.SCFG;
import wasp.scfg.Rule;
import wasp.scfg.SCFGModel;
import wasp.util.Arrays;
import wasp.util.BitSet;
import wasp.util.Bool;
import wasp.util.Permute;

/**
 * A bottom-up chart generator based on lambda-SCFG.
 * 
 * @author ywwong
 *
 */
public class LambdaSCFGGenerator extends Generator {

	private static final Logger logger = Logger.getLogger(LambdaSCFGGenerator.class.getName());
	static {
		logger.setLevel(Level.FINER);
	}
	
	private SCFG gram;
	private NgramModel lm;
	private LogLinearModel llm;
	private short kbest;
	
	private VariableAssignment vaEmpty;
	private VariableSet vsEmpty;
	private Nonterminal[] naEmpty;
	private short[] saEmpty;
	
	/**
	 * Creates an SCFG generator based on the specified SCFG and n-gram language model.
	 * 
	 * @param tm the SCFG translation model.
	 * @param lm the n-gram language model for the target NL.
	 * @param llm the log-linear generation model.
	 * @param kbest the number of top-scoring generated sentences to return.
	 */
	public LambdaSCFGGenerator(SCFGModel tm, NgramModel lm, LogLinearModel llm, int kbest) {
		short N = Short.parseShort(Config.get(Config.NGRAM_N));
		Item.setN(N);
		gram = tm.gram;
		this.lm = lm;
		this.llm = llm;
		this.kbest = (short) kbest;
		if (!Config.get(Config.TRANSLATION_PROB_MODEL).equals("relative-freq"))
			throw new RuntimeException("Probabilistic model must be 'relative-freq'");
		if (Bool.parseBool(Config.get(Config.SCFG_ALLOW_GAPS)))
			throw new RuntimeException("Must not allow word gaps");
		vaEmpty = new VariableAssignment();
		vsEmpty = new VariableSet();
		naEmpty = new Nonterminal[0];
		saEmpty = new short[0];
	}
	
	public LambdaSCFGGenerator(SCFGModel tm, NgramModel lm, LogLinearModel llm) {
		this(tm, lm, llm, Config.getKBest());
	}
	
	public boolean batch() {
		return false;
	}
	
	public Iterator generate(Meaning F) {
		//logger.finer(F.str);
		Chart c = new Chart(gram, F, kbest);
		for (short root = 0; root <= c.maxPos; ++root)
			predict(F, c, root);
		for (short root = c.maxPos; root > 0; --root)
			for (short card = 0; card <= c.maxPos; ++card) {
				c.prune(root, card);
				complete(F, c, root, card);
			}
		c.prune((short) 0, c.maxPos);
		completeRoot(F, c);
		if (kbest > 1)
			findKBest(c);
		return new GenIterator(c);
	}
	
	private void predict(Meaning F, Chart c, short root) {
		Rule[] rules = gram.getRules();
		for (int i = 0; i < rules.length; ++i) {
			if (!rules[i].isActive())
				continue;
			if (rules[i].isWildcard()) {
				Terminal term = matchWildcard(rules[i], F, root);
				if (term == null)
					continue;
				Item item = new Item(rules[i], term, llm);
				item.args = naEmpty;
				item.pos = saEmpty;
				item.root = root;
				item.set = new BitSet((short) F.linear.length);
				item.set.set(root, true);
				item.vars = vaEmpty;
				item.fvars = vsEmpty;
				//logger.finer(rules[i].toString());
				//logger.finer("predict "+item.toString());
				c.addItem(item);
			} else {
				Coverage cov = match(parse(rules[i]), F, root);
				if (cov == null)
					continue;
				if (!cov.isEmpty())
					;
					//logger.finer(rules[i].toString());
				for (short j = 0; j < cov.size(); ++j) {
					Item item = new Item(rules[i], lm, llm);
					item.args = cov.getArgs(j);
					if (item.args.length == 0)
						item.args = naEmpty;
					item.pos = cov.getPos(j);
					if (item.pos.length == 0)
						item.pos = saEmpty;
					item.root = cov.getRoot(j);
					item.set = cov.getSet(j);
					item.vars = cov.getVars(j);
					if (item.vars.isEmpty())
						item.vars = vaEmpty;
					item.fvars = item.vars.free(rules[i].getLhs());
					if (item.fvars.isEmpty())
						item.fvars = vsEmpty;
					//logger.finer("predict "+item.toString());
					c.addItem(item);
				}
			}
		}
	}
	
	private Terminal matchWildcard(Rule rule, Meaning F, short root) {
		Production prod = rule.getProduction();
		Production Fprod = F.lprods[root];
		// see if the symbols match
		if (!prod.matches(Fprod))
			return null;
		return (Terminal) Fprod.getRhs((short) 0);
	}
	
	private Node parse(Rule rule) {
		Node parse = rule.getProduction().getParse().deepCopy();
		Node[] n1 = parse.getDescends(Nonterminal.class);
		Nonterminal[] n2 = (Nonterminal[]) Arrays.subseq(rule.getF(), Nonterminal.class);
		for (short j = 0; j < n1.length; ++j)
			n1[j].getParent().replaceChild(n1[j], new Node(n2[j]));
		return parse;
	}
	
	private Coverage match(Node parse, Meaning F, short root) {
		Production prod = ((ProductionSymbol) parse.getSymbol()).getProduction();
		Production Fprod = F.lprods[root];
		// see if the symbols match
		if (!prod.matches(Fprod))
			return null;
		// see if the variables match
		VariableAssignment vars = prod.matchesVar(Fprod);
		if (vars == null)
			return null;
		// see if the child nodes match
		short length = (short) F.linear.length;
		Coverage c1 = new Coverage(length, root, root, vars);
		if (prod.isAC()) {
			// quit if there are not enough child nodes
			Node[] ch = parse.getChildren();
			short N = parse.countChildren();
			short m = (short) F.child[root].length;
			if (N > m)
				return null;
			// memoize to save time
			ArrayList lc = new ArrayList();
			ArrayList lcp = new ArrayList();
			for (short i = 0; i < N; ++i) {
				Symbol sym = ch[i].getSymbol();
				if (sym instanceof ProductionSymbol) {
					Coverage[] a = new Coverage[m];
					for (short j = 0; j < m; ++j)
						a[j] = match(ch[i], F, F.child[root][j]);
					lc.add(a);
				} else
					lcp.add(new Coverage(length, (Nonterminal) sym, root));
			}
			Coverage[][] c = (Coverage[][]) lc.toArray(new Coverage[0][]);
			Coverage[] cp = (Coverage[]) lcp.toArray(new Coverage[0]);
			short n = (short) c.length;
			short np = (short) cp.length;
			// try all possible matchings
			if (n > 0) {
				Coverage c2 = new Coverage();
				Permute s = new Permute(m, n, c);
				short[] idx;
				PERMUTE: while ((idx = s.nextPermute()) != null) {
					Coverage c3 = null;
					for (short i = 0; i < n; ++i)
						if (c3 == null)
							c3 = c[i][idx[i]];
						else {
							c3 = c3.product(c[i][idx[i]]);
							if (c3.isEmpty())
								continue PERMUTE;
						}
					if (c3 != null)
						c2.addAll(c3);
				}
				if (c2.isEmpty())
					return null;
				c1 = c1.product(c2);
			}
			if (np > 0) {
				Coverage c2 = new Coverage();
				Permute t = new Permute(np, np);
				short[] idx;
				while ((idx = t.nextPermute()) != null) {
					Coverage c3 = cp[idx[np-1]];
					for (short i = (short) (np-2); i >= 0; --i)
						c3 = c3.product(cp[idx[i]]);
					c2.addAll(c3);
				}
				c1 = c1.product(c2);
			}
		} else {
			// there is only one possible matching
			for (short i = 0; i < parse.countChildren(); ++i) {
				Node child = parse.getChild(i);
				Symbol csym = child.getSymbol();
				if (csym instanceof ProductionSymbol) {
					Coverage c = match(child, F, F.child[root][i]);
					if (c == null)
						return null;
					c1 = c1.product(c);
					if (c1.isEmpty())
						return null;
				} else {
					Coverage c = new Coverage(length, (Nonterminal) csym, F.child[root][i]);
					c1 = c1.product(c);
				}
			}
		}
		return c1;
	}
	
	private void complete(Meaning F, Chart c, short root, short card) {
		COMP: while (!c.comps[root][card].isEmpty()) {
			Item comp = (Item) c.comps[root][card].extractMin();
			if (!comp.isActive())
				continue COMP;
			Nonterminal cn = comp.rule.getLhs();
			BitSet cs = comp.set;
			// check completeness of COMP subtree
			if (F.lprods[root].isAC())
				for (short k = 0; k < F.child[root].length; ++k) {
					short ch = F.child[root][k];
					if (cs.get(ch) && !cs.and(ch, F.lastd[ch]))
						continue COMP;
				}
			boolean full = cs.and(root, F.lastd[root]);
			ArrayList[] lists = new ArrayList[2];
			lists[0] = c.toComps[root][cn.getId()];
			lists[1] = c.toComps[F.parent[root]][cn.getId()];
			for (int i = 0; i < 2; ++i) {
				if (lists[i] == null)
					continue;
				ITEM: for (int j = 0; j < lists[i].size(); ++j) {
					Item item = (Item) lists[i].get(j);
					Nonterminal in = item.args[item.dot];
					BitSet is = item.set;
					if (in.countArgs() != cn.countArgs())
						continue ITEM;
					if (i == 1 && !is.get(F.parent[root]))
						continue ITEM;
					boolean combineAC = is.get(root);
					if (!combineAC && !full)
						continue ITEM;
					Item next = new Item(item, comp, lm, llm);
					// see if variable assignments are OK
					if (item.vars.containsSome(comp.fvars))
						continue ITEM;
					next.vars = item.vars.union(comp.vars.upwardMap(in, cn));
					if (next.vars == null)
						continue ITEM;
					next.fvars = item.fvars.union(comp.fvars);
					// combine COMP and ITEM subtrees
					next.root = item.root;
					if (is.intersect(cs).cardinality() > ((combineAC) ? 1 : 0))
						continue ITEM;
					next.set = is.union(cs);
					// add new item finally
					//logger.finer("complete "+item+" + "+comp+" -> "+next);
					c.addItem(next);
				}
			}
		}
	}
	
	private void completeRoot(Meaning F, Chart c) {
		Nonterminal start = gram.getStart();
		while (!c.comps[0][c.maxPos].isEmpty()) {
			Item comp = (Item) c.comps[0][c.maxPos].extractMin();
			if (!comp.isActive())
				continue;
			Nonterminal cn = comp.rule.getLhs();
			if (!cn.equals(start))
				continue;
			Item next = new Item(comp, lm, llm);
			//logger.finer("complete dummy + "+comp+" -> "+next);
			c.addItem(next);
		}
	}
	
	private void findKBest(Chart c) {
		if (c.root == null)
			return;
		lazyKthBest(c.root, kbest);
		clearCandidates(c.root);
	}
	
	private void lazyKthBest(Item item, short k) {
		if (item.cand == null)
			getCandidates(item);
		while (item.D.length < k) {
			lazyNext(item.cand, item.D[item.D.length-1]);
			if (item.cand.size() > 0) {
				Derivation max = (Derivation) item.cand.extractMin();
				item.D = (Derivation[]) Arrays.append(item.D, max);
			} else
				break;
		}
	}
	
	private void getCandidates(Item item) {
		item.cand = new SimpleHeap(kbest);
		if (item.bstar.length == 1)
			return;
		// FIXME not sure why this can be true
		if (item.bstar[0].arity() == 0)
			return;
		Derivation[] temp = new Derivation[item.bstar.length-1];
		for (int i = 0, j = 0; i < item.bstar.length; ++i)
			if (item.bstar[i] != item.D[0].edge)
				temp[j++] = derivation(item.bstar[i], (short) 0, (short) 0);
		double min = select(temp, 0, temp.length-1, kbest-1).weight;
		for (int i = 0; i < temp.length; ++i)
			if (temp[i].weight >= min)
				item.cand.add(temp[i]);
	}
	
	private Derivation derivation(Hyperarc edge, short ptr1, short ptr2) {
		Derivation d = new Derivation(edge, ptr1, ptr2);
		if (edge.arity() == 1)
			Item.completeRoot(d, lm, llm);
		else  // edge.arity() == 2
			Item.complete(d, lm, llm);
		return d;
	}
	
	private Derivation select(Derivation[] temp, int p, int r, int i) {
		if (p == r)
			return temp[p];
		int q = partition(temp, p, r);
		int k = q-p+1;
		if (i <= k)
			return select(temp, p, q, i);
		else
			return select(temp, q+1, r, i-k);
	}
	
	private int partition(Derivation[] temp, int p, int r) {
		// randomize to avoid worst-case input
		int rand = Math.random(p, r);
		Derivation tmp = temp[p];
		temp[p] = temp[rand];
		temp[rand] = tmp;
		// partition
		double w = temp[p].weight;
		int i = p-1;
		int j = r+1;
		while (true) {
			do {
				--j;
			} while (temp[j].weight < w);
			do {
				++i;
			} while (temp[i].weight > w);
			if (i < j) {
				tmp = temp[i];
				temp[i] = temp[j];
				temp[j] = tmp;
			} else
				return j;
		}
	}
	
	private void lazyNext(SimpleHeap cand, Derivation d) {
		int arity = d.edge.arity();
		if (arity >= 1) {
			short ptr1 = (short) (d.ptr1+1);
			lazyKthBest(d.edge.tail1, (short) (ptr1+1));
			if (ptr1 < d.edge.tail1.D.length) {
				Derivation d1 = derivation(d.edge, ptr1, d.ptr2);
				if (!cand.contains(d1))
					cand.add(d1);
			}
		}
		if (arity == 2) {
			short ptr2 = (short) (d.ptr2+1);
			lazyKthBest(d.edge.tail2, (short) (ptr2+1));
			if (ptr2 < d.edge.tail2.D.length) {
				Derivation d2 = derivation(d.edge, d.ptr1, ptr2);
				if (!cand.contains(d2))
					cand.add(d2);
			}
		}
	}
	
	private void clearCandidates(Item item) {
		if (item.cand == null)
			return;
		item.cand = null;
		for (short i = 0; i < item.D.length; ++i) {
			Item tail1 = item.D[i].edge.tail1;
			Item tail2 = item.D[i].edge.tail2;
			if (tail1 != null)
				clearCandidates(tail1);
			if (tail2 != null)
				clearCandidates(tail2);
		}
	}
	
	private class GenIterator implements Iterator {
		private Chart c;
		private int next;
		public GenIterator(Chart c) {
			this.c = c;
		}
		public boolean hasNext() {
			return c.root != null && next < c.root.D.length;
		}
		public Object next() {
			if (c.root == null || next >= c.root.D.length)
				throw new NoSuchElementException();
			return new LambdaSCFGGen(c.root.D[next++]);
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
}

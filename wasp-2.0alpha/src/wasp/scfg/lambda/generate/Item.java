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

import java.util.HashMap;

import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.data.VariableAssignment;
import wasp.data.VariableSet;
import wasp.main.generate.LogLinearModel;
import wasp.math.Math;
import wasp.nl.NgramModel;
import wasp.scfg.Rule;
import wasp.util.Arrays;
import wasp.util.BitSet;

/**
 * Chart items used in the lambda-SCFG-based chart generator.
 * 
 * @author ywwong
 *
 */
public class Item {

	private static short N;
	private static Terminal[] ngram;

	static void setN(short N) {
		Item.N = N;
		ngram = new Terminal[N];
	}
	
	private static HashMap initContexts = new HashMap();
	/**
	 * The surrounding context in which phrases appear in the output sentence.
	 * 
	 * @author ywwong
	 *
	 */
	private static class Context {
		public short[] args;
		public Terminal[] context;
		public boolean equals(Object o) {
			if (o == this)
				return true;
			return o instanceof Context && Arrays.equal(context, ((Context) o).context);
		}
		public int hashCode() {
			return Arrays.hashCode(context);
		}
	}
	
	public Rule rule;
	/** The non-terminals to be rewritten. */
	public Nonterminal[] args;
	/** The position indices of non-terminals to be rewritten, sorted in descending order. */
	public short[] pos;
	/** The next non-terminal(s) to be rewritten. */
	public short dot;
	/** The root position of the MR sub-parse that have been covered. */
	public short root;
	/** The MR sub-parse that have been covered. */
	public BitSet set;
	/** The current variable assignment. */
	public VariableAssignment vars;
	/** The current set of free variables. */
	public VariableSet fvars;
	/** The surrounding context of argument positions. */
	public Context context;
	public int timestamp;
	
	// for lazy k-best parsing
	public Hyperarc[] bstar;
	public Derivation[] D;
	public SimpleHeap cand;
	
	/**
	 * Creates a prediction item.
	 * 
	 * @param rule the SCFG rule to predict.
	 * @param lm the n-gram language model.
	 * @param llm the log-linear generation model.
	 */
	public Item(Rule rule, NgramModel lm, LogLinearModel llm) {
		this.rule = rule;
		args = null;
		pos = null;
		dot = 0;
		root = 0;
		set = null;
		vars = null;
		fvars = null;
		timestamp = 0;
		
		// re-use the initial context vector of the given rule, or create one if necessary
		context = (Context) initContexts.get(rule);
		if (context == null) {
			Context c = new Context();
			rule.getScores().lm = 0;
			// determine what words to include in the initial context vector
			short len = rule.lengthE();
			boolean[] incl = new boolean[len];
			for (short i = 0, edge = i; i < len; ++i) {
				Symbol sym = rule.getE(i);
				if (sym instanceof Terminal && i < edge+N-1)
					incl[i] = true;
				if (sym instanceof Nonterminal)
					edge = (short) (i+1);
			}
			for (short i = (short) (len-1), edge = i; i >= 0; --i) {
				Symbol sym = rule.getE(i);
				if (sym instanceof Terminal && i > edge-N+1)
					incl[i] = true;
				if (sym instanceof Nonterminal)
					edge = (short) (i-1);
			}
			// create the initial context vector
			short nargs = rule.countArgs();
			c.args = new short[nargs];
			c.context = new Terminal[Arrays.count(incl)+nargs+2];
			Arrays.clear(ngram);
			for (short i = 0, idx = 1; i < len; ++i) {
				Symbol sym = rule.getE(i);
				if (sym instanceof Nonterminal) {
					c.args[sym.getIndex()-1] = idx;
					c.context[idx++] = null;
					Arrays.clear(ngram);
				} else {  // sym instanceof Terminal
					if (incl[i])
						c.context[idx++] = (Terminal) sym;
					ngram[N-1] = (Terminal) sym;
					if (ngram[0] != null)
						rule.getScores().lm += lm.score(ngram);
					Arrays.shiftLeft(ngram);
				}
			}
			context = c;
			// store the initial context vector
			initContexts.put(rule, context);
		}
		
		bstar = new Hyperarc[1];
		bstar[0] = new Hyperarc(this);
		D = new Derivation[1];
		D[0] = new Derivation(bstar[0]);
		D[0].scores = rule.getScores();
		D[0].weight = llm.dot(D[0].scores);
		//D[0].weight = llm.dotExceptWP(D[0].scores);
		cand = null;
	}

	/**
	 * Creates a prediction item for a wildcard rule.
	 * 
	 * @param rule the wildcard rule.
	 * @param term the current MR symbol.
	 * @param llm the log-linear generation model.
	 */
	public Item(Rule rule, Terminal term, LogLinearModel llm) {
		this.rule = new Rule(rule, term);
		args = null;
		pos = null;
		dot = 0;
		root = 0;
		set = null;
		vars = null;
		fvars = null;
		timestamp = 0;
		
		// create a new context vector
		Context c = new Context();
		c.args = new short[0];
		c.context = new Terminal[3];
		c.context[1] = term;
		context = c;
		
		bstar = new Hyperarc[1];
		bstar[0] = new Hyperarc(this);
		D = new Derivation[1];
		D[0] = new Derivation(bstar[0]);
		D[0].scores = rule.getScores();
		D[0].weight = llm.dot(D[0].scores);
		//D[0].weight = llm.dotExceptWP(D[0].scores);
		cand = null;
	}
	
	/**
	 * Creates a completion item, assuming zero back pointers.
	 * 
	 * @param back the item to be completed.
	 * @param comp the complete item.
	 * @param lm the current n-gram language model.
	 * @param llm the current log-linear generation model.
	 */
	public Item(Item back, Item comp, NgramModel lm, LogLinearModel llm) {
		rule = back.rule;
		args = back.args;
		pos = back.pos;
		dot = (short) (back.dot+1);
		root = 0;
		set = null;
		vars = null;
		fvars = null;
		timestamp = 0;
		
		bstar = new Hyperarc[1];
		bstar[0] = new Hyperarc(back, comp, this);
		D = new Derivation[1];
		D[0] = new Derivation(bstar[0]);
		D[0].scores = back.D[0].scores.add(comp.D[0].scores);
		D[0].weight = back.D[0].weight+comp.D[0].weight;
		cand = null;
		
		// update the context vector
		Context c = new Context();
		Context bc = back.context;
		Context cc = comp.context;
		int index = back.args[back.dot].getIndex();
		short here = bc.args[index-1];
		short prev = prev(bc.context, here);
		short next = next(bc.context, here);
		short seglen1 = (short) (next-prev-1);
		short arglen = (short) (cc.context.length-2);
		short seglen2 = (short) (seglen1+arglen-1);
		short diff = (short) (Math.min(seglen2, 2*(N-1))-seglen1);
		c.args = update(bc.args, here, diff);
		c.context = new Terminal[bc.context.length+diff];
		short idx = 0;
		short segIdx = 0;
		Arrays.clear(ngram);
		for (short i = 0; i <= prev; ++i)
			c.context[idx++] = bc.context[i];
		for (short i = (short) (prev+1); i < here; ++i, ++segIdx) {
			if (incl(seglen2, segIdx))
				c.context[idx++] = bc.context[i];
			ngram[N-1] = bc.context[i];
			Arrays.shiftLeft(ngram);
		}
		for (short i = 1; i < cc.context.length-1; ++i, ++segIdx) {
			if (incl(seglen2, segIdx))
				c.context[idx++] = cc.context[i];
			ngram[N-1] = cc.context[i];
			if (i < N && ngram[0] != null) {
				double s = lm.score(ngram);
				D[0].scores.lm += s;
				D[0].weight += llm.wLM*s;
			}
			Arrays.shiftLeft(ngram);
		}
		for (short i = (short) (here+1); i < next; ++i, ++segIdx) {
			if (incl(seglen2, segIdx))
				c.context[idx++] = bc.context[i];
			ngram[N-1] = bc.context[i];
			if (i < here+N && ngram[0] != null) {
				double s = lm.score(ngram);
				D[0].scores.lm += s;
				D[0].weight += llm.wLM*s;
			}
			Arrays.shiftLeft(ngram);
		}
		for (short i = next; i < bc.context.length; ++i)
			c.context[idx++] = bc.context[i];
		context = c;
	}
	
	public static void complete(Derivation d, NgramModel lm, LogLinearModel llm) {
		Item back = d.edge.tail1;
		Item comp = d.edge.tail2;
		d.scores = back.D[d.ptr1].scores.add(comp.D[d.ptr2].scores);
		d.weight = back.D[d.ptr1].weight+comp.D[d.ptr2].weight;
		
		Context bc = back.context;
		Context cc = comp.context;
		int index = back.args[back.dot].getIndex();
		short here = bc.args[index-1];
		short prev = prev(bc.context, here);
		short next = next(bc.context, here);
		Arrays.clear(ngram);
		for (short i = (short) (prev+1); i < here; ++i) {
			ngram[N-1] = bc.context[i];
			Arrays.shiftLeft(ngram);
		}
		for (short i = 1; i < cc.context.length-1; ++i) {
			ngram[N-1] = cc.context[i];
			if (i < N && ngram[0] != null) {
				double s = lm.score(ngram);
				d.scores.lm += s;
				d.weight += llm.wLM*s;
			}
			Arrays.shiftLeft(ngram);
		}
		for (short i = (short) (here+1); i < next; ++i) {
			ngram[N-1] = bc.context[i];
			if (i < here+N && ngram[0] != null) {
				double s = lm.score(ngram);
				d.scores.lm += s;
				d.weight += llm.wLM*s;
			}
			Arrays.shiftLeft(ngram);
		}
	}

	private static short prev(Terminal[] context, short i) {
		while (context[--i] != null)
			;
		return i;
	}
	
	private static short next(Terminal[] context, short i) {
		while (context[++i] != null)
			;
		return i;
	}
	
	private short[] update(short[] array, short from, short diff) {
		if (diff == 0)
			return array;
		short[] a = array;
		for (short i = 0; i < array.length; ++i)
			if (array[i] >= from) {
				if (a == array)
					a = (short[]) array.clone();
				a[i] += diff;
			}
		return a;
	}
	
	private boolean incl(short len, short i) {
		return i >= 0 && i < len && (i < N-1 || i >= len-N+1);
	}
	
	/**
	 * Creates a completion item for the dummy rule.
	 * 
	 * @param comp the complete item.
	 * @param lm the current n-gram language model.
	 * @param llm the current log-linear generation model.
	 */
	public Item(Item comp, NgramModel lm, LogLinearModel llm) {
		rule = null;
		args = null;
		pos = null;
		dot = 1;
		root = 0;
		set = null;
		vars = null;
		fvars = null;
		timestamp = 0;
		
		bstar = new Hyperarc[1];
		bstar[0] = new Hyperarc(comp, this);
		D = new Derivation[1];
		D[0] = new Derivation(bstar[0]);
		D[0].scores = new LogLinearModel.Scores(comp.D[0].scores);
		D[0].weight = comp.D[0].weight;
		//D[0].weight += llm.wWP*D[0].scores.wp;
		cand = null;

		for (short i = 2; i <= N-1 && i+1 < comp.context.context.length; ++i) {
			Terminal[] ngram = new Terminal[i];
			for (short j = 1; j <= i; ++j)
				ngram[j-1] = comp.context.context[j];
			double s = lm.score(ngram);
			D[0].scores.lm += s;
			D[0].weight += llm.wLM*s;
		}
		context = null;
	}
	
	public static void completeRoot(Derivation d, NgramModel lm, LogLinearModel llm) {
		Item comp = d.edge.tail1;
		d.scores = new LogLinearModel.Scores(comp.D[d.ptr1].scores);
		d.weight = comp.D[d.ptr1].weight;
		//d.weight += llm.wWP*d.scores.wp;

		for (short i = 2; i <= N-1 && i+1 < comp.context.context.length; ++i) {
			Terminal[] ngram = new Terminal[i];
			for (short j = 1; j <= i; ++j)
				ngram[j-1] = comp.context.context[j];
			double s = lm.score(ngram);
			d.scores.lm += s;
			d.weight += llm.wLM*s;
		}
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof Item) {
			Item i = (Item) o;
			return
			dot == i.dot
			&& root == i.root
			&& ((rule==null) ? i.rule==null : rule.equals(i.rule))
			&& ((args==null) ? i.args==null : Arrays.equal(args, i.args))
			&& ((pos==null) ? i.pos==null : Arrays.equal(pos, i.pos))
			&& ((set==null) ? i.set==null : set.equals(i.set))
			&& ((vars==null) ? i.vars==null : vars.equals(i.vars))
			&& ((fvars==null) ? i.fvars==null : fvars.equals(i.fvars))
			&& ((context==null) ? i.context==null : context.equals(i.context));
		}
		return false;
	}

	public int hashCode() {
		int hash = 1;
		hash = 31*hash + dot;
		hash = 31*hash + ((rule==null) ? 0 : rule.hashCode());
		hash = 31*hash + ((args==null) ? 0 : Arrays.hashCode(args));
		hash = 31*hash + ((pos==null) ? 0 : Arrays.hashCode(pos));
		hash = 31*hash + ((set==null) ? 0 : set.hashCode());
		hash = 31*hash + ((vars==null) ? 0 : vars.hashCode());
		hash = 31*hash + ((context==null) ? 0 : context.hashCode());
		return hash;
	}
	
	/**
	 * Indicates if all non-terminals have been rewritten in this item, i.e.&nbsp;the dot is at the
	 * end of the MRL string.
	 * 
	 * @return <code>true</code> if all non-terminals have been rewritten in this item;
	 * <code>false</code> otherwise.
	 */
	public boolean isComplete() {
		return dot == args.length;
	}
	
	public boolean isRoot() {
		return rule == null;
	}
	
	public void max(Item item) {
		if (D[0].weight < item.D[0].weight) {
			bstar = item.bstar;
			D[0] = item.D[0];
			D[0].edge.head = this;
		}
	}
	
	/**
	 * Do this only <b>before</b> putting any derivations into a heap!
	 */
	public void merge(Item item) {
		bstar = (Hyperarc[]) Arrays.append(bstar, item.D[0].edge);
		item.D[0].edge.head = this;
		if (D[0].weight < item.D[0].weight)
			D[0] = item.D[0];
	}

	public boolean isActive() {
		return root >= 0;
	}
	
	public void deactivate() {
		root = -1;
	}
	
}

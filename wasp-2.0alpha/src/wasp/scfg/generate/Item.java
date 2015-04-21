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

import java.util.HashMap;

import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.main.generate.LogLinearModel;
import wasp.math.Math;
import wasp.nl.NgramModel;
import wasp.scfg.Rule;
import wasp.util.Arrays;

/**
 * Chart items used in the SCFG-based chart generator.
 * 
 * @author ywwong
 *
 */
public class Item implements AnyItem {

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
	public static class Context {
		public short[] args;
		public short[] gaps;
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
	/** The MRL dot position. */
	public short dotF;
	/** The NL dot position. */
	public short dotE;
	/** The MRL start position. */
	public short start;
	/** The MRL current position. */
	public short current;
	/** The surrounding context of argument positions. */
	public Context context;
	/** Various components of the inner score. */
	public LogLinearModel.Scores scores;
	/** The inner score. */
	public double inner;
	public int timestamp;
	public Item back;
	public AnyItem backComp;
	
	/**
	 * Creates an item for the prediction step.
	 * 
	 * @param rule the SCFG rule to predict.
	 * @param start the start position.
	 * @param lm the current n-gram language model.
	 * @param llm the current log-linear generation model.
	 */
	public Item(Rule rule, short start, NgramModel lm, LogLinearModel llm) {
		this.rule = rule;
		dotF = 0;
		dotE = 0;
		this.start = start;
		current = start;
		timestamp = 0;
		back = null;
		backComp = null;
		
		// rules with wildcard terminals have no initial context vector
		if (rule.isWildcard()) {
			context = null;
			scores = rule.getScores();
			inner = llm.dot(scores);
			return;
		}
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
				if (sym instanceof Nonterminal || rule.getGap((short) (i+1)) > 0)
					edge = (short) (i+1);
			}
			for (short i = (short) (len-1), edge = i; i >= 0; --i) {
				if (rule.getGap((short) (i+1)) > 0)
					edge = i;
				Symbol sym = rule.getE(i);
				if (sym instanceof Terminal && i > edge-N+1)
					incl[i] = true;
				if (sym instanceof Nonterminal)
					edge = (short) (i-1);
			}
			// create the initial context vector
			short nargs = rule.countArgs();
			short ngaps = rule.countGaps();
			c.args = new short[nargs];
			c.gaps = new short[ngaps];
			c.context = new Terminal[Arrays.count(incl)+nargs+ngaps+2];
			Arrays.clear(ngram);
			for (short i = 0, idx = 1, gapIdx = 0; i < len; ++i) {
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
				if (rule.getGap((short) (i+1)) > 0) {
					c.gaps[gapIdx++] = idx;
					c.context[idx++] = null;
					Arrays.clear(ngram);
				}
			}
			context = c;
			// store the initial context vector
			initContexts.put(rule, context);
		}
		scores = rule.getScores();
		inner = llm.dot(scores);
	}

	/**
	 * Creates an item for the scanning step.
	 * 
	 * @param back the back-pointer item.
	 * @param sym the symbol that has been scanned.
	 */
	public Item(Item back, Symbol sym) {
		if (back.rule.isWildcard() && sym instanceof Terminal)
			rule = new Rule(back.rule, (Terminal) sym);
		else
			rule = back.rule;
		dotF = (short) (back.dotF+1);
		dotE = back.dotE;
		start = back.start;
		current = (short) (back.current+1);
		scores = back.scores;
		inner = back.inner;
		timestamp = 0;
		this.back = back;
		backComp = null;
		
		// create a new context vector if the rule has a wildcard terminal
		if (back.rule.isWildcard()) {
			Context c = new Context();
			c.args = new short[0];
			c.gaps = new short[0];
			c.context = new Terminal[3];
			c.context[1] = (Terminal) rule.getE((short) 0);
			context = c;
		} else
			context = back.context;
	}
	
	/**
	 * Creates an item for the completion step.
	 * 
	 * @param back the item to be completed.
	 * @param comp the complete item.
	 * @param lm the current n-gram language model.
	 * @param llm the current log-linear generation model.
	 */
	public Item(Item back, Item comp, NgramModel lm, LogLinearModel llm) {
		rule = back.rule;
		dotF = (short) (back.dotF+1);
		dotE = back.dotE;
		start = back.start;
		current = comp.current;
		scores = back.scores.add(comp.scores);
		inner = back.inner+comp.inner;
		timestamp = 0;
		this.back = back;
		backComp = comp;
		
		// update the context vector
		Context c = new Context();
		Context bc = back.context;
		Context cc = comp.context;
		short here = bc.args[0];
		short prev = prev(bc.context, here);
		short next = next(bc.context, here);
		short seglen1 = (short) (next-prev-1);
		short arglen = (short) (cc.context.length-2);
		short seglen2 = (short) (seglen1+arglen-1);
		short diff = (short) (Math.min(seglen2, 2*(N-1))-seglen1);
		c.args = Arrays.remove(bc.args, 0);
		updateInPlace(c.args, here, diff);
		c.gaps = update(bc.gaps, here, diff);
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
				scores.lm += s;
				inner += llm.wLM*s;
			}
			Arrays.shiftLeft(ngram);
		}
		for (short i = (short) (here+1); i < next; ++i, ++segIdx) {
			if (incl(seglen2, segIdx))
				c.context[idx++] = bc.context[i];
			ngram[N-1] = bc.context[i];
			if (i < here+N && ngram[0] != null) {
				double s = lm.score(ngram);
				scores.lm += s;
				inner += llm.wLM*s;
			}
			Arrays.shiftLeft(ngram);
		}
		for (short i = next; i < bc.context.length; ++i)
			c.context[idx++] = bc.context[i];
		context = c;
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
	
	private void updateInPlace(short[] array, short from, short diff) {
		if (diff == 0)
			return;
		for (short i = 0; i < array.length; ++i)
			if (array[i] >= from)
				array[i] += diff;
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
	 * Creates an item for advancing the NL dot.
	 * 
	 * @param back the back-pointer item.
	 */
	public Item(Item back) {
		rule = back.rule;
		dotF = back.dotF;
		dotE = (short) (back.dotE+1);
		start = back.start;
		current = back.current;
		scores = back.scores;
		inner = back.inner;
		timestamp = 0;
		this.back = back;
		backComp = null;
		context = back.context;
	}
	
	/**
	 * Creates an item for advancing the NL dot and generating words from a word gap.
	 * 
	 * @param back the back-pointer item.
	 * @param comp the complete word-gap item.
	 */
	public Item(Item back, GapItem comp) {
		rule = back.rule;
		dotF = back.dotF;
		dotE = (short) (back.dotE+1);
		start = back.start;
		current = back.current;
		scores = back.scores.add(comp.scores);
		inner = back.inner+comp.inner;
		timestamp = 0;
		this.back = back;
		backComp = comp;
		
		// update the context vector
		Context c = new Context();
		Context bc = back.context;
		GapItem.Context cc = comp.context;
		short here = bc.gaps[0];
		short prev = prev(bc.context, here);
		short next = next(bc.context, here);
		short seglen1 = (short) (next-prev-1);
		short arglen = (short) (cc.to-cc.from);
		short seglen2 = (short) (seglen1+arglen-1);
		short diff = (short) (Math.min(seglen2, 2*(N-1))-seglen1);
		c.args = bc.args;  // all arguments have been added
		c.gaps = Arrays.remove(bc.gaps, 0);
		updateInPlace(c.gaps, here, diff);
		c.context = new Terminal[bc.context.length+diff];
		short idx = 0;
		short segIdx = 0;
		for (short i = 0; i <= prev; ++i)
			c.context[idx++] = bc.context[i];
		for (short i = (short) (prev+1); i < here; ++i, ++segIdx)
			if (incl(seglen2, segIdx))
				c.context[idx++] = bc.context[i];
		for (short i = cc.from; i < cc.to; ++i, ++segIdx)
			if (incl(seglen2, segIdx))
				c.context[idx++] = cc.context[i];
		for (short i = (short) (here+1); i < next; ++i, ++segIdx)
			if (incl(seglen2, segIdx))
				c.context[idx++] = bc.context[i];
		for (short i = next; i < bc.context.length; ++i)
			c.context[idx++] = bc.context[i];
		context = c;
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof Item) {
			Item i = (Item) o;
			return rule.equals(i.rule) && dotF == i.dotF && dotE == i.dotE && start == i.start
			&& current == i.current && ((context==null) ? i.context==null : context.equals(i.context));
		}
		return false;
	}

	public int hashCode() {
		int hash = 1;
		hash = 31*hash + rule.hashCode();
		hash = 31*hash + dotF;
		hash = 31*hash + dotE;
		hash = 31*hash + start;
		hash = 31*hash + current;
		hash = 31*hash + ((context==null) ? 0 : context.hashCode());
		return hash;
	}
	
	public int compareTo(Object o) {
		if (inner > ((Item) o).inner)
			return -1;
		else if (inner < ((Item) o).inner)
			return 1;
		else
			return 0;
	}
	
	/**
	 * Replaces the content of this item with that of the specified item, such that this item will
	 * remain "the same" in data structures like heaps and hash maps.  The specified item must be
	 * <i>equal</i> to this item.  For efficiency reasons, this method does not check for equality of
	 * items.  But keep in mind that using items that are not equal will lead to <b>disastrous</b> 
	 * effects!
	 *   
	 * @param item an item that is equal to this item.
	 */
	public void replace(Item item) {
		scores = item.scores;
		inner = item.inner;
		back = item.back;
		backComp = item.backComp;
	}

	/**
	 * Indicates if all non-terminals have been rewritten in this item, i.e.&nbsp;the dot is at the
	 * end of the MRL string.
	 * 
	 * @return <code>true</code> if all non-terminals have been rewritten in this item;
	 * <code>false</code> otherwise.
	 */
	public boolean isCompleteF() {
		return dotF == rule.lengthF();
	}
	
	/**
	 * Indicates if all word gaps have been filled in this item, i.e.&nbsp;the dot is at the end of
	 * the NL string.
	 * 
	 * @return <code>true</code> if all word gaps have been filled in this item; <code>false</code>
	 * otherwise.
	 */
	public boolean isCompleteE() {
		return dotE == rule.lengthE();
	}

	/**
	 * Adjusts the inner score based on the beginning n-grams of the sentence.
	 * 
	 * @param lm the current n-gram language model.
	 * @param llm the current log-linear generation model.
	 */
	public void adjustInner(NgramModel lm, LogLinearModel llm) {
		for (short i = 2; i <= N-1 && i+1 < context.context.length; ++i) {
			Terminal[] ngram = new Terminal[i];
			for (short j = 1; j <= i; ++j)
				ngram[j-1] = context.context[j];
			double s = lm.score(ngram);
			scores.lm += s;
			inner += llm.wLM*s;
		}
	}
	
}

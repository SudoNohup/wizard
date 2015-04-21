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

import wasp.data.Terminal;
import wasp.main.generate.LogLinearModel;
import wasp.math.Math;
import wasp.nl.NgramModel;
import wasp.util.Arrays;

/**
 * Chart items used in <code>GapGenerator</code>.
 * 
 * @see wasp.scfg.generate.GapGenerator
 * @author ywwong
 *
 */
public class GapItem implements AnyItem {

	private static short N;
	private static Terminal[] ngram;

	static void setN(short N) {
		GapItem.N = N;
		ngram = new Terminal[N];
	}
	
	/**
	 * The surrounding context in which a gap-filling phrase appears in the output sentence.
	 * 
	 * @author ywwong
	 *
	 */
	public static class Context {
		public short from;
		public short to;
		public Terminal[] context;
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (o instanceof Context) {
				Context c = (Context) o;
				return from == c.from && to == c.to && Arrays.equal(context, c.context);
			}
			return false;
		}
		public int hashCode() {
			int hash = 1;
			hash = 31*hash + from;
			hash = 31*hash + to;
			hash = 31*hash + Arrays.hashCode(context);
			return hash;
		}
	}
		
	/** Number of words that have been generated from the word gap. */
	public short dot;
	/** The surrounding context. */
	public Context context;
	/** Various components of the inner score. */
	public LogLinearModel.Scores scores;
	/** The inner score. */
	public double inner;
	/** The back pointer. */
	public GapItem back;
	/** The word that has just been generated. */
	public Terminal backWord;

	/**
	 * Creates a word-gap item for the prediction step.
	 * 
	 * @param bc the surrounding context of the word gap.
	 * @param scores contribution of the upcoming gap-filling phrase to the inner score (for the 
	 * WASP<sup>-1</sup>++ model).
	 * @param llm the current log-linear generation model.
	 */
	public GapItem(Item.Context bc, LogLinearModel.Scores scores, LogLinearModel llm) {
		dot = 0;
		this.scores = scores;
		inner = llm.dot(scores);
		back = null;
		backWord = null;
		
		// initialize the context vector
		Context c = new Context();
		short here = bc.gaps[0];
		short prev = prev(bc.context, here);
		short next = next(bc.context, here);
		short from = (short) Math.max(here-N+1, prev+1);
		short to = (short) Math.min(here+N, next);
		c.from = c.to = (short) (here-from);
		c.context = new Terminal[to-from-1];
		for (short i = from; i < here; ++i)
			c.context[i-from] = bc.context[i];
		for (short i = (short) (here+1); i < to; ++i)
			c.context[i-from-1] = bc.context[i];
		context = c;
	}
	
	/**
	 * Creates a word-gap item for the prediction step, with zero contribution to the inner score (for the
	 * WASP<sup>-1</sup>++ model).
	 * 
	 * @param bc the surrounding context of the word gap.
	 * @param llm the current log-linear generation model.
	 */
	public GapItem(Item.Context bc, LogLinearModel llm) {
		this(bc, new LogLinearModel.Scores(), llm);
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
	
	/**
	 * Creates a word-gap item for the scanning step.
	 * 
	 * @param back the back-pointer item.
	 * @param word a word generated from the word gap; part of the gap-filling phrase.
	 * @param lm the current n-gram language model.
	 * @param llm the current log-linear generation model.
	 */
	public GapItem(GapItem back, Terminal word, NgramModel lm, LogLinearModel llm) {
		dot = (short) (back.dot+1);
		scores = new LogLinearModel.Scores(back.scores);
		--scores.wp;
		inner = back.inner - llm.wWP;
		this.back = back;
		backWord = word;
		
		Context c = new Context();
		Context bc = back.context;
		c.from = bc.from;
		c.to = bc.to;
		// update the inner score if there is a new n-gram
		if (c.to >= N-1) {
			for (short i = (short) (c.to-N+1); i < c.to; ++i)
				ngram[i-c.to+N-1] = bc.context[i];
			ngram[N-1] = word;
			double s = lm.score(ngram);
			scores.lm += s;
			inner += llm.wLM*s;
		}
		// update the context vector
		if (c.to-c.from < 2*(N-1)) {
			c.context = (Terminal[]) Arrays.insert(bc.context, c.to, word);
			++c.to;
		} else {  // c.to-c.from == 2*(N-1)
			c.context = (Terminal[]) bc.context.clone();
			for (short i = (short) (c.to-N+1); i < c.to-1; ++i)
				c.context[i] = c.context[i+1];
			c.context[c.to-1] = word;
		}
		context = c;
	}
	
	/**
	 * Creates a word-gap item that marks the end of a gap-filling phrase.
	 * 
	 * @param back the back-pointer item.
	 * @param lm the current n-gram language model.
	 * @param llm the current log-linear generation model.
	 */
	public GapItem(GapItem back, NgramModel lm, LogLinearModel llm) {
		dot = back.dot;
		scores = new LogLinearModel.Scores(back.scores);
		inner = back.inner;
		this.back = back;
		backWord = null;
		
		// update the inner score if there are new n-grams
		Context c = back.context;
		for (short i = c.to; i < c.context.length; ++i)
			if (i >= N-1) {
				for (short j = (short) (i-N+1); j <= i; ++j)
					ngram[j-i+N-1] = c.context[j];
				double s = lm.score(ngram);
				scores.lm += s;
				inner += llm.wLM*s;
			}
		// re-use the context vector
		context = c;
	}
	
	public int compareTo(Object o) {
		if (inner > ((GapItem) o).inner)
			return -1;
		else if (inner < ((GapItem) o).inner)
			return 1;
		else
			return 0;
	}
	
	/**
	 * Indicates if all words in the gap-filling phrase have been spelled out in this item.
	 * 
	 * @return <code>true</code> if the gap-filling phrase is complete; <code>false</code> otherwise.
	 */
	public boolean isComplete() {
		return back != null && backWord == null;
	}
	
	public String toString() {
		return Arrays.toString(context.context);
	}
	
}

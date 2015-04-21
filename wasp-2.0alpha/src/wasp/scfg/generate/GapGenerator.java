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
import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.data.Symbol;
import wasp.main.generate.LogLinearModel;
import wasp.nl.BasicGapModel;
import wasp.nl.GapFiller;
import wasp.nl.NgramModel;

/**
 * The part of the SCFG-based generator that generates words from word gaps.  For efficiency, it only
 * generates previously seen gap fillers.
 * 
 * @author ywwong
 *
 */
public class GapGenerator {

	private static Logger logger = Logger.getLogger(GapGenerator.class.getName());
	static {
		logger.setLevel(Level.FINER);
	}
	
	private BasicGapModel gm;
	private NgramModel lm;
	private LogLinearModel llm;

	/**
	 * Constructs a new word-gap generator based on the given word-gap model, n-gram language model
	 * and log-linear generation model.
	 * 
	 * @param gm the word-gap model which stores the observed gap fillers.
	 * @param lm the n-gram language model for the target NL.
	 * @param llm the log-linear generation model.
	 */
	public GapGenerator(BasicGapModel gm, NgramModel lm, LogLinearModel llm) {
		this.gm = gm;
		this.lm = lm;
		this.llm = llm;
	}
	
	/**
	 * Returns a list of possible gap fillers given the size of the word gap and its surrounding context.
	 * 
	 * @param bc the surrounding context of word gaps.
	 * @param gap the size of this particular word gap.
	 * @param prev the symbol that appears before this particular word gap.
	 * @param next the symbol that appears after this particular word gap.
	 * @return a list of possible gap fillers, in the form of word-gap items that are used in the
	 * completion step of the Earley-chart generator.
	 */
	public GapItem[] generate(Item.Context bc, short gap, Symbol prev, Symbol next) {
		ArrayList list = new ArrayList();
		GapItem item = new GapItem(bc, llm);
		item = new GapItem(item, lm, llm);
		list.add(item);
		for (Iterator it = gm.getFillers(prev, next).iterator(); it.hasNext();) {
			GapFiller f = (GapFiller) it.next();
			if (f.filler.length <= gap) {
				item = new GapItem(bc, f.scores, llm);
				for (short j = 0; j < f.filler.length; ++j)
					item = new GapItem(item, f.filler[j], lm, llm);
				item = new GapItem(item, lm, llm);
				list.add(item);
			}
		}
		return (GapItem[]) list.toArray(new GapItem[0]);
	}
	
}

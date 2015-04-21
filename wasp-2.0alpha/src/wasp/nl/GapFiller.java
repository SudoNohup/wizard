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
package wasp.nl;

import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.main.generate.LogLinearModel;
import wasp.util.Arrays;

/**
 * Phrases that fill word gaps, i.e.&nbsp;<i>gap fillers</i>.
 * 
 * @author ywwong
 *
 */
public class GapFiller {

	/** The actual phrase that fills a word gap. */
	public Terminal[] filler;
	/** The symbol appearing before the gap-filling phrase. */
	public Symbol before;
	/** The symbol appearing after the gap-filling phrase. */
	public Symbol after;
	/** The phrase's component scores in the log-linear generation model. */
	public LogLinearModel.Scores scores;

	/**
	 * Constructs a new gap filler.
	 * 
	 * @param filler the actual phrase that fills a word gap.
	 * @param before the symbol appearing before the gap-filling phrase.
	 * @param after the symbol appearing after the gap-filling phrase.
	 */
	public GapFiller(Terminal[] filler, Symbol before, Symbol after) {
		this.filler = filler;
		this.before = before;
		this.after = after;
		scores = new LogLinearModel.Scores();
	}

	public boolean equals(Object o) {
		return o instanceof GapFiller && Arrays.equal(filler, ((GapFiller) o).filler);
	}

	public int hashCode() {
		return Arrays.hashCode(filler);
	}
	
}
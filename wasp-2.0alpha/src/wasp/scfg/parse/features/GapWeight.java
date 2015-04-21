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
package wasp.scfg.parse.features;

import java.io.IOException;

import wasp.data.Terminal;
import wasp.scfg.SCFGModel;
import wasp.scfg.parse.Item;

/**
 * The feature set based on words generated from word gaps.
 * 
 * @author ywwong
 *
 */
public class GapWeight extends ParseFeature {

	public GapWeight(SCFGModel model) {
		super(model);
	}

	public double weight(Terminal[] E, Item item, Item next) {
		return (item.dot==next.dot) ? gm.getWeight(E[item.current]) : 0;
	}
	
	///
	/// Parameter estimation
	///

	public int countParams() {
		return gm.countParams();
	}

	public double[] getWeightVector() {
		return gm.getWeightVector();
	}
	
	public void setWeightVector(double[] weights) {
		gm.setWeightVector(weights);
	}
	
	public double[] getOuterScores() {
		return gm.getOuterScores();
	}
	
	public void addOuterScore(Terminal[] E, Item item, Item next, double inc) {
		if (item.dot == next.dot)
			gm.addOuterScores(E[item.current], inc+gm.getWeight(E[item.current]));
	}
	
	public void resetOuterScores() {
		gm.resetOuterScores();
	}
	
	///
	/// File I/O
	///
	
	public void read() throws IOException {}

	public void write() throws IOException {}
	
}

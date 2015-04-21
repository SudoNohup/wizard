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
import wasp.nl.BasicGapModel;
import wasp.scfg.SCFG;
import wasp.scfg.SCFGModel;
import wasp.scfg.parse.Item;
import wasp.util.Arrays;

/**
 * The abstract class for feature sets used for semantic parsing.
 * 
 * @author ywwong
 *
 */
public abstract class ParseFeature {

	protected SCFG gram;
	protected BasicGapModel gm;
	protected double[] weights;
	protected double[] outers;

	protected ParseFeature(SCFGModel model) {
		gram = model.gram;
		gm = (BasicGapModel) model.gm;
		weights = null;
		outers = null;
	}
	
	public double weight(Terminal[] E, Item item) {
		return 0;
	}
	
	public double weight(Terminal[] E, Item item, Item next) {
		return 0;
	}
	
	public double weight(Terminal[] E, Item item, Item comp, Item next) {
		return 0;
	}

	///
	/// Parameter estimation
	///
	
	/**
	 * Returns the number of parameters.
	 */
	public abstract int countParams();
	
	/**
	 * Returns the current parameters listed in a fixed (yet unspecified) order.
	 * 
	 * @return the current parameters listed in a fixed order.
	 */
	public double[] getWeightVector() {
		if (weights == null)
			weights = new double[countParams()];
		return weights;
	}
	
	/**
	 * Sets the parameters.
	 * 
	 * @param weights the model parameters listed in a fixed (yet unspecified) order.
	 */
	public void setWeightVector(double[] weights) {
		this.weights = (double[]) weights.clone();
	}
	
	public double[] getOuterScores() {
		return outers;
	}
	
	public void addOuterScore(Terminal[] E, Item next, double inc) {}
	
	public void addOuterScore(Terminal[] E, Item item, Item next, double inc) {}
	
	public void addOuterScore(Terminal[] E, Item item, Item comp, Item next, double inc) {}
	
	public void resetOuterScores() {
		if (outers == null)
			outers = new double[countParams()];
		Arrays.fill(outers, Double.NEGATIVE_INFINITY);
	}
	
	///
	/// File I/O
	///
	
	public abstract void read() throws IOException;
	
	public abstract void write() throws IOException;
	
}

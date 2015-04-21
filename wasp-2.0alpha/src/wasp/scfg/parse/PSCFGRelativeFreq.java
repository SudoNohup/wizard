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

import java.util.Iterator;
import java.util.logging.Logger;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.math.Math;
import wasp.nl.PSCFGGapModel;
import wasp.scfg.Rule;
import wasp.scfg.SCFGModel;
import wasp.util.Double;

/**
 * Code for estimating the parameters of a probabilistic SCFG using relative frequencies.
 * 
 * @author ywwong
 *
 */
public class PSCFGRelativeFreq {

	private static Logger logger = Logger.getLogger(PSCFGRelativeFreq.class.getName());

	private SCFGModel model;
	
	private Examples examples;
	
	public PSCFGRelativeFreq(SCFGModel model) {
		this.model = model;
	}
	
	/**
	 * Estimates the parameters of the PSCFG-based translation model based on relative frequencies.
	 * 
	 * @param examples a set of training examples.
	 */
	public void estimate(Examples examples) {
		logger.info("Estimating the parameters of the PSCFG translation model");
		this.examples = examples;
		estimate();
		logger.info("Parameter estimation of the PSCFG translation model is done");
	}
	
	private void estimate() {
		setWeights();
		// for debugging purpose
		insideOutside();
	}
	
	private void setWeights() {
		normalizeRuleWeights();
		((PSCFGGapModel) model.gm).normalizeWordWeights();
	}
	
	private void normalizeRuleWeights() {
		int nlhs = model.gram.countNonterms();
		for (int i = 0; i < nlhs; ++i) {
			Rule[] rules = model.gram.getRules(i);
			double sum = 0;
			for (int j = 0; j < rules.length; ++j)
				if (rules[j].isActive() && rules[j] == model.gram.tied(rules[j]))
					sum += rules[j].getCount();
			if (sum == 0)
				// shouldn't happen; makes rule weights negative infinity
				sum = Double.POSITIVE_INFINITY;
			for (int j = 0; j < rules.length; ++j)
				if (rules[j].isActive())
					rules[j].setWeight(Math.log(model.gram.tied(rules[j]).getCount()/sum));
		}
	}
	
	private double insideOutside() {
		double prob = 0;
		SCFGParser parser = new SCFGParser(model, true);
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			logger.finest("example "+ex.id);
			double z_E = Double.NEGATIVE_INFINITY;
			double z_EF = Double.NEGATIVE_INFINITY;
			for (Iterator jt = parser.parse(ex.E(), ex.F); jt.hasNext();) {
				SCFGParse parse = (SCFGParse) jt.next();
				z_E = Math.logAdd(z_E, parse.score);
				if (parse.item.cov.isFull())
					z_EF = Math.logAdd(z_EF, parse.score);
			}
			if (z_EF > Double.NEGATIVE_INFINITY) {
				prob += z_EF;
				parser.outside(true);
				logger.fine(ex.id+" "+(z_EF-z_E));
			} else
				logger.fine(ex.id+" X");
		}
		logger.fine("log Pr(E,F) = "+prob);
		return prob;
	}
	
}

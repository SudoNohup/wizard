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
import wasp.scfg.Rule;
import wasp.scfg.SCFGModel;
import wasp.scfg.parse.Item;
import wasp.util.Arrays;
import wasp.util.Double;

/**
 * The feature set based on the weights of SCFG rules.
 * 
 * @author ywwong
 *
 */
public class RuleWeight extends ParseFeature {

	public RuleWeight(SCFGModel model) {
		super(model);
	}

	public double weight(Terminal[] E, Item next) {
		return next.rule.getWeight();
	}
	
	///
	/// Parameter estimation
	///

	public int countParams() {
		return gram.countRules();
	}

	public double[] getWeightVector() {
		int nr = gram.countRules();
		if (weights == null)
			weights = new double[nr];
		Arrays.fill(weights, 0);
		for (int i = 0; i < nr; ++i) {
			Rule rule = gram.getRule(i);
			if (rule.isActive() && rule == gram.tied(rule))
				weights[i] = rule.getWeight();
		}
		return weights;
	}
	
	public void setWeightVector(double[] weights) {
		int nr = gram.countRules();
		for (int i = 0; i < nr; ++i) {
			Rule rule = gram.getRule(i);
			if (rule.isActive())
				rule.setWeight(weights[rule.ruleId]);
		}
	}
	
	public double[] getOuterScores() {
		int nr = gram.countRules();
		if (outers == null)
			outers = new double[nr];
		Arrays.fill(outers, Double.NEGATIVE_INFINITY);
		for (int i = 0; i < nr; ++i) {
			Rule rule = gram.getRule(i);
			if (rule.isActive() && rule == gram.tied(rule))
				outers[i] = rule.getOuterScore();
		}
		return outers;
	}
	
	public void addOuterScore(Terminal[] E, Item next, double inc) {
		gram.tied(next.rule).addOuterScore(inc);
	}
	
	public void resetOuterScores() {
		gram.resetOuterScores();
	}
	
	///
	/// File I/O
	///
	
	public void read() throws IOException {}

	public void write() throws IOException {}
	
}

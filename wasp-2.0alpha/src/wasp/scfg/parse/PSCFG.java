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

import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.data.Node;
import wasp.math.Math;
import wasp.nl.PSCFGGapModel;
import wasp.scfg.Rule;
import wasp.scfg.RuleSymbol;
import wasp.scfg.SCFGModel;
import wasp.util.Arrays;
import wasp.util.Double;

/**
 * Code for estimating the parameters of a probabilistic SCFG using the inside-outside algorithm.
 * 
 * @author ywwong
 *
 */
public class PSCFG {

	private static Logger logger = Logger.getLogger(PSCFG.class.getName());

	private static final boolean DO_VITERBI_APPROX = true;
	
	// for slower (and better) training
	private static final int MAX_ITERATIONS = 5;
	private static final double ABS_TOL = 1e-1;

	// for quicker training
	private static final int QUICK_MAX_ITERATIONS = 3;
	private static final double QUICK_ABS_TOL = 1;
	
	private SCFGModel model;
	
	private Examples examples;
	private int maxIters;
	private double absTol;
	
	public PSCFG(SCFGModel model) {
		this.model = model;
	}
	
	/**
	 * Estimates the parameters of the PSCFG-based translation model such that the joint log-likelihood 
	 * of the specified training examples is maximized.
	 * 
	 * @param examples a set of training examples.
	 * @param full indicates if more time should be spent on training to allow better results.
	 */
	public void estimate(Examples examples, boolean full) {
		logger.info("Estimating the parameters of the PSCFG translation model");
		this.examples = examples;
		if (full) {
			maxIters = MAX_ITERATIONS;
			absTol = ABS_TOL;
		} else {
			maxIters = QUICK_MAX_ITERATIONS;
			absTol = QUICK_ABS_TOL;
		}
		setInitWeights();
		maximize();
		logger.info("Parameter estimation of the PSCFG translation model is done");
	}
	
	private void setInitWeights() {
		int nr = model.gram.countRules();
		for (int i = 0; i < nr; ++i)
			model.gram.getRule(i).setWeight(0);
		normalizeRuleWeights();
		((PSCFGGapModel) model.gm).setInitWeights();
	}
	
	private void normalizeRuleWeights() {
		int nlhs = model.gram.countNonterms();
		for (int i = 0; i < nlhs; ++i) {
			Rule[] rules = model.gram.getRules(i);
			double sum = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < rules.length; ++j)
				if (rules[j].isActive() && rules[j] == model.gram.tied(rules[j]))
					sum = Math.logAdd(sum, rules[j].getWeight());
			for (int j = 0; j < rules.length; ++j)
				if (rules[j].isActive())
					rules[j].setWeight(rules[j].getWeight()-sum);
		}
	}
	
	private void maximize() {
		boolean done = false;
		int iter = 0;
		do {
			double lastProb = insideOutside();
			for (int i = 0; i < maxIters; ++i) {
				logger.info("EM iteration "+(iter++));
				double prob = insideOutside();
				if (prob-lastProb < absTol)
					break;
				lastProb = prob;
			}
			if (!viterbiApprox())
				done = true;
		} while (!done);
	}
	
	private double insideOutside() {
		double[] ruleZ = new double[model.gram.countRules()];
		double[] gapZ = new double[((PSCFGGapModel) model.gm).countParams()];
		Arrays.fill(ruleZ, Double.NEGATIVE_INFINITY);
		// Laplace smoothing
		Arrays.fill(gapZ, 0);
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
				addZ(ruleZ, gapZ, z_EF);
				logger.fine(ex.id+" "+(z_EF-z_E));
			} else
				logger.fine(ex.id+" X");
		}
		logger.fine("log Pr(E,F) = "+prob);
		setWeights(ruleZ, gapZ);
		return prob;
	}
	
	private void addZ(double[] ruleZ, double[] gapZ, double z) {
		int nr = model.gram.countRules();
		for (int i = 0; i < nr; ++i) {
			Rule rule = model.gram.getRule(i);
			if (rule.isActive())
				ruleZ[rule.ruleId] = Math.logAdd(ruleZ[rule.ruleId], rule.getWeight()+rule.getOuterScore()-z);
		}
		double[] weights = ((PSCFGGapModel) model.gm).getWeightVector();
		double[] outers = ((PSCFGGapModel) model.gm).getOuterScores();
		for (int i = 0; i < weights.length; ++i)
			gapZ[i] = Math.logAdd(gapZ[i], weights[i]+outers[i]-z);
	}
	
	private void setWeights(double[] ruleZ, double[] gapZ) {
		for (int i = 0; i < ruleZ.length; ++i) {
			Rule rule = model.gram.getRule(i);
			rule.setWeight(ruleZ[rule.ruleId]);
		}
		normalizeRuleWeights();
		((PSCFGGapModel) model.gm).setWeightVector(gapZ);
	}
	
	private boolean viterbiApprox() {
		if (DO_VITERBI_APPROX) {
			HashSet set = new HashSet();
			SCFGParser parser = new SCFGParser(model, 1);
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = (Example) it.next();
				for (Iterator jt = parser.parse(ex.E(), ex.F); jt.hasNext();) {
					SCFGParse parse = (SCFGParse) jt.next();
					markRules(set, parse.toTree());
				}
			}
			boolean changed = false;
			int nr = model.gram.countRules();
			for (int i = 0; i < nr; ++i) {
				Rule rule = model.gram.getRule(i);
				if (rule.isActive() && !rule.isInit() && !isRuleMarked(set, rule)) {
					rule.deactivate();
					logger.fine("deactivate "+rule);
					changed = true;
				}
			}
			if (changed) {
				normalizeRuleWeights();
				return true;
			}
		}
		return false;
	}
	
	private void markRules(HashSet set, Node node) {
		Rule rule = ((RuleSymbol) node.getSymbol()).getRule();
		set.add(model.gram.tied(rule));
		short nc = node.countChildren();
		for (short i = 0; i < nc; ++i)
			markRules(set, node.getChild(i));
	}
	
	private boolean isRuleMarked(HashSet set, Rule rule) {
		return set.contains(model.gram.tied(rule));
	}
	
}
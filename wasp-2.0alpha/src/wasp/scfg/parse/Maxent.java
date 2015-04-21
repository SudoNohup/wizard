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
import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.data.Node;
import wasp.main.Config;
import wasp.math.LBFGS;
import wasp.math.Math;
import wasp.math.Vectors;
import wasp.scfg.Rule;
import wasp.scfg.RuleSymbol;
import wasp.scfg.SCFGModel;
import wasp.scfg.parse.features.RuleWeight;
import wasp.util.Arrays;
import wasp.util.Double;
import wasp.util.Int;

/**
 * Code for estimating the parameters of a maximum-entropy probabilistic model.
 * 
 * @author ywwong
 *
 */
public class Maxent implements LBFGS.Objective {

	private static Logger logger = Logger.getLogger(Maxent.class.getName());
	static {
		logger.setLevel(Level.FINE);
	}
	
	private static final boolean DO_VITERBI_APPROX = true;
	private static final double PRIOR_VARIANCE_RULE_WEIGHT = 100;
	private static final double PRIOR_VARIANCE_OTHER = 1;
	
	// for slower (and better) training
	private static final int VITERBI_APPROX_ITERATIONS =
		Int.parseInt(Config.get(Config.MAXENT_VITERBI_APPROX_ITERATIONS));
	
	// for quicker training
	private static final int QUICK_VITERBI_APPROX_ITERATIONS = Math.max(VITERBI_APPROX_ITERATIONS/2, 1);
	
	private SCFGModel model;

	private Examples examples;
	private int vaIters;
	private double[] lastX;
	private double lastVal;
	private double[] lastGrad;
	
	public Maxent(SCFGModel model) {
		this.model = model;
	}
	
	/**
	 * Estimates the parameters of the translation model such that the conditional log-likelihood 
	 * of the specified training examples is maximized.
	 * 
	 * @param examples a set of training examples.
	 * @param full indicates if more time should be spent on training to allow better results.
	 */
	public void estimate(Examples examples, boolean full) {
		logger.info("Estimating the parameters of the SCFG translation model");
		this.examples = examples;
		vaIters = (full) ? VITERBI_APPROX_ITERATIONS : QUICK_VITERBI_APPROX_ITERATIONS;
		reset();
		double[] weights = getInitWeightVector();
		new LBFGS().minimize(this, weights, full);
		setWeightVector(weights);
		reset();
		logger.info("Parameter estimation of the SCFG translation model is done");
	}

	private void reset() {
		lastX = null;
		lastVal = Double.NaN;
		lastGrad = null;
	}
	
	/**
	 * Returns the initial parameters of the translation model.  The parameters are listed in a
	 * domain-specific order.
	 * 
	 * @return the initial parameters of the translation model.
	 */
	private double[] getInitWeightVector() {
		int n = 0;
		for (int i = 0; i < model.pf.all.length; ++i)
			n += model.pf.all[i].countParams();
		return new double[n];
	}
	
	public void getValueAndGradient(double[] X, Double val, double[] grad) {
		if (lastX != null && Arrays.equal(lastX, X)) {
			val.val = lastVal;
			Vectors.assign(grad, lastGrad);
			return;
		}
		double[] T_E = new double[X.length];
		double[] T_EF = new double[X.length];
		Arrays.fill(T_E, Double.NEGATIVE_INFINITY);
		Arrays.fill(T_EF, Double.NEGATIVE_INFINITY);
		val.val = 0;
		setWeightVector(X);
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
				val.val += z_E - z_EF;
				parser.outside(false);
				addT(T_E, z_E);
				parser.outside(true);
				addT(T_EF, z_EF);
				logger.fine(ex.id+" "+(z_EF-z_E));
			} else
				logger.fine(ex.id+" X");
		}
		logger.fine("log Pr(F|E) = "+(-val.val));
		for (int index = 0, i = 0; i < model.pf.all.length; ++i) {
			double var = PRIOR_VARIANCE_OTHER;
			if (model.pf.all[i] instanceof RuleWeight)
				var = PRIOR_VARIANCE_RULE_WEIGHT;
			int n = model.pf.all[i].countParams();
			for (int j = index; j < index+n; ++j) {
				val.val += X[j]*X[j]/(2*var);
				grad[j] = Math.exp(T_E[j])-Math.exp(T_EF[j]);
				grad[j] += X[j]/var;
			}
			index += n;
		}
		logger.fine("obj func = "+val);
		logger.fine("norm(G) = "+Vectors.twoNorm(grad));
		lastX = (double[]) X.clone();
		lastVal = val.val;
		lastGrad = (double[]) grad.clone();
	}
	
	/**
	 * Sets the parameters of the translation model to the specified values.
	 * 
	 * @param weights the parameter values to use; parameters are listed in the same order as in the
	 * <code>getWeightVector</code> method.
	 */
	private void setWeightVector(double[] weights) {
		int index = 0;
		for (int i = 0; i < model.pf.all.length; ++i) {
			int n = model.pf.all[i].countParams();
			model.pf.all[i].setWeightVector(Arrays.subarray(weights, index, index+n));
			index += n;
		}
	}
	
	private void addT(double[] T, double z) {
		int index = 0;
		for (int i = 0; i < model.pf.all.length; ++i) {
			int n = model.pf.all[i].countParams();
			double[] weights = model.pf.all[i].getWeightVector();
			double[] outers = model.pf.all[i].getOuterScores();
			for (int j = 0; j < n; ++j)
				if (outers[j] > Double.NEGATIVE_INFINITY)
					T[index+j] = Math.logAdd(T[index+j], weights[j]+outers[j]-z);
			index += n;
		}
	}
	
	public void getX(double[] X) {
		Vectors.assign(X, getWeightVector());
	}
	
	/**
	 * Returns the current parameter vector of the translation model.  The parameters are listed in a
	 * domain-specific order.
	 * 
	 * @return the current parameter vector of the translation model.
	 */
	private double[] getWeightVector() {
		double[] weights = new double[0];
		for (int i = 0; i < model.pf.all.length; ++i)
			weights = Arrays.concat(weights, model.pf.all[i].getWeightVector());
		return weights;
	}
	
	/**
	 * Deactivates rules that are not used in any of the top-ranked parses of the current training
	 * examples.  This is a trick called <i>Viterbi approximation</i>.  Deactivation of rules changes
	 * the objective function being considered, so the underlying <code>LBFGS</code> object has to be
	 * reset.
	 */
	public void check(LBFGS lbfgs, int iter, boolean isLastIter) {
		if (DO_VITERBI_APPROX) {
			if (isLastIter || (iter+1) % vaIters == 0) {
				HashSet set = new HashSet();
				SCFGParser parser = new SCFGParser(model, 1);
				for (Iterator it = examples.iterator(); it.hasNext();) {
					Example ex = (Example) it.next();
					//logger.finest("example "+ex.id);
					for (Iterator jt = parser.parse(ex.E(), ex.F); jt.hasNext();) {
						SCFGParse parse = (SCFGParse) jt.next();
						markRules(set, parse.toTree());
						//logger.finest(parse.toTree().toPrettyString());
					}
				}
				boolean reset = false;
				int nr = model.gram.countRules();
				for (int i = 0; i < nr; ++i) {
					Rule rule = model.gram.getRule(i);
					if (rule.isActive() && !rule.isInit() && !isRuleMarked(set, rule)) {
						rule.deactivate();
						logger.fine("deactivate "+rule);
						reset = true;
					}
				}
				if (reset)
					lbfgs.reset();
			}
		}
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

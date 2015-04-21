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
package wasp.main.generate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.Generator;
import wasp.math.Math;
import wasp.util.Arrays;
import wasp.util.Float;
import wasp.util.Int;

/**
 * The log-linear generation model with minimum error-rate training.
 * 
 * @author ywwong
 *
 */
public class MinErrorRateModel extends LogLinearModel {

	private static Logger logger = Logger.getLogger(MinErrorRateModel.class.getName());
	
	/** The minimum weight to explore for each model component. */
	private static final float[] MIN_WEIGHTS = {
		0f,    // TM
		0.5f,  // PFE
		0.5f,  // PEF
		0.5f,  // PwFE
		0.5f,  // PwEF
		0.5f,  // LM
		-3f    // WP
	};
	/** The maximum weight to explore for each model component. */
	private static final float[] MAX_WEIGHTS = {
		0f,     // TM
		1.5f,   // PFE
		1.5f,   // PEF
		1.5f,   // PwFE
		1.5f,   // PwEF
		1.5f,   // LM
		-1f     // WP
	};
	private static final double PRECISION = 0.0001;
	
	private int NUM_FOLDS;
	private int NUM_GREEDY_SEARCHES;
	private int KBEST;
	
	/**
	 * An interface for potential objective functions.  The objective is <b>maximized</b> during minimum
	 * error-rate training.
	 * 
	 * @author ywwong
	 */
	protected static interface Objective {
		/**
		 * Initializes this objective-function evaluator.
		 * 
		 * @param gold a set of test examples that contain the reference sentences.
		 * @throws IOException if an I/O error occurs.
		 */
		public abstract void init(Examples gold);
		/**
		 * Evaluates the objective function for the generated sentences.
		 * 
		 * @param examples a set of test examples that contain the generated sentences.
		 * @return the value of the objective function.
		 * @throws IOException if an I/O error occurs.
		 */
		public abstract double evaluate(Examples examples) throws IOException;
	}
	
	private static class Segment {
		public LogLinearGen gen;
		public float min;
		public float max;
		public Segment prev;
		public Segment next;
		public Segment(LogLinearGen gen, float min, float max) {
			this.gen = gen;
			this.min = min;
			this.max = max;
			prev = next = null;
		}
	}
	
	private class Partition {
		private Segment first;
		private Segment last;
		private Segment ptr;
		private int index;
		public Partition(int index) {
			first = last = ptr = null;
			this.index = index;
		}
		public boolean add(LogLinearGen gen) {
			if (first == null) {
				first = last = new Segment(gen, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
				logger.finest(intercept(gen)+"+x*"+slope(gen)+" (-inf inf)");
				return true;
			}
			Segment s1, s2, same;
			float x1 = 0, x2 = 0;
			for (s1 = first; s1 != null; s1 = s1.next)
				if (slope(gen) > slope(s1.gen)) {
					x1 = intersect(gen, s1.gen);
					if (s1.min < x1 && x1 < s1.max)
						break;
					if (x1 == s1.max && slope(gen) > slope(s1.next.gen))
						break;
				}
			for (s2 = last; s2 != null; s2 = s2.prev)
				if (slope(gen) < slope(s2.gen)) {
					x2 = intersect(gen, s2.gen);
					if (s2.min < x2 && x2 < s2.max)
						break;
					if (s2.min == x2 && slope(gen) < slope(s2.prev.gen))
						break;
				}
			for (same = first; same != null; same = same.next)
				if (slope(gen) == slope(same.gen))
					break;
			if (s1 == null && s2 != null) {
				first = new Segment(gen, Float.NEGATIVE_INFINITY, x2);
				if (s2.min < x2)
					s2.min = x2;
				first.next = s2;
				s2.prev = first;
				logger.finest(intercept(gen)+"+x*"+slope(gen)+" (-inf "+x2+"]");
				return true;
			} else if (s1 != null && s2 == null) {
				last = new Segment(gen, x1, Float.POSITIVE_INFINITY);
				if (x1 < s1.max)
					s1.max = x1;
				last.prev = s1;
				s1.next = last;
				logger.finest(intercept(gen)+"+x*"+slope(gen)+" ["+x1+" inf)");
				return true;
			} else if (s1 != null && s2 != null) {
				Segment s = new Segment(gen, x1, x2);
				if (x1 < s1.max)
					s1.max = x1;
				s.prev = s1;
				s1.next = s;
				if (s2.min < x2)
					s2.min = x2;
				s.next = s2;
				s2.prev = s;
				logger.finest(intercept(gen)+"+x*"+slope(gen)+" ["+x1+" "+x2+"]");
				return true;
			} else if (first == same && same == last && intercept(gen) > intercept(same.gen)) {
				first = last = new Segment(gen, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
				logger.finest(intercept(gen)+"+x*"+slope(gen)+" (-inf inf)");
				return true;
			}
			logger.finest(intercept(gen)+"+x*"+slope(gen)+" X");
			return false;
		}
		private float slope(LogLinearGen gen) {
			return (float) Math.round(gen.scores.get(index), PRECISION);
		}
		private float intercept(LogLinearGen gen) {
			return (float) Math.round(dotExcept(gen.scores, index), PRECISION);
		}
		private float intersect(LogLinearGen g1, LogLinearGen g2) {
			return (intercept(g2)-intercept(g1)) / (slope(g1)-slope(g2));
		}
		public void resetPtr() {
			ptr = first;
		}
		public Segment ptr() {
			return ptr;
		}
		public void toNext() {
			ptr = ptr.next;
		}
	}
	
	/**
	 * Constructs a new WASP<sup>-1</sup>++ generation model, with the initial model weights read from the
	 * configuration file (via the keys <code>Config.LOG_LINEAR_WEIGHT_*</code>).  If the initial weights
	 * are not specified, then their default values are zero.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public MinErrorRateModel() throws IOException {
		super();
		NUM_FOLDS = Int.parseInt(Config.get(Config.MIN_ERROR_RATE_NUM_FOLDS));
		NUM_GREEDY_SEARCHES = Int.parseInt(Config.get(Config.MIN_ERROR_RATE_NUM_GREEDY));
		KBEST = Int.parseInt(Config.get(Config.MIN_ERROR_RATE_KBEST));
	}
	
	public void train(Examples examples) throws IOException {
		int nfolds = Math.min(examples.size(), NUM_FOLDS);
		Examples[][] splits = examples.crossValidate(nfolds);
		LogLinearModel[] models = new LogLinearModel[nfolds];
		ArrayList[][] kbests = new ArrayList[nfolds][];
		Partition[][] parts = new Partition[nfolds][];
		for (int i = 0; i < nfolds; ++i) {
			models[i] = new LogLinearModel();
			models[i].train(splits[i][0], false);
			models[i].read();
			kbests[i] = new ArrayList[splits[i][1].size()];
			parts[i] = new Partition[splits[i][1].size()];
		}

		float[] lastWeights = null;
		int lastSize = 0;
		for (int iter1 = 0; ; ++iter1) {
			logger.info("Minimum error-rate training iteration ("+iter1+")");

			// compute the k-best generated sentences for all examples
			logger.fine("weights = "+Arrays.toString(getWeights()));
			if (Arrays.equal(getWeights(), lastWeights))
				break;  // from outer loop (iter1)
			int size = 0;
			for (int i = 0; i < nfolds; ++i) {
				models[i].setWeights(getWeights());
				Generator generator = models[i].getGenerator(KBEST);
				for (int j = 0; j < splits[i][1].size(); ++j) {
					Example ex = splits[i][1].getNth(j);
					logger.finer("example "+ex.id);
					ArrayList list = new ArrayList();
					for (Iterator kt = generator.generate(ex.F); kt.hasNext();)
						addGen(list, (LogLinearGen) kt.next());
					if (kbests[i][j] != null)
						for (Iterator kt = kbests[i][j].iterator(); kt.hasNext();)
							addGen(list, (LogLinearGen) kt.next());
					kbests[i][j] = list;
					size += list.size();
				}
			}
			logger.fine("# parses = "+size);
			if (size <= lastSize)
				break;  // from outer loop (iter1)
			lastWeights = getWeights();
			lastSize = size;
			
			// iterate through a number of random starting points
			double bestOverallScore = 0;
			float[] bestOverallWeights = null;
			for (int iter2 = 0; iter2 < NUM_GREEDY_SEARCHES; ++iter2) {
				logger.fine("iteration ("+iter1+","+iter2+")");
				// for iter2 == 0, use the current weights
				if (iter2 > 0)
					for (int i = 0; i < NUM_COMPONENTS; ++i)
						setWeight(i, (float) (MIN_WEIGHTS[i] + Math.random()*(MAX_WEIGHTS[i]-MIN_WEIGHTS[i])));
				logger.fine("initial weights = "+Arrays.toString(getWeights()));
				
				// greedy search
				double bestScore = 0;
				int skipIndex = -1;
				for (int iter3 = 0; ; ++iter3) {
					logger.fine("iteration ("+iter1+","+iter2+","+iter3+")");
					int bestIndex = -1;
					float bestWeight = 0;
					for (int index = 0; index < NUM_COMPONENTS; ++index) {
						if (index == skipIndex)
							continue;

						// create evaluators for the objective function
						Objective[] objs = new Objective[nfolds];
						for (int i = 0; i < nfolds; ++i) {
							objs[i] = createNewObj();
							objs[i].init(splits[i][1]);
						}
						
						// find segments
						for (int i = 0; i < nfolds; ++i)
							for (int j = 0; j < parts[i].length; ++j) {
								parts[i][j] = new Partition(index);
								for (Iterator kt = kbests[i][j].iterator(); kt.hasNext();)
									parts[i][j].add((LogLinearGen) kt.next());
								parts[i][j].resetPtr();
								// add generated sentence
								Example ex = splits[i][1].getNth(j);
								ex.parses.clear();
								if (parts[i][j].ptr() != null)
									ex.parses.add(parts[i][j].ptr().gen);
							}

						// for each segment, compute the objective function
						for (float w = Float.NEGATIVE_INFINITY, nextw = next(parts); !Float.isNaN(nextw);
						toNext(splits, parts, nextw), w = nextw, nextw = next(parts)) {
							float low = w;
							float high = nextw;
							if (low < MIN_WEIGHTS[index])
								low = MIN_WEIGHTS[index];
							if (high > MAX_WEIGHTS[index])
								high = MAX_WEIGHTS[index];
							if (high < low)
								continue;
							float mid = (low+high)/2;
							logger.fine("weight "+index+" = "+mid);
							double score = evaluate(splits, objs);
							logger.fine("score = "+score);
							if (bestScore < score) {
								bestScore = score;
								bestIndex = index;
								bestWeight = mid;
							}
						}
					}
					if (bestIndex < 0)
						break;  // from greedy search (iter3)
					setWeight(bestIndex, bestWeight);
					skipIndex = bestIndex;
					logger.fine("best score = "+bestScore);
					logger.fine("best weights = "+Arrays.toString(getWeights()));
				}
				
				if (bestOverallScore < bestScore) {
					bestOverallScore = bestScore;
					bestOverallWeights = getWeights();
				}
			}  // iter2
			if (bestOverallWeights != null) {
				setWeights(bestOverallWeights);
				logger.fine("best overall score = "+bestOverallScore);
				logger.fine("best overall weights = "+Arrays.toString(bestOverallWeights));
			}
		}  // iter1
		writeWeights();
		logger.info("Full training of component models begins");
		super.train(examples);
	}
	
	private void addGen(ArrayList list, LogLinearGen gen) {
		for (Iterator it = list.iterator(); it.hasNext();) {
			LogLinearGen g = (LogLinearGen) it.next();
			if (g.scores.equals(gen.scores))
				return;
		}
		list.add(gen);
	}

	private Objective createNewObj() {
		String obj = Config.get(Config.MIN_ERROR_RATE_OBJECTIVE);
		if (obj.equals("bleu"))
			return new BLEU();
		else
			return null;
	}
	
	private float next(Partition[][] parts) {
		float next = Float.NaN;
		for (int i = 0; i < parts.length; ++i)
			for (int j = 0; j < parts[i].length; ++j) {
				Segment s = parts[i][j].ptr();
				if (s != null && (Float.isNaN(next) || next > s.max))
					next = s.max;
			}
		return next;
	}
	
	private void toNext(Examples[][] splits, Partition[][] parts, float next) {
		for (int i = 0; i < parts.length; ++i)
			for (int j = 0; j < parts[i].length; ++j) {
				Segment s = parts[i][j].ptr();
				if (s != null && s.max == next) {
					parts[i][j].toNext();
					// add generated sentence
					Example ex = splits[i][1].getNth(j);
					ex.parses.clear();
					if (parts[i][j].ptr() != null)
						ex.parses.add(parts[i][j].ptr().gen);
				}
			}
	}

	private double evaluate(Examples[][] splits, Objective[] objs) throws IOException {
		double val = 0;
		for (int i = 0; i < splits.length; ++i)
			val += objs[i].evaluate(splits[i][1]);
		val /= splits.length;
		return Math.round(val, PRECISION);
	}
	
	public void read() throws IOException {
		super.read();
		readWeights();
	}
	
}

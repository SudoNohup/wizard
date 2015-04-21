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

import wasp.data.Dictionary;
import wasp.data.Terminal;
import wasp.math.Math;
import wasp.util.Arrays;
import wasp.util.Double;

/**
 * This class implements the word-gap model for probabilistic SCFG.  It assigns weights to every word
 * generated from word gaps based on (expected) relative frequencies.  Non-zero weights are assigned to
 * previously unseen words through add-one discounting.
 * 
 * @author ywwong
 *
 */
public class PSCFGGapModel extends BasicGapModel {

	public double getWeight(Terminal word) {
		return getWeight(word.getId());
	}
	
	private double getWeight(int id) {
		return (id>=wordWeights.length || Double.isNaN(wordWeights[id])) ? defWeight : wordWeights[id];
	}
	
	/**
	 * Sets the default word weight based on the current dictionary.
	 */
	public void setInitWeights() {
		int nw = Dictionary.countWords();
		defWeight = -Math.log(nw+1);
		wordWeights = new double[Dictionary.countTerms()];
		Arrays.fill(wordWeights, Double.NaN);
	}
	
	public int countParams() {
		return Dictionary.countWords();
	}
	
	public double[] getWeightVector() {
		double[] weights = new double[Dictionary.countWords()];
		for (int i = 0, j = 0; i < Dictionary.countTerms(); ++i)
			if (Dictionary.isWord(i))
				weights[j++] = getWeight(i);
		return weights;
	}
	
	/**
	 * Sets the parameters of this model.
	 * 
	 * @param weights the <i>unnormalized</i> weights assigned to each word (i.e.&nbsp;their
	 * expected frequencies).
	 */
	public void setWeightVector(double[] weights) {
		double sum = Math.logSum(weights);
		int nw = Dictionary.countWords();
		double denom = Math.logAdd(sum, Math.log(nw+1));
		defWeight = -denom;
		if (wordWeights.length < Dictionary.countTerms())
			wordWeights = new double[Dictionary.countTerms()];
		Arrays.fill(wordWeights, Double.NaN);
		for (int i = 0, j = 0; i < Dictionary.countTerms(); ++i)
			if (Dictionary.isWord(i)) {
				double w = weights[j++];
				w = Math.logAdd(w, 0);  // w+1
				wordWeights[i] = w-denom;
			}
	}
	
	public double[] getOuterScores() {
		double[] outers = new double[Dictionary.countWords()];
		for (int i = 0, j = 0; i < Dictionary.countTerms(); ++i)
			if (Dictionary.isWord(i))
				outers[j++] = (i>=wordOuters.length) ? Double.NEGATIVE_INFINITY : wordOuters[i];
		return outers;
	}
	
	public void addOuterScores(Terminal word, double z) {
		int id = word.getId();
		if (id >= wordOuters.length)
			wordOuters = Arrays.resize(wordOuters, Dictionary.countTerms(), Double.NEGATIVE_INFINITY);
		wordOuters[id] = Math.logAdd(wordOuters[id], z-getWeight(id));
	}
	
	public void resetOuterScores() {
		Arrays.fill(wordOuters, Double.NEGATIVE_INFINITY);
	}
	
	public void normalizeWordWeights() {
		double sum = 0;
		for (int i = 0; i < wordCounts.length; ++i)
			sum += wordCounts[i];
		int nw = Dictionary.countWords();
		double denom = Math.log(sum+nw+1);
		defWeight = -denom;
		if (wordWeights.length < Dictionary.countTerms())
			wordWeights = new double[Dictionary.countTerms()];
		Arrays.fill(wordWeights, Double.NaN);
		for (int i = 0; i < wordCounts.length; ++i)
			if (wordCounts[i] > 0) {
				double w = Math.log(wordCounts[i]+1);
				wordWeights[i] = w-denom;
			}
	}
	
}

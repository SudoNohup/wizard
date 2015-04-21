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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.Generator;
import wasp.main.TranslationModel;
import wasp.nl.NLModel;
import wasp.nl.NgramModel;
import wasp.scfg.SCFGModel;
import wasp.scfg.generate.SCFGGenerator;
import wasp.scfg.lambda.generate.LambdaSCFGGenerator;
import wasp.util.Double;
import wasp.util.FileWriter;
import wasp.util.Float;
import wasp.util.TokenReader;

/**
 * The log-linear generation model (whether with fixed or learned component weights).
 * 
 * @author ywwong
 *
 */
public class LogLinearModel extends GenerateModel {

	/** Component index for the e-to-f translation model. */
	protected static final int TM_INDEX = 0;
	/** Component index for the e-to-f phrase-translation probability. */
	protected static final int PFE_INDEX = 1;
	/** Component index for the f-to-e phrase-translation probability. */
	protected static final int PEF_INDEX = 2;
	/** Component index for the e-to-f lexical weight. */
	protected static final int PwFE_INDEX = 3;
	/** Component index for the f-to-e lexical weight. */
	protected static final int PwEF_INDEX = 4;
	/** Component index for the language model. */
	protected static final int LM_INDEX = 5;
	/** Component index for the word penalty. */
	protected static final int WP_INDEX = 6;
	/** Total number of components in this log-linear model. */
	protected static final int NUM_COMPONENTS = 7;
	
	private TranslationModel tm;
	private NLModel lm;
	
	/** The component scores of a parse. */
	public static class Scores {
		/** The score given by the e-to-f translation model.  This score is unnormalized during chart
		 * generation, and is normalized after an extra reranking step. */
		public double tm;
		/** The score given by the e-to-f phrase-translation probability component. */
		public double PFE;
		/** The score given by the f-to-e phrase-translation probability component. */
		public double PEF;
		/** The score given by the e-to-f lexical weight component. */
		public double PwFE;
		/** The score given by the f-to-e lexical weight component. */
		public double PwEF;
		/** The score given by the language model component. */
		public double lm;
		/** The score given by the word penalty component. */
		public double wp;
		/** All component scores are initially zero. */
		public Scores() {
			tm = PFE = PEF = PwFE = PwEF = lm = wp = 0;
		}
		/** Copies the component scores stored in the specified object. */
		public Scores(Scores s) {
			tm = s.tm;
			PFE = s.PFE;
			PEF = s.PEF;
			PwFE = s.PwFE;
			PwEF = s.PwEF;
			lm = s.lm;
			wp = s.wp;
		}
		public boolean equals(Object o) {
			if (o instanceof Scores) {
				Scores s = (Scores) o;
				return tm == s.tm && PFE == s.PFE && PEF == s.PEF && PwFE == s.PwFE
				&& PwEF == s.PwEF && lm == s.lm && wp == s.wp;
			}
			return false;
		}
		public int hashCode() {
			int hash = 1;
			hash = 31*hash + Double.hashCode(tm);
			hash = 31*hash + Double.hashCode(PFE);
			hash = 31*hash + Double.hashCode(PEF);
			hash = 31*hash + Double.hashCode(PwFE);
			hash = 31*hash + Double.hashCode(PwEF);
			hash = 31*hash + Double.hashCode(lm);
			hash = 31*hash + Double.hashCode(wp);
			return hash;
		}
		/**
		 * Treats this object as a vector and retrieves a component score by the component index.
		 *
		 * @param i the component index.
		 * @return the score of the specified component.
		 */
		public double get(int i) {
			switch (i) {
			case TM_INDEX:
				return tm;
			case PFE_INDEX:
				return PFE;
			case PEF_INDEX:
				return PEF;
			case PwFE_INDEX:
				return PwFE;
			case PwEF_INDEX:
				return PwEF;
			case LM_INDEX:
				return lm;
			case WP_INDEX:
				return wp;
			}
			return 0;
		}
		/**
		 * Returns a new set of component scores that is the sum of the current component scores and the
		 * given set of component scores.
		 * 
		 * @param s the component scores to add.
		 * @return the sum of the current component scores and the given set of component scores.
		 */
		public Scores add(Scores s) {
			Scores sum = new Scores(this);
			sum.tm += s.tm;
			sum.PFE += s.PFE;
			sum.PEF += s.PEF;
			sum.PwFE += s.PwFE;
			sum.PwEF += s.PwEF;
			sum.lm += s.lm;
			sum.wp += s.wp;
			return sum;
		}
	};
	
	/** Component weight for the e-to-f translation model. */
	public float wTM;
	/** Component weight for the e-to-f phrase-translation probability. */
	public float wPFE;
	/** Component weight for the f-to-e phrase-translation probability. */
	public float wPEF;
	/** Component weight for the e-to-f lexical weight component. */
	public float wPwFE;
	/** Component weight for the f-to-e lexical weight component. */
	public float wPwEF;
	/** Component weight for the language model. */
	public float wLM;
	/** Component weight for the word penalty. */
	public float wWP;
	
	/**
	 * Constructs a new log-linear NL generation model, with model weights read from the configuration
	 * file (via the keys <code>Config.LOG_LINEAR_WEIGHT_*</code>).  If the model weights are not
	 * specified, then their default values are zero.
	 * <p>
	 * For the noisy-channel model, the component weights for the e-to-f translation model and the
	 * language model should both be <code>1</code>, while the weights for all other components are zero.
	 * For the WASP<sup>-1</sup>++ model, the e-to-f translation model should have a zero weight, while
	 * other component weights may vary. 
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public LogLinearModel() throws IOException {
		tm = TranslationModel.createNew();
		lm = NLModel.createNew();
		String s = Config.get(Config.LOG_LINEAR_WEIGHT_TM);
		wTM = (s==null) ? 0 : Float.parseFloat(s);
		s = Config.get(Config.LOG_LINEAR_WEIGHT_PHR_PROB_FE);
		wPFE = (s==null) ? 0 : Float.parseFloat(s);
		s = Config.get(Config.LOG_LINEAR_WEIGHT_PHR_PROB_EF);
		wPEF = (s==null) ? 0 : Float.parseFloat(s);
		s = Config.get(Config.LOG_LINEAR_WEIGHT_LEX_WEIGHT_FE);
		wPwFE = (s==null) ? 0 : Float.parseFloat(s);
		s = Config.get(Config.LOG_LINEAR_WEIGHT_LEX_WEIGHT_EF);
		wPwEF = (s==null) ? 0 : Float.parseFloat(Config.get(Config.LOG_LINEAR_WEIGHT_LEX_WEIGHT_EF));
		s = Config.get(Config.LOG_LINEAR_WEIGHT_LM);
		wLM = (s==null) ? 0 : Float.parseFloat(Config.get(Config.LOG_LINEAR_WEIGHT_LM));
		s = Config.get(Config.LOG_LINEAR_WEIGHT_WORD_PENALTY);
		wWP = (s==null) ? 0 : Float.parseFloat(Config.get(Config.LOG_LINEAR_WEIGHT_WORD_PENALTY));
	}

	/**
	 * Returns the number of components in this log-linear NL generation model.
	 * 
	 * @return the number of components in this log-linear NL generation model.
	 */
	public int dim() {
		return NUM_COMPONENTS;
	}
	
	/**
	 * Returns a newly-created array that contains the current component weights.
	 * 
	 * @return a newly-created array that contains the current component weights.
	 */
	public float[] getWeights() {
		float[] weights = new float[NUM_COMPONENTS];
		weights[TM_INDEX] = wTM;
		weights[PFE_INDEX] = wPFE;
		weights[PEF_INDEX] = wPEF;
		weights[PwFE_INDEX] = wPwFE;
		weights[PwEF_INDEX] = wPwEF;
		weights[LM_INDEX] = wLM;
		weights[WP_INDEX] = wWP;
		return weights;
	}
	
	/**
	 * Retrieves a component weight by the component index.
	 * 
	 * @param i the component index.
	 * @return the weight of the specified component.
	 */
	public float getWeight(int i) {
		switch (i) {
		case TM_INDEX:
			return wTM;
		case PFE_INDEX:
			return wPFE;
		case PEF_INDEX:
			return wPEF;
		case PwFE_INDEX:
			return wPwFE;
		case PwEF_INDEX:
			return wPwEF;
		case LM_INDEX:
			return wLM;
		case WP_INDEX:
			return wWP;
		}
		return 0;
	}
	
	/**
	 * Assigns new values to all component weights.
	 * 
	 * @param weights an array that contains the new component weights.
	 */
	public void setWeights(float[] weights) {
		wTM = weights[TM_INDEX];
		wPFE = weights[PFE_INDEX];
		wPEF = weights[PEF_INDEX];
		wPwFE = weights[PwFE_INDEX];
		wPwEF = weights[PwEF_INDEX];
		wLM = weights[LM_INDEX];
		wWP = weights[WP_INDEX];
	}
	
	/**
	 * Assigns a new value to a particular component weight.
	 * 
	 * @param i the component index.
	 * @param weight the new weight of the specified component.
	 */
	public void setWeight(int i, float weight) {
		switch (i) {
		case TM_INDEX:
			wTM = weight;
			break;
		case PFE_INDEX:
			wPFE = weight;
			break;
		case PEF_INDEX:
			wPEF = weight;
			break;
		case PwFE_INDEX:
			wPwFE = weight;
			break;
		case PwEF_INDEX:
			wPwEF = weight;
			break;
		case LM_INDEX:
			wLM = weight;
			break;
		case WP_INDEX:
			wWP = weight;
			break;
		}
	}
	
	/**
	 * Returns the dot product of the current component weights and the given component scores.
	 * 
	 * @param s a set of component scores.
	 * @return the dot product of the current component weights and the given component scores.
	 */
	public double dot(Scores s) {
		return ((wTM==0) ? 0 : wTM*s.tm)
		+ ((wPFE==0) ? 0 : wPFE*s.PFE)
		+ ((wPEF==0) ? 0 : wPEF*s.PEF)
		+ ((wPwFE==0) ? 0 : wPwFE*s.PwFE)
		+ ((wPwEF==0) ? 0 : wPwEF*s.PwEF)
		+ ((wLM==0) ? 0 : wLM*s.lm)
		+ ((wWP==0) ? 0 : wWP*s.wp);
	}
	
	/**
	 * Returns the dot product of the current component weights and the given component scores
	 * <b>except</b> word penalty.
	 * 
	 * @param s a set of component scores.
	 * @return the dot product of the current component weights and the given component scores except
	 * word penalty.
	 */
	public double dotExceptWP(Scores s) {
		return ((wTM==0) ? 0 : wTM*s.tm)
		+ ((wPFE==0) ? 0 : wPFE*s.PFE)
		+ ((wPEF==0) ? 0 : wPEF*s.PEF)
		+ ((wPwFE==0) ? 0 : wPwFE*s.PwFE)
		+ ((wPwEF==0) ? 0 : wPwEF*s.PwEF)
		+ ((wLM==0) ? 0 : wLM*s.lm);
	}
	
	/**
	 * Returns the dot product of the current component weights and the given component scores, but
	 * ignores the contribution of the specified component.
	 * 
	 * @param s a set of component scores.
	 * @param i the index of the component to ignore.
	 * @return the dot product of the current component weights and the given component scores, minus the
	 * contribution of the specified component.
	 */
	public double dotExcept(Scores s, int i) {
		float w = getWeight(i);
		return dot(s) - ((w==0) ? 0 : w*s.get(i));
	}
	
	public Generator getGenerator() throws IOException {
		String mrl = Config.getMRL();
		if (tm instanceof SCFGModel && lm instanceof NgramModel) {
			if (mrl.equals("geo-prolog"))
				return new LambdaSCFGGenerator((SCFGModel) tm, (NgramModel) lm, this);
			else
				return new SCFGGenerator((SCFGModel) tm, (NgramModel) lm, this);
		}
		return null;
	}
	
	/**
	 * Creates and returns a new NL generator, based on the log-linear generation model, which returns a
	 * <i>k</i>-best list of parses rather than a single best parse.
	 * 
	 * @param kbest the maximum number of top-scoring parses that the new generator returns.
	 * @return a new NL generator with the specified <i>k</i>-best setting.
	 * @throws IOException if an I/O error occurs.
	 */
	public Generator getGenerator(int kbest) throws IOException {
		String mrl = Config.getMRL();
		if (tm instanceof SCFGModel && lm instanceof NgramModel) {
			if (mrl.equals("geo-prolog"))
				return new LambdaSCFGGenerator((SCFGModel) tm, (NgramModel) lm, this, kbest);
			else
				return new SCFGGenerator((SCFGModel) tm, (NgramModel) lm, this, kbest);
		}
		return null;
	}

	public void train(Examples examples) throws IOException {
		train(examples, true);
	}

	/**
	 * A version of the <code>train</code> method that allows quicker training.
	 * 
	 * @param examples a set of training examples.
	 * @param full indicates if stricter termination criteria are used when training the translation
	 * model (i.e. longer training time).
	 * @throws IOException if an I/O error occurs.
	 */
	protected void train(Examples examples, boolean full) throws IOException {
		lm.train(examples);
		tm.train(examples, full);
	}
	
	public void read() throws IOException {
		tm.read();
		lm.read();
	}

	private static final String LOG_LINEAR_WEIGHTS = "log-linear-weights";
	
	protected void readWeights() throws IOException {
		File modelFile = new File(Config.getModelDir(), LOG_LINEAR_WEIGHTS);
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(modelFile)));
		String[] line;
		while ((line = in.readLine()) != null) {
			if (line[0].equals("translation-model"))
				wTM = Float.parseFloat(line[1]);
			if (line[0].equals("phr-prob-f|e"))
				wPFE = Float.parseFloat(line[1]);
			if (line[0].equals("phr-prob-e|f"))
				wPEF = Float.parseFloat(line[1]);
			if (line[0].equals("lex-weight-f|e"))
				wPwFE = Float.parseFloat(line[1]);
			if (line[0].equals("lex-weight-e|f"))
				wPwEF = Float.parseFloat(line[1]);
			if (line[0].equals("nl-model"))
				wLM = Float.parseFloat(line[1]);
			if (line[0].equals("word-penalty"))
				wWP = Float.parseFloat(line[1]);
		}
		in.close();
	}
	
	protected void writeWeights() throws IOException {
		File modelFile = new File(Config.getModelDir(), LOG_LINEAR_WEIGHTS);
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(modelFile)));
		if (wTM != 0)
			out.println("translation-model"+wTM);
		if (wPFE != 0)
			out.println("phr-prob-f|e "+wPFE);
		if (wPEF != 0)
			out.println("phr-prob-e|f "+wPEF);
		if (wPwFE != 0)
			out.println("lex-weight-f|e "+wPwFE);
		if (wPwEF != 0)
			out.println("lex-weight-e|f "+wPwEF);
		if (wLM != 0)
			out.println("nl-model "+wLM);
		if (wWP != 0)
			out.println("word-penalty "+wWP);
		out.close();
	}
	
}

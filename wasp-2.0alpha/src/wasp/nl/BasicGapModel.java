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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import wasp.data.Dictionary;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.math.Math;
import wasp.util.Arrays;
import wasp.util.Double;
import wasp.util.Pair;
import wasp.util.Short;
import wasp.util.TokenReader;

/**
 * This class implements the basic word-gap model, which assigns weights to every word
 * generated from word gaps.  The word weights take the following form:
 * <blockquote>
 * weight(<code>word</code>) = w<sub>0</sub> + w<sub><code>word</code></sub>
 * </blockquote>
 * where w<sub>0</sub> is the default word weight and w<sub><code>word</code></sub> is the
 * word-specific weight.
 * 
 * @author ywwong
 *
 */
public class BasicGapModel extends GapModel {

	private static final Logger logger = Logger.getLogger(BasicGapModel.class.getName());
	
	/** Gap fillers that have been observed in training data. */
	protected HashMap fillers;
	/** The default word weight. */
	protected double defWeight;
	/** The word-specific weights. */
	protected double[] wordWeights;

	/** The outer score corresponding to the default weight. */
	protected double defOuter;
	/** The outer scores corresponding to the word-specific weights. */
	protected double[] wordOuters;
	/** The absolute frequencies of words generated from word gaps. */
	protected int[] wordCounts;
	
	public BasicGapModel() {
		fillers = new HashMap();
		defWeight = 0;
		wordWeights = new double[Dictionary.countTerms()];
		Arrays.fill(wordWeights, Double.NaN);
		defOuter = Double.NEGATIVE_INFINITY;
		wordOuters = new double[Dictionary.countTerms()];
		Arrays.fill(wordOuters, Double.NEGATIVE_INFINITY);
		wordCounts = new int[Dictionary.countTerms()];
	}
	
	///
	/// Gap fillers
	///
	
	/**
	 * Returns a list of gap fillers that have been observed in the training data with the 
	 * surrounding context of <code>before</code> and <code>after</code> symbols.  The
	 * returned gap fillers can be of any length.
	 * 
	 * @param before a symbol that precedes the gap filler; it can be a symbol of any type,
	 * including non-terminals.
	 * @param after a symbol that follows the gap filler; it can be a symbol of any type, 
	 * including non-terminals.
	 * @return a list of gap fillers that have been observed in the training data with the
	 * surrounding context of <code>before</code> and <code>after</code>.
	 */
	public ArrayList getFillers(Symbol before, Symbol after) {
		// remove symbol indices if any
		if (before.getIndex() > 0) {
			before = (Symbol) before.copy();
			before.removeIndex();
		}
		if (after.getIndex() > 0) {
			after = (Symbol) after.copy();
			after.removeIndex();
		}
		ArrayList list = (ArrayList) fillers.get(new Pair(before, after));
		return (list==null) ? new ArrayList() : list;
	}
	
	/**
	 * Returns a list of gap fillers that have been observed in the training data, regardless
	 * of surrounding context and filler length.
	 * 
	 * @return a list of gap fillers that have been observed in the training data.
	 */
	public ArrayList getFillers() {
		ArrayList list = new ArrayList();
		for (Iterator it = fillers.values().iterator(); it.hasNext();)
			list.addAll((ArrayList) it.next());
		return list;
	}
	
	/**
	 * Adds an observed gap filler.
	 * 
	 * @param f a gap filler that have been observed in the training data.
	 * @return <code>f</code> if the filler is new; an interned copy of <code>f</code> if one
	 * exists.
	 */
	public GapFiller addFiller(GapFiller f) {
		Pair key = new Pair(f.before, f.after);
		ArrayList list = (ArrayList) fillers.get(key);
		if (list == null) {
			list = new ArrayList();
			fillers.put(key, list);
		}
		for (int i = 0; i < list.size(); ++i)
			if (list.get(i).equals(f))
				return (GapFiller) list.get(i);
		list.add(f);
		return f;
	}
	
	/**
	 * Sets the weights of all observed gap fillers, based on the word weights given by the
	 * word-gap model.
	 */
	public void setFillerWeights() {
		for (Iterator it = fillers.values().iterator(); it.hasNext();) {
			ArrayList list = (ArrayList) it.next();
			for (Iterator jt = list.iterator(); jt.hasNext();) {
				GapFiller f = (GapFiller) jt.next();
				f.scores.tm = 0;
				for (short k = 0; k < f.filler.length; ++k)
					f.scores.tm += getWeight(f.filler[k]);
			}
		}
	}
	
	///
	/// Word weights
	///
	
	/**
	 * Returns the weight assigned to the given word.
	 * 
	 * @param word an NL word.
	 * @return the weight assigned to the <code>word</code> argument.
	 */
	public double getWeight(Terminal word) {
		return defWeight + wordWeight(word.getId());
	}
	
	private double wordWeight(int id) {
		return (id>=wordWeights.length || Double.isNaN(wordWeights[id])) ? 0 : wordWeights[id];
	}
	
	///
	/// Estimation of word weights
	///

	/**
	 * Returns the number of parameters of this model.
	 */
	public int countParams() {
		return Dictionary.countWords()+1;
	}
	
	/**
	 * Returns the current parameters of this model listed in a domain-specific order.
	 * 
	 * @return the current parameters of this model.
	 */
	public double[] getWeightVector() {
		double[] weights = new double[Dictionary.countWords()+1];
		weights[0] = defWeight;
		for (int i = 0, j = 1; i < Dictionary.countTerms(); ++i)
			if (Dictionary.isWord(i))
				weights[j++] = wordWeight(i);
		return weights;
	}
	
	/**
	 * Sets the parameters of this model.
	 * 
	 * @param weights the model parameters, listed in a domain-specific order. 
	 */
	public void setWeightVector(double[] weights) {
		defWeight = weights[0];
		if (wordWeights.length < Dictionary.countTerms())
			wordWeights = new double[Dictionary.countTerms()];
		Arrays.fill(wordWeights, Double.NaN);
		for (int i = 0, j = 1; i < Dictionary.countTerms(); ++i)
			if (Dictionary.isWord(i)) {
				double w = weights[j++];
				if (w != 0)
					wordWeights[i] = w;
			}
	}
	
	public double[] getOuterScores() {
		double[] outers = new double[Dictionary.countWords()+1];
		outers[0] = defOuter;
		for (int i = 0, j = 1; i < Dictionary.countTerms(); ++i)
			if (Dictionary.isWord(i))
				outers[j++] = (i>=wordOuters.length) ? Double.NEGATIVE_INFINITY : wordOuters[i];
		return outers;
	}
	
	public void addOuterScores(Terminal word, double z) {
		defOuter = Math.logAdd(defOuter, z-defWeight);
		int id = word.getId();
		if (id >= wordOuters.length)
			wordOuters = Arrays.resize(wordOuters, Dictionary.countTerms(), Double.NEGATIVE_INFINITY);
		wordOuters[id] = Math.logAdd(wordOuters[id], z-wordWeight(id));
	}
	
	public void resetOuterScores() {
		defOuter = Double.NEGATIVE_INFINITY;
		Arrays.fill(wordOuters, Double.NEGATIVE_INFINITY);
	}
	
	public void addCount(Terminal word) {
		int id = word.getId();
		if (id >= wordCounts.length)
			wordCounts = Arrays.resize(wordCounts, Dictionary.countTerms());
		++wordCounts[id];
	}
	
	///
	/// File I/O
	///
	
	public void read() throws IOException {
		TokenReader in = getReader();
		readBasic(in);
		in.close();
	}
	
	protected void readBasic(TokenReader in) throws IOException {
		Terminal.readWords = true;
		String[] line;
		line = in.readLine();  // begin fillers
		line = in.readLine();
		while (!(line[0].equals("end") && line[1].equals("fillers"))) {
			short i = 0;
			short len = Short.parseShort(line[i++]);
			Symbol before = Symbol.read(line[i++]);
			Terminal[] filler = new Terminal[len];
			for (short j = 0; j < len; ++j)
				filler[j] = (Terminal) Terminal.read(line[i++]);
			Symbol after = Symbol.read(line[i++]);
			GapFiller f = new GapFiller(filler, before, after);
			if (i < line.length && line[i].equals("weight")) {
				f.scores.tm = Double.parseDouble(line[i+1]);
				i += 2;
			}
			if (i < line.length && line[i].equals("phr-prob-f|e")) {
				f.scores.PFE = Double.parseDouble(line[i+1]);
				i += 2;
			}
			if (i < line.length && line[i].equals("phr-prob-e|f")) {
				f.scores.PEF = Double.parseDouble(line[i+1]);
				i += 2;
			}
			if (i < line.length && line[i].equals("lex-weight-f|e")) {
				f.scores.PwFE = Double.parseDouble(line[i+1]);
				i += 2;
			}
			if (i < line.length && line[i].equals("lex-weight-e|f")) {
				f.scores.PwEF = Double.parseDouble(line[i+1]);
				i += 2;
			}
			addFiller(f);
			line = in.readLine();
		}
		line = in.readLine();  // begin default-weight
		line = in.readLine();
		if (!(line[0].equals("end") && line[1].equals("default-weight"))) {
			defWeight = Double.parseDouble(line[0]);
			line = in.readLine();
		}
		line = in.readLine();  // begin word-weights
		wordWeights = new double[Dictionary.countTerms()];
		Arrays.fill(wordWeights, Double.NaN);
		line = in.readLine();
		while (!(line[0].equals("end") && line[1].equals("word-weights"))) {
			int id = Terminal.read(line[0]).getId();
			if (id < wordWeights.length)
				wordWeights[id] = Double.parseDouble(line[1]);
			else
				// TODO shouldn't have happened
				logger.warning("Word '"+line[0]+"' skipped: adjust charset settings?");
			line = in.readLine();
		}
	}
	
	public void write() throws IOException {
		PrintWriter out = getWriter();
		writeBasic(out);
		out.close();
	}
	
	protected void writeBasic(PrintWriter out) throws IOException {
		out.println("begin fillers");
		for (Iterator it = fillers.values().iterator(); it.hasNext();) {
			ArrayList list = (ArrayList) it.next();
			for (Iterator jt = list.iterator(); jt.hasNext();) {
				GapFiller f = (GapFiller) jt.next();
				out.print(f.filler.length);
				out.print(' ');
				out.print(f.before);
				for (short k = 0; k < f.filler.length; ++k) {
					out.print(' ');
					out.print(f.filler[k]);
				}
				out.print(' ');
				out.print(f.after);
				if (f.scores.tm != 0) {
					out.print(" weight ");
					out.print(f.scores.tm);
				}
				if (f.scores.PFE != 0) {
					out.print(" phr-prob-f|e ");
					out.print(f.scores.PFE);
				}
				if (f.scores.PEF != 0) {
					out.print(" phr-prob-e|f ");
					out.print(f.scores.PEF);
				}
				if (f.scores.PwFE != 0) {
					out.print(" lex-weight-f|e ");
					out.print(f.scores.PwFE);
				}
				if (f.scores.PwEF != 0) {
					out.print(" lex-weight-e|f ");
					out.print(f.scores.PwEF);
				}
				out.println();
			}
		}
		out.println("end fillers");
		out.println("begin default-weight");
		if (defWeight != 0)
			out.println(defWeight);
		out.println("end default-weight");
		out.println("begin word-weights");
		for (int i = 0; i < wordWeights.length; ++i)
			if (!Double.isNaN(wordWeights[i]) && wordWeights[i] != 0) {
				Terminal word = new Terminal(i);
				out.print(word);
				out.print(' ');
				out.println(wordWeights[i]);
			}
		out.println("end word-weights");
	}
	
}

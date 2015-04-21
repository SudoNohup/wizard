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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import wasp.data.Dictionary;
import wasp.data.Terminal;
import wasp.main.Config;
import wasp.math.Math;
import wasp.util.Arrays;
import wasp.util.Float;
import wasp.util.Int;
import wasp.util.Numberer;
import wasp.util.TokenReader;

/**
 * The abstract class for the n-gram NL language model.  It contains code for reading n-gram models
 * written in the ARPA format and for computing n-gram scores, but it does not contain any training code.
 * 
 * @author ywwong
 *
 */
public abstract class NgramModel extends NLModel {

	private static class Ngram {
		public int word;
		public float prob;
		public float alpha;
		public int bound;
		public Ngram(float prob, float alpha) {
			this.prob = prob;
			this.alpha = alpha;
		}
		public Ngram(float prob) {
			this.prob = prob;
		}
		public Ngram(int word, float prob, float alpha) {
			this.word = word;
			this.prob = prob;
			this.alpha = alpha;
		}
		public Ngram(int word, float prob) {
			this.word = word;
			this.prob = prob;
		}
	}

	private Numberer vocab;
	private int[] toVocab;
	private String unkStr;
	private String sentBeginStr;
	private String sentEndStr;
	private int unk;
	private int sentBegin;
	private int sentEnd;
	private Ngram[][] ngrams;
	
	protected NgramModel(String unkStr, String sentBeginStr, String sentEndStr) {
		this.unkStr = unkStr;
		this.sentBeginStr = sentBeginStr;
		this.sentEndStr = sentEndStr;
		ngrams = new Ngram[0][];
	}
	
	/**
	 * Returns the score of the given n-gram.
	 * 
	 * @param T an n-gram consisting of terminal symbols.
	 * @return the score of the given n-gram.
	 */
	public float score(Terminal[] T) {
		return score(T, (short) 0);
	}
	
	private float score(Terminal[] T, short beg) {
		int w = word(T, beg);
		return score(T, beg, (short) (beg+1), w);
	}

	private int word(Terminal[] T, short i) {
		if (i == 0)
			return (T[0].isBoundary()) ? sentBegin : word(T[0]);
		else if (i < T.length-1)
			return word(T[i]);
		else
			return (T[i].isBoundary()) ? sentEnd : word(T[i]);
	}
	
	private int word(Terminal t) {
		int tid = t.getId();
		return (tid < toVocab.length && toVocab[tid] >= 0) ? toVocab[tid] : unk;
	}
	
	private float score(Terminal[] T, short beg, short cur, int index) {
		short n = (short) (cur-beg);
		if (cur == T.length) {
			//logger.finer(Arrays.toString(T)+":"+beg+" = "+ngrams[n-1][index].prob);
			return ngrams[n-1][index].prob;
		}
		int w = word(T, cur);
		int next = find(w, n, ((index==0) ? 0 : ngrams[n-1][index-1].bound), ngrams[n-1][index].bound);
		if (next >= 0)
			return score(T, beg, (short) (cur+1), next);
		else if (cur == T.length-1) {
			//logger.finer(Arrays.toString(T)+":"+beg+" = "+Arrays.toString(T)+":"+(beg+1)+" + "+ngrams[n-1][index].alpha);
			return score(T, (short) (beg+1)) + ngrams[n-1][index].alpha;
		}
		else {
			//logger.finer(Arrays.toString(T)+":"+beg+" = "+Arrays.toString(T)+":"+(beg+1));
			return score(T, (short) (beg+1));
		}
	}
	
	private int find(int word, short i, int from, int to) {
		while (from < to) {
			int mid = from + (to-from)/2;
			if (ngrams[i][mid].word == word)
				return mid;
			else if (ngrams[i][mid].word > word)
				to = mid;
			else
				from = mid+1;
		}
		return -1;
	}
	
	protected static final String NGRAM_MODEL = "ngram-model.arpa.gz";
	
	/**
	 * Returns the pathname of the n-gram model file.
	 * 
	 * @return the pathname of the n-gram model file.
	 */
	public File getModelFile() {
		return new File(Config.getModelDir(), NGRAM_MODEL);
	}

	public void read() throws IOException {
		File modelFile = new File(Config.getModelDir(), NGRAM_MODEL);
		TokenReader in = new TokenReader(new BufferedReader(new InputStreamReader(new GZIPInputStream
				(new FileInputStream(modelFile)))));
		vocab = new Numberer();
		toVocab = new int[Dictionary.countTerms()];
		Arrays.fill(toVocab, -1);
		boolean isData = false;
		short n = 1;
		String[] line;
		while ((line = in.readLine()) != null) {
			if (line.length == 0)
				continue;
			if (line[0].equals("\\data\\"))
				isData = true;
			else if (!isData)
				continue;
			if (line[0].equals("ngram")) {
				int count = Int.parseInt(line[1].substring(2));
				ngrams = (Ngram[][]) Arrays.append(ngrams, new Ngram[count]);
			} else if (line[0].matches("\\\\(\\d+)-grams:")) {
				if (n == 1) {
					for (int i = 0; i < ngrams[0].length; ++i) {
						line = in.readLine();
						vocab.addObj(line[1]);
						if (line[1].equals(unkStr))
							unk = i;
						else if (line[1].equals(sentBeginStr))
							sentBegin = i;
						else if (line[1].equals(sentEndStr))
							sentEnd = i;
						else {
							int id = Dictionary.term(line[1], true, false);
							if (id >= 0)
								toVocab[id] = i;
						}
						float prob = baseE(Float.parseFloat(line[0]));
						if (line.length == 3) {
							float alpha = baseE(Float.parseFloat(line[2]));
							ngrams[0][i] = new Ngram(prob, alpha);
						} else
							ngrams[0][i] = new Ngram(prob);
					}
				} else {
					int index = 0;
					int[] lastContext = new int[n-1];
					int[] context = new int[n-1];
					for (int i = 0; i < ngrams[n-1].length; ++i) {
						line = in.readLine();
						for (short j = 0; j < n-1; ++j)
							context[j] = vocab.getId(line[j+1], false);
						int word = vocab.getId(line[n], false);
						float prob = baseE(Float.parseFloat(line[0]));
						if (n < ngrams.length && line.length == n+2) {
							float alpha = baseE(Float.parseFloat(line[n+1]));
							ngrams[n-1][i] = new Ngram(word, prob, alpha);
						} else
							ngrams[n-1][i] = new Ngram(word, prob);
						if (!Arrays.equal(lastContext, context)) {
							int next = context[0];
							for (short j = 1; j < n-1; ++j)
								next = find(context[j], j, ((next==0) ? 0 : ngrams[j-1][next-1].bound),
										ngrams[j-1][next].bound);
							for (; index < next; ++index)
								ngrams[n-2][index].bound = i;
							Arrays.set(lastContext, context);
						}
					}
					for (; index < ngrams[n-2].length; ++index)
						ngrams[n-2][index].bound = ngrams[n-1].length;
				}
				++n;
			}
		}
		in.close();
	}

	private static float baseE(float x) {
		return (float) (x*Math.log(10));
	}
	
}

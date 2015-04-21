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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.data.Terminal;
import wasp.main.Config;
import wasp.main.Parse;
import wasp.math.Math;
import wasp.util.Arrays;
import wasp.util.Bool;
import wasp.util.RadixMap;
import wasp.util.Short;

/**
 * My own implementation of the BLEU metric.  This implementation is fast enough to be used in
 * maximum-BLEU training.
 * 
 * @author ywwong
 *
 */
public class BLEU extends Evaluator implements MinErrorRateModel.Objective {
	
	private static Logger logger = Logger.getLogger(BLEU.class.getName());

	/** Indicates whether to use multiple reference sentences for each example.  Normally each example has
	 * only one reference sentence (for each NL).  But it is possible to use other sentences as reference
	 * sentencnes as long as they are mapped to the same MR.  In any case, only sentences taken from the
	 * test set are used as reference sentences (to avoid inflating the BLEU score). */
	private static final boolean DO_MULTI_REF = false;
	/** The maximum length of n-grams being considered when calculating the BLEU score. */
	private static final short MAX_N = 4;
	
	/** Indicates whether to ignore examples for which NL generation have not been successful
	 * (i.e.&nbsp;no sentences have been generated.). */
	private boolean IGNORE_EMPTY;
	
	/** Data structure for n-grams. */
	private static class Ngram {
		private Terminal[] ngram;
		public Ngram(Terminal[] E, short from, short len) {
			ngram = new Terminal[len];
			for (short i = 0; i < len; ++i)
				ngram[i] = E[from+i];
		}
		public boolean equals(Object o) {
			if (o instanceof Ngram) {
				Ngram n = (Ngram) o;
				if (ngram.length != n.ngram.length)
					return false;
				for (short i = 0; i < ngram.length; ++i)
					if (!ngram[i].equals(n.ngram[i]))
						return false;
				return true;
			}
			return false;
		}
		public int hashCode() {
			int hash = 1;
			for (short i = 0; i < ngram.length; ++i)
				hash = 31*hash + ngram[i].hashCode();
			return hash;
		}
	}
	
	private RadixMap refNgrams;
	private RadixMap refLengths;
	private int refLength;
	private RadixMap tstExamples;
	private int[] correctNgrams;
	private int[] totalNgrams;
	private int tstLength;

	public BLEU() {
		IGNORE_EMPTY = Bool.parseBool(Config.get(Config.MTEVAL_IGNORE_EMPTY));
	}
	
	public void init(Examples gold) {
		init(gold, gold);
	}
	
	private void init(Examples gold, Examples examples) {
		RadixMap refs = extractRefs(gold, examples);
		processRefs(examples, refs);
		tstExamples = new RadixMap();
		correctNgrams = new int[MAX_N];
		totalNgrams = new int[MAX_N];
		tstLength = 0;
	}
	
	/**
	 * Finds all reference translations given a set of test MRs.  If <code>DO_MULTI_REF</code> is
	 * <code>false</code>, then each test MR has exactly one reference translation, i.e. the
	 * corresponding NL sentence in the test example.  If <code>DO_MULTI_REF</code> is <code>true</code>,
	 * then a test MR may have multiple reference translations, i.e. all sentences that are mapped to the
	 * given MR.
	 * 
	 * @param gold the gold standard to compare against.
	 * @param examples the test examples to evaluate.
	 * @return a mapping from test example IDs to reference translations (represented as string arrays).
	 */
	private RadixMap extractRefs(Examples gold, Examples examples) {
		if (DO_MULTI_REF) {
			HashMap map = new HashMap();
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = gold.get(((Example) it.next()).id);
				String F = normalize(ex.F);
				ArrayList list = (ArrayList) map.get(F);
				if (list == null) {
					list = new ArrayList();
					map.put(F, list);
				}
				list.add(Arrays.subarray(ex.E(), 1, ex.E().length-1));
			}
			RadixMap refs = new RadixMap();
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = gold.get(((Example) it.next()).id);
				String F = normalize(ex.F);
				refs.put(ex.id, map.get(F));
			}
			return refs;
		} else {
			RadixMap refs = new RadixMap();
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = gold.get(((Example) it.next()).id);
				ArrayList list = new ArrayList();
				list.add(Arrays.subarray(ex.E(), 1, ex.E().length-1));
				refs.put(ex.id, list);
			}
			return refs;
		}
	}
	
	private void processRefs(Examples examples, RadixMap refs) {
		refNgrams = new RadixMap();
		refLengths = new RadixMap();
		refLength = 0;
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			ArrayList list = (ArrayList) refs.get(ex.id);
			HashMap[] ngrams = new HashMap[MAX_N];
			short len = Short.MAX_VALUE;
			for (Iterator jt = list.iterator(); jt.hasNext();) {
				Terminal[] E = (Terminal[]) jt.next();
				for (short k = 0; k < MAX_N; ++k) {
					HashMap map = extractNgrams(E, (short) (k+1));
					if (ngrams[k] == null)
						ngrams[k] = map;
					else
						for (Iterator lt = map.entrySet().iterator(); lt.hasNext();) {
							Map.Entry entry = (Map.Entry) lt.next();
							Ngram ngram = (Ngram) entry.getKey();
							Short c1 = (Short) entry.getValue();
							Short c2 = (Short) ngrams[k].get(ngram);
							if (c2 == null)
								ngrams[k].put(ngram, c1);
							else if (c2.val < c1.val)
								c2.val = c1.val;
						}
				}
				if (len > E.length)
					len = (short) E.length;
			}
			refNgrams.put(ex.id, ngrams);
			refLengths.put(ex.id, new Short(len));
			refLength += len;
		}
	}
	
	private HashMap extractNgrams(Terminal[] E, short n) {
		HashMap map = new HashMap();
		for (short i = 0; i <= E.length-n; ++i) {
			Ngram ngram = new Ngram(E, i, n);
			Short count = (Short) map.get(ngram);
			if (count == null)
				map.put(ngram, new Short(1));
			else
				++count.val;
		}
		return map;
	}
	
	public double evaluate(Examples examples) {
		int ref = refLength;
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex1 = (Example) it.next();
			Parse r1 = (ex1.parses.isEmpty()) ? null : (Parse) ex1.parses.get(0);
			if (IGNORE_EMPTY && r1 == null)
				ref -= ((Short) refLengths.get(ex1.id)).val;
			Example ex2 = (Example) tstExamples.get(ex1.id);
			Parse r2 = (ex2 == null || ex2.parses.isEmpty()) ? null : (Parse) ex2.parses.get(0);
			if (r1 != r2) {
				HashMap[] ngrams = (HashMap[]) refNgrams.get(ex1.id);
				// plus ex1
				Terminal[] E1 = (r1==null) ? new Terminal[0] : tokenize(r1.toStr());
				for (short j = 0; j < MAX_N; ++j) {
					HashMap n1 = extractNgrams(E1, (short) (j+1));
					for (Iterator kt = n1.entrySet().iterator(); kt.hasNext();) {
						Map.Entry entry = (Map.Entry) kt.next();
						Ngram n = (Ngram) entry.getKey();
						Short c = (Short) entry.getValue();
						Short correct = (Short) ngrams[j].get(n);
						if (correct != null)
							correctNgrams[j] += Math.min(c.val, correct.val);
						totalNgrams[j] += c.val;
					}
				}
				tstLength += E1.length;
				// minus ex2
				Terminal[] E2 = (r2==null) ? new Terminal[0] : tokenize(r2.toStr());
				for (short j = 0; j < MAX_N; ++j) {
					HashMap n2 = extractNgrams(E2, (short) (j+1));
					for (Iterator kt = n2.entrySet().iterator(); kt.hasNext();) {
						Map.Entry entry = (Map.Entry) kt.next();
						Ngram n = (Ngram) entry.getKey();
						Short c = (Short) entry.getValue();
						Short correct = (Short) ngrams[j].get(n);
						if (correct != null)
							correctNgrams[j] -= Math.min(c.val, correct.val);
						totalNgrams[j] -= c.val;
					}
				}
				tstLength -= E2.length;
				// add example
				tstExamples.put(ex1.id, ex1.copy());
			}
		}
		double brevity = (tstLength < ref) ? Math.exp(1-((double) ref)/tstLength) : 1;
		logger.finest("brevity penalty = "+brevity+" "+tstLength+" "+ref);
		double precision = 0;
		logger.finest("individual n-gram scoring");
		for (short i = 0; i < MAX_N; ++i) {
			precision += Math.log(((double) correctNgrams[i])/totalNgrams[i]);
			logger.finest((i+1)+"-gram = "+(((double) correctNgrams[i])/totalNgrams[i])+" "
					+correctNgrams[i]+" "+totalNgrams[i]);
		}
		return brevity * Math.exp(precision/MAX_N);
	}

	private Terminal[] tokenize(String str) {
		Terminal.readWords = true;
		String[] tokens = Arrays.tokenize(str);
		Terminal[] E = new Terminal[tokens.length];
		for (short i = 0; i < tokens.length; ++i)
			E[i] = (Terminal) Terminal.read(tokens[i]);
		return E;
	}
	
	protected void evaluate(PrintWriter out, Examples gold, Examples[] examples) throws IOException {
		double[] bleu = new double[examples.length];
		for (int i = 0; i < examples.length; ++i) {
			init(gold, examples[i]);
			bleu[i] = evaluate(examples[i]);
		}
		out.println("begin bleu");
		out.println("mean "+Math.mean(bleu));
		double[] interval = Math.confInterval95(bleu);
		out.println("95%-confidence-interval "+interval[0]+" "+interval[1]);
	}
	
}

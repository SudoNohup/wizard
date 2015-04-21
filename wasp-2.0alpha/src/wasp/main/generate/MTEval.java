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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.main.Config;
import wasp.main.Parse;
import wasp.math.Math;
import wasp.util.Bool;
import wasp.util.Double;
import wasp.util.InputStreamTee;
import wasp.util.InputStreamWriter;
import wasp.util.RadixMap;
import wasp.util.TokenReader;

/**
 * This evaluator computes the BLEU and NIST scores using the <code>MTEval</code> utility developed by
 * NIST (<a href="http://www.nist.gov/speech/tests/mt/resources/scoring.htm" target="_new">http://www.nist.gov/speech/tests/mt/resources/scoring.htm</a>).
 * This evaluator is very slow, and is not suitable for use in minimum error-rate training, but it
 * provides the "gold standard" for automatic evaluation metrics.
 * 
 * @author ywwong
 *
 */
public class MTEval extends Evaluator {

	private static Logger logger = Logger.getLogger(MTEval.class.getName());
	
	private static class Scores {
		public double nist;
		public double bleu;
	}
	
	/** Indicates whether to use multiple reference sentences for each example.  Normally each example has
	 * only one reference sentence (for each NL).  But it is possible to use other sentences as reference
	 * sentences as long as they are mapped to the same MR.  In any case, only sentences taken from the
	 * test set are used as reference sentences (to avoid inflating the scores).  Also note that using
	 * multiple reference sentences will lead to <b>wrong</b> NIST scores, due to a restriction in the
	 * <code>MTEval</code> utility. */
	private static final boolean DO_MULTI_REF = false;

	/** Indicates whether to ignore examples for which NL generation have not been successful
	 * (i.e.&nbsp;no sentences have been generated.). */
	private boolean IGNORE_EMPTY;

	public MTEval() {
		IGNORE_EMPTY = Bool.parseBool(Config.get(Config.MTEVAL_IGNORE_EMPTY));
	}
	
	protected void evaluate(PrintWriter out, Examples gold, Examples[] examples) throws IOException {
		double[] nist = new double[examples.length];
		double[] bleu = new double[examples.length];
		RadixMap srcs = extractSrcs(gold);
		for (int i = 0; i < examples.length; ++i) {
			RadixMap refs = extractRefs(gold, examples[i]);
			evaluate(refs, srcs, examples, nist, bleu, i);
		}
		out.println("begin nist");
		out.println("mean "+Math.mean(nist));
		double[] interval = Math.confInterval95(nist);
		out.println("95%-confidence-interval "+interval[0]+" "+interval[1]);
		out.println("end nist");
		out.println("begin bleu");
		out.println("mean "+Math.mean(bleu));
		interval = Math.confInterval95(bleu);
		out.println("95%-confidence-interval "+interval[0]+" "+interval[1]);
		out.println("end bleu");
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
	private static RadixMap extractRefs(Examples gold, Examples examples) {
		if (DO_MULTI_REF) {
			HashMap map = new HashMap();
			int nrefs = 0;
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = gold.get(((Example) it.next()).id);
				String F = normalize(ex.F);
				String E = normalize(ex.E());
				ArrayList list = (ArrayList) map.get(F);
				if (list == null) {
					list = new ArrayList();
					map.put(F, list);
				}
				list.add(E);
				if (nrefs < list.size())
					nrefs = list.size();
			}
			RadixMap refs = new RadixMap();
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = gold.get(((Example) it.next()).id);
				String F = normalize(ex.F);
				ArrayList list = (ArrayList) map.get(F);
				// The mteval evaluation script requires every input sentence to have the same number of
				// reference translations.  Since in the Geoquery and RoboCup corpora, some MRs have fewer
				// reference translations than others, certain reference translations are repeated to make
				// the evaluation script run.  While this repetition does not affect the BLEU score at all,
				// it does affect the NIST score!!
				String[] array = new String[nrefs];
				for (int j = 0; j < nrefs; ++j)
					array[j] = (String) list.get(j % list.size());
				refs.put(ex.id, array);
			}
			return refs;
		} else {
			RadixMap refs = new RadixMap();
			for (Iterator it = examples.iterator(); it.hasNext();) {
				Example ex = gold.get(((Example) it.next()).id);
				String E = normalize(ex.E());
				String[] array = new String[1];
				array[0] = E;
				refs.put(ex.id, array);
			}
			return refs;
		}
	}
	
	private static RadixMap extractSrcs(Examples gold) {
		RadixMap srcs = new RadixMap();
		for (Iterator it = gold.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			srcs.put(ex.id, normalize(ex.F));
		}
		return srcs;
	}
	
	private static final String PREFIX = "mteval";

	private void evaluate(RadixMap refs, RadixMap srcs, Examples[] examples, double[] nist, double[] bleu,
			int i) throws IOException {
		File refFile = File.createTempFile(PREFIX, ".ref.sgml");
		File srcFile = File.createTempFile(PREFIX, ".src.sgml");
		File tstFile = File.createTempFile(PREFIX, ".tst.sgml");
		File outputFile = File.createTempFile(PREFIX, ".txt");
		refFile.deleteOnExit();
		srcFile.deleteOnExit();
		tstFile.deleteOnExit();
		outputFile.deleteOnExit();
		boolean doEval = writeMTEvalInput(refs, srcs, examples[i], refFile, srcFile, tstFile);
		if (doEval) {
			runMTEval(refFile, srcFile, tstFile, outputFile);
			Scores scores = readMTEvalOutput(outputFile);
			nist[i] = scores.nist;
			bleu[i] = scores.bleu;
		} else
			nist[i] = bleu[i] = 0;
	}
	
	private boolean writeMTEvalInput(RadixMap refs, RadixMap srcs, Examples examples, File refFile,
			File srcFile, File tstFile) throws IOException {
		PrintWriter ref = new PrintWriter(new BufferedWriter(new FileWriter(refFile)));
		PrintWriter src = new PrintWriter(new BufferedWriter(new FileWriter(srcFile)));
		PrintWriter tst = new PrintWriter(new BufferedWriter(new FileWriter(tstFile)));
		ref.println("<refset setid=\"refset\" srclang=\""+Config.getMRL()+"\" trglang=\""+Config.getTargetNL()+"\">");
		src.println("<srcset setid=\"tstset\" srclang=\""+Config.getMRL()+"\">");
		tst.println("<tstset setid=\"tstset\" srclang=\""+Config.getMRL()+"\" trglang=\""+Config.getTargetNL()+"\">");
		int ntst = 0;
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			if (IGNORE_EMPTY && ex.getSortedParses().length == 0)
				continue;
			String[] array = (String[]) refs.get(ex.id);
			for (int j = 0; j < array.length; ++j) {
				ref.println("<doc docid=\"doc"+ex.id+"\" sysid=\"sys"+j+"\">");
				ref.println("<seg>");
				ref.println(array[j]);
				ref.println("</seg>");
				ref.println("</doc>");
			}
			src.println("<doc docid=\"doc"+ex.id+"\">");
			src.println("<seg>");
			src.println(srcs.get(ex.id));
			src.println("</seg>");
			src.println("</doc>");
			tst.println("<doc docid=\"doc"+ex.id+"\" sysid=\"wasp\">");
			tst.println("<seg>");
			Parse[] parses = ex.getSortedParses();
			if (parses.length > 0) {
				tst.println(normalize(parses[0].toStr()));
				++ntst;
			}
			tst.println("</seg>");
			tst.println("</doc>");
		}
		ref.println("</refset>");
		src.println("</srcset>");
		tst.println("</tstset>");
		ref.close();
		src.close();
		tst.close();
		return ntst > 0;
	}
	
	private void runMTEval(File refFile, File srcFile, File tstFile, File outputFile) throws IOException {
		String[] cmd = new String[7];
		cmd[0] = Config.get(Config.MTEVAL_EXEC);
		cmd[1] = "-r";
		cmd[2] = refFile.getPath();
		cmd[3] = "-s";
		cmd[4] = srcFile.getPath();
		cmd[5] = "-t";
		cmd[6] = tstFile.getPath();
        try {
    		logger.info("MTEval starts");
            Process proc = Runtime.getRuntime().exec(cmd);
            Thread outThread = new InputStreamTee(proc.getInputStream(), System.err, outputFile);
            Thread errThread = new InputStreamWriter(proc.getErrorStream(), System.err);
            outThread.start();
            errThread.start();
            int exitVal = proc.waitFor();
            if (exitVal != 0) {
            	logger.severe("MTEval terminates abnormally");
                throw new RuntimeException();
            }
			outThread.join();
			errThread.join();
			logger.info("MTEval ends");
        } catch (InterruptedException e) {}
	}
	
	private Scores readMTEvalOutput(File outputFile) throws IOException {
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(outputFile)));
		Scores scores = new Scores();
		String[] line;
		while ((line = in.readLine()) != null)
			if (line.length > 8 && line[0].equals("NIST") && line[4].equals("BLEU")) {
				scores.nist = Double.parseDouble(line[3]);  // NIST score
				scores.bleu = Double.parseDouble(line[7]);  // BLEU score
			}
		in.close();
		return scores;
	}
	
}

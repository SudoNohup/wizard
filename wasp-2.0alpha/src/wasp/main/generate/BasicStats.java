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
import java.util.Iterator;

import wasp.data.Example;
import wasp.data.Examples;
import wasp.main.Parse;
import wasp.math.Math;
import wasp.util.Arrays;

/**
 * This program collects basic statistics of the output of a tactical generator, including:
 * <ol>
 * <li>the average length of reference sentences (<code>nl-length</code>);
 * <li>the average length of input MRs, in terms of the number of tokens and the number of nodes in the
 * MR parse trees (<code>mrl-length</code> and <code>mrl-parse-size</code>);
 * <li>the average length of generated sentences (<code>gen-length</code>); <i>and</i>
 * <li>the coverage, i.e. the percentage of input MRs that have been translated into NL
 * (<code>coverage</code>).
 * </ol>
 * 
 * @author ywwong
 *
 */
public class BasicStats extends Evaluator {

	protected void evaluate(PrintWriter out, Examples gold, Examples[] examples) throws IOException {
		double[] nlLengths = new double[examples.length];
		double[] mrlLengths = new double[examples.length];
		double[] mrlParseSizes = new double[examples.length];
		double[] genLengths = new double[examples.length];
		double[] coverages = new double[examples.length];
		for (int i = 0; i < examples.length; ++i) {
			nlLengths[i] = nlLength(gold, examples[i]);
			mrlLengths[i] = mrlLength(gold, examples[i]);
			mrlParseSizes[i] = mrlParseSize(gold, examples[i]);
			genLengths[i] = genLength(examples[i]);
			coverages[i] = coverage(examples[i]);
		}
		out.println("begin nl-length");
		out.println("mean "+Math.mean(nlLengths));
		double[] interval = Math.confInterval95(nlLengths);
		out.println("95%-confidence-interval "+interval[0]+" "+interval[1]);
		out.println("end nl-length");
		out.println("begin mrl-length");
		out.println("mean "+Math.mean(mrlLengths));
		interval = Math.confInterval95(mrlLengths);
		out.println("95%-confidence-interval "+interval[0]+" "+interval[1]);
		out.println("end mrl-length");
		out.println("begin mrl-parse-size");
		out.println("mean "+Math.mean(mrlParseSizes));
		interval = Math.confInterval95(mrlParseSizes);
		out.println("95%-confidence-interval "+interval[0]+" "+interval[1]);
		out.println("end mrl-parse-size");
		out.println("begin gen-length");
		out.println("mean "+Math.mean(genLengths));
		interval = Math.confInterval95(genLengths);
		out.println("95%-confidence-interval "+interval[0]+" "+interval[1]);
		out.println("end gen-length");
		out.println("begin coverage");
		out.println("mean "+Math.mean(coverages));
		interval = Math.confInterval95(coverages);
		out.println("95%-confidence-interval "+interval[0]+" "+interval[1]);
		out.println("end coverage");
	}

	private static double nlLength(Examples gold, Examples examples) {
		int totalLength = 0;
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			totalLength += gold.get(ex.id).E().length-2;
		}
		return ((double) totalLength)/examples.size();
	}
	
	private static double mrlLength(Examples gold, Examples examples) {
		int totalLength = 0;
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			totalLength += gold.get(ex.id).F.syms.length;
		}
		return ((double) totalLength)/examples.size();
	}
	
	private static double mrlParseSize(Examples gold, Examples examples) {
		int totalSize = 0;
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			totalSize += gold.get(ex.id).F.linear.length;
		}
		return ((double) totalSize)/examples.size();
	}
	
	private static double genLength(Examples examples) {
		int totalLength = 0;
		int ngenerated = 0;
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			Parse[] parses = ex.getSortedParses();
			if (parses.length > 0) {
				String[] tokens = Arrays.tokenize(parses[0].toStr());
				totalLength += tokens.length;
				++ngenerated;
			}
		}
		return ((double) totalLength)/ngenerated;
	}

	private static double coverage(Examples examples) {
		int ngenerated = 0;
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			Parse[] parses = ex.getSortedParses();
			if (parses.length > 0)
				++ngenerated;
		}
		return ((double) ngenerated)/examples.size();
	}
}

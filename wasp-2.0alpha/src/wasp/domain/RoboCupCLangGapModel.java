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
package wasp.domain;

import java.io.IOException;
import java.io.PrintWriter;

import wasp.data.Terminal;
import wasp.math.Math;
import wasp.nl.BasicGapModel;
import wasp.util.Arrays;
import wasp.util.Double;
import wasp.util.TokenReader;

/**
 * A word-gap model for the RoboCup domain.
 * 
 * @author ywwong
 *
 */
public class RoboCupCLangGapModel extends BasicGapModel {

	private static final int NUM = 0;
	private static final int UNUM = 1;
	private static final int IDENT = 2;
	private static final int NUM_WORD_CLASSES = 3;
	
	/** The word-class-specific weights. */
	private double[] wordClassWeights;
	
	private double[] wordClassOuters;
	
	public RoboCupCLangGapModel() {
		wordClassWeights = new double[NUM_WORD_CLASSES];
		wordClassOuters = new double[NUM_WORD_CLASSES];
		Arrays.fill(wordClassOuters, Double.NEGATIVE_INFINITY);
	}
	
	public double getWeight(Terminal word) {
		double weight = super.getWeight(word);
		if (word.isNum())
			weight += wordClassWeights[NUM];
		if (word.isUnum())
			weight += wordClassWeights[UNUM];
		if (word.isIdent())
			weight += wordClassWeights[IDENT];
		return weight;
	}

	public int countParams() {
		return super.countParams()+NUM_WORD_CLASSES;
	}
	
	public double[] getWeightVector() {
		return Arrays.concat(super.getWeightVector(), wordClassWeights);
	}
	
	public void setWeightVector(double[] weights) {
		super.setWeightVector(Arrays.subarray(weights, 0, weights.length-NUM_WORD_CLASSES));
		wordClassWeights = Arrays.subarray(weights, weights.length-NUM_WORD_CLASSES, weights.length);
	}
	
	public double[] getOuterScores() {
		return Arrays.concat(super.getOuterScores(), wordClassOuters);
	}
	
	public void addOuterScores(Terminal word, double z) {
		super.addOuterScores(word, z);
		if (word.isNum())
			wordClassOuters[NUM] = Math.logAdd(wordClassOuters[NUM], z-wordClassWeights[NUM]);
		if (word.isUnum())
			wordClassOuters[UNUM] = Math.logAdd(wordClassOuters[UNUM], z-wordClassWeights[UNUM]);
		if (word.isIdent())
			wordClassOuters[IDENT] = Math.logAdd(wordClassOuters[IDENT], z-wordClassWeights[IDENT]);
	}
	
	public void resetOuterScores() {
		super.resetOuterScores();
		Arrays.fill(wordClassOuters, Double.NEGATIVE_INFINITY);
	}

	public void read() throws IOException {
		TokenReader in = getReader();
		readBasic(in);
		String[] line = in.readLine();  // begin word-class-weights
		wordClassWeights = new double[NUM_WORD_CLASSES];
		line = in.readLine();
		while (!(line[0].equals("end") && line[1].equals("word-class-weights"))) {
			if (line[0].equals("Num"))
				wordClassWeights[NUM] = Double.parseDouble(line[1]);
			else if (line[0].equals("Unum"))
				wordClassWeights[UNUM] = Double.parseDouble(line[1]);
			else if (line[0].equals("Ident"))
				wordClassWeights[IDENT] = Double.parseDouble(line[1]);
			line = in.readLine();
		}
		in.close();
	}
	
	public void write() throws IOException {
		PrintWriter out = getWriter();
		writeBasic(out);
		out.println("begin word-class-weights");
		if (wordClassWeights[NUM] != 0) {
			out.print("Num ");
			out.println(wordClassWeights[NUM]);
		}
		if (wordClassWeights[UNUM] != 0) {
			out.print("Unum ");
			out.println(wordClassWeights[UNUM]);
		}
		if (wordClassWeights[IDENT] != 0) {
			out.print("Ident ");
			out.println(wordClassWeights[IDENT]);
		}
		out.println("end word-class-weights");
		out.close();
	}

}

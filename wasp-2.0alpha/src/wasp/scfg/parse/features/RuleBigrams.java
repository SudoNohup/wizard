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
package wasp.scfg.parse.features;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import wasp.data.Terminal;
import wasp.main.Config;
import wasp.math.Math;
import wasp.scfg.PartialRule;
import wasp.scfg.SCFGModel;
import wasp.scfg.parse.Item;
import wasp.util.Double;
import wasp.util.FileWriter;
import wasp.util.Int;
import wasp.util.TokenReader;

/**
 * The <i>rule-bigrams</i> feature set (Collins & Koo, 2005).
 * 
 * @author ywwong
 *
 */
public class RuleBigrams extends ParseFeature {

	public RuleBigrams(SCFGModel model) {
		super(model);
	}

	public double weight(Terminal[] E, Item item, Item comp, Item next) {
		if (weights == null)
			return 0;
		return weights[item.lastRule*gram.countPartialRules() + comp.rule.partialRuleId];
	}
	
	///
	/// Parameter estimation
	///

	public int countParams() {
		return gram.countPartialRules()*gram.countPartialRules();
	}

	public void addOuterScore(Terminal[] E, Item item, Item comp, Item next, double inc) {
		int i = item.lastRule*gram.countPartialRules() + comp.rule.partialRuleId;
		outers[i] = Math.logAdd(outers[i], inc);
	}
	
	///
	/// File I/O
	///
	
	private static final String SCFG_RULE_BIGRAMS = "scfg-rule-bigrams";

	public void read() throws IOException {
		File file = new File(Config.getModelDir(), SCFG_RULE_BIGRAMS);
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(file)));
		String[] line;
		while ((line = in.readLine()) != null) {
			Int index = new Int(0);
			PartialRule rule1 = null;
			if (line[index.val].equals("null"))
				++index.val;
			else {
				rule1 = PartialRule.read(line, index);
				if (rule1 == null)
					throw new RuntimeException();
			}
			if (!line[index.val++].equals("//"))
				throw new RuntimeException();
			PartialRule rule2 = PartialRule.read(line, index);
			if (rule2 == null)
				throw new RuntimeException();
			if (index.val < line.length && line[index.val].equals("weight")) {
				int i1 = (rule1==null) ? 0 : gram.getPartialRuleId(rule1, false);
				int i2 = gram.getPartialRuleId(rule2, false);
				if (i1 < 0 || i2 < 0)
					continue;
				int nrules = gram.countPartialRules();
				if (weights == null)
					weights = new double[nrules*nrules];
				weights[i1*nrules+i2] = Double.parseDouble(line[index.val+1]);
				index.val += 2;
			}
			if (index.val < line.length)
				throw new RuntimeException();
		}
		in.close();
	}

	public void write() throws IOException {
		File file = new File(Config.getModelDir(), SCFG_RULE_BIGRAMS);
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(file)));
		if (weights != null) {
			int nrules = gram.countPartialRules();
			for (int i = 0; i < nrules; ++i) {
				PartialRule rule1 = (i==0) ? null : gram.getPartialRule(i);
				for (int j = 0; j < nrules; ++j) {
					double w = weights[i*nrules+j];
					if (w != 0) {
						PartialRule rule2 = gram.getPartialRule(j);
						out.print(rule1);
						out.print(" // ");
						out.print(rule2);
						out.print(" weight ");
						out.println(w);
					}
				}
			}
		}
		out.close();
	}
	
}

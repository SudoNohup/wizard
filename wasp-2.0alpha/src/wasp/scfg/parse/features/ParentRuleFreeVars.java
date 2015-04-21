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
import wasp.util.Short;
import wasp.util.TokenReader;

/**
 * The feature set based on the rule being completed and the number of free variables being added.
 * 
 * @author ywwong
 *
 */
public class ParentRuleFreeVars extends ParseFeature {

	private static final short NUM_BINS = ParseFeatures.NUM_BINS_FREE_VARS;
	
	public ParentRuleFreeVars(SCFGModel model) {
		super(model);
	}

	public double weight(Terminal[] E, Item item, Item comp, Item next) {
		if (weights == null)
			return 0;
		return weights[item.rule.partialRuleId*NUM_BINS + comp.nfvars];
	}

	///
	/// Parameter estimation
	///

	public int countParams() {
		return gram.countPartialRules()*NUM_BINS;
	}

	public void addOuterScore(Terminal[] E, Item item, Item comp, Item next, double inc) {
		int i = item.rule.partialRuleId*NUM_BINS + comp.nfvars;
		outers[i] = Math.logAdd(outers[i], inc);
	}
	
	///
	/// File I/O
	///
	
	private static final String SCFG_PARENT_RULE_FREE_VARS = "scfg-parent-rule-free-vars";

	public void read() throws IOException {
		File file = new File(Config.getModelDir(), SCFG_PARENT_RULE_FREE_VARS);
		TokenReader in = new TokenReader(new BufferedReader(new FileReader(file)));
		String[] line;
		while ((line = in.readLine()) != null) {
			Int index = new Int(0);
			PartialRule rule = PartialRule.read(line, index);
			if (rule == null)
				throw new RuntimeException();
			if (!line[index.val++].equals("//"))
				throw new RuntimeException();
			short nfvars = Short.parseShort(line[index.val++]);
			if (index.val < line.length && line[index.val].equals("weight")) {
				int id = gram.getPartialRuleId(rule, false);
				if (id < 0)
					continue;
				if (weights == null)
					weights = new double[gram.countPartialRules()*NUM_BINS];
				weights[id*NUM_BINS+nfvars] = Double.parseDouble(line[index.val+1]);
				index.val += 2;
			}
			if (index.val < line.length)
				throw new RuntimeException();
		}
		in.close();
	}

	public void write() throws IOException {
		File file = new File(Config.getModelDir(), SCFG_PARENT_RULE_FREE_VARS);
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(file)));
		if (weights != null)
			for (int i = 0; i < gram.countPartialRules(); ++i) {
				PartialRule rule = gram.getPartialRule(i);
				for (short j = 0; j < NUM_BINS; ++j) {
					double w = weights[i*NUM_BINS+j];
					if (w != 0) {
						out.print(rule);
						out.print(" // ");
						out.print(j);
						out.print(" weight ");
						out.println(w);
					}
				}
			}
		out.close();
	}
	
}
